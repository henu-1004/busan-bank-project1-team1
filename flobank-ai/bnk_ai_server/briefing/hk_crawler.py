# hk_crawler.py
import requests, time, traceback, re
from datetime import datetime, timedelta
from bs4 import BeautifulSoup
from db import insert_article, get_existing_urls, exists_url


HEADERS = {"User-Agent": "Mozilla/5.0"}

HK_SECTIONS = {
    "macro": "https://www.hankyung.com/economy/macro",
    "forex": "https://www.hankyung.com/economy/forex",
}


# -------------------------------------
# 에러 로그 기록
# -------------------------------------
def log_error(msg):
    with open("hk_error.log", "a", encoding="utf-8") as f:
        f.write(f"[{datetime.now()}] {msg}\n")
        f.write(traceback.format_exc() + "\n\n")


# -------------------------------------
# 리스트 날짜 파싱(1차 필터)
# -------------------------------------
def parse_date_preview(date_str):
    try:
        if " " in date_str:
            return datetime.strptime(date_str.strip(), "%Y.%m.%d %H:%M")
        else:
            return datetime.strptime(date_str.strip(), "%Y.%m.%d")
    except:
        return None


# -------------------------------------
# 본문 정리
# -------------------------------------
def clean_hk_content(text):
    text = re.sub(r".*기자.*", "", text)
    text = re.sub(r"\S+@hankyung\.com", "", text)
    text = re.sub(r"ⓒ.*", "", text)
    lines = [line.strip() for line in text.split("\n")]
    return "\n".join([l for l in lines if l])


# -------------------------------------
# 상세 페이지 파싱
# -------------------------------------
def hk_parse_article(url):
    try:
        res = requests.get(url, headers=HEADERS)
        soup = BeautifulSoup(res.text, "html.parser")

        # 입력 시간
        dt_real = None
        dt_tags = soup.select("div.datetime span.item .txt-date")
        if dt_tags:
            dt_str = dt_tags[0].get_text(strip=True)
            try:
                dt_real = datetime.strptime(dt_str, "%Y.%m.%d %H:%M")
            except:
                pass

        # 요약
        summary_tag = soup.select_one("div.summary")
        summary = summary_tag.get_text(strip=True) if summary_tag else ""

        # 본문
        body_tag = soup.select_one("div.article-body")
        content = body_tag.get_text("\n", strip=True) if body_tag else ""
        content = clean_hk_content(content)

        return dt_real, summary, content

    except:
        log_error(f"HK 상세 실패: {url}")
        return None, "", ""


# -------------------------------------
# 리스트 페이지 수집
# -------------------------------------
def hk_fetch_list(url):
    try:
        res = requests.get(url, headers=HEADERS)
        soup = BeautifulSoup(res.text, "html.parser")

        items = []
        for li in soup.select("ul.news-list li"):
            a = li.find("a")
            if not a:
                continue

            title = a.get_text(strip=True)
            link = a["href"]
            if not link.startswith("http"):
                link = "https://www.hankyung.com" + link

            date_tag = li.select_one("p.txt-date")
            if not date_tag:
                continue
            date_text = date_tag.get_text(strip=True)

            items.append((title, link, date_text))

        next_btn = soup.select_one("a.btn-next")
        next_page = next_btn["href"] if next_btn else None
        if next_page and not next_page.startswith("http"):
            next_page = "https://www.hankyung.com" + next_page

        return items, next_page

    except:
        log_error(f"HK LIST 실패: {url}")
        return [], None


# -------------------------------------
# 메인 크롤러
# -------------------------------------
def crawl_hk(section, mode="today"):
    base_url = HK_SECTIONS[section]
    now = datetime.now()

    today_str = now.strftime("%Y.%m.%d")
    limit_5d = now - timedelta(days=5)

    existing_urls = get_existing_urls()
    print(f"\n🔥 기존 URL {len(existing_urls)}개 로딩됨\n")

    page_url = base_url

    while True:

        page_list, next_page = hk_fetch_list(page_url)

        for title, url, date_text in page_list:

            if url in existing_urls:
                print("⏩ DB 스킵:", title)
                continue

            # -------------------------------
            # 1차 필터 (리스트 기준)
            # -------------------------------
            dt_preview = parse_date_preview(date_text)
            if not dt_preview:
                continue

            if mode == "today":
                if dt_preview.strftime("%Y.%m.%d") != today_str:
                    continue
            else:  # recent5
                if dt_preview < limit_5d:
                    print("⛔ HK: 최근5일 이전 기사 → STOP")
                    return

            # -------------------------------
            # 상세 페이지 파싱
            # -------------------------------
            print("🆕 HK 상세:", title)
            dt_real, summary, content = hk_parse_article(url)

            if not dt_real:
                continue

            # -------------------------------
            # 상세 날짜 기준 2차 필터 + STOP
            # -------------------------------
            if mode == "today":
                if dt_real.strftime("%Y.%m.%d") != today_str:
                    continue
            else:
                if dt_real < limit_5d:
                    print("⛔ HK 상세: 최근5일 이전 기사 → STOP")
                    return

            # -------------------------------
            # DB 저장
            # -------------------------------
            insert_article(
                company="HK",
                category=section,
                title=title,
                url=url,
                written_at=dt_real,
                summary=summary,
                content=content,
            )

            existing_urls.add(url)
            time.sleep(0.2)

        if not next_page:
            break

        page_url = next_page
