import logging

from fastapi import APIRouter, Depends, Query, BackgroundTasks
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
    model: str = Query(..., alias="model-id"),
) -> None:
    background_tasks.add_task(svc.run, model)
