# 1단계: Python 베이스 이미지 선택
FROM python:3.11-slim

# 작업 디렉토리 설정
WORKDIR /app
ENV PYTHONPATH=/app

# curl 설치
RUN apt-get update && apt-get install -y curl

# requirements.txt 파일을 먼저 복사하여 캐시 활용
COPY requirements.txt .

# 필요한 라이브러리 설치 (타임아웃 시간을 600초로 늘림)
RUN pip install --no-cache-dir --timeout=600 -r requirements.txt

# 나머지 소스 코드 복사
COPY . .

# API 서버가 사용하는 포트 노출
EXPOSE 5001

# API 서버 실행
CMD ["gunicorn", "--workers", "4", "--timeout", "300", "--bind", "0.0.0.0:5001", "api:app"]
