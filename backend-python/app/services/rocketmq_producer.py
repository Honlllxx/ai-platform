"""RocketMQ Producer — sends pipeline results back to Java.

Flow:
    Python Consumer (done) → this Producer → pipeline-result topic
      → Java PipelineResultConsumer → updates Document/Pipeline status
"""

import json
import logging
from typing import Optional

from app.config import get_settings

logger = logging.getLogger(__name__)


class RocketMQResultProducer:
    """Sends pipeline processing results to the pipeline-result topic."""

    def __init__(self):
        self.settings = get_settings()
        self._producer = None

    def _ensure_producer(self):
        """Lazy-init the producer (RocketMQ client is not thread-safe at init)."""
        if self._producer is not None:
            return
        from rocketmq.client import Producer
        producer = Producer(self.settings.rocketmq_producer_group)
        producer.set_name_server_address(self.settings.rocketmq_namesrv)
        producer.start()
        self._producer = producer
        logger.info("RocketMQ Producer started (group=%s, topic=%s)",
                     self.settings.rocketmq_producer_group,
                     self.settings.rocketmq_result_topic)

    def send_result(self, result: dict) -> bool:
        """Send a pipeline result message to the pipeline-result topic.

        Args:
            result: dict with keys:
                taskId, documentId, pipelineId, status, duration,
                extractionCount, errorMessage

        Returns:
            True if sent successfully, False otherwise.
        """
        try:
            self._ensure_producer()
            from rocketmq.client import Message

            body = json.dumps(result, ensure_ascii=False)
            msg = Message(self.settings.rocketmq_result_topic)
            msg.set_keys(result.get("taskId", ""))
            msg.set_tags("result")
            msg.set_body(body.encode("utf-8"))

            ret = self._producer.send_sync(msg)
            logger.info("Pipeline result sent: taskId=%s, status=%s, msgId=%s",
                        result.get("taskId"), result.get("status"), ret.msg_id)
            return True

        except Exception as e:
            logger.error("Failed to send pipeline result: taskId=%s, error=%s",
                         result.get("taskId"), e)
            # Reset producer on error so next call re-creates it
            self._shutdown_producer()
            return False

    def _shutdown_producer(self):
        """Shut down and clear the producer."""
        if self._producer is not None:
            try:
                self._producer.shutdown()
            except Exception:
                pass
            self._producer = None

    def shutdown(self):
        """Graceful shutdown."""
        self._shutdown_producer()
        logger.info("RocketMQ Producer shut down")


# ── Singleton ──
rocketmq_producer = RocketMQResultProducer()
