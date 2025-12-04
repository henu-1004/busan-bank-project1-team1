# extractor.py
import fitz  # PyMuPDF
import os

def extract_text_from_pdf(pdf_path: str) -> str:
    """
    PyMuPDF 기반 텍스트 추출 (상품설명서 최적화)
    """
    if not os.path.exists(pdf_path):
        raise FileNotFoundError(f"PDF 파일을 찾을 수 없음: {pdf_path}")

    print(f"📄 PyMuPDF 텍스트 추출 시작: {pdf_path}")

    try:
        doc = fitz.open(pdf_path)
    except Exception as e:
        print(f"❌ PDF 열기 실패: {e}")
        raise e

    text_parts = []

    try:
        for page in doc:
            page_text = page.get_text("text")
            text_parts.append(page_text)

        full_text = "\n".join(text_parts)

    except Exception as e:
        print(f"❌ 텍스트 추출 오류: {e}")
        raise e

    finally:
        doc.close()

    print(f"✅ 텍스트 추출 완료 (길이: {len(full_text)}자)")
    return full_text


def extract_and_save(pdf_path: str, save_dir="pdf_temp"):
    """
    텍스트 추출 후 txt 파일로 저장.
    pdf_temp/폴더 아래에 pdf와 동일한 이름으로 저장.
    """
    text = extract_text_from_pdf(pdf_path)

    os.makedirs(save_dir, exist_ok=True)

    base_name = os.path.basename(pdf_path).replace(".pdf", ".txt")
    save_path = os.path.join(save_dir, base_name)

    with open(save_path, "w", encoding="utf-8") as f:
        f.write(text)

    print(f"💾 텍스트 파일 저장 완료 → {save_path}")
    return save_path


# ===============================================
# ⭐ main.py 호환용 pdf_to_txt() 래퍼 함수
# ===============================================
def pdf_to_txt(pdf_path: str, save_dir="pdf_temp") -> str:
    """
    main.py에서 그대로 사용하기 위한 래퍼 함수
    """
    return extract_and_save(pdf_path, save_dir)
