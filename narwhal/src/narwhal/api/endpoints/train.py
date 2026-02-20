import logging
from fastapi import APIRouter, Depends, BackgroundTasks
from starlette import status

from narwhal.services.train import TrainingService

logger = logging.getLogger(__name__)
router = APIRouter()


def get_training_service() -> TrainingService:
    return TrainingService()


@router.post("/train", status_code=status.HTTP_202_ACCEPTED)
def train(
    background_tasks: BackgroundTasks,
    svc: TrainingService = Depends(get_training_service),
) -> None:
    background_tasks.add_task(svc.run)
