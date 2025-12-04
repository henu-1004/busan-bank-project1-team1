import requests
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))  # 현재 파일(pdf_comment 폴더) 절대경로
SAVE_DIR = os.path.join(BASE_DIR, "pdf_temp")


def download_pdf(download_url: str, stored_name: str) -> str:
    """
    Spring 서버에서 AI 서버로 파일을 다운로드.
    download_url: Spring이 넘겨준 HTTP 주소
    stored_name: 저장할 파일명
    """

    # 작업 폴더 생성
    os.makedirs(SAVE_DIR, exist_ok=True)

    # 저장될 파일 경로
    dst_path = os.path.join(SAVE_DIR, stored_name)

    print(f"📥 PDF 다운로드 시작: {download_url}")
    
    # HTTP GET 요청
    response = requests.get(download_url)
    response.raise_for_status()  # 실패 시 예외 발생

    # 파일로 저장
    with open(dst_path, "wb") as f:
        f.write(response.content)

    print(f"📂 PDF 다운로드 완료: {dst_path}")

    return dst_path
