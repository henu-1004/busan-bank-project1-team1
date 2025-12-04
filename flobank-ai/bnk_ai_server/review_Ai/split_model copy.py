from transformers import AutoTokenizer, AutoModel
from sentence_transformers import SentenceTransformer, util
import torch, re

# ✅ 모델 로드 (처음 한 번만 실행)
elec_tokenizer = AutoTokenizer.from_pretrained("monologg/koelectra-small-v3-discriminator")
elec_model = AutoModel.from_pretrained("monologg/koelectra-small-v3-discriminator")
sbert_model = SentenceTransformer('jhgan/ko-sbert-multitask')

# ✅ 감정 단서 정의
POSITIVE_CUES = ["좋다", "빠르다", "친절", "만족", "편하다", "괜찮다", "깔끔", "추천"]
NEGATIVE_CUES = ["불친절", "늦다", "불편", "별로", "문제", "싫다", "나쁘다", "더럽다", "짜증", "불만족"]
NEGATIVE_PROTECTED = ["불친절", "불편", "불만족"]

# ✅ 접두 부정 병합 함수
def merge_prefix_negatives(sentences):
    merged, skip = [], False
    for i, s in enumerate(sentences):
        if skip:
            skip = False
            continue
        if s in ["불", "못", "안"] and i + 1 < len(sentences):
            merged.append(s + sentences[i+1])
            skip = True
        else:
            merged.append(s)
    return merged

# ✅ ELECTRA 기반 규칙 분리 + 보호 처리
def electra_split(text: str):
    base_splits = re.split(r'[.!?]', text)
    base_splits = [s.strip() for s in base_splits if s.strip()]
    refined = []

    for sent in base_splits:
        # 보호 단어를 먼저 마킹 (__불친절__ 등)
        for word in NEGATIVE_PROTECTED:
            sent = re.sub(fr"{word}", f"__{word}__", sent)

        # 접속사 단위 분리
        subs = re.split(r'(?:그리고|하지만|며서|면서|또는|또|그러나)', sent)
        subs = [s.strip() for s in subs if s.strip()]

        temp = []
        for s in subs:
            # 감정어 기준 분리
            pattern = r'(?=(?:' + "|".join([re.escape(cue) for cue in POSITIVE_CUES + NEGATIVE_CUES]) + r'))'
            chunks = re.split(pattern, s)
            chunks = [c.strip() for c in chunks if c.strip()]
            temp.extend(chunks)

        # 접두부정 병합
        temp = merge_prefix_negatives(temp)
        # 마커 복원 (__불친절__ → 불친절)
        temp = [c.replace("__", "") for c in temp]
        refined.extend(temp)

    return refined

# ✅ SBERT 의미 유사도 병합
def sbert_merge(sentences):
    if not sentences:
        return []
    merged = [sentences[0]]
    for i in range(1, len(sentences)):
        emb1 = sbert_model.encode(merged[-1], convert_to_tensor=True)
        emb2 = sbert_model.encode(sentences[i], convert_to_tensor=True)
        sim = util.cos_sim(emb1, emb2).item()

        # 의미 유사도 기준 병합
        if sim < 0.78:
            merged.append(sentences[i])
        else:
            merged[-1] += " " + sentences[i]

    # 종결어미 정규화
    merged = [
        s.replace("습니다", "다")
         .replace("어요", "다")
         .replace("네요", "다")
         .strip()
        for s in merged
    ]
    return merged

# ✅ 최종 하이브리드 분리 파이프라인
def hybrid_split(text: str):
    first_pass = electra_split(text)
    result = sbert_merge(first_pass)
    return result
