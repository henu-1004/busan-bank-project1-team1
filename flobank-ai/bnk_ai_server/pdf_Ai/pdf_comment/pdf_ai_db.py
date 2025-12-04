# ================================================
#          pdf_ai_db.py (MINIMAL VERSION)
#   TB_PDF_AI: 상품 정보 업데이트 전용
# ================================================

import oracledb

# ----------------------------------------
# DB 설정
# ----------------------------------------
DB_USER = "flobank"
DB_PASSWORD = "1234"
DB_DSN = "34.64.225.88:1521/XEPDB1"


# ----------------------------------------
# Connection Pool
# ----------------------------------------
pool = oracledb.create_pool(
    user=DB_USER,
    password=DB_PASSWORD,
    dsn=DB_DSN,
    min=1,
    max=3,
    increment=1
)


# ----------------------------------------
# Connection getter
# ----------------------------------------
def get_conn():
    conn = pool.acquire()
    cur = conn.cursor()
    cur.execute("ALTER SESSION SET TIME_ZONE = 'Asia/Seoul'")
    cur.close()
    return conn


# ----------------------------------------
# (선택) PDF 조회 — 필요 없으면 삭제 가능
# ----------------------------------------
def get_pdf(pdf_id):
    conn = get_conn()
    cur = conn.cursor()

    cur.execute("SELECT * FROM TB_PDF_AI WHERE PDF_ID = :id", {"id": pdf_id})
    row = cur.fetchone()

    cur.close()
    pool.release(conn)
    return row


# ----------------------------------------
# (선택) 상태 변경 — 필요하면 keep
# ----------------------------------------
def update_status(pdf_id, status):
    conn = get_conn()
    cur = conn.cursor()

    sql = """
        UPDATE TB_PDF_AI
           SET STATUS = :st,
               UPDATED_AT = CURRENT_TIMESTAMP
         WHERE PDF_ID = :id
    """

    try:
        cur.execute(sql, {"st": status, "id": pdf_id})
        conn.commit()
    except Exception as e:
        print("❌ update_status 실패:", e)
        conn.rollback()
    finally:
        cur.close()
        pool.release(conn)


# ----------------------------------------
# ⭐ 핵심: 상품정보 업데이트 ONLY (product_features 추가)
# ----------------------------------------
def update_product_info(pdf_id, info: dict):

    conn = get_conn()
    cur = conn.cursor()

    sql = """
        UPDATE TB_PDF_AI
           SET PRODUCT_NAME          = :product_name,
               PRODUCT_SHORT_DESC    = :description_short,
               PRODUCT_FEATURES      = :product_features,
               DEPOSIT_TYPE          = :deposit_type,
               CURRENCIES            = :curr_list,
               EXCHANGE_RATE_POLICY  = :exchange_rate_policy,
               TERM_TYPE             = :subscription_period_type,
               MIN_MONTH             = :min_month,
               MAX_MONTH             = :max_month,
               ELIGIBILITY           = :eligibility,
               PARTIAL_WITHDRAWAL    = :partial_withdrawal,
               AUTO_RENEWAL          = :auto_renewal,
               ADDITIONAL_DEPOSIT    = :additional_deposit,
               UPDATED_AT            = CURRENT_TIMESTAMP
         WHERE PDF_ID = :pdf_id
    """

    try:
        cur.execute(sql, {
            "product_name": info.get("product_name"),
            "description_short": info.get("description_short"),
            "product_features": info.get("product_features"),
            "deposit_type": info.get("deposit_type"),
            "curr_list": ",".join(info.get("currencies") or []),
            "exchange_rate_policy": info.get("exchange_rate_policy"),
            "subscription_period_type": info.get("subscription_period_type"),
            "min_month": info.get("min_month"),
            "max_month": info.get("max_month"),
            "eligibility": info.get("eligibility"),
            "partial_withdrawal": info.get("partial_withdrawal"),
            "auto_renewal": info.get("auto_renewal"),
            "additional_deposit": info.get("additional_deposit"),
            "pdf_id": pdf_id
        })
        conn.commit()

        print(f"✅ PDF_ID={pdf_id} 상품 정보 업데이트 성공")

    except Exception as e:
        print("❌ update_product_info 실패:", e)
        conn.rollback()

    finally:
        cur.close()
        pool.release(conn)



# ----------------------------------------
# ⭐ 위험 분석 결과 업데이트 (AI 분석 결과 저장) — UPDATED
# ----------------------------------------
def update_ai_risk(pdf_id, overall_risk, llm_comment):
    conn = get_conn()
    cur = conn.cursor()

    sql = """
        UPDATE TB_PDF_AI
           SET AI_OVERALL_RISK = :overall_risk,
               AI_COMMENT      = :ai_comment,
               UPDATED_AT      = CURRENT_TIMESTAMP
         WHERE PDF_ID = :pdf_id
    """

    try:
        cur.execute(sql, {
            "overall_risk": overall_risk,
            "ai_comment": llm_comment,
            "pdf_id": pdf_id
        })
        conn.commit()

        print(f"⚠️ PDF_ID={pdf_id} 위험 분석 결과 DB 저장 완료")

    except Exception as e:
        print("❌ update_ai_risk 실패:", e)
        conn.rollback()

    finally:
        cur.close()
        pool.release(conn)


