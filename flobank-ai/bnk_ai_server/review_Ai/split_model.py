import kss

def hybrid_split(text: str):
    """KSS 기반 문장 분리 (부정어 보호 포함)"""
    protected_words = ["불친절", "불편", "불만족", "불쾌", "못함", "안좋다", "부족"]
    for w in protected_words:
        text = text.replace(w, f"__{w}__")

    sentences = kss.split_sentences(text)
    cleaned = [s.replace("__", "").strip() for s in sentences if len(s.strip()) > 1]
    return cleaned
