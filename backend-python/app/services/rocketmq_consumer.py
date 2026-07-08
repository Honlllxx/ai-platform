"""RocketMQ Consumer — listens on pipeline-task topic and runs AI pipeline.

Replaces the Java-side HTTP bridge: instead of Java consuming its own
RocketMQ message and calling Python via REST, Python now consumes
directly from RocketMQ, downloads files from MinIO, and runs the
LangGraph pipeline.

Flow:
    Java Producer → RocketMQ (pipeline-task) → Python Consumer
      → download file (MinIO) → LangGraph pipeline → DB persistence
      → Python Producer → RocketMQ (pipeline-result) → Java Consumer
"""

import json
import logging
import threading
import time
from io import BytesIO
from typing import Optional

import cv2
import numpy as np
from PIL import Image
from minio import Minio
from sqlalchemy.ext.asyncio import AsyncSession

from app.config import get_settings
from app.database import AsyncSessionLocal
from app.services.graph_service import graph_service
from app.services.rocketmq_producer import rocketmq_producer

logger = logging.getLogger(__name__)


class RocketMQPipelineConsumer:
    """Consumes pipeline-task messages from RocketMQ and executes AI pipelines."""

    def __init__(self):
        self.settings = get_settings()
        self._consumer = None
        self._running = False
        self._thread: Optional[threading.Thread] = None
        self._minio_client: Optional[Minio] = None

    # ── MinIO client ──────────────────────────────────────────────

    @property
    def minio_client(self) -> Minio:
        if self._minio_client is None:
            self._minio_client = Minio(
                self.settings.minio_endpoint,
                access_key=self.settings.minio_access_key,
                secret_key=self.settings.minio_secret_key,
                secure=False,
            )
        return self._minio_client

    # ── Start / Stop ──────────────────────────────────────────────

    def start(self):
        """Start the RocketMQ consumer in a background daemon thread."""
        if self._running:
            logger.warning("RocketMQ consumer is already running")
            return

        self._running = True
        self._thread = threading.Thread(
            target=self._consume_loop,
            name="rocketmq-consumer",
            daemon=True,
        )
        self._thread.start()
        logger.info("RocketMQ consumer started (topic=%s, group=%s)",
                     self.settings.rocketmq_consumer_topic,
                     self.settings.rocketmq_consumer_group)

    def stop(self):
        """Shut down the consumer gracefully."""
        self._running = False
        if self._consumer is not None:
            try:
                self._consumer.shutdown()
            except Exception as e:
                logger.warning("Error shutting down RocketMQ consumer: %s", e)
        logger.info("RocketMQ consumer stopped")

    # ── Message handling ──────────────────────────────────────────

    def _consume_loop(self):
        """Main consume loop with automatic reconnect."""
        platform_errors = 0
        while self._running:
            try:
                self._run_consumer()
            except Exception as e:
                err_msg = str(e).lower()
                # Don't retry on platform issues (Windows, missing native libs, etc.)
                if any(kw in err_msg for kw in ("windows", "not support", "unsupported", "cannot", "dll", ".so", "librocketmq")):
                    platform_errors += 1
                    if platform_errors <= 1:
                        logger.warning(
                            "RocketMQ consumer unavailable on this platform (%s). "
                            "AI pipeline will work via HTTP API only.", e
                        )
                    self._running = False
                    break
                logger.error("RocketMQ consumer error (retrying in 10s): %s", e)
                if self._running:
                    time.sleep(10)

    def _run_consumer(self):
        """Set up PushConsumer and block until stopped or error."""
        from rocketmq.client import PushConsumer, ConsumeStatus

        consumer = PushConsumer(self.settings.rocketmq_consumer_group)
        consumer.set_name_server_address(self.settings.rocketmq_namesrv)
        consumer.subscribe(
            self.settings.rocketmq_consumer_topic,
            self._handle_message,
        )
        self._consumer = consumer
        logger.info("RocketMQ PushConsumer initialized, starting message loop...")
        consumer.start()

        # Block while running; consumer.start() is blocking
        while self._running:
            time.sleep(1)

        consumer.shutdown()

    def _handle_message(self, msg):
        """Callback invoked by RocketMQ PushConsumer for each message."""
        from rocketmq.client import ConsumeStatus

        task_id = "unknown"
        document_id = None
        try:
            body = msg.body.decode("utf-8") if isinstance(msg.body, bytes) else str(msg.body)
            task = json.loads(body)
            task_id = task.get("taskId", msg.get_property("taskId") or "unknown")
            document_id = task.get("documentId")

            logger.info("Received pipeline task: taskId=%s, documentId=%s, file=%s",
                        task_id, document_id, task.get("fileName", "?"))

            # Process the task
            success = self._process_task(task)

            if success:
                logger.info("Pipeline task completed: taskId=%s", task_id)
            else:
                logger.error("Pipeline task failed: taskId=%s", task_id)

        except json.JSONDecodeError as e:
            logger.error("Invalid message body (not JSON): %s", e)
        except Exception as e:
            logger.error("Unhandled error processing message: taskId=%s, error=%s",
                         task_id, e, exc_info=True)

        # Always return success to avoid infinite redelivery
        return ConsumeStatus.CONSUME_SUCCESS

    # ── Task processing ───────────────────────────────────────────

    def _process_task(self, task: dict) -> bool:
        """Download file from MinIO, run LangGraph pipeline, send result."""
        import asyncio

        document_id = task.get("documentId")
        task_id = task.get("taskId", "")
        file_url = task.get("fileUrl", "")
        metadata_fields = task.get("metadataFields") or []
        pipeline_id = task.get("pipelineId")

        try:
            # 1. Download file from MinIO
            image = self._download_image(file_url)
            if image is None:
                self._send_failure(task_id, document_id, pipeline_id,
                                   "Failed to download file from MinIO")
                return False

            # 2. Run LangGraph pipeline (async → sync via asyncio)
            result = asyncio.run(self._run_pipeline(
                image=image,
                metadata_fields=metadata_fields,
                document_id=document_id,
                task_id=task_id,
            ))

            # 3. Send result back to Java via RocketMQ
            self._send_result(task_id, document_id, pipeline_id, result)
            return True

        except Exception as e:
            logger.error("Pipeline processing failed: %s", e, exc_info=True)
            self._send_failure(task_id, document_id, pipeline_id, str(e))
            return False

    async def _run_pipeline(
        self,
        image: np.ndarray,
        metadata_fields: list,
        document_id: int,
        task_id: str,
    ) -> dict:
        """Execute the LangGraph pipeline with a DB session."""
        async with AsyncSessionLocal() as db:
            result = await graph_service.run(
                image=image,
                metadata_fields=metadata_fields,
                document_id=document_id,
                db=db,
                task_id=task_id,
            )
            return result

    # ── MinIO file download ───────────────────────────────────────

    def _download_image(self, file_url: str) -> Optional[np.ndarray]:
        """Download a document image from MinIO and decode to numpy array."""
        if not file_url:
            logger.error("Empty file_url")
            return None

        # file_url format: /{bucket}/documents/{date}/{uuid}.ext
        # → object_name: documents/{date}/{uuid}.ext
        bucket = self.settings.minio_bucket
        object_name = file_url.lstrip("/")
        # Remove bucket prefix if present (e.g. "/documents/..." → "documents/...")
        parts = object_name.split("/", 1)
        if parts[0] == bucket:
            object_name = parts[1] if len(parts) > 1 else object_name
        elif len(parts) > 1:
            # file_url = "/documents/2026/07/uuid.pdf", bucket = "documents"
            # → object_name = "2026/07/uuid.pdf"
            object_name = parts[1]

        try:
            response = self.minio_client.get_object(bucket, object_name)
            file_bytes = response.read()
            response.close()
            response.release_conn()

            # Determine file type and decode
            if object_name.lower().endswith(".pdf"):
                # PDF → read first page as image via Pillow + pdf2image fallback
                # For now, try as image directly (supports image-based PDFs)
                try:
                    pil_image = Image.open(BytesIO(file_bytes)).convert("RGB")
                    return cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)
                except Exception:
                    logger.warning(
                        "Cannot read PDF directly; install pdf2image for PDF support. "
                        "file=%s", object_name
                    )
                    return None
            else:
                pil_image = Image.open(BytesIO(file_bytes)).convert("RGB")
                return cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)

        except Exception as e:
            logger.error("Failed to download/decode image from MinIO: %s/%s → %s",
                         bucket, object_name, e)
            return None

    # ── Result reporting ──────────────────────────────────────────

    def _send_result(self, task_id: str, document_id: int,
                     pipeline_id: Optional[int], result: dict):
        """Send successful pipeline result to pipeline-result topic."""
        try:
            rocketmq_producer.send_result({
                "taskId": task_id,
                "documentId": document_id,
                "pipelineId": pipeline_id or result.get("pipeline_id"),
                "status": result.get("status", "completed"),
                "duration": result.get("duration", 0),
                "extractionCount": len(result.get("extraction", [])),
                "errorMessage": None,
            })
        except Exception as e:
            logger.error("Failed to send pipeline result: %s", e)

    def _send_failure(self, task_id: str, document_id: int,
                      pipeline_id: Optional[int], error_message: str):
        """Send failure result to pipeline-result topic."""
        try:
            rocketmq_producer.send_result({
                "taskId": task_id,
                "documentId": document_id,
                "pipelineId": pipeline_id,
                "status": "failed",
                "duration": 0,
                "extractionCount": 0,
                "errorMessage": error_message,
            })
        except Exception as e:
            logger.error("Failed to send failure result: %s", e)


# ── Singleton ──
rocketmq_consumer = RocketMQPipelineConsumer()
