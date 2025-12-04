# ===========================
#     summarize_articles.py
# ===========================

import os
import json
import time
import traceback
from datetime import datetime

import torch
from transformers import PreTrainedTokenizerFast, BartForConditionalGeneration


# -------------------------------------------------------------
# 모델 로딩
# -------------------------------------------------------------
print("📌 Loading KoBART Summarization Model...")

tokenizer = PreTrainedTokenizerFast.from_pretrained("gogamza/kobart-summarization")
model = BartForConditionalGeneration.from_pretrained("gogamza/kobart-summarization")

device = "cuda" if torch.cuda.is_available() else "cpu"
model = model.to(device)

print(f"🚀 Model Loaded on: {device}")


# -------------------------------------------------------------
# 에러 로그 저장
# -------------------------------------------------------------
def log_error(msg):
    with open("summary_error.log", "a", encoding="utf-8") as f:
        f.write(f"[{datetime.now()}] {msg}\n")
        f.write(traceback.format_exc() + "\n\n")


# -------------------------------------------------------------
# 요약 함수
# -------------------------------------------------------------
def summarize_text(text: str) -> str:
    try:
        if not text or len(text.strip()) < 20:
            return ""

        text = text.replace("\n", " ")
        text = " ".join(text.split())
        text = text[:3000]

        inputs = tokenizer(
            text,
            return_tensors="pt",
            max_length=1024,
            truncation=True
        ).to(device)

        summary_ids = model.generate(
            inputs["input_ids"],
            max_length=120,
            min_length=30,
            num_beams=4,
            early_stopping=True
        )

        result = tokenizer.decode(summary_ids[0], skip_special_tokens=True)
        return result.strip()

    except Exception as e:
        log_error(f"요약 실패: {e}")
        return ""


# -------------------------------------------------------------
# JSON 하나 요약
# -------------------------------------------------------------
def summarize_file(json_path: str, out_dir="summaries"):
    print(f"\n📄 처리 파일: {json_path}")

    if not os.path.exists(json_path):
        print("❌ 파일 없음:", json_path)
        return

    os.makedirs(out_dir, exist_ok=True)

    base = os.path.basename(json_path)
    out_path = os.path.join(out_dir, base.replace(".json", "_summary.json"))

    # 이미 요약된 파일이면 스킵
    if os.path.exists(out_path):
        print(f"⏩ 이미 요약된 파일 존재 → 스킵: {out_path}")
        return

    try:
        with open(json_path, "r", encoding="utf-8") as f:
            articles = json.load(f)
    except:
        log_error(f"JSON 로딩 실패: {json_path}")
        return

    # 요약 처리
    for i, art in enumerate(articles):
        if art.get("summary_ai"):
            print(f"⏩ {i+1}/{len(articles)} 이미 요약됨 → 스킵")
            continue

        print(f"  ▶ {i+1}/{len(articles)} 요약 중...")

        text_to_summarize = (
            f"{art.get('title','')}\n"
            f"{art.get('summary','')}\n"
            f"{art.get('content','')}"
        )

        art["summary_ai"] = summarize_text(text_to_summarize)
        time.sleep(0.2)

    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(articles, f, ensure_ascii=False, indent=4)

    print(f"✨ 저장 완료 → {out_path}")


# -------------------------------------------------------------
# 실행 (오늘 날짜 기준 파일만 요약)
# -------------------------------------------------------------
if __name__ == "__main__":

    # 🔥 오늘 날짜 기준
    today_str = datetime.now().strftime("%Y%m%d")

    print(f"\n📌 오늘 날짜({today_str}) 기준 요약할 파일 선택 중...\n")

    # 🔥 파일 필터링: 오늘 날짜 + .json
    targets = [
        f for f in os.listdir(".")
        if f.endswith(".json") and today_str in f
    ]

    if not targets:
        print("❌ 오늘 날짜에 해당하는 JSON 파일이 없습니다.")
        exit()

    print("📌 요약 대상 JSON 파일:")
    for t in targets:
        print(" -", t)

    for file in targets:
        summarize_file(file)
