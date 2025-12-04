# /home/g5223sho/bnk_ai_server/auto_qna/qna_db.py
# ============================================
# TB_QNA_HDR 업데이트 전용 모듈
#  - SAFE: draft + reply + status
#  - MID/HIGH: draft + status
# ============================================

import oracledb

# 👉 pdf_ai_db.py에서 쓰는 설정이랑 맞춰줄 것
DB_USER = "flobank"
DB_PASSWORD = "1234"
DB_DSN = "34.64.225.88:1521/XEPDB1"

# 커넥션 풀 생성
pool = oracledb.create_pool(
    user=DB_USER,
    password=DB_PASSWORD,
    dsn=DB_DSN,
    min=1,
    max=3,
    increment=1,
    getmode=oracledb.SPOOL_ATTRVAL_WAIT,
)


def update_qna_safe(qna_no: int, answer: str, draft: str):
    """
    SAFE 일 때:
    - QNA_DRAFT  = draft (초안)
    - QNA_REPLY  = answer (최종 답변, 유저에게 노출)
    - QNA_STATUS = 'SAFE'
    """
    sql = """
        UPDATE TB_QNA_HDR
           SET QNA_DRAFT  = :draft,
               QNA_REPLY  = :answer,
               QNA_STATUS = 'SAFE'
         WHERE QNA_NO     = :qna_no
    """

    binds = {
        "draft": draft,
        "answer": answer,
        "qna_no": qna_no,
    }

    with pool.acquire() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, binds)
        conn.commit()

    print(f"💾 TB_QNA_HDR SAFE 업데이트 완료: QNA_NO={qna_no}")


def update_qna_mid_high(qna_no: int, draft: str, level: str):
    """
    MID / HIGH 일 때:
    - QNA_DRAFT  = draft (초안)
    - QNA_REPLY  는 건드리지 않음 (NULL 유지)
    - QNA_STATUS = 'MID' 또는 'HIGH'
    """
    if level not in ("MID", "HIGH"):
        raise ValueError(f"level must be MID or HIGH, got {level}")

    sql = """
        UPDATE TB_QNA_HDR
           SET QNA_DRAFT  = :draft,
               QNA_STATUS = :status
         WHERE QNA_NO     = :qna_no
    """

    binds = {
        "draft": draft,
        "status": level,   # level 값은 status 바인드로 넘김
        "qna_no": qna_no,
    }

    with pool.acquire() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, binds)
        conn.commit()

    print(f"💾 TB_QNA_HDR {level} 업데이트 완료: QNA_NO={qna_no}")
