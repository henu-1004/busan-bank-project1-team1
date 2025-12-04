# /home/g5223sho/bnk_ai_server/auto_qna/answer.py
# ============================================
# QnA 동작 로직 모듈
#  - 벡터DB 로딩
#  - 질문 레벨링 (SAFE / MID / HIGH)
#  - 컨텍스트 검색 (RAG)
#  - 답변 생성
#  - TB_QNA 업데이트 (SAFE / MID / HIGH)
#  - MID/HIGH: draft 앞에 "AI 생성 초안입니다. 검토가 필요합니다." 고정문구 추가
#  - 레벨링/답변 생성은 제목 + 내용을 함께 사용
# ============================================

import os
import json
from typing import Optional, List, Dict, Any

from openai import OpenAI
from langchain_openai import OpenAIEmbeddings, ChatOpenAI
from langchain_community.vectorstores import FAISS

from qna_db import update_qna_safe, update_qna_mid_high

# -----------------------------
# 0. 경로/모델 설정
# -----------------------------
BASE_DIR = os.path.dirname(os.path.abspath(__file__))      # /home/.../auto_qna
VECTOR_DB_DIR = os.path.join(BASE_DIR, "vector_db")        # ./vector_db

EMBEDDING_MODEL_NAME = "text-embedding-3-large"
LLM_MODEL_NAME = "gpt-4o-mini"

client = OpenAI()
embeddings = OpenAIEmbeddings(model=EMBEDDING_MODEL_NAME)
llm = ChatOpenAI(model=LLM_MODEL_NAME, temperature=0.2)


# -----------------------------
# 1. 벡터 DB 로딩
# -----------------------------
def load_vector_db():
    if not os.path.exists(VECTOR_DB_DIR):
        raise FileNotFoundError(f"벡터 DB 디렉토리 없음: {VECTOR_DB_DIR}")
    print(f"📂 QnA 벡터 DB 로딩: {VECTOR_DB_DIR}")

    db = FAISS.load_local(
        VECTOR_DB_DIR,
        embeddings,
        allow_dangerous_deserialization=True,
    )
    return db


vector_db = load_vector_db()


# -----------------------------
# 2. 질문 레벨링 (SAFE / MID / HIGH)
# -----------------------------
def build_level_prompt(question: str, meta: Optional[dict] = None) -> str:
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


def classify_question(question: str, meta: Optional[dict] = None) -> dict:
    prompt = build_level_prompt(question, meta)

    completion = client.chat.completions.create(
        model=LLM_MODEL_NAME,
        messages=[{"role": "user", "content": prompt}],
        temperature=0,
    )

    raw = completion.choices[0].message.content.strip()
    print("🧩 QnA 분류 원본 응답:", raw)

    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        print("⚠ JSON 파싱 실패. SAFE/basic 로 fallback")
        data = {
            "level": "SAFE",
            "complexity": "basic",
            "reason": f"JSON 파싱 실패: raw={raw}",
            "tags": [],
        }

    return data


# -----------------------------
# 3. 벡터 DB에서 문맥 검색
# -----------------------------
def retrieve_context(question: str, k: int = 5) -> str:
    docs = vector_db.similarity_search(question, k=k)
    print(f"🔍 관련 문서 {len(docs)}개 검색")

    parts: List[str] = []
    for i, d in enumerate(docs, start=1):
        parts.append(f"[문서 {i}]\n{d.page_content}\n")

    return "\n\n".join(parts)


# -----------------------------
# 4. 답변 생성
# -----------------------------
def build_answer_prompt(question: str,
                        level: str,
                        complexity: str,
                        tags,
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

[참고 자료 (RAG 컨텍스트)]
아래 내용은 은행 내부 규정/상품 설명서 등에서 가져온 참고자료입니다.
필요한 내용만 요약해서 답변에 반영하고, 그대로 복붙하지는 마세요.

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
        context=context,
    )

    completion = client.chat.completions.create(
        model=LLM_MODEL_NAME,
        messages=[{"role": "user", "content": prompt}],
        temperature=0.2,
    )

    answer = completion.choices[0].message.content.strip()
    return answer


# -----------------------------
# 5. 메인 처리 함수
#    (FastAPI에서는 이 함수만 호출)
# -----------------------------
def process_qna(qna_no: int,
                question: str,
                title: Optional[str] = None,
                meta: Optional[dict] = None) -> Dict[str, Any]:
    """
    QnA 하나에 대해:
    - (제목 + 내용) 기반으로 레벨링
    - (제목 + 내용) 기반으로 컨텍스트 검색
    - (제목 + 내용) 기반으로 답변 생성
    - MID/HIGH일 때 드래프트 앞에 안내 문구 추가
    - TB_QNA (QNA_DRAFT, QNA_REPLY, QNA_STATUS) 업데이트
    """
    print(f"🚀 QnA 처리 시작 (answer.py): qnaNo={qna_no}")

    # 🔹 제목 + 내용을 하나로 합치기
    if title:
        full_question = f"[제목]\n{title}\n\n[내용]\n{question}"
    else:
        full_question = question

    # 1) 레벨링 (제목+내용 기준)
    cls = classify_question(full_question, meta)

    # 2) 컨텍스트 검색 (제목+내용 기준)
    context = retrieve_context(full_question, k=5)

    # 3) 답변 생성 (제목+내용 기준)
    answer = generate_answer(full_question, cls, context)

    # 4) 드래프트 텍스트 생성
    level = cls.get("level", "SAFE")
    draft = answer

    # MID/HIGH면 안내 문구를 반드시 앞에 붙임
    if level in ("MID", "HIGH"):
        prefix = "AI 생성 초안입니다. 검토가 필요합니다.\n\n"
        draft = prefix + answer

    # 5) DB 업데이트 (TB_QNA)
    try:
        if level == "SAFE":
            # SAFE: 초안은 draft, 유저에게 바로 보여줄 reply는 answer
            update_qna_safe(qna_no, answer=answer, draft=draft)
        elif level in ("MID", "HIGH"):
            # MID/HIGH: reply는 비워두고 draft + status만 설정
            update_qna_mid_high(qna_no, draft=draft, level=level)
        else:
            # 이상한 값이면 일단 SAFE처럼 처리
            print(f"⚠ 알 수 없는 level({level}), SAFE로 처리: qnaNo={qna_no}")
            update_qna_safe(qna_no, answer=answer, draft=draft)
    except Exception as e:
        print(f"❌ TB_QNA 업데이트 실패: qnaNo={qna_no}, error={e}")

    result: Dict[str, Any] = {
        "qnaNo": qna_no,
        "level": level,
        "complexity": cls.get("complexity", "basic"),
        "reason": cls.get("reason", ""),
        "tags": cls.get("tags", []),
        "draft": draft,
        "answer": answer,
    }

    print(f"✅ QnA 처리 완료 (answer.py): {result}")
    return result
