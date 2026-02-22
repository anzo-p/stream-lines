FROM python:3.11-slim

# Prevent Python from writing .pyc files and enable clean logs
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

# safely curl service in private subnet
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

COPY pyproject.toml ./

RUN pip install --no-cache-dir --upgrade pip \
  && pip install --no-cache-dir .

COPY src/ ./src/

ENV PYTHONPATH=/app/src

CMD ["uvicorn", "narwhal.main:app", "--host", "0.0.0.0", "--port", "8000"]
