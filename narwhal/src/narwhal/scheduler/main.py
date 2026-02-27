import logging
import signal
from functools import partial
from typing import Type

from apscheduler.schedulers.blocking import BlockingScheduler
from apscheduler.triggers.cron import CronTrigger

from narwhal.scheduler.runnable import Runnable
from narwhal.scheduler.schedule import SCHEDULE

logger = logging.getLogger(__name__)


def _handle_sigterm(scheduler: BlockingScheduler) -> None:
    logger.info("Scheduler: received SIGTERM, shutting down")
    scheduler.shutdown(wait=False)


def _run_service(service_cls: Type[Runnable]) -> None:
    name = getattr(service_cls, "__name__", str(service_cls))
    logger.info("Scheduler: starting %s job", name)
    try:
        service_cls().run()
        logger.info("Scheduler: %s job finished", name)
    except Exception:
        logger.exception("Scheduler: %s job failed", name)


def main() -> None:
    logging.basicConfig(level=logging.INFO)

    scheduler = BlockingScheduler(timezone="UTC")

    signal.signal(
        signalnum=signal.SIGTERM,
        handler=lambda signum, frame: _handle_sigterm(scheduler),
    )

    logger.info("Adding scheduler jobs")
    for hour_expr, job_config in SCHEDULE.items():
        trigger = CronTrigger(
            day_of_week="mon-fri",
            hour=hour_expr,
            minute=0,
            timezone="UTC",
        )

        scheduler.add_job(
            func=partial(_run_service, job_config.service),
            trigger=trigger,
            id=job_config.job_id,
            replace_existing=True,
        )

        logger.info(
            "Scheduled job '%s' for service %s",
            job_config.job_id,
            getattr(job_config.service, "__name__", job_config.job_id),
        )

    logger.info("Scheduler: starting")
    try:
        scheduler.start()
    finally:
        logger.info("Scheduler stopped")


if __name__ == "__main__":
    main()
