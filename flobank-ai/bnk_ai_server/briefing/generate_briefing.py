# ============================================
#     generate_briefing.py (DB 기반 브리핑)
# ============================================

import os
from datetime import datetime, timedelta
from openai import OpenAI

from db import (
    briefing_exists_today,
    get_last_briefing_time,
    has_article_after,
    get_articles_for_briefing,
    insert_briefing,
    get_latest_briefing_content
)

# ----------------------------------------
# GPT 연결
# ----------------------------------------
MODEL = "gpt-4.1-mini"


# ----------------------------------------
# 프롬프트 생성
# ----------------------------------------
def build_prompt(mode):
    today = datetime.now()

    if mode == "oneday":
        title = today.strftime("%Y년 %m월 %d일 경제 브리핑")

    else:  # recent5
        start = (today - timedelta(days=4)).strftime("%Y년 %m월 %d일")
        end = today.strftime("%Y년 %m월 %d일")
        title = f"{start}~{end} 최근 5일 경제 브리핑"

    return f"""
당신은 경제 전문 애널리스트입니다.
여러 기사(summary + summary_ai)를 바탕으로
‘{title}’ 내용을 작성하세요.

요약 규칙:
- **"{title}"절대 출력 금지**
- 총 7줄의 분석형 브리핑 작성
- 각 문장은 12~20자 내외
- 단순 기사 나열 금지, 원인·영향·맥락 중심
- 경제 지표 흐름과 인과 관계 중심
- 기사 문장 복사 금지, 반드시 새 문장
- 종결어미는 ‘명사형 종결’만 사용하고 모든 단정형 어미는 사용 금지
- 각 문장은 엔터(\n)로 구분해라 
"""


# ----------------------------------------
# GPT 브리핑 생성
# ----------------------------------------
def generate_briefing_text(mode, articles):
    prompt = build_prompt(mode)
    joined = "\n\n---\n\n".join(articles)

    response = client.chat.completions.create(
        model=MODEL,
        messages=[
            {"role": "system", "content": prompt},
            {"role": "user", "content": joined}
        ],
        temperature=0.4
    )

    return response.choices[0].message.content.strip(), response.usage


# ----------------------------------------
# 토큰/비용 로그
# ----------------------------------------
PRICING = {
    "gpt-4.1-mini": {
        "prompt": 0.15 / 1_000_000,
        "completion": 0.60 / 1_000_000,
    }
}

def log_usage(model, usage):
    prompt_cost = usage.prompt_tokens * PRICING[model]["prompt"]
    comp_cost = usage.completion_tokens * PRICING[model]["completion"]
    total = prompt_cost + comp_cost

    path = f"token_usage_{datetime.now().strftime('%Y%m%d')}.log"
    with open(path, "a", encoding="utf-8") as f:
        f.write(
            f"[{datetime.now()}] model={model}\n"
            f"prompt={usage.prompt_tokens}, completion={usage.completion_tokens}, total={usage.total_tokens}\n"
            f"cost_prompt=${prompt_cost:.6f}, cost_comp=${comp_cost:.6f}, cost_total=${total:.6f}\n"
            f"--------------------------------------------------\n"
        )
    return total


# ----------------------------------------
# 브리핑 생성 전체 로직
# ----------------------------------------
def generate_briefing(mode):

    print(f"\n===== 시작: mode={mode} =====")

    # 1) 오늘 브리핑이 없다 → 무조건 새로 생성
    if not briefing_exists_today(mode):
        print("🆕 오늘 브리핑 없음 → 새로 생성")

        articles = get_articles_for_briefing(mode)
        print(f"🔍 기사 {len(articles)}개 불러옴")

        if len(articles) == 0:
            print("❌ 브리핑용 기사 없음")
            return None

        text, usage = generate_briefing_text(mode, articles)
        log_usage(MODEL, usage)
        insert_briefing(mode, None, text)

        print("✨ 브리핑 생성 완료!")
        return text

    # 2) 이미 오늘 브리핑 있음 → last_dt 조회
    last_dt = get_last_briefing_time(mode)
    print(f"📌 마지막 생성 시각: {last_dt}")

    # 3) last_dt 이후 새 기사 있는지 체크
    if not has_article_after(last_dt):
        print("⏩ 새 기사 없음 → 기존 브리핑 재사용")

        existing = get_latest_briefing_content(mode)
        return existing

    # 4) 새 기사 있음 → 새로 생성
    print("🆕 새 기사 발견 → 새 브리핑 생성")

    articles = get_articles_for_briefing(mode)
    print(f"🔍 기사 {len(articles)}개 불러옴")

    if len(articles) == 0:
        print("❌ 브리핑용 기사 없음")
        return None

    text, usage = generate_briefing_text(mode, articles)
    log_usage(MODEL, usage)
    insert_briefing(mode, None, text)

    print("✨ 새로운 브리핑 생성 완료!")
    return text


# ----------------------------------------
# 실행
# ----------------------------------------
if __name__ == "__main__":
    oneday = generate_briefing("oneday")
    print("\n===== [oneday 브리핑 결과] =====\n")
    print(oneday)

    recent5 = generate_briefing("recent5")
    print("\n===== [recent5 브리핑 결과] =====\n")
    print(recent5)
