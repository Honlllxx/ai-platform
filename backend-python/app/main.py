import os
import logging
# Disable ONEDNN/MKLDNN to avoid PaddlePaddle 3.0 CPU PIR compatibility issues
os.environ["FLAGS_use_onednn"] = "0"

from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import get_settings

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

settings = get_settings()


# ── Lifespan: start/stop RocketMQ consumer ──────────────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Start RocketMQ consumer on startup, shut down on exit."""
    logger.info("Starting RocketMQ consumer...")
    try:
        from app.services.rocketmq_consumer import rocketmq_consumer
        rocketmq_consumer.start()
        logger.info("RocketMQ consumer started successfully")
    except Exception as e:
        logger.warning("RocketMQ consumer not started (MQ may be unavailable): %s", e)
    yield
    logger.info("Shutting down RocketMQ consumer...")
    try:
        from app.services.rocketmq_consumer import rocketmq_consumer
        rocketmq_consumer.stop()
    except Exception:
        pass


app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# CORS — allow frontend dev server
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://127.0.0.1:3000"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/api/health")
async def health_check():
    return {"code": 200, "data": {"status": "ok", "service": "python-ai"}, "message": "success"}


# Import and register routers
from app.routers import ai, prompt, finetune, models_router

app.include_router(ai.router, prefix="/api/ai", tags=["AI Processing"])
app.include_router(prompt.router, prefix="/api/prompt", tags=["Prompt Management"])
app.include_router(finetune.router, prefix="/api/finetune", tags=["Fine-tuning"])
app.include_router(models_router.router, prefix="/api/models", tags=["Model Assets"])
