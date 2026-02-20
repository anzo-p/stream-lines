FROM python:3.11-slim

# Prevent Python from writing .pyc files and enable clean logs
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

WORKDIR /app

COPY pyproject.toml ./

RUN pip install --no-cache-dir --upgrade pip \
  && pip install --no-cache-dir .

COPY src/ ./src/

ENV PYTHONPATH=/app/src

CMD ["uvicorn", "narwhal.main:app", "--host", "0.0.0.0", "--port", "8000"]
