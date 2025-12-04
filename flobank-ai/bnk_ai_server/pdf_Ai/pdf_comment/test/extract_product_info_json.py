# ================================================
#   extract_product_info.py
#   PDF → TXT 입력에서 상품 정보 구조화 추출
# ================================================

import json
from openai import OpenAI

client = OpenAI()

# ------------------------------------------------
# 1) 추출 프롬프트 생성
# ------------------------------------------------
def build_prompt(text: str):
    return f"""
당신의 임무는 PDF에서 추출된 비정형 텍스트에서
아래 상품 입력폼 정보만 정확하게 추출하는 것입니다.

⚠ 절대 규칙:
- 텍스트에 "실제로 존재하는 정보만" 사용하며, 추측하거나 생성 금지.
- 텍스트에 없으면 null로 넣는다.
- JSON 이외의 설명, Markdown, 주석 금지.
- 통화(currencies)는 여러 개 선택 가능하며, USD 등 코드만 배열로 추출.
- 선택지 항목 중 단일 선택 구조(예: 예금유형, 가입기간유형, 분할인출, 자동연장, 가입대상)는 반드시 하나만 선택.
- exchange_rate_basis는 텍스트 여부와 관계없이 항상 "납입시환율"로 설정.

------------------------------------------------------------
[원문]
{text}
------------------------------------------------------------

아래 JSON 스키마대로 출력:

{{
  "product_name": "",
  "description_short": "",
  "deposit_type": "",
  "currencies": [],
  "exchange_rate_basis": "납입시환율",
  "product_overview": "",
  "subscription_period_type": "",
  "eligibility": "",
  "partial_withdrawal": "",
  "auto_renewal": "",
  "min_month": null,
  "max_month": null
}}
"""


# ------------------------------------------------
# 2) LLM 호출
# ------------------------------------------------
def extract_product_info(raw_text: str):
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
        data = json.loads(output)
    except json.JSONDecodeError:
        print("❌ JSON 파싱 실패. LLM 출력:")
        print(output)
        raise

    return data


# ------------------------------------------------
# 3) 파일 로딩
# ------------------------------------------------
def load_text(txt_path: str) -> str:
    with open(txt_path, "r", encoding="utf-8") as f:
        return f.read()


# ------------------------------------------------
# 4) 단독 실행 테스트
# ------------------------------------------------
if __name__ == "__main__":
    INPUT = "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/test/pdf_temp/FLOBANK 더 와이드 상품설명서.txt"

    print("📄 텍스트 로드 중...")
    text = load_text(INPUT)

    print("🧠 상품 정보 추출 중...")
    info = extract_product_info(text)

    print(json.dumps(info, ensure_ascii=False, indent=4))
