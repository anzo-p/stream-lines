import logging
from typing import Optional

from fastapi import APIRouter, BackgroundTasks, Depends, HTTPException, Query
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
    program: Optional[str] = Query(None, alias="program"),
    model: Optional[str] = Query(None, alias="model-id"),
) -> None:
    if bool(program) != bool(model):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Parameters 'program' and 'model-id' must either both be provided or both be null.",
        )

    background_tasks.add_task(svc.run, program, model)
