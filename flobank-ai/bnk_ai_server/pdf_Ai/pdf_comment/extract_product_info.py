# ================================================
#   extract_product_info.py
#   PDF/TXT 복원 결과 → 상품 정보 구조화 추출
# ================================================

import json
from openai import OpenAI

client = OpenAI()

# ------------------------------------------------
# 1) LLM Prompt 생성
# ------------------------------------------------
def build_prompt(text: str):
    return f"""
당신의 임무는 PDF에서 추출된 비정형 텍스트에서
아래 상품 입력폼 정보를 정확하게 추출하는 것입니다.

⚠ 절대 규칙:
- 텍스트에 실제로 존재하는 정보만 사용한다.
- 추측 금지 / 과장 금지 / 원문에 없는 정보 추가 금지.
- 텍스트에 없으면 null로 넣는다.
- 단, description_short(한 줄 요약)만 예외로 '원문 기반 요약(15자 이내)'을 생성해도 된다.
- JSON 외의 설명, Markdown 금지.
- currencies는 ["USD", "JPY"] 같은 코드 배열.
- 선택지 항목 중 단일 선택 구조(예: 예금유형, 가입기간유형, 분할인출, 자동연장, 가입대상)는 반드시 하나만 선택.
- exchange_rate_basis는 항상 "납입시환율"로 고정.

------------------------------------------------------------
[원문]
{text}
------------------------------------------------------------

출력 JSON 스키마는 다음과 같아야 한다:
{{
  "product_name": "",
  "description_short": "",
  "product_features": "",
  "deposit_type": "",
  "currencies": [],
  "exchange_rate_policy": "납입시환율",
  "subscription_period_type": "",
  "min_month": null,
  "max_month": null,
  "eligibility": "",
  "partial_withdrawal": "",
  "auto_renewal": "",
  "additional_deposit": ""
}}
"""


# ------------------------------------------------
# 2) LLM 호출
# ------------------------------------------------
def extract_product_info(raw_text: str) -> dict:

    prompt = build_prompt(raw_text)

    resp = client.chat.completions.create(
        model="gpt-4o-mini",
        temperature=0,
        messages=[
            {"role": "system", "content": "너는 금융상품 정보를 구조화하는 전문 분석가이다."},
            {"role": "user", "content": prompt},
        ],
    )

    output = resp.choices[0].message.content.strip()

    try:
        return json.loads(output)
    except json.JSONDecodeError:
        print("❌ JSON 파싱 실패. LLM 응답:")
        print(output)
        raise ValueError("LLM output is not valid JSON.")


# ------------------------------------------------
# 3) TXT 로딩
# ------------------------------------------------
def load_text(txt_path: str) -> str:
    with open(txt_path, "r", encoding="utf-8") as f:
        return f.read()
