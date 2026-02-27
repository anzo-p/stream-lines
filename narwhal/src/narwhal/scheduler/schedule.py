from narwhal.scheduler.job_config import JobConfig
from narwhal.services.predict import PredictionService
from narwhal.services.train import TrainingService


SCHEDULE: dict[str, JobConfig] = {
    "15": JobConfig(job_id="weekdaily_training_job", service=TrainingService),
    "14-22/2": JobConfig(job_id="weekdaily_intraday_prediction_job", service=PredictionService),
}
