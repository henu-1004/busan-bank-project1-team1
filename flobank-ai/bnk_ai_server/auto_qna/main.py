# /home/g5223sho/bnk_ai_server/auto_qna/main.py
# ============================================
# QnA 자동처리 서버 (얇은 레이어)
#  - 실제 로직은 answer.py의 process_qna에서 처리
# ============================================

from typing import Optional, List

from fastapi import FastAPI
from pydantic import BaseModel

from answer import process_qna  # 같은 폴더 기준

app = FastAPI(title="Auto QnA Server", version="0.1")


# -----------------------------
# 1. Pydantic 모델
# -----------------------------
class QnaAutoRequest(BaseModel):
    qnaNo: int
    question: str
    title: Optional[str] = None
    meta: Optional[dict] = None   # 필요하면 나중에 확장


class QnaAutoResponse(BaseModel):
    qnaNo: int
    level: str           # SAFE / MID / HIGH
    complexity: str      # basic / intermediate / advanced
    tags: List[str]
    reason: str
    draft: str           # 항상 AI 초안
    answer: str          # SAFE일 때는 reply로 바로 사용 가능


# -----------------------------
# 2. 헬스체크
# -----------------------------
@app.get("/health")
def health():
    return {"status": "ok"}


# -----------------------------
# 3. 메인 엔드포인트
# -----------------------------
@app.post("/api/qna/auto_process", response_model=QnaAutoResponse)
def auto_process(req: QnaAutoRequest):
    """
    Spring에서 QnA INSERT 후 호출:
    - req.qnaNo: 방금 생성된 QNA_NO
    - req.question: QNA_CONTENT
    - title/meta는 옵션
    """
    print(f"🚀 QnA 자동처리 시작 (main.py): qnaNo={req.qnaNo}")

    result = process_qna(
        qna_no=req.qnaNo,
        question=req.question,
        title=req.title,
        meta=req.meta,
    )

    # dict -> Pydantic 모델로 변환
    res = QnaAutoResponse(**result)

    print(f"✅ QnA 자동처리 완료 (main.py): qnaNo={res.qnaNo}, level={res.level}")
    return res
