# ============================================
#                 db.py
#     (GCP Oracle XE + Connection Pool)
# ============================================

import oracledb
from datetime import datetime

# ----------------------------------------
# DB 설정 (GCP VM + Oracle XE)
# ----------------------------------------
DB_USER = "flobank"
DB_PASSWORD = "1234"
DB_DSN = "34.64.225.88:1521/XEPDB1"   # HOST:PORT/SERVICE_NAME


# ----------------------------------------
# Connection Pool 생성 (프로세스 전체에서 1번만 실행)
# ----------------------------------------
pool = oracledb.create_pool(
    user=DB_USER,
    password=DB_PASSWORD,
    dsn=DB_DSN,
    min=1,      # 최소 세션 1개
    max=3,      # 최대 세션 3개 → 절대 세션 폭발 없음
    increment=1,
    timeout=60,        # idle connection 60초 후 반환
    wait_timeout=10,   # 10초 이상 기다리면 에러
)


# ----------------------------------------
# 커넥션 획득 함수 (재사용됨)
# ----------------------------------------
def get_conn():
    conn = pool.acquire()

    # ⭐ 매 연결 시 세션 타임존 KST로 고정
    cur = conn.cursor()
    cur.execute("ALTER SESSION SET TIME_ZONE = 'Asia/Seoul'")
    cur.close()

    return conn


# ----------------------------------------
# 기존 URL 전체 로딩
# ----------------------------------------
def get_existing_urls():
    conn = get_conn()
    cur = conn.cursor()

    cur.execute("SELECT URL FROM TB_ARTICLE")
    rows = cur.fetchall()

    urls = set([row[0] for row in rows])

    cur.close()
    pool.release(conn)
    return urls


# ----------------------------------------
# URL 존재 여부
# ----------------------------------------
def exists_url(url: str) -> bool:
    conn = get_conn()
    cur = conn.cursor()
    cur.execute("SELECT COUNT(*) FROM TB_ARTICLE WHERE URL = :u", {"u": url})
    count = cur.fetchone()[0]
    cur.close()
    pool.release(conn)
    return count > 0


# ----------------------------------------
# 날짜 변환
# ----------------------------------------
def normalize_datetime(dt_str):
    if isinstance(dt_str, datetime):
        return dt_str

    for fmt in ("%Y-%m-%d %H:%M:%S", "%Y.%m.%d %H:%M"):
        try:
            return datetime.strptime(dt_str, fmt)
        except:
            pass

    return datetime.now()


# ----------------------------------------
# 기사 INSERT
# ----------------------------------------
def insert_article(company, category, title, url, written_at, summary, content):
    conn = get_conn()
    cur = conn.cursor()
    sql = """
        INSERT INTO TB_ARTICLE (
            ARTICLE_ID, COMPANY, CATEGORY, TITLE, URL,
            WRITTEN_AT, SUMMARY, CONTENT
        )
        VALUES (
            TB_ARTICLE_SEQ.NEXTVAL,
            :c, :cat, :t, :u,
            :w, :s, :ct
        )
    """
    try:
        cur.execute(sql, {
            "c": company[:10],
            "cat": category[:15],
            "t": title,
            "u": url,
            "w": normalize_datetime(written_at),
            "s": summary,
            "ct": content
        })
        conn.commit()
    except Exception as e:
        print("❌ insert_article 실패:", e)
        conn.rollback()
    finally:
        cur.close()
        pool.release(conn)


# ----------------------------------------
# SUMMARY_AI 업데이트
# ----------------------------------------
def update_summary_ai(url, summary_ai):
    conn = get_conn()
    cur = conn.cursor()

    sql = """
        UPDATE TB_ARTICLE
           SET SUMMARY_AI = :ai,
               UPDATED_AT = CURRENT_TIMESTAMP
         WHERE URL = :u
    """

    try:
        cur.execute(sql, {"ai": summary_ai, "u": url})
        conn.commit()
    except Exception as e:
        print("❌ update_summary_ai 실패:", e)
        conn.rollback()
    finally:
        cur.close()
        pool.release(conn)


# ===================================================================
#                     브리핑 관련 로직
# ===================================================================

def briefing_exists_today(mode):
    conn = get_conn()
    cur = conn.cursor()

    sql = """
        SELECT COUNT(*)
          FROM TB_BRIEFING_LOG
         WHERE BRIEFING_MODE = :m
           AND TRUNC(BRIEFING_DATE) = TRUNC(SYSDATE)
    """

    cur.execute(sql, {"m": mode})
    count = cur.fetchone()[0]

    cur.close()
    pool.release(conn)
    return count > 0


def get_last_briefing_time(mode):
    conn = get_conn()
    cur = conn.cursor()

    sql = """
        SELECT MAX(CREATED_AT)
        FROM TB_BRIEFING_LOG
        WHERE BRIEFING_MODE = :m
    """

    cur.execute(sql, {"m": mode})
    last_dt = cur.fetchone()[0]

    cur.close()
    pool.release(conn)
    return last_dt


def has_article_after(dt):
    if dt is None:
        return True

    conn = get_conn()
    cur = conn.cursor()

    sql = """
        SELECT COUNT(*)
          FROM TB_ARTICLE
         WHERE WRITTEN_AT > :dt
    """

    cur.execute(sql, {"dt": dt})
    count = cur.fetchone()[0]

    cur.close()
    pool.release(conn)
    return count > 0


def get_articles_for_briefing(mode):
    conn = get_conn()
    cur = conn.cursor()

    if mode == "oneday":
        sql = """
            SELECT SUMMARY, SUMMARY_AI, CONTENT
            FROM TB_ARTICLE
            WHERE TRUNC(WRITTEN_AT) = TRUNC(SYSDATE)
            ORDER BY WRITTEN_AT DESC
        """
    else:
        sql = """
            SELECT SUMMARY, SUMMARY_AI, CONTENT
            FROM TB_ARTICLE
            WHERE WRITTEN_AT >= (TRUNC(SYSDATE) - 4)
            ORDER BY WRITTEN_AT DESC
        """

    cur.execute(sql)
    rows = cur.fetchall()

    result = []

    for summary, summary_ai, content in rows:
        def read_clob(val):
            if val is None:
                return ""
            if hasattr(val, "read"):
                try:
                    return val.read()
                except:
                    return str(val)
            return str(val)

        s = read_clob(summary)
        s_ai = read_clob(summary_ai)
        c = read_clob(content)

        if s_ai and s_ai.strip():
            result.append(s_ai.strip())
        elif s and s.strip():
            result.append(s.strip())
        else:
            result.append(c.replace("\n", " ")[:200])

    cur.close()
    pool.release(conn)
    return result


def get_latest_briefing_content(mode):
    conn = get_conn()
    cur = conn.cursor()

    sql = """
        SELECT CONTENT
        FROM TB_BRIEFING_LOG
        WHERE BRIEFING_MODE = :m
        ORDER BY BRIEFING_DATE DESC
    """

    cur.execute(sql, {"m": mode})
    row = cur.fetchone()

    if not row:
        cur.close()
        pool.release(conn)
        return None

    clob = row[0]
    try:
        text = clob.read()
    except:
        text = str(clob)

    cur.close()
    pool.release(conn)
    return text


def insert_briefing(mode, date_unused, content):
    conn = get_conn()
    cur = conn.cursor()

    sql = """
        INSERT INTO TB_BRIEFING_LOG (
            BRIEFING_MODE, BRIEFING_DATE, CONTENT
        )
        VALUES (:m, CURRENT_TIMESTAMP, :c)
    """

    cur.execute(sql, {"m": mode, "c": content})
    conn.commit()

    cur.close()
    pool.release(conn)
