from dataclasses import dataclass
from typing import Type

from narwhal.scheduler.runnable import Runnable


@dataclass(frozen=True)
class JobConfig:
    job_id: str
    service: Type[Runnable]
