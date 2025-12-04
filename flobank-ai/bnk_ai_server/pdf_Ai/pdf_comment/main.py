# ================================================
#                   main.py (경량화 버전)
# ================================================
from fastapi import FastAPI
import requests
import subprocess
import os

app = FastAPI()

def push_progress(pdf_id: int, progress: int):
    url = "http://34.64.124.33:8080/pdf-ai/progress"
    try:
        requests.post(url, json={"pdfId": pdf_id, "progress": progress})
        print(f"📡 Progress 전송 성공: {pdf_id} → {progress}%")
    except Exception as e:
        print("🚨 Progress 전송 실패:", e)


@app.post("/api/pdf/process")
def process_pdf(dto: dict):
    print("🔥 DTO 수신:", dto)

    pdf_id = dto.get("pdfId")
    download_url = dto.get("downloadUrl")
    stored_name = dto.get("storedFileName")

    if not pdf_id or not download_url or not stored_name:
        return {"error": "pdfId, downloadUrl, storedFileName are required"}

    # ⭐ worker.py 프로세스 시작
    subprocess.Popen([
        "python3",
        "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/worker.py",
        str(pdf_id),
        download_url,
        stored_name
    ])

    return {"status": "received", "pdfId": pdf_id}
