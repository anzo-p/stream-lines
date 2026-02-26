import logging
from typing import Optional

from fastapi import APIRouter, BackgroundTasks, Depends, Query
from starlette import status

from narwhal.services.predict import PredictionService

logger = logging.getLogger(__name__)
router = APIRouter()


def get_prediction_service() -> PredictionService:
    return PredictionService()


@router.post("/predict", status_code=status.HTTP_202_ACCEPTED)
def predict(
    background_tasks: BackgroundTasks,
    svc: PredictionService = Depends(get_prediction_service),
    model: Optional[str] = Query(None, alias="model-id"),
) -> None:
    background_tasks.add_task(svc.run, model)
