import re

INPUT_FILE = "외국환거래업무취급지침_전국은행연합회외국환전문위원회.txt"
OUTPUT_FILE = INPUT_FILE.replace(".txt", "_cleaned.txt")


def clean_fx(text: str) -> str:
    lines = text.split("\n")
    cleaned = []
    buffer = ""

    # 페이지 번호 패턴: "- 3 -" / "-10-" 같은 것 제거
    def is_page_number(line):
        return re.match(r"^\s*-?\s*\d+\s*-\s*$", line) is not None

    # "제1 장", "제2 절" 등 장/절 제목
    def is_section_title(line):
        return bool(re.match(r"^\s*제\s*\d+\s*(장|절)", line))

    prev_was_title = False

    for line in lines:
        stripped = line.strip()

        # 1) 페이지 번호 제거
        if is_page_number(stripped):
            continue

        # 2) 장/절 제목 연속 중복 제거
        if is_section_title(stripped):
            if prev_was_title:
                continue
            prev_was_title = True
        else:
            prev_was_title = False

        # 3) 문장 중간 개행 제거
        if buffer:
            # 이전 줄 끝이 문장부호가 아니고, 다음 줄이 한글/숫자/영문으로 바로 시작 → 한 문장으로 이어붙임
            if (not re.search(r"[.?!]$", buffer.strip())
                and re.match(r"^[가-힣a-z0-9]", stripped)):
                buffer += " " + stripped
                continue
            else:
                cleaned.append(buffer)
                buffer = stripped
        else:
            buffer = stripped

    # 마지막 줄도 추가
    if buffer:
        cleaned.append(buffer)

    return "\n".join(cleaned)


def main():
    print("📄 입력 파일:", INPUT_FILE)

    with open(INPUT_FILE, "r", encoding="utf-8") as f:
        raw = f.read()

    print("⚙️ 전처리 중…")
    cleaned = clean_fx(raw)

    print("💾 저장:", OUTPUT_FILE)
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write(cleaned)

    print("\n🎉 완료되었습니다!")
    print("➡️ 출력 파일:", OUTPUT_FILE)


if __name__ == "__main__":
    main()
