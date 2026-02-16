import csv
import gzip
import io

from typing import Iterable

from src.features.create_dataset import TrainingData


def to_gzipped_csv(rows: Iterable[TrainingData]) -> bytes:
    buf = io.BytesIO()

    with gzip.GzipFile(fileobj=buf, mode="wb") as gz:
        text = io.TextIOWrapper(gz, encoding="utf-8", newline="")
        writer = csv.writer(text)

        for r in rows:
            writer.writerow(
                [
                    r.fwd_max_drawdown,  # label first
                    r.members_daily_spread,
                    r.index_over_moving_avg,
                    r.index_over_kaufman_avg,
                    r.volume_over_moving_avg,
                    r.current_drawdown,
                    r.days_since_dip,
                ]
            )

        text.flush()

    return buf.getvalue()
