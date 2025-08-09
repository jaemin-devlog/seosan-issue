from transformers import BartForConditionalGeneration, AutoTokenizer

# Load the BART model and tokenizer
model = BartForConditionalGeneration.from_pretrained("gogamza/kobart-summarization")
tokenizer = AutoTokenizer.from_pretrained("gogamza/kobart-summarization")

def summarize_text(content: str) -> str:
    """
    주어진 텍스트를 요약하는 함수.
    """
    input_text = content.strip().replace("\n", " ")
    inputs = tokenizer([input_text], max_length=1024, return_tensors="pt")
    summary_ids = model.generate(
        inputs["input_ids"],
        max_length=200,
        min_length=30,
        num_beams=2,
        length_penalty=2.0,
        early_stopping=True
    )
    summary = tokenizer.decode(summary_ids[0], skip_special_tokens=True)
    return summary