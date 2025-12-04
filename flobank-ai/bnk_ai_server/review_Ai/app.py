from fastapi import FastAPI
from pydantic import BaseModel
import kss

app = FastAPI(title="문장 분리 API", description="KSS 기반 간단한 문장 분리")

class TextInput(BaseModel):
    text: str

@app.post("/split")
def split_text(data: TextInput):
    """문장 분리만 수행"""
    sentences = kss.split_sentences(data.text)
    return {"sentences": sentences}
