# crawler-Dockerfile

FROM python:3.11-slim

WORKDIR /app
ENV PYTHONPATH=/app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --timeout=600 -r requirements.txt

# context가 crawler/이므로 그냥 . 복사
COPY . .

# Render/로컬 모두 $PORT 지원 (기본 5001)
EXPOSE 5001
CMD ["sh","-c","gunicorn --workers 4 --timeout 300 --bind 0.0.0.0:${PORT:-5001} api:app"]