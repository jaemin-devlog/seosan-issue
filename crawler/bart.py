from transformers import BartForConditionalGeneration, AutoTokenizer
import torch, re

model_name = "gogamza/kobart-summarization"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = BartForConditionalGeneration.from_pretrained(model_name)
model.eval()

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model.to(device)

def _preclean(text: str) -> str:
    if not text:
        return ""
    # 줄바꿈/공백 정리
    t = re.sub(r"\s+", " ", text.strip())
    # 너무 긴 입력은 잘라서 요약 품질 유지 (토큰 기준 1024와 대응)
    return t[:4000]  # 대략 문자 기준 컷—필요시 조절

def summarize_text(content: str) -> str:
    text = _preclean(content)
    if not text:
        return ""

    inputs = tokenizer([text], max_length=1024, truncation=True, return_tensors="pt").to(device)

    with torch.inference_mode():
        summary_ids = model.generate(
            inputs["input_ids"],
            max_length=140,
            min_length=30,
            do_sample=True,        # 샘플링 사용
            top_p=0.92,            # 누클리어스
            top_k=50,
            temperature=0.9,
            no_repeat_ngram_size=4,
            repetition_penalty=1.3,
            early_stopping=True
    )

    summary = tokenizer.decode(summary_ids[0], skip_special_tokens=True)

    # 후처리: 공백/반복 토큰 정리(과하면 끄세요)
    summary = re.sub(r"\s+", " ", summary).strip()
    summary = re.sub(r"([가-힣A-Za-z0-9]{1,3})(\s+\1){2,}", r"\1", summary)  # 같은 토큰 3회↑ 연속 축약
    return summary
