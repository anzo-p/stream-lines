def check_schema(entity: str, expected: set[str], actual: set[str]) -> None:
    if actual != expected:
        missing = sorted(expected - actual)
        extra = sorted(actual - expected)
        raise TypeError(f"{entity} schema mismatch: missing={missing}, extra={extra}")
