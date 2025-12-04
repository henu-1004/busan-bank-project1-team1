# ================================================
#   rag_build_vectorstore_no_preprocess.py
#   TXT → 청크 → 임베딩 → FAISS DB 저장
# ================================================

import os
from pathlib import Path

from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_openai import OpenAIEmbeddings




# ----------------------------------------
# 1) TXT 로딩
# ----------------------------------------
def load_all_txt(directory: str):
    texts = []
    metadata = []

    for file in Path(directory).glob("*.txt"):
        raw = file.read_text(encoding="utf-8", errors="ignore")
        texts.append(raw)
        metadata.append({"source": file.name})

    return texts, metadata


# ----------------------------------------
# 2) 청크화
# ----------------------------------------
def chunk_texts(texts, meta):
    splitter = RecursiveCharacterTextSplitter(
        chunk_size=1200,
        chunk_overlap=200
    )

    chunks = []
    metainfo = []

    for text, m in zip(texts, meta):
        split = splitter.split_text(text)
        chunks.extend(split)
        metainfo.extend([m] * len(split))

    return chunks, metainfo


# ----------------------------------------
# 3) 임베딩 + 벡터DB 저장
# ----------------------------------------
def build_vectorstore(chunks, metadata, save_path="faiss_db"):
    embed = OpenAIEmbeddings(model="text-embedding-3-large")

    db = FAISS.from_texts(chunks, embed, metadatas=metadata)
    db.save_local(save_path)

    print(f"💾 저장 완료: {save_path}/")
    return db


# ----------------------------------------
# 4) 전체 파이프라인 실행
# ----------------------------------------
def run_pipeline(txt_dir="data/txt", save_path="faiss_db"):
    print("📁 TXT 스캔 중…")
    texts, meta = load_all_txt(txt_dir)
    print(f"📝 총 {len(texts)}개 파일 로드 완료\n")

    print("✂️ 청크화 중…")
    chunks, metadata = chunk_texts(texts, meta)
    print(f"총 청크 개수: {len(chunks)}\n")

    print("🧠 임베딩 + 벡터DB 생성 중…")
    build_vectorstore(chunks, metadata, save_path)

    print("\n🚀 전체 완료!")


if __name__ == "__main__":
    run_pipeline(txt_dir="/home/g5223sho/bnk_ai_server/pdf_Ai/data/txt", save_path="faiss_db")
