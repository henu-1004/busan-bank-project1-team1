# fxreg_preprocess.py
# 외국환거래규정(기획재정부고시)(제2021-22호).txt
# 전처리 자동 수행 스크립트

import re

INPUT_FILE = "금융소비자 보호에 관한 법률(법률)(제20305호)(20240814) (1).txt"
OUTPUT_FILE = "금융소비자 보호에 관한 법률(법률)(제20305호)(20240814) (1).txt_cleaned.txt"


def preprocess_fxreg(text: str):
    lines = text.split("\n")
    cleaned = []

    for line in lines:
        # RULE 1: 법제처/페이지 정보 삭제
        if re.match(r"^법제처$", line.strip()):
            continue
        if re.match(r"^국가법령정보센터$", line.strip()):
            continue
        if re.match(r"^- \d+ ?/ ?\d+ -$", line.strip()):
            continue
        
        cleaned.append(line)
    
    text = "\n".join(cleaned)

    # RULE 2: <...> 내부 개행 제거
    text = re.sub(
        r"<[^>]*>",
        lambda m: m.group(0).replace("\n", ""),
        text
    )

    # RULE 5-1: 한글 + 개행 + 한글 → 붙이기
    text = re.sub(r"([가-힣])\n([가-힣])", r"\1\2", text)

    # RULE 5-2: (문장 중간 개행) 한글/숫자 + 개행 + 한글 → 공백
    text = re.sub(r"([가-힣0-9])\n([가-힣])", r"\1 \2", text)

    # RULE 6: 다중 개행 축소
    text = re.sub(r"\n{2,}", "\n\n", text)

    # RULE 7: 중복 공백 제거
    text = re.sub(r"[ ]{2,}", " ", text)

    return text.strip()


def main():
    print("📄 입력 파일 읽는 중:", INPUT_FILE)
    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        raw_text = f.read()

    print("⚙️ 전처리 수행 중...")
    cleaned = preprocess_fxreg(raw_text)

    print("💾 결과 저장:", OUTPUT_FILE)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(cleaned)

    print("\n🎉 완료되었습니다!")
    print(f"➡️ 출력 파일: {OUTPUT_FILE}")


if __name__ == "__main__":
    main()
