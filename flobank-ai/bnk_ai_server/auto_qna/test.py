# ============================================
# auto_qna_pipeline.py
# QnA: 1차 레벨링(SAFE/MID/HIGH) + RAG 답변 생성
# 결과를 txt로 저장해서 확인용
# ============================================

import os
import json
from datetime import datetime

from openai import OpenAI
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_community.vectorstores import FAISS

# -----------------------------
# 0. 설정
# -----------------------------
VECTOR_DB_DIR = "/home/g5223sho/bnk_ai_server/auto_qna/vector_db"
LOG_DIR = "./qna_logs"

os.makedirs(LOG_DIR, exist_ok=True)

# OpenAI 클라이언트 (환경변수 OPENAI_API_KEY 필요)
client = OpenAI()

# LLM & Embedding (임베딩 모델은 벡터DB 만들 때와 동일해야 함!)
EMBEDDING_MODEL_NAME = "text-embedding-3-large"  # 필요시 수정
LLM_MODEL_NAME = "gpt-4o-mini"

embeddings = OpenAIEmbeddings(model=EMBEDDING_MODEL_NAME)
llm = ChatOpenAI(model=LLM_MODEL_NAME, temperature=0.2)


# -----------------------------
# 1. 벡터 DB 로딩
# -----------------------------
def load_vector_db():
    if not os.path.exists(VECTOR_DB_DIR):
        raise FileNotFoundError(f"벡터 DB 디렉토리 없음: {VECTOR_DB_DIR}")
    print(f"📂 벡터 DB 로딩: {VECTOR_DB_DIR}")

    # langchain 0.2+ 에선 allow_dangerous_deserialization=True 필요할 수 있음
    db = FAISS.load_local(
        VECTOR_DB_DIR,
        embeddings,
        allow_dangerous_deserialization=True
    )
    return db


vector_db = load_vector_db()


# -----------------------------
# 2. 질문 레벨링 (SAFE / MID / HIGH)
# -----------------------------
def build_level_prompt(question: str, meta: dict | None = None) -> str:
    meta_text = f"\n[메타데이터]\n{meta}\n" if meta else ""
    return f"""
당신은 은행 QnA 질문의 위험도를 SAFE, MID, HIGH 세 단계로 분류하는 어시스턴트입니다.

[레벨 정의]
- SAFE:
  - 영업시간, 지점 위치, 단순 절차, 기본 상품 안내 등
  - 규제/민원 리스크가 거의 없는 질문
- MID:
  - 이자율, 우대금리, 수수료, 중도해지 조건, 환율 적용 방식 등
  - 금융정보이지만 "수익 보장"이나 "손실 책임"까지는 아닌 질문
- HIGH:
  - 손실/수익 보장, 수익률/환율 수익 계산, 투자 추천
  - 손실 책임, 분쟁, 소송, 보전 요구 등
  - 잘못 답변 시 법적/규제 리스크가 큰 질문

[난이도]
- basic / intermediate / advanced 중 하나로 선택:
  - basic: 아주 단순, 초보자 수준
  - intermediate: 조건/예외가 일부 포함된 보통 수준
  - advanced: 복잡한 시나리오, 전문적인 내용

아래 JSON 형식으로만 출력하세요. 다른 설명 금지.

{{
  "level": "SAFE" | "MID" | "HIGH",
  "complexity": "basic" | "intermediate" | "advanced",
  "reason": "이 레벨을 선택한 이유를 한글로 한두 문장",
  "tags": ["키워드1", "키워드2"]
}}

[질문]
{question}
{meta_text}
"""


def classify_question(question: str, meta: dict | None = None) -> dict:
    prompt = build_level_prompt(question, meta)

    completion = client.chat.completions.create(
        model=LLM_MODEL_NAME,
        messages=[{"role": "user", "content": prompt}],
        temperature=0
    )

    raw = completion.choices[0].message.content.strip()
    print("🧩 분류 원본 응답:", raw)

    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        # 혹시 JSON 포맷 안 지키면 다시 한 번 강하게 요청하거나, fallback
        print("⚠ JSON 파싱 실패. raw를 그대로 level=SAFE로 fallback")
        data = {
            "level": "SAFE",
            "complexity": "basic",
            "reason": f"JSON 파싱 실패. raw={raw}",
            "tags": []
        }

    return data


# -----------------------------
# 3. 벡터 DB에서 문맥 검색
# -----------------------------
def retrieve_context(question: str, k: int = 5) -> str:
    """
    벡터DB에서 관련 문서 k개 검색하고, 하나의 문자열로 합침
    """
    docs = vector_db.similarity_search(question, k=k)
    print(f"🔍 관련 문서 {len(docs)}개 검색")

    context_parts = []
    for i, d in enumerate(docs, start=1):
        context_parts.append(f"[문서 {i}]\n{d.page_content}\n")

    return "\n\n".join(context_parts)


# -----------------------------
# 4. 답변 생성 (레벨 + 문맥 활용)
# -----------------------------
def build_answer_prompt(question: str,
                        level: str,
                        complexity: str,
                        tags: list[str],
                        context: str) -> str:
    tags_text = ", ".join(tags or [])

    guardrail = ""
    if level == "MID":
        guardrail = """
- 사실과 다른 정보를 추측해서 말하지 않는다.
- 약관/상품설명서 확인이 필요함을 안내한다.
- 수익 보장, 손실 미발생 등 확정적인 표현은 피한다.
"""
    elif level == "HIGH":
        guardrail = """
- 수익 보장, 손실 보전, 환율 방향 예측 등을 단정적으로 말하지 않는다.
- 투자 판단 및 최종 책임이 고객에게 있다는 점을 분명히 알린다.
- 구체적인 투자 추천 대신, 일반적인 원칙과 주의사항 위주로 설명한다.
- 분쟁/책임 관련 판단은 하지 않고, 정식 상담/민원 절차로 안내한다.
"""

    return f"""
당신은 은행 고객센터 상담원입니다.

[질문]
{question}

[질문 레벨]
- 위험도: {level}
- 난이도: {complexity}
- 태그: {tags_text}

[설명 난이도 규칙]
- basic: 어려운 용어 없이 아주 쉽게, 예시 포함
- intermediate: 금융 용어 사용 가능하되 간단히 풀어 설명
- advanced: 비교적 전문적인 설명 허용

[리스크 가드레일]
{guardrail}

[참고 자료(RAG 컨텍스트)]
아래 내용은 은행 내부 규정/상품 설명서에서 가져온 참고용 자료입니다.
필요한 내용만 요약해서 답변에 반영하고, 직접 복붙은 피하세요.

{context}

[출력 형식]
- 고객에게 바로 보여줄 수 있는 답변만 작성
- 3~7문장, 정중한 존댓말 (~~입니다, ~~하세요)
- 불필요한 서론/결론 없이, 핵심 내용 위주로 답변
"""


def generate_answer(question: str, cls: dict, context: str) -> str:
    level = cls.get("level", "SAFE")
    complexity = cls.get("complexity", "basic")
    tags = cls.get("tags", [])

    prompt = build_answer_prompt(
        question=question,
        level=level,
        complexity=complexity,
        tags=tags,
        context=context
    )

    completion = client.chat.completions.create(
        model=LLM_MODEL_NAME,
        messages=[{"role": "user", "content": prompt}],
        temperature=0.2
    )

    answer = completion.choices[0].message.content.strip()
    return answer


# -----------------------------
# 5. 결과를 TXT로 저장
# -----------------------------
def save_result_to_txt(question: str, cls: dict, context: str, answer: str) -> str:
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = os.path.join(LOG_DIR, f"qna_result_{ts}.txt")

    with open(filename, "w", encoding="utf-8") as f:
        f.write("=== QNA AUTO PIPELINE 결과 ===\n\n")
        f.write("[질문]\n")
        f.write(question + "\n\n")

        f.write("[분류 결과]\n")
        f.write(json.dumps(cls, ensure_ascii=False, indent=2) + "\n\n")

        f.write("[검색된 컨텍스트 요약]\n")
        f.write(context + "\n\n")

        f.write("[최종 답변]\n")
        f.write(answer + "\n")

    print(f"💾 결과 저장: {filename}")
    return filename


# -----------------------------
# 6. 메인 루프 (터미널에서 테스트)
# -----------------------------
def run_interactive():
    print("=== QnA AUTO (레벨링 + 답변생성) 테스트 ===")
    print("질문을 입력하세요. 빈 줄 입력 시 종료.\n")

    while True:
        question = input("Q> ").strip()
        if not question:
            print("종료합니다.")
            break

        # 1) 레벨링
        cls = classify_question(question)
        print("\n🧷 분류 결과:", cls, "\n")

        # 2) 컨텍스트 검색
        context = retrieve_context(question, k=5)

        # 3) 답변 생성
        answer = generate_answer(question, cls, context)
        print("\n💬 생성된 답변:\n", answer, "\n")

        # 4) TXT 저장
        save_result_to_txt(question, cls, context, answer)
        print("=" * 60 + "\n")


if __name__ == "__main__":
    run_interactive()
