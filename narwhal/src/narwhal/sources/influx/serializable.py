from typing import Protocol


class Serializable(Protocol):
    def to_line_protocol(self) -> str: ...
