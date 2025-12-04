from fastapi import FastAPI
from pydantic import BaseModel
import re, kss
from sentence_transformers import SentenceTransformer, util

app = FastAPI(title="Semantic Polarity Splitter")

model = SentenceTransformer('BM-K/KoSimCSE-roberta-multitask')

POSITIVE_CUES = [r"좋[아요다았았음]", r"괜찮", r"만족", r"빠르", r"편하", r"깔끔", r"친절"]
NEGATIVE_CUES = [r"불친절", r"불편", r"별로", r"나쁘", r"늦", r"짜증", r"싫", r"문제", r"불만"]
CONNECTIVES = ["하지만", "그러나", "반면에", "그런데", "근데", "인데", "지만", "는데"]

def split_on_polarity_shift(text):
    for conn in CONNECTIVES:
        text = re.sub(fr'\b{conn}\b', f'. {conn}', text)
    return re.split(r'[.!?]', text)

def semantic_split_polar(text, threshold=0.65):
    sents = []
    for base in kss.split_sentences(text):
        subs = split_on_polarity_shift(base)
        sents.extend([s.strip() for s in subs if s.strip()])

    if len(sents) <= 1:
        return sents

    embeddings = model.encode(sents)
    sims = [util.cos_sim(embeddings[i], embeddings[i+1]).item() for i in range(len(sents)-1)]

    groups, temp = [], [sents[0]]
    for i, sim in enumerate(sims):
        if sim < threshold:
            groups.append(' '.join(temp))
            temp = [sents[i+1]]
        else:
            temp.append(sents[i+1])
    if temp:
        groups.append(' '.join(temp))

    refined = []
    for s in groups:
        has_pos = any(re.search(p, s) for p in POSITIVE_CUES)
        has_neg = any(re.search(n, s) for n in NEGATIVE_CUES)
        if has_pos and has_neg:
            subs = re.split(r'(?:하지만|그러나|는데|지만)', s)
            refined.extend([x.strip() for x in subs if x.strip()])
        else:
            refined.append(s)
    return refined

class InputText(BaseModel):
    text: str

@app.post("/split")
def split_api(req: InputText):
    result = semantic_split_polar(req.text)
    return {"sentences": result}
