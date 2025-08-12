FROM python:3.11-slim

# Set environment variables for Hugging Face and PyTorch
ENV HF_HOME=/models \
    TRANSFORMERS_OFFLINE=0 \
    TOKENIZERS_PARALLELISM=false \
    TORCH_NUM_THREADS=2

WORKDIR /app
ENV PYTHONPATH=/app

RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --timeout=600 -r requirements.txt

# Pre-fetch the model and tokenizer during the build
# This will be cached by Docker unless requirements.txt changes.
RUN python - <<'PY'
from transformers import AutoTokenizer, BartForConditionalGeneration
print("Downloading and caching model and tokenizer...")
model = BartForConditionalGeneration.from_pretrained("gogamza/kobart-summarization")
tokenizer = AutoTokenizer.from_pretrained("gogamza/kobart-summarization")
print("Pre-fetching complete.")
PY

# Set TRANSFORMERS_OFFLINE to 1 for the runtime stage
# to prevent any accidental downloads
ENV TRANSFORMERS_OFFLINE=1

COPY . .

EXPOSE 5001
# Use gthread worker for concurrent requests, keep timeout reasonable
CMD ["sh","-c","gunicorn --workers 1 --worker-class gthread --threads 8 --timeout 120 --bind 0.0.0.0:${PORT:-5001} --access-logfile - api:app"]
