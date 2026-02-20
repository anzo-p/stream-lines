from fastapi import FastAPI

from narwhal.api.router import api_router
from narwhal.config import setup_logging


def create_app() -> FastAPI:
    setup_logging()

    app = FastAPI(title="Training & Prediction API")
    app.include_router(api_router)
    return app


app = create_app()
