import logging
import os
from typing import Protocol, Type

from narwhal.services.predict import PredictionService
from narwhal.services.train import TrainingService

logger = logging.getLogger(__name__)


class Runnable(Protocol):
    def run(self) -> None: ...


JOB_MAP: dict[str, Type[Runnable]] = {
    "weekdaily_training_job": TrainingService,
    "weekdaily_intraday_prediction_job": PredictionService,
}


def main() -> None:
    logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))

    job_name = os.getenv("SCHEDULED_JOB_NAME")
    if not job_name:
        logger.error("Missing env var SCHEDULED_JOB_NAME")
        raise SystemExit(1)

    service_cls = JOB_MAP.get(job_name)
    if not service_cls:
        logger.error("Unknown value for SCHEDULED_JOB_NAME=%s", job_name)
        raise SystemExit(2)

    logger.info("Starting job=%s service=%s", job_name, service_cls.__name__)

    try:
        service_cls().run()
    except Exception:
        logger.exception("Job failed: %s", job_name)
        raise  # error to ECS
    else:
        logger.info("Job succeeded: %s", job_name)
        return  # tells ECS to teardown


if __name__ == "__main__":
    main()
