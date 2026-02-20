from fastapi import APIRouter

from narwhal.api.endpoints import health, predict, train

api_router = APIRouter()
api_router.include_router(health.router, tags=["health"])
api_router.include_router(predict.router, tags=["predict"])
api_router.include_router(train.router, tags=["train"])
