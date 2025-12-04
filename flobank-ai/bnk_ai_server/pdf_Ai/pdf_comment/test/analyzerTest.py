# ================================================
#   rag_query.py
#   저장된 FAISS → RAG 검색 + 답변 생성
# ================================================

import os
from langchain_community.vectorstores import FAISS
from langchain_openai import OpenAIEmbeddings, ChatOpenAI

# 🔥 API KEY



# ----------------------------------------
# 1) FAISS DB 불러오기
# ----------------------------------------
def load_vectorstore(path="faiss_db"):
    embed = OpenAIEmbeddings(model="text-embedding-3-large")

    db = FAISS.load_local(
        folder_path=path,
        embeddings=embed,
        allow_dangerous_deserialization=True
    )

    print("📚 벡터DB 로드 완료")
    return db


# ----------------------------------------
# 2) 검색기(retriever) 생성
# ----------------------------------------
def make_retriever(db):
    retriever = db.as_retriever(
        search_kwargs={"k": 10}   # 상위 5개 청크 검색
    )
    return retriever


# ----------------------------------------
# 3) RAG 질의응답
# ----------------------------------------
def ask_question(query, retriever):
    # 최신 방식
    docs = retriever.invoke(query)

    print("\n🔎 관련 문서 중 일부:\n")
    for i, d in enumerate(docs[:7]):
        print(f"--- 문서 {i+1} ---")
        print(d.page_content[:800].replace("\n", " "))
        print("\n")

    context = "\n\n".join([d.page_content for d in docs])

    prompt = f"""
다음은 금융법규/상품설명서/규정 등의 원문 일부이다.
이를 참고하여 아래 질문에 정확히 답하라.

[Context]
{context}

[Question]
{query}

답변은 다음 형식으로 작성:
- 규정 근거가 있다면 근거 조항도 함께 제시
- 문장 길이는 간결
"""

    llm = ChatOpenAI(model="gpt-4o-mini", temperature=0.0)
    response = llm.invoke(prompt)

    return response.content



# ----------------------------------------
# 4) 실행 예시
# ----------------------------------------
if __name__ == "__main__":
    db = load_vectorstore("faiss_db")
    retriever = make_retriever(db)

    q = "외국환거래규정에서 해외예금 신고는 어떻게 해야 해?"
    answer = ask_question(q, retriever)

    print("\n💡 최종 답변 ↓↓↓\n")
    print(answer)
