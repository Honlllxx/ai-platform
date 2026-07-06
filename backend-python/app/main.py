from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import get_settings

settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    docs_url="/docs",
    redoc_url="/redoc",
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
