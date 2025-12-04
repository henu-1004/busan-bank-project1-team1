# mk_crawler.py
import requests
import time
from bs4 import BeautifulSoup
from datetime import datetime, timedelta
from db import insert_article, get_existing_urls

HEADERS = {"User-Agent": "Mozilla/5.0"}


# ---------------------------------------------------
# 리스트 페이지 파싱 (정확한 #list_area 영역만)
# ---------------------------------------------------
def mk_fetch_list(url):
    res = requests.get(url, headers=HEADERS)
    soup = BeautifulSoup(res.text, "html.parser")

    results = []

    # 🔥 원하는 기사 리스트: #list_area 내부만!
    container = soup.select_one("#list_area")
    if not container:
        return [], None

    for a in container.select("a.news_item"):
        title_tag = a.select_one("h3.news_ttl")
        if not title_tag:
            continue

        title = title_tag.get_text(strip=True)

        link = a.get("href")
        if not link.startswith("http"):
            link = "https://www.mk.co.kr" + link

        # 날짜 텍스트
        date_tag = a.select_one(".time_info")
        date_text = date_tag.get_text(strip=True) if date_tag else ""

        results.append((title, link, date_text))

    # 🔥 다음 페이지(“더보기”) 버튼 → 동적 API 기반
    btn = soup.select_one("button.drop_sub_news_btn")
    next_page = None
    if btn:
        api_input = soup.select_one(btn.get("data-source-selector"))
        if api_input:
            api_value = api_input.get("value")  # //www.mk.co.kr/_CP/42
            if api_value:
                next_page = "https:" + api_value

    return results, next_page


# ---------------------------------------------------
# 리스트 날짜 해석 (절대 → datetime)
# ---------------------------------------------------
def parse_mk_list_date(date_text):
    date_text = date_text.strip()

    # 절대시간
    for fmt in ["%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M"]:
        try:
            return datetime.strptime(date_text, fmt)
        except:
            continue

    # 상대시간 ("2시간 전") → 상세 필요
    if "전" in date_text:
        return None

    return None


# ---------------------------------------------------
# 상세 페이지 파싱
# ---------------------------------------------------
def mk_parse_detail(url):
    res = requests.get(url, headers=HEADERS)
    soup = BeautifulSoup(res.text, "html.parser")

    # 날짜 파싱
    dt_real = None
    dt_tag = soup.select_one("dl.registration dd")
    if dt_tag:
        dt_text = dt_tag.get_text(strip=True)
        for fmt in ["%Y-%m-%d %H:%M:%S", "%Y-%m-%d %H:%M"]:
            try:
                dt_real = datetime.strptime(dt_text, fmt)
                break
            except:
                pass

    # 요약
    summary_tag = soup.select_one("div.midtitle_text")
    summary = summary_tag.get_text(" ", strip=True) if summary_tag else ""

    # 본문 파싱
    parent = soup.select_one("div.news_cnt_detail_wrap")
    if parent:
        content = "\n".join(
            [p.get_text(" ", strip=True) for p in parent.select("p") if p.get_text(strip=True)]
        )
    else:
        content = ""

    return dt_real, summary, content


# ---------------------------------------------------
# MK 크롤러 실행
# ---------------------------------------------------
def crawl_mk(section, base_url, mode="oneday"):
    now = datetime.now()
    today_str = now.strftime("%Y-%m-%d")
    limit_dt = now - timedelta(days=5)

    existing_urls = get_existing_urls()
    page_url = base_url

    while True:
        page_list, next_page = mk_fetch_list(page_url)

        for title, url, date_text in page_list:

            if url in existing_urls:
                print("⏩ skip:", title)
                continue

            dt_preview = parse_mk_list_date(date_text)
            need_detail = dt_preview is None

            # 리스트에서 절대 시간 존재 + 최근5 STOP
            if not need_detail and mode == "recent5":
                if dt_preview < limit_dt:
                    print("⛔ MK: 리스트에서 최근5일 이전 기사 → STOP")
                    return

            # 상세 요청
            print("🆕 MK 상세:", title)
            dt_real, summary, content = mk_parse_detail(url)
            if not dt_real:
                continue

            # 상세 STOP
            if mode == "oneday":
                if dt_real.strftime("%Y-%m-%d") != today_str:
                    continue
            else:
                if dt_real < limit_dt:
                    print("⛔ MK 상세: 최근5일 이전 기사 → STOP")
                    return

            insert_article(
                company="MK",
                category=section,
                title=title,
                url=url,
                written_at=dt_real,
                summary=summary,
                content=content,
            )

            existing_urls.add(url)
            time.sleep(0.3)

        # 다음 페이지 없으면 종료
        if not next_page:
            break

        page_url = next_page
