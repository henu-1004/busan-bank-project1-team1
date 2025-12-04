# fxreg_preprocess_strict.py
# 외국환거래법위반사례집_fix.txt 같은 문서용

import re

def clean_text(text: str):
    lines = text.split("\n")
    cleaned = []

    for line in lines:
        s = line.strip()

        # RULE 1-1: "- 6 -" 같은 페이지 번호 제거
        if re.match(r"^-\s*\d+\s*-$", s):
            continue
        
        # RULE 1-2: 단독 숫자 줄 제거 (1~4자리)
        if re.match(r"^\d{1,4}$", s):
            continue

        cleaned.append(line)

    text = "\n".join(cleaned)

    # RULE 2-1: 한글+개행+한글 → 붙임
    text = re.sub(r"([가-힣])\n([가-힣])", r"\1\2", text)

    # RULE 2-2: 한글/숫자 + 개행 + 한글/숫자 → 공백
    text = re.sub(r"([가-힣0-9])\n([가-힣0-9])", r"\1 \2", text)

    # RULE 3: 3개 이상의 개행 → 2개
    text = re.sub(r"\n{3,}", "\n\n", text)

    # RULE 4: <...> 안의 개행 제거
    text = re.sub(
        r"<[^>]*>",
        lambda m: m.group(0).replace("\n", ""),
        text
    )

    return text.strip()



def main():
    INPUT = "외국환거래법위반사례집_fix.txt"
    OUTPUT = "외국환거래법위반사례집_cleaned.txt"

    print("📄 입력 파일:", INPUT)
    with open(INPUT, "r", encoding="utf-8") as f:
        raw = f.read()

    print("⚙ 전처리 수행 중...")
    cleaned = clean_text(raw)

    print("💾 저장:", OUTPUT)
    with open(OUTPUT, "w", encoding="utf-8") as f:
        f.write(cleaned)

    print("🎉 완료!")

if __name__ == "__main__":
    main()
