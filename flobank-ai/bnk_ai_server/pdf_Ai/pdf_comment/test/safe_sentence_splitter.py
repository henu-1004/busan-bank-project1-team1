# ============================================
#   run_sentence_split.py
#   hanatest.txt → 문장 단위 분리 → 저장
# ============================================

from kiwipiepy import Kiwi


# -----------------------------
# 안정적인 슬라이딩 윈도우 청킹
# -----------------------------
def chunk_text(text: str, size: int = 3000, overlap: int = 300):
    chunks = []
    start = 0
    text_len = len(text)

    while start < text_len:
        end = min(start + size, text_len)
        chunks.append(text[start:end])
        start += (size - overlap)

    return chunks


# -----------------------------
# 문장 분리 전체 수행
# -----------------------------
def split_sentences_safe(text: str):
    kiwi = Kiwi()
    chunks = chunk_text(text)

    final_sentences = []

    for ch in chunks:
        sents = kiwi.split_into_sents(ch)
        for s in sents:
            sentence = s.text.strip()
            if sentence:
                final_sentences.append(sentence)

    return final_sentences


# -----------------------------
# 중복 문장 제거
# -----------------------------
def deduplicate_sentences(sentences):
    seen = set()
    final = []
    for s in sentences:
        if s not in seen:
            final.append(s)
            seen.add(s)
    return final


# -----------------------------
# 실행부
# -----------------------------
if __name__ == "__main__":
    input_path = "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/test/pdf_temp/FLOBANK 더 와이드 상품설명서.txt"
    output_path = "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/test/pdf_temp/FLOBANK 더 와이드 상품설명서.txt_sentences.txt"

    print(f"📄 입력 파일: {input_path}")

    # 텍스트 읽기
    with open(input_path, "r", encoding="utf-8") as f:
        text = f.read()

    print("✂️ 문장 분리 수행 중…")

    sentences = split_sentences_safe(text)

    # ⬇️ 중복 제거 추가
    sentences = deduplicate_sentences(sentences)

    # 저장 (여기만 안전마커로 변경)
    with open(output_path, "w", encoding="utf-8") as f:
        for idx, s in enumerate(sentences, 1):
            safe_marker = f"<<<!SENT_{idx:05d}!>>>"
            f.write(f"{safe_marker} {s}\n")

    print(f"✅ 완료! 결과 저장됨 → {output_path}")
    print(f"총 문장 수: {len(sentences)}")
