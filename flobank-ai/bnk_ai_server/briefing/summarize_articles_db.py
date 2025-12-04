# ===============================================
#   summarize_articles_db.py  
#   TB_ARTICLE에 SUMMARY_AI 생성/업데이트
# ===============================================

import traceback
import time
from datetime import datetime

import torch
from transformers import PreTrainedTokenizerFast, BartForConditionalGeneration

from db import get_conn   # 🔥 DB connection (Thin mode)

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
# 🔥 [표] 기사 판별 함수
# -------------------------------------------------------------
def is_table_article(title: str) -> bool:
    return "[표]" in title


# -------------------------------------------------------------
# 로그 저장
# -------------------------------------------------------------
def log_error(msg):
    with open("summary_error.log", "a", encoding="utf-8") as f:
        f.write(f"[{datetime.now()}] {msg}\n")
        f.write(traceback.format_exc() + "\n\n")


# -------------------------------------------------------------
# 요약 함수 (KoBART)
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
# DB에서 SUMMARY_AI가 NULL인 기사 불러오기 (CLOB → str 변환)
# -------------------------------------------------------------
def fetch_articles_without_ai():
    conn = get_conn()
    cur = conn.cursor()

    sql = """
        SELECT ARTICLE_ID, TITLE, SUMMARY, CONTENT
          FROM TB_ARTICLE
         WHERE SUMMARY_AI IS NULL
         ORDER BY WRITTEN_AT DESC
    """

    cur.execute(sql)

    rows = []
    for article_id, title, summary, content in cur:

        # 🔥 CLOB → 문자열 변환
        if hasattr(summary, "read"):
            summary = summary.read()

        if hasattr(content, "read"):
            content = content.read()

        rows.append((article_id, title, summary, content))

    cur.close()
    conn.close()

    return rows


# -------------------------------------------------------------
# SUMMARY_AI 업데이트
# -------------------------------------------------------------
def update_summary_ai(article_id, text):
    conn = get_conn()
    cur = conn.cursor()

    sql = """
        UPDATE TB_ARTICLE
           SET SUMMARY_AI = :summary_ai,
               UPDATED_AT = CURRENT_TIMESTAMP
         WHERE ARTICLE_ID = :article_id
    """

    cur.execute(sql, {"summary_ai": text, "article_id": article_id})
    conn.commit()

    cur.close()
    conn.close()


# -------------------------------------------------------------
# 전체 실행 파이프라인
# -------------------------------------------------------------
def process_summary_ai():
    rows = fetch_articles_without_ai()

    print(f"\n📌 SUMMARY_AI 생성 필요 기사: {len(rows)}개")

    if not rows:
        print("⏩ 새 요약 작업 없음.")
        return

    for article_id, title, summary, content in rows:
        print(f"\n📝 요약 중: [{article_id}] {title}")

        # ---------------------------------------------------------
        # 🔥 [표] 필터링: 표 기반 기사는 요약 건너뛰고 고정 문구 저장
        # ---------------------------------------------------------
        if is_table_article(title):
            fixed_summary = "표/형식 기반 기사입니다."
            update_summary_ai(article_id, fixed_summary)
            print(f"⏩ [표] 기사 처리 완료 (ARTICLE_ID={article_id})")
            continue

        # ---------------------------------------------------------
        # 🔥 일반 기사 요약
        # ---------------------------------------------------------
        combined_text = f"{title}\n{summary}\n{content}"
        ai_summary = summarize_text(combined_text)

        update_summary_ai(article_id, ai_summary)

        print(f"✔ 저장 완료 (ARTICLE_ID={article_id})")
        time.sleep(0.2)


# -------------------------------------------------------------
# 실행
# -------------------------------------------------------------
if __name__ == "__main__":
    process_summary_ai()
