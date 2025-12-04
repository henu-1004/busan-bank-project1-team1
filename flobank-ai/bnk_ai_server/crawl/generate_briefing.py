# ============================================
#     generate_briefing.py (오늘의 브리핑)
# ============================================

import os
import json
from datetime import datetime, timedelta
from openai import OpenAI

# -------------------------------------------------
# 🔐 GPT API 연결
# -------------------------------------------------


# -------------------------------------------------
# 💰 모델 단가 설정
# -------------------------------------------------
MODEL_PRICING = {
    "gpt-4.1-mini": {
        "prompt": 0.15 / 1_000_000,
        "completion": 0.60 / 1_000_000
    }
}

def calc_cost(model, prompt_tokens, completion_tokens):
    p = MODEL_PRICING.get(model)
    if not p:
        return 0, 0, 0

    cost_prompt = prompt_tokens * p["prompt"]
    cost_completion = completion_tokens * p["completion"]
    total = cost_prompt + cost_completion
    return cost_prompt, cost_completion, total


# -------------------------------------------------
# 날짜 기반 제목 생성
# -------------------------------------------------
def get_briefing_title(mode):
    today = datetime.now()

    if mode == "oneday":
        return today.strftime("%Y년 %m월 %d일 경제 브리핑")

    elif mode == "recent5":
        start = (today - timedelta(days=4)).strftime("%Y년 %m월 %d일")
        end = today.strftime("%Y년 %m월 %d일")
        return f"{start}~{end} 최근 5일 경제 브리핑"

    return "경제 브리핑"


# -------------------------------------------------
# mode에 맞춘 프롬프트 생성
# -------------------------------------------------
def build_prompt(mode):
    title = get_briefing_title(mode)
    return f"""
당신은 경제 전문 애널리스트입니다.
여러 기사(summary + summary_ai)를 바탕으로
‘{title}’ 내용을 작성하세요.
요약 규칙:
- 총 7줄로 구성된 분석형 헤드라인 브리핑 작성.
- 각 문장은 12~20자 내외로 간결하게.
- 단순 사건 나열 금지. 반드시 ‘원인·영향·맥락’ 포함.
- 하루치 기사처럼 정보가 적어도 내용 밀도를 높여 작성.
- 경제 흐름·정책·지표를 중심으로 인과관계 위주 재구성.
- 기사 문장 그대로 복사 금지. 완전히 새 문장으로 재작성.
- 종결어미: “명사형 종결(흐름, 압력, 전망, 확대 등)”만 사용.
- ‘~다, ~이다, ~고 있다, ~며, ~보이며’ 금지.
"""


# -------------------------------------------------
# summary + summary_ai 결합 로딩
# -------------------------------------------------
def load_summary_json(path):
    try:
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)

        combined_list = []
        for item in data:
            s1 = item.get("summary", "")
            s2 = item.get("summary_ai", "")
            combined_list.append((s1 + "\n" + s2).strip())

        return combined_list

    except Exception as e:
        print(f"❌ JSON 로딩 실패: {path} / {e}")
        return []


# -------------------------------------------------
# GPT 브리핑 생성 + 토큰/비용 계산 + 로그
# -------------------------------------------------
def generate_briefing(mode, text_list):

    model = "gpt-4.1-mini"
    prompt = build_prompt(mode)
    joined = "\n\n---\n\n".join(text_list)

    response = client.chat.completions.create(
        model=model,
        messages=[
            {"role": "system", "content": prompt},
            {"role": "user", "content": joined}
        ],
        temperature=0.4
    )

    result = response.choices[0].message.content.strip()

    # 사용량 & 비용 계산
    usage = response.usage
    prompt_tokens = usage.prompt_tokens
    completion_tokens = usage.completion_tokens
    total_tokens = usage.total_tokens

    cost_prompt, cost_completion, cost_total = calc_cost(
        model, prompt_tokens, completion_tokens
    )

    # 로그 저장
    log_path = f"token_usage_{datetime.now().strftime('%Y%m%d')}.log"
    with open(log_path, "a", encoding="utf-8") as f:
        f.write(
            f"[{datetime.now()}] model={model}\n"
            f"prompt_tokens={prompt_tokens}, completion_tokens={completion_tokens}, total_tokens={total_tokens}\n"
            f"prompt_cost=${cost_prompt:.6f}, completion_cost=${cost_completion:.6f}, total_cost=${cost_total:.6f}\n"
            f"------------------------------------------------------\n"
        )

    print("\n📌 토큰 사용량:", prompt_tokens, completion_tokens, total_tokens)
    print("💰 비용:", f"${cost_total:.6f}")

    return result


# -------------------------------------------------
# summary 파일 자동 선택
# -------------------------------------------------
def find_summary_files(mode, today):
    files = os.listdir("summaries/")
    result = [
        os.path.join("summaries", f)
        for f in files
        if mode in f and today in f and f.endswith("_summary.json")
    ]
    return result


# -------------------------------------------------
# 전체 파이프라인
# -------------------------------------------------
def generate_daily_briefing(mode="oneday"):
    today = datetime.now().strftime("%Y%m%d")

    print(f"\n📅 날짜: {today}")
    print(f"📌 모드: {mode}")

    summary_files = find_summary_files(mode, today)
    if not summary_files:
        print("❌ summary 파일 없음.")
        return

    print("\n📄 대상 summary 파일:")
    for f in summary_files:
        print(" -", f)

    all_texts = []
    for path in summary_files:
        all_texts.extend(load_summary_json(path))

    print(f"\n🔍 기사 {len(all_texts)}개 기반으로 브리핑 생성...")

    briefing = generate_briefing(mode, all_texts)

    # 저장
    os.makedirs("briefings", exist_ok=True)
    out_path = f"briefings/{mode}_briefing_{today}.txt"

    with open(out_path, "w", encoding="utf-8") as f:
        f.write(briefing)

    print("\n✨ 브리핑 생성 완료!")
    print("📁 저장:", out_path)
    print("\n===== 브리핑 내용 =====\n")
    print(briefing)


# -------------------------------------------------
# 실행
# -------------------------------------------------
if __name__ == "__main__":
    generate_daily_briefing("oneday")
    generate_daily_briefing("recent5")
