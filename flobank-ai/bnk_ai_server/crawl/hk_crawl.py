import requests
from bs4 import BeautifulSoup
from datetime import datetime, timedelta
import json
import time
import os
import traceback
import re

HEADERS = {"User-Agent": "Mozilla/5.0"}


def log_error(msg):
    with open("hk_error.log", "a", encoding="utf-8") as f:
        f.write(f"[{datetime.now()}] {msg}\n")
        f.write(traceback.format_exc() + "\n\n")


def load_existing(filename):
    if not os.path.exists(filename):
        return [], set()
    try:
        with open(filename, "r", encoding="utf-8") as f:
            data = json.load(f)
        return data, {item["url"] for item in data}
    except:
        log_error(f"JSON 로딩 실패: {filename}")
        return [], set()


def hk_parse_date(date_str):
    return datetime.strptime(date_str.strip(), "%Y.%m.%d %H:%M")


def clean_hk_content(text):
    if not text:
        return text

    text = re.sub(r".*기자.*", "", text)
    text = re.sub(r"\S+@hankyung\.com", "", text)
    text = re.sub(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}", "", text)
    text = re.sub(r"ⓒ.*", "", text)

    lines = [line.strip() for line in text.split("\n")]
    return "\n".join([l for l in lines if l]).strip()


def hk_parse_article(url):
    try:
        res = requests.get(url, headers=HEADERS)
        soup = BeautifulSoup(res.text, "html.parser")

        summary_tag = soup.select_one("div.summary")
        summary = summary_tag.get_text(strip=True) if summary_tag else ""

        body_tag = soup.select_one("div.article-body")
        content = body_tag.get_text("\n", strip=True) if body_tag else ""
        content = clean_hk_content(content)

        return summary, content
    except:
        log_error(f"HK 상세 파싱 실패: {url}")
        return "", ""


def hk_fetch_list(url):
    try:
        res = requests.get(url, headers=HEADERS)
        soup = BeautifulSoup(res.text, "html.parser")

        items = soup.select("ul.news-list li")
        results = []

        for li in items:
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
            results.append((title, link, date_text))

        next_btn = soup.select_one("a.btn-next")
        next_page = next_btn["href"] if next_btn else None

        return results, next_page
    except:
        log_error(f"HK 리스트 파싱 실패: {url}")
        return [], None


HK_SECTIONS = {
    "macro": "https://www.hankyung.com/economy/macro",
    "forex": "https://www.hankyung.com/economy/forex"
}


def crawl_hk(section_key, mode="today"):
    base_url = HK_SECTIONS[section_key]

    now = datetime.now()
    today_str = now.strftime("%Y.%m.%d")
    limit_5d = now - timedelta(days=5)

    # 🔥 저장 파일명만 변경
    save_mode = "oneday" if mode == "today" else "recent5"
    filename = f"hk_{section_key}_{save_mode}_{now.strftime('%Y%m%d')}.json"

    existing, existing_urls = load_existing(filename)
    print(f"\n[HK-{section_key}-{mode}] 기존 {len(existing_urls)}개")

    new_list = []
    page_url = base_url

    while True:
        page_list, next_link = hk_fetch_list(page_url)

        for title, url, date_text in page_list:

            if mode == "today":
                if not date_text.startswith(today_str):
                    continue
            else:
                try:
                    dt = hk_parse_date(date_text)
                    if dt < limit_5d:
                        continue
                except:
                    continue

            if url in existing_urls:
                print("⏩ HK 스킵:", title)
                continue

            print("🆕 HK 신규:", title)
            summary, content = hk_parse_article(url)

            new_list.append({
                "title": title,
                "date": date_text,
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
        json.dump(total, f, indent=4, ensure_ascii=False)

    return total


if __name__ == "__main__":
    crawl_hk("macro", "today")
    crawl_hk("macro", "recent5")
    crawl_hk("forex", "today")
    crawl_hk("forex", "recent5")
