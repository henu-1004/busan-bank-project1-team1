# ================================================
#   llm_auto_preprocess.py
#   LLM 기반 Adaptive Chunking + Safe Cleaning
#   (모든 출력은 ver_llm_preprocessing/ 아래 저장)
# ================================================

import os
import re
import json
from pathlib import Path
from langchain_openai import ChatOpenAI

llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)


# =================================================
# 0) 화이트스페이스 Normalize
# =================================================

def normalize_ws(text: str):
    return re.sub(r"\s+", "", text)


# =================================================
# 1) LLM 기반 문서 chunking
# =================================================

def llm_chunk_document(full_text: str):

    prompt = f"""
너는 문서 구조 분석 전문가다.

다음 텍스트를 '의미 단위(chunk)'로 분리하라.
chunk 개수는 5~50개 사이로 안정적으로 생성하라.

출력 형식 (JSON 코드블록 금지):
chunks: [
  {{
    "title": "요약 제목",
    "content": "해당 chunk의 원문 (절대 수정 금지)"
  }},
  ...
]

규칙:
- 원문 수정/삭제/추가 금지
- OCR 오류 있어도 원문 그대로 유지
- 표는 하나의 chunk로 묶기
- 조문/항목/절 단위도 chunk 시작점 될 수 있음
- 제목(title)은 짧게 3~10자

문서 전체:

======================
{full_text}
======================

위 규칙대로 chunk 목록을 만들어라.
"""

    return llm.invoke(prompt).content


# =================================================
# 2) JSON-like chunk 파싱
# =================================================

def parse_llm_chunks(raw_text: str):

    match = re.search(r"chunks\s*:\s*\[(.*)\]", raw_text, re.DOTALL)
    if not match:
        return []

    body = match.group(1)

    text_json = "[" + body + "]"
    text_json = text_json.replace("\n", " ")
    text_json = re.sub(r",\s*}", "}", text_json)

    try:
        chunks = json.loads(text_json)
    except:
        chunks = [{"title": "chunk", "content": body}]

    final_chunks = []
    for c in chunks:
        if "content" in c:
            final_chunks.append(c["content"].strip())

    return final_chunks


# =================================================
# 3) chunk별 안전 전처리
# =================================================

def preprocess_chunk_safe(chunk: str):

    prompt = f"""
너는 문서 전처리 전문가다.

조건:
- 원문 단어/문장 절대 수정 금지
- OCR로 붙은 문장만 분리 가능
- 리스트는 항목 단위 줄바꿈
- 표는 행 단위 정렬
- 내용 삭제/추가/변경 금지

[청크]
{chunk}

위 원문을 사람이 읽기 좋게 정리하되,
원문의 의미와 단어는 절대 바꾸지 마라.
"""

    cleaned = llm.invoke(prompt).content

    # diff 검사 (WS 제거 후 비교)
    if normalize_ws(cleaned) != normalize_ws(chunk):
        return chunk
    else:
        return cleaned


# =================================================
# 4) 전체 txt 파일 처리 with ver_llm_preprocessing/
# =================================================

def process_txt_file(path: Path):

    print(f"\n📄 처리 시작: {path}")

    # 출력 디렉토리 생성
    out_dir = path.parent / "ver_llm_preprocessing"
    out_dir.mkdir(exist_ok=True)

    full_text = path.read_text(encoding="utf-8", errors="ignore")

    # (1) LLM chunking 수행
    print("🔍 LLM 기반 chunk 분석 중...")
    raw_chunk_text = llm_chunk_document(full_text)

    # chunk 구조 저장
    chunk_struct_path = out_dir / f"{path.stem}_chunk_structure.txt"
    chunk_struct_path.write_text(raw_chunk_text, encoding="utf-8")

    # (2) chunk 파싱
    chunks = parse_llm_chunks(raw_chunk_text)
    print(f"✔ LLM이 생성한 chunk 개수: {len(chunks)}")

    # (3) chunk별 safe cleaning
    processed_chunks = []

    for idx, c in enumerate(chunks):
        print(f"  ➤ chunk {idx+1}/{len(chunks)} 전처리 중...")
        try:
            cleaned = preprocess_chunk_safe(c)
        except:
            cleaned = c
        processed_chunks.append(cleaned)

    # (4) 최종 저장
    out_path = out_dir / f"{path.stem}_llm_preprocessed.txt"
    out_path.write_text("\n\n".join(processed_chunks), encoding="utf-8")

    print(f"✅ 저장 완료: {out_path}")


# =================================================
# 5) 전체 폴더 처리
# =================================================

def process_all_txt(directory: str):
    for f in Path(directory).glob("*.txt"):
        process_txt_file(f)


# =================================================
# 6) 실행
# =================================================

if __name__ == "__main__":
    process_all_txt("/home/g5223sho/bnk_ai_server/pdf_Ai/data/txt")
