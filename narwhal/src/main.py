import logging
import time
from typing import List

from src.features.create_dataset import fetch_and_compose
from src.features.schema import TrainingData
from src.filepush.compose_csv import to_gzipped_csv
from src.filepush.export_file import export_file
from src.sources.influxdb.client import get_historical_data, InfluxHandle, close_all_influx_clients


def run():
    logging.basicConfig(
        level=logging.INFO, format="%(asctime)s | %(levelname)s | %(name)s | %(message)s"
    )

    logger = logging.getLogger(__name__)
    logger.info(f"Reading training data")

    try:
        historical_data: InfluxHandle = get_historical_data()
        training_data: List[TrainingData] = fetch_and_compose(historical_data)
        close_all_influx_clients()

        content: bytes = to_gzipped_csv(training_data)
        export_file(content)

        logger.info(f"Traning data successfully composed and exported")
    except Exception as e:
        logger.error(f"Failed to export training data: {e}")
        return 1


if __name__ == "__main__":
    time.sleep(3 * 60)
    run()
