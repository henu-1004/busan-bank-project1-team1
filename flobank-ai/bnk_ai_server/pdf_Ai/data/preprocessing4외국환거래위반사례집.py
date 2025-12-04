import re

def fix_vertical_text(text: str) -> str:
    """
    세로로 나열된 한글(예: 유\n의\n사\n항)을 정상 단어로 합치는 함수
    """
    # 세로 한글 패턴: 한글 2~6글자가 줄바꿈으로 이어지는 구조
    pattern = r'((?:[가-힣]\s*\n\s*){2,6}[가-힣])'

    def join_vertical(match):
        block = match.group()
        # 줄바꿈 및 공백 제거 → 글자만 연결
        return re.sub(r'\s+', '', block)

    # 패턴을 연결된 단어로 변환
    fixed = re.sub(pattern, join_vertical, text)
    return fixed


def process_file(input_path="./txt/외국환거래법위반사례집.txt",
                 output_path="./txt/외국환거래법위반사례집_fix.txt"):
    """
    txt 파일을 불러서 세로 글자 문제를 자동 수정하고 새로운 파일로 저장하는 함수
    """
    # 원본 읽기
    with open(input_path, "r", encoding="utf-8") as f:
        original = f.read()

    # 세로 글자 교정
    fixed = fix_vertical_text(original)

    # 출력 저장
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(fixed)

    print(f"완료 ✅  → '{output_path}' 로 저장됨")


if __name__ == "__main__":
    process_file()
