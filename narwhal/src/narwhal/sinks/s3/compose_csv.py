from narwhal.domain.schema.training_fields_base import TrainingFieldsBase

import csv, io, gzip
from typing import Iterable


def to_gzipped_csv(data: Iterable[TrainingFieldsBase]) -> bytes:
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode="wb") as gz:
        text = io.TextIOWrapper(gz, encoding="utf-8", newline="")
        writer = csv.writer(text)
        for e in data:
            writer.writerow(e.to_feature_set())
        text.flush()
    return buf.getvalue()
