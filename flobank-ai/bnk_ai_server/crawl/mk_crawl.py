# ===========================
#        mk_crawler.py
# ===========================
import requests
from bs4 import BeautifulSoup
from datetime import datetime, timedelta
import json, os, time, traceback

HEADERS = {"User-Agent": "Mozilla/5.0"}

# ----------------------------------------
# 공통 에러 로그
# ----------------------------------------
def log_error(msg):
    with open("error.log", "a", encoding="utf-8") as f:
        f.write(f"[{datetime.now()}] {msg}\n")
        f.write(traceback.format_exc() + "\n\n")


# ----------------------------------------
# JSON 로드
# ----------------------------------------
def load_existing(filename):
    if not os.path.exists(filename):
        return [], set()

    try:
        with open(filename, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data, {x["url"] for x in data}
    except:
        log_error(f"MK JSON 로딩 실패: {filename}")
        return [], set()


# ----------------------------------------
# 리스트 페이지 파싱
# ----------------------------------------
def mk_fetch_list(url):
    try:
        res = requests.get(url, headers=HEADERS)
        soup = BeautifulSoup(res.text, "html.parser")

        items = soup.select("a.news_item")
        results = []

        for i in items:
            title_tag = i.select_one("h3.news_ttl")
            if not title_tag:
                continue

            title = title_tag.get_text(strip=True)
            link = i["href"]
            if not link.startswith("http"):
                link = "https://www.mk.co.kr" + link

            results.append((title, link))

        next_btn = soup.select_one("a.btn_next")
        next_page = next_btn["href"] if next_btn else None

        return results, next_page

    except:
        log_error(f"MK 리스트 실패: {url}")
        return [], None


# ----------------------------------------
# 상세 페이지 파싱
# ----------------------------------------
def mk_parse_article(url):
    try:
        res = requests.get(url, headers=HEADERS)
        soup = BeautifulSoup(res.text, "html.parser")

        # 날짜
        dt_tag = soup.select_one("dl.registration dd")
        if dt_tag:
            t = dt_tag.get_text(strip=True)
            try:
                dt = datetime.strptime(t, "%Y-%m-%d %H:%M:%S")
            except:
                dt = None
        else:
            dt = None

        # 요약 (mid title)
        summary_tag = soup.select_one("div.midtitle_text")
        summary = summary_tag.get_text(" ", strip=True) if summary_tag else ""

        # 본문
        parent = soup.select_one("div.news_cnt_detail_wrap")
        content_list = []

        if parent:
            for p in parent.select("p"):
                tx = p.get_text(" ", strip=True)
                if tx:
                    content_list.append(tx)

        content = "\n".join(content_list)

        return dt, summary, content

    except:
        log_error(f"MK 상세 실패: {url}")
        return None, "", ""


# ----------------------------------------
# MK 크롤 함수
# ----------------------------------------
def crawl_mk(section, base_url, mode="today"):
    now = datetime.now()
    today_str = now.strftime("%Y-%m-%d")
    limit_dt = now - timedelta(days=5)

    # 🔥 저장 파일명 변경 (today → oneday)
    save_mode = "oneday" if mode == "today" else "recent5"
    filename = f"mk_{section}_{save_mode}_{now.strftime('%Y%m%d')}.json"

    existing, existing_urls = load_existing(filename)
    print(f"\n[MK-{section}-{mode}] 기존 {len(existing_urls)}개")

    new_list = []
    page_url = base_url

    while True:
        page_list, next_link = mk_fetch_list(page_url)

        for title, url in page_list:
            if url in existing_urls:
                print("⏩ MK 스킵:", title)
                continue

            date_dt, summary, content = mk_parse_article(url)
            if not date_dt:
                continue

            # 🔥 today 필터링은 그대로 유지
            if mode == "today":
                if date_dt.strftime("%Y-%m-%d") != today_str:
                    continue
            else:
                if date_dt < limit_dt:
                    continue

            print("🆕 MK 신규:", title)

            new_list.append({
                "title": title,
                "date": date_dt.strftime("%Y-%m-%d %H:%M:%S"),
                "url": url,
                "summary": summary,
                "content": content
            })

            existing_urls.add(url)
            time.sleep(0.2)

        if not next_link:
            break
        page_url = next_link

    total = existing + new_list
    with open(filename, "w", encoding="utf-8") as f:
        json.dump(total, f, ensure_ascii=False, indent=4)

    return total


# ----------------------------------------
# 실행
# ----------------------------------------
if __name__ == "__main__":
    MK_TRADE = "https://www.mk.co.kr/news/economy/trade/"
    MK_FX = "https://www.mk.co.kr/news/economy/foreign-exchange/"

    crawl_mk("trade", MK_TRADE, "today")
    crawl_mk("trade", MK_TRADE, "recent5")

    crawl_mk("fx", MK_FX, "today")
    crawl_mk("fx", MK_FX, "recent5")
