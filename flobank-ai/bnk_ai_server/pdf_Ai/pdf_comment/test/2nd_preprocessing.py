# ============================================
#   2nd_preprocessing_llm_restore_only.py
#   SENT 태그 기반 → LLM 구조 복원 (청킹 없음)
# ============================================

from openai import OpenAI
import re

client = OpenAI()

# --------------------------------------------
# SYSTEM 프롬프트 — 원문 복원 전용
# --------------------------------------------
SYSTEM_PROMPT = """
당신의 임무는 PDF → 텍스트 → 문장 분리 과정에서 생성된 문장 리스트를
원래 문서 구조로 복원하는 것입니다.

이 텍스트는 <<<!SENT_xxxx!>>> 같은 태그 기준으로 인위적으로 잘려 있습니다.
다음 규칙을 반드시 지키세요:

1. 원문의 단어, 숫자, 기호, 표기법은 절대 수정하거나 제거하지 말 것
2. 모든 SENT 태그는 완전히 제거할 것
3. 잘린 문장은 자연스럽게 이어붙일 것
4. 표/불릿/리스트는 하나의 블록으로 유지
5. 개행은 원래 문서 구조를 유지하면서 정리
6. 문장의 순서는 절대 변경하지 말 것
7. 요약 금지, 새로운 문장 생성 금지
8. 출력은 오직 복원된 원문만 포함
"""


# --------------------------------------------
# LLM 호출 함수 — 복원만 수행
# --------------------------------------------
def ask_llm(prev_overlap: str, chunk: str, next_overlap: str):
    user_payload = f"""
아래 세 구간은 원문에서 서로 이어지는 실제 텍스트입니다.

[previous_overlap]
{prev_overlap}

[current_chunk]
{chunk}

[next_overlap]
{next_overlap}

요청:
- 세 구간을 참고하여, 현재 chunk의 문장과 구조를 원문 그대로 정확히 복원해 주세요.
- 복원된 텍스트만 출력하세요.
- previous_overlap 또는 next_overlap에 포함된 문장을 중복 생성하지 말 것
- overlap은 참고용이며, 이미 포함된 구문은 반복 출력 금지
"""
    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_payload},
        ],
        temperature=0.0
    )
    return response.choices[0].message.content


# --------------------------------------------
# 슬라이딩 윈도우
# --------------------------------------------
def sliding_window_text(text, size=7000, overlap=1000):
    chunks = []
    starts = []
    start = 0
    total = len(text)

    while start < total:
        end = min(start + size, total)
        chunks.append(text[start:end])
        starts.append(start)
        start += (size - overlap)

    return chunks, starts


# --------------------------------------------
# SENT 리스트 → raw text 병합
# --------------------------------------------
def load_raw_text_with_tags(path: str):
    merged = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue

            # [123] 제거
            line = re.sub(r"^\s*\[\d+\]\s*", "", line)
            merged.append(line)
    return "\n".join(merged)


# --------------------------------------------
# 실행부
# --------------------------------------------
if __name__ == "__main__":

    input_path = "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/test/pdf_temp/FLOBANK 더 와이드 상품설명서.txt_sentences.txt"
    output_path = "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/test/pdf_temp/FLOBANK 더 와이드 상품설명서_restored.txt"

    print("📄 입력 파일 로드 중…")
    raw_text = load_raw_text_with_tags(input_path)

    print("🔗 슬라이딩 윈도우 생성…")
    win_chunks, starts = sliding_window_text(raw_text)

    outputs = []
    total = len(win_chunks)

    print("🧠 LLM 원문 복원 요청 중…")
    for i, chunk in enumerate(win_chunks):
        print(f" - 처리 중: {i+1}/{total}")

        text = chunk.strip()
        if len(text) == 0:
            print("   → 빈 chunk → 스킵")
            continue

        prev_overlap = raw_text[max(0, starts[i] - 1000): starts[i]] if i > 0 else ""
        next_overlap = raw_text[starts[i] + len(chunk): starts[i] + len(chunk) + 1000] if i < total - 1 else ""

        out = ask_llm(prev_overlap, chunk, next_overlap)
        outputs.append(out)

    print("📦 전체 복원 병합 중…")
    final_text = "\n".join(outputs)

    print("💾 저장 중…")
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(final_text)

    print("✅ 완료! 저장됨:", output_path)
