# ================================================
#               worker.py  (Heavy Job)
# ================================================
import sys
import os
import requests
import json

from downloader import download_pdf
from extractor import pdf_to_txt
from extract_product_info import extract_product_info
from pdfAnalyzer import run_ai_risk_analysis
from pdf_ai_db import update_product_info, update_ai_risk, update_status

SAVE_BASE_DIR = "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/analysis_result"


# ----------------------------------------
# Progress 전송
# ----------------------------------------
def push_progress(pdf_id: int, progress: int):
    url = "http://34.64.124.33:8080/flobank/pdf-ai/progress"
    try:
        requests.post(url, json={"pdfId": pdf_id, "progress": progress})
        print(f"📡 Progress 전송 성공: {pdf_id} → {progress}%")
    except Exception as e:
        print("🚨 Progress 전송 실패:", e)



# ----------------------------------------
# 메인 파이프라인
# ----------------------------------------
def run_worker(pdf_id: int, download_url: str, stored_name: str):

    try:
        push_progress(pdf_id, 5)

        # (1) PDF 다운로드
        pdf_path = download_pdf(download_url, stored_name)
        print(f"📥 PDF 다운로드 완료: {pdf_path}")

        push_progress(pdf_id, 20)

        # (2) PDF → TXT
        txt_path = pdf_to_txt(pdf_path)
        print(f"📝 TXT 변환 완료: {txt_path}")

        push_progress(pdf_id, 40)

        # (3) 상품정보 LLM 추출
        raw_text = open(txt_path, "r", encoding="utf-8").read()
        product_info = extract_product_info(raw_text)
        print("🏦 상품 정보 LLM 추출:", product_info)
        update_product_info(pdf_id, product_info)

        push_progress(pdf_id, 60)

        # (4) 위험문구 분석
        original_file_name = os.path.basename(pdf_path).replace(".pdf", "")
        risk_result = run_ai_risk_analysis(
            pdf_id=pdf_id,
            txt_path=txt_path,
            save_base_dir=SAVE_BASE_DIR,
            original_file_name=original_file_name
        )
        print("⚠️ 위험 분석 결과:", risk_result)

        update_ai_risk(
            pdf_id=pdf_id,
            overall_risk=risk_result["overall"],
            llm_comment=open(
                risk_result["final_comment_path"], "r", encoding="utf-8"
            ).read()
        )

        push_progress(pdf_id, 90)

        # (5) 완료 상태 업데이트
        update_status(pdf_id, "done")

        push_progress(pdf_id, 100)
        print(f"✅ PDF_ID={pdf_id} 전체 파이프라인 완료")

    except Exception as e:
        print(f"❌ Worker Error(pdf_id={pdf_id}):", e)
        update_status(pdf_id, "error")
        push_progress(pdf_id, -1)



# ----------------------------------------
# 명령행 인자 받아 실행
# ----------------------------------------
if __name__ == "__main__":
    pdf_id = int(sys.argv[1])
    download_url = sys.argv[2]
    stored_name = sys.argv[3]

    run_worker(pdf_id, download_url, stored_name)
