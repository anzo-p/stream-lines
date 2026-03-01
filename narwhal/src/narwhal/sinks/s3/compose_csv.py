from narwhal.domain.schema.training_data_base import TrainingDataBase

import csv, io, gzip
from typing import Iterable


def to_gzipped_csv(rows: Iterable[TrainingDataBase]) -> bytes:
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode="wb") as gz:
        text = io.TextIOWrapper(gz, encoding="utf-8", newline="")
        writer = csv.writer(text)
        for r in rows:
            writer.writerow([getattr(r, c) for c in r.TRAINING_FIELDS])
        text.flush()
    return buf.getvalue()
