# ================================================
#   run_test_local.py  (FINAL WORKING VERSION)
# ================================================
import os
from extractor import pdf_to_txt
from extract_product_info import extract_product_info
from pdfAnalyzer import run_ai_risk_analysis
from pdf_ai_db import update_product_info, update_ai_risk, update_status

PDF_ID = 2
PDF_PATH = "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/test/pdf_temp/상품설명서pdf/fake상품설명서.pdf"
SAVE_BASE_DIR = "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/analysis_result"

print("🚀 LOCAL TEST START")

# ------------------------------------------------
# 1) PDF → TXT
# ------------------------------------------------
txt_path = pdf_to_txt(PDF_PATH)
print("📄 TXT 파일:", txt_path)

# ------------------------------------------------
# 2) 상품 정보 추출 (LLM)
# ------------------------------------------------
raw_text = open(txt_path, "r", encoding="utf-8").read()
product_info = extract_product_info(raw_text)
print("🏦 PRODUCT INFO:", product_info)

# → DB Insert
update_product_info(PDF_ID, product_info)

# ------------------------------------------------
# 3) 위험 문구 분석
# ------------------------------------------------
original_file_name = os.path.basename(PDF_PATH).replace(".pdf", "")

risk_result = run_ai_risk_analysis(
    pdf_id=PDF_ID,
    txt_path=txt_path,
    save_base_dir=SAVE_BASE_DIR,
    original_file_name=original_file_name
)

# ------------------------------------------------
# 4) 위험문구 분석 결과 DB 반영 (NEW)
# ------------------------------------------------
final_comment = open(risk_result["final_comment_path"], "r", encoding="utf-8").read()

update_ai_risk(
    pdf_id=PDF_ID,
    overall_risk=risk_result["overall"],
    llm_comment=final_comment
)

# ------------------------------------------------
# 5) 완료
# ------------------------------------------------
update_status(PDF_ID, "done")

print("🎉 LOCAL PIPELINE DONE")
