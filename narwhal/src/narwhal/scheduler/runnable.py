from typing import Protocol


class Runnable(Protocol):
    def run(self) -> None: ...
