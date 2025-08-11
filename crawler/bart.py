import os
import torch
from transformers import AutoTokenizer, BartForConditionalGeneration

_MODEL_ID = os.getenv("KOBART_MODEL_ID", "gogamza/kobart-summarization")
_tokenizer = None
_model = None
MODEL_READY = False

def load_model():
    """
    Loads the model and tokenizer from pre-fetched files.
    This function is intended to be called in a background thread.
    """
    global _tokenizer, _model, MODEL_READY
    if MODEL_READY:
        return
    
    print("요약 모델을 로딩합니다... (백그라운드 워밍업)")
    try:
        # Set thread count for PyTorch
        torch.set_num_threads(int(os.getenv("TORCH_NUM_THREADS", "2")))
        
        # local_files_only=True forces it to use files from HF_HOME cache
        _tokenizer = AutoTokenizer.from_pretrained(_MODEL_ID, local_files_only=True)
        _model = BartForConditionalGeneration.from_pretrained(_MODEL_ID, local_files_only=True)
        _model.eval() # Set model to evaluation mode
        
        MODEL_READY = True
        print("요약 모델 로딩 완료.")
    except Exception as e:
        print(f"모델 로딩 중 오류 발생: {e}")


@torch.no_grad()
def summarize_text(content: str) -> str:
    """
    Summarizes the given text. If the model is not ready,
    it will return an informative message.
    """
    if not MODEL_READY:
        print("모델이 아직 준비되지 않았습니다. 요약을 건너뜁니다.")
        return "요약 모델이 준비 중입니다. 잠시 후 다시 시도해주세요."

    inputs = _tokenizer([content.strip().replace("\n", " ")], return_tensors="pt", truncation=True, max_length=1024)
    summary_ids = _model.generate(
        inputs["input_ids"],
        max_length=200,
        min_length=30,
        num_beams=4,
        early_stopping=True,
        no_repeat_ngram_size=3
    )
    summary = _tokenizer.decode(summary_ids[0], skip_special_tokens=True)
    return summary