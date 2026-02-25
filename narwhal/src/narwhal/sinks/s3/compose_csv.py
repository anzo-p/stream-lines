from narwhal.domain.schema.training_data import TrainingData


import csv, io, gzip
from typing import Iterable

CSV_COLUMNS = (*TrainingData.TRAINING_FIELDS,)


def to_gzipped_csv(rows: Iterable[TrainingData]) -> bytes:
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode="wb") as gz:
        text = io.TextIOWrapper(gz, encoding="utf-8", newline="")
        writer = csv.writer(text)
        for r in rows:
            writer.writerow([getattr(r, c) for c in CSV_COLUMNS])
        text.flush()
    return buf.getvalue()
