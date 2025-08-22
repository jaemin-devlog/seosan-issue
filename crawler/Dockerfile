# crawler-Dockerfile
FROM python:3.11-slim

WORKDIR /app
ENV PYTHONPATH=/app \
    PIP_NO_CACHE_DIR=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# 시스템 패키지 (lxml 등 네이티브 의존 대비)
RUN apt-get update && apt-get install -y --no-install-recommends \
      curl ca-certificates \
      libxml2 libxslt1.1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
# 빌드 타임아웃 여유 + 기본 인덱스만 사용
RUN pip install --timeout=600 -r requirements.txt

# 앱 소스 복사
COPY . .

# 런타임 환경 변수 (요약 모델/스레드 안정화)
ENV TOKENIZERS_PARALLELISM=false \
    OMP_NUM_THREADS=1 \
    MKL_NUM_THREADS=1

# Render/로컬 모두 $PORT 지원 (기본 5001)
EXPOSE 5001

# Gunicorn 실행: gthread 워커, 타임아웃/재시작 전략, tmp-dir
CMD ["sh","-c","gunicorn --workers 2 --timeout 300 --bind 0.0.0.0:${PORT:-5001} api:app"]
