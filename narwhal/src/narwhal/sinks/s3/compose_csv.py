import csv
import gzip
import io

from typing import Iterable

from narwhal.domain.training_data import TrainingData


def to_single_line_csv(row: TrainingData) -> bytes:
    buf = io.BytesIO()
    text = io.TextIOWrapper(buf, encoding="utf-8", newline="")
    writer = csv.writer(text)
    writer.writerow(
        [
            row.fwd_max_drawdown,
            row.members_daily_spread,
            row.index_over_moving_avg,
            row.index_over_kaufman_avg,
            row.volume_over_moving_avg,
            row.current_drawdown,
            row.days_since_dip,
            row.vix,
        ]
    )
    text.flush()
    return buf.getvalue()


def to_gzipped_csv(rows: Iterable[TrainingData]) -> bytes:
    buf = io.BytesIO()

    with gzip.GzipFile(fileobj=buf, mode="wb") as gz:
        text = io.TextIOWrapper(gz, encoding="utf-8", newline="")
        writer = csv.writer(text)

        for r in rows:
            writer.writerow(
                [
                    r.fwd_max_drawdown,
                    r.members_daily_spread,
                    r.index_over_moving_avg,
                    r.index_over_kaufman_avg,
                    r.volume_over_moving_avg,
                    r.current_drawdown,
                    r.days_since_dip,
                    r.vix,
                ]
            )

        text.flush()

    return buf.getvalue()
