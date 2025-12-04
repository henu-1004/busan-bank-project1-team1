import os
from extractor import pdf_to_txt

# PDF가 있는 폴더 경로
PDF_DIR = "/home/g5223sho/bnk_ai_server/auto_qna/data/상품설명서"

# TXT 저장 폴더 (원하면 경로 수정 가능)
SAVE_DIR = "/home/g5223sho/bnk_ai_server/auto_qna/data/txt"


def batch_extract(pdf_dir=PDF_DIR, save_dir=SAVE_DIR):
    if not os.path.exists(pdf_dir):
        raise FileNotFoundError(f"📁 경로가 존재하지 않음: {pdf_dir}")

    os.makedirs(save_dir, exist_ok=True)

    files = os.listdir(pdf_dir)
    pdf_files = [f for f in files if f.lower().endswith(".pdf")]

    print(f"🔍 총 {len(pdf_files)}개의 PDF 발견")

    for i, pdf_file in enumerate(pdf_files, start=1):
        pdf_path = os.path.join(pdf_dir, pdf_file)
        print(f"\n[{i}/{len(pdf_files)}] ▶ {pdf_file} 변환 중...")

        try:
            txt_path = pdf_to_txt(pdf_path, save_dir)
            print(f"✅ 완료: {txt_path}")
        except Exception as e:
            print(f"❌ 오류 발생 ({pdf_file}): {e}")


if __name__ == "__main__":
    batch_extract()
