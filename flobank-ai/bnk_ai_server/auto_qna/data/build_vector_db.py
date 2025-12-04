import os
from langchain_openai import OpenAIEmbeddings
from langchain_community.vectorstores import FAISS

# ============================================
# 1) 설정
# ============================================
TXT_DIR = "/home/g5223sho/bnk_ai_server/auto_qna/data/txt"
DB_DIR = "/home/g5223sho/bnk_ai_server/auto_qna/vector_db"

MAX_CHARS = 1200
OVERLAP = 200


# ============================================
# 2) 텍스트 → 청킹 함수
# ============================================
def chunk_text(text: str, max_chars=MAX_CHARS, overlap=OVERLAP):
    chunks = []
    start = 0
    end = max_chars

    while start < len(text):
        chunk = text[start:end]
        chunks.append(chunk)

        # next window
        start = end - overlap
        end = start + max_chars

    return chunks


# ============================================
# 3) 폴더 내 모든 TXT → 전체 청크 로드
# ============================================
def load_all_chunks(txt_dir: str):
    if not os.path.exists(txt_dir):
        raise FileNotFoundError(f"❌ TXT 폴더가 없음: {txt_dir}")

    chunks = []
    meta = []

    files = os.listdir(txt_dir)
    txt_files = [f for f in files if f.lower().endswith(".txt")]

    print(f"📂 TXT 파일 {len(txt_files)}개 발견")

    for fname in txt_files:
        fpath = os.path.join(txt_dir, fname)

        with open(fpath, "r", encoding="utf-8") as f:
            text = f.read()

        file_chunks = chunk_text(text)
        chunks.extend(file_chunks)
        meta.extend([{"source": fname}] * len(file_chunks))

        print(f"📄 {fname}: {len(file_chunks)} chunks 생성")

    print(f"\n📌 전체 청크 개수: {len(chunks)}")

    return chunks, meta


# ============================================
# 4) 벡터 DB 생성
# ============================================
def build_vector_db():
    # DB 폴더 생성
    os.makedirs(DB_DIR, exist_ok=True)

    # Load chunks
    chunks, metadata = load_all_chunks(TXT_DIR)

    print("\n🔍 임베딩 생성 시작 (text-embedding-3-large)")
    embedder = OpenAIEmbeddings(model="text-embedding-3-large")

    # Build DB
    db = FAISS.from_texts(chunks, embedder, metadatas=metadata)

    db.save_local(DB_DIR)
    print(f"\n💾 벡터 DB 저장 완료 → {DB_DIR}")


# ============================================
# 5) 실행
# ============================================
if __name__ == "__main__":
    build_vector_db()
