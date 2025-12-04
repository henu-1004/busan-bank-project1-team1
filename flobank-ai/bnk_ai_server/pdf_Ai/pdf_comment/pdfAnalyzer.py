# ================================================
#   analyzer_v3.py
#   MULTI-RISK 검사 + 문맥 기반 Danger/Safe 분석기
# ================================================

import os
import json
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from langchain_community.vectorstores import FAISS

llm = ChatOpenAI(model="gpt-4o-mini", temperature=0)


# -------------------------------------------------------
# 1) TXT 로딩
# -------------------------------------------------------
def load_lines(txt_path: str):
    if not os.path.exists(txt_path):
        raise FileNotFoundError(f"TXT 파일 없음: {txt_path}")

    print(f"📄 TXT 로딩: {txt_path}")

    with open(txt_path, "r", encoding="utf-8") as f:
        lines = [line.rstrip("\n") for line in f]

    print(f"📌 총 {len(lines)}개 라인 로드됨")
    return lines


# -------------------------------------------------------
# 2) 라인, 글자 수 기반 청킹
# -------------------------------------------------------
def chunk_by_lines(lines, max_chars=1200, overlap_lines=9):
    chunks = []
    current = []
    cur_len = 0

    for line in lines:
        line_len = len(line)

        if cur_len + line_len > max_chars:
            chunks.append("\n".join(current))
            current = current[-overlap_lines:] if overlap_lines > 0 else []
            cur_len = sum(len(l) for l in current)

        current.append(line)
        cur_len += line_len

    if current:
        chunks.append("\n".join(current))

    print(f"🔗 생성된 청크 수: {len(chunks)}")
    return chunks

def chunk_by_chars(text, max_chars=1200, overlap_chars=400):
    chunks = []
    text_len = len(text)

    if overlap_chars >= max_chars:
        raise ValueError("overlap_chars must be smaller than max_chars")

    start = 0

    while start < text_len:
        end = min(start + max_chars, text_len)
        chunk = text[start:end]
        chunks.append(chunk)

        # 다음 start는 반드시 증가하도록 보장해야 한다
        new_start = end - overlap_chars

        if new_start <= start:  # 무한루프 방지
            start = end
        else:
            start = new_start

    return chunks




# -------------------------------------------------------
# 3) 위험 가능 문구 프리플래그
# -------------------------------------------------------
def extract_risky_phrases(chunk: str):
    prompt = f"""
너는 금융상품 설명 문구의 규제 위험성을 판단하는 심사관이다.
다음 텍스트에서 '소비자보호 규제상 위험 가능성이 있는 문구'만 추출하라. (추측·과잉 해석 금지. 문구와 문맥 자체만 평가한다.)
아래 기준에 해당하는 문구만 뽑아라:
- 소비자의 청약철회권·해지권·자료열람권 등 권리를 포기·제한하도록 유도
- 이해하지 못해도 가입 가능
- 손실이 절대 없다, 무조건 보장
- 위험 없음·확정 수익 등 사실과 다른 표현
- 정보를 숨기거나 은폐·누락한다고 직접 표현됨
- 손실 없음·확정 수익 등 소비자 오인 유발 가능 표현
- 소비자에게 불이익을 강요·기망·속임으로 유도하는 표현
너무 넓게 잡지 말고, 진짜 위험문구만 리스트로 뽑아라.

※ 아래 유형은 정상적인 법적 의무 고지이며 절대 위험문구로 간주하지 않는다:
- 정상적 절차 안내
- 수수료, 환율 변동, 위험 안내 등 필수 고지
- 단순한 설명/주의문구
- 압류, 가압류, 질권설정 등에 따른 지급 제한 안내
- “~제한될 수 있습니다”, “~불가합니다”, “~적용됩니다” 형태의 절차/법령 고지
- 예금자보호 안내, 환율 안내, 리스크 안내 등 필수 고지

JSON만 출력:
- 절대 백틱(```) 또는 markdown 코드블록을 사용하지 말아라.
- JSON은 순수 JSON만 출력하라.

출력(JSON):
{{
  "phrases": ["...", "..."]
}}

----------------------------------------
[분석대상]
{chunk}
"""
    res = llm.invoke(prompt).content
    try:
        data = json.loads(res)
        return data.get("phrases", [])
    except:
        return []


# -------------------------------------------------------
# 4) 규정 RAG 리랭크
# -------------------------------------------------------
def rerank_rules(query: str, docs: list):
    pairs = []

    for d in docs:
        prompt = f"""
너는 금융 규정 문서 리랭커이다.

[분석 대상]
{query}

[후보 규정]
{d.page_content}

이 규정이 분석 대상과 얼마나 관련 있는지
0~100 점수로 평가하라.

반드시 "score: XX" 형태로만 출력.
"""
        score_txt = llm.invoke(prompt).content.strip()
        try:
            score = float(score_txt.replace("score:", "").strip())
        except:
            score = 0.0

        pairs.append((score, d.page_content))

    pairs.sort(key=lambda x: x[0], reverse=True)
    return [p[1] for p in pairs]


# -------------------------------------------------------
# 5) Danger/Safe 판단 (문구 + 문맥 + 규정)
# -------------------------------------------------------
def check_violation(phrase: str, chunk_text: str, related_rules: str):
    prompt = f"""
너는 금융상품 설명 문구의 규제 위험성을 판단하는 심사관이다.

이 문장은 Danger 또는 Safe 중 하나로만 분류한다.
추측·과잉 해석 금지. 문구와 문맥 자체만 평가한다.

-----------------------------------------
[ Danger — 아래 상황이 직접 명시된 경우만 ]
-----------------------------------------
- 소비자의 청약철회권·해지권·자료열람권 등 권리를 포기·제한하도록 유도
- 정보를 숨기거나 은폐·누락한다고 직접 표현됨
- 사실과 다름을 직접적으로 말함
- 손실 없음·확정 수익 등 소비자 오인 유발 가능 표현
- 소비자에게 불이익을 강요·기망·속임으로 유도하는 표현

※ 아래는 Danger 아님(Safe) [아래 유형은 절대 금소법 위반 아님 ]:
- 정상적 절차 안내
- "설명내용을 제대로 이해하지 못하였음에도 불구하고 서명하면 권리구제가 어려울 수 있습니다" 등
  소비자에게 주의를 주는 안내문 (주의·경고 목적의 문장)
- 수수료, 환율 변동, 위험 안내 등 필수 고지
- 단순한 설명/주의문구(- "~어려울 수 있습니다"는 소비자 보호 안내문일 뿐, 위법 유도 아님)
- 법적 의무사항 고지(fact-based legal notice)
- 압류/가압류/질권설정 등 지급 제한 안내
- "~제한될 수 있습니다", "~불가합니다", "~적용됩니다" 형태의 정상 절차 안내
- 예금자보호, 위험 안내, 환율/수수료, 중도해지 불이익 고지 등 규제가 요구하는 필수 설명
- 소비자 오인을 유발하지 않는 factual 안내

JSON만 출력:
- 절대 백틱(```) 또는 markdown 코드블록을 사용하지 말아라.
- JSON은 순수 JSON만 출력하라.
-----------------------------------------
출력(JSON만):
{{
  "violation": true/false,
  "safe": true/false,
  "rule": "관련 규정 또는 null",
  "reason": "핵심 이유",
  "risky_words": []
}}

-----------------------------------------
논의 여지는 참고만 하고  
판단은 반드시 문맥 전체를 기준으로 하라

[논의 여지]
{phrase}

[문맥 전체]
{chunk_text}

[관련 규정]
{related_rules}
"""
    return llm.invoke(prompt).content


# -------------------------------------------------------
# 6) 청크 기반 멀티 분석 (리랭커 옵션 추가)
# -------------------------------------------------------
def analyze_chunk(chunk, retriever, rules_per_phrase=5, use_reranker=False):
    risky_list = extract_risky_phrases(chunk)

    # 위험 문구 없으면 chunk 전체를 분석
    if len(risky_list) == 0:
        risky_list = [chunk]

    results = []

    for phrase in risky_list:
        # 1) FAISS Top-k 검색
        retrieved_docs = retriever.invoke(phrase)[:rules_per_phrase]

        # 2) 리랭커 옵션 적용 여부
        if use_reranker and retrieved_docs:
            ranked_rules = rerank_rules(phrase, retrieved_docs)
            all_rules_text = "\n".join(ranked_rules)
            first_rule_line = ranked_rules[0].split("\n")[0].strip()
        else:
            if retrieved_docs:
                all_rules_text = "\n".join([d.page_content for d in retrieved_docs])
                first_rule_line = retrieved_docs[0].page_content.split("\n")[0].strip()
            else:
                all_rules_text = "관련 규정 없음"
                first_rule_line = "관련 규정 없음"

        # 3) Danger/Safe 판정
        violation_json = check_violation(
            phrase=phrase,
            chunk_text=chunk,
            related_rules=all_rules_text
        )

        results.append({
            "phrase": phrase,
            "rule_first_line": first_rule_line,
            "violation": violation_json
        })

    return results





# -------------------------------------------------------
# 7) 전체 TXT 분석 (리랭커 옵션 전달)
# -------------------------------------------------------
def analyze(lines, retriever, use_reranker=False):
    full_text = "\n".join(lines)
    chunks = chunk_by_chars(full_text, max_chars=1000, overlap_chars=300)
    results = []

    print(f"🔍 총 {len(chunks)}개 청크 분석 시작")

    for idx, chunk in enumerate(chunks):
        print(f"\n📌 청크 분석: {idx+1}/{len(chunks)}")

        # 옵션 넘기기
        chunk_result = analyze_chunk(
            chunk,
            retriever,
            use_reranker=use_reranker
        )

        results.append({
            "chunk_index": idx,
            "chunk": chunk,
            "details": chunk_result
        })

    return results



# -------------------------------------------------------
# 8) 최종 AI 코멘트 생성
# -------------------------------------------------------
def generate_ai_comment(all_results):
    dangers = []
    safes = []

    for chunk_info in all_results:
        for d in chunk_info["details"]:
            try:
                v = json.loads(d["violation"])
            except:
                continue

            if v.get("violation"):
                dangers.append({
                    "chunk_index": chunk_info["chunk_index"],
                    "phrase": d["phrase"],
                    "details": v
                })
            else:
                safes.append({
                    "chunk_index": chunk_info["chunk_index"],
                    "phrase": d["phrase"],
                    "details": v
                })

    overall = "Danger" if len(dangers) > 0 else "Safe"

    prompt = f"""
다음은 위험 분석 결과이다.

[Danger]
{json.dumps(dangers, ensure_ascii=False, indent=2)}

[Safe]
{json.dumps(safes, ensure_ascii=False, indent=2)}

전체 등급: {overall}

-----------------------------------------------
코멘트 작성 규칙
-----------------------------------------------
- Danger 존재: 위험 문구 인용 + 위반 이유 + 개선 제안
- Safe만 존재: “위반 또는 오해 가능성이 나타나지 않았습니다.” 포함
- Danger 없을 경우 Danger 항목 제거
- Safe 항목 1개 이상 포함

JSON만 출력:
- 절대 백틱(```) 또는 markdown 코드블록을 사용하지 말아라.
- JSON은 순수 JSON만 출력하라.

JSON만 출력:
{{
  "overall_risk": "{overall}",
  "comments": []
}}
"""

    final = llm.invoke(prompt).content

    return {
        "llm_comment": final,
        "overall": overall,
        "danger_count": len(dangers)
    }


# =====================================================================
# ⭐⭐ FastAPI가 호출하는 통합 분석 함수 ⭐⭐
# =====================================================================
def run_ai_risk_analysis(pdf_id: int, txt_path: str, save_base_dir: str, original_file_name: str):

    print("📚 규정 벡터DB 로드 중...")
    FAISS_PATH = "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/test/faiss_db"

    embed = OpenAIEmbeddings(model="text-embedding-3-large")
    db = FAISS.load_local(FAISS_PATH, embed, allow_dangerous_deserialization=True)    
    retriever = db.as_retriever(search_kwargs={"k": 7})

    # 1) TXT 로드
    lines = load_lines(txt_path)

    # 2) 위험 분석
    results = analyze(lines, retriever, use_reranker=True)#False

    # 저장 디렉토리
    save_dir = os.path.join(save_base_dir, str(pdf_id))
    os.makedirs(save_dir, exist_ok=True)

    # 3) 디테일 TXT 저장
    detail_filename = f"{original_file_name}_analyze_detail.txt"
    detail_path = os.path.join(save_dir, detail_filename)
    with open(detail_path, "w", encoding="utf-8") as f:
        for chunk_info in results:
            f.write(f"=== Chunk {chunk_info['chunk_index']} ===\n")
            for d in chunk_info["details"]:
                f.write(f"- 문구: {d['phrase']}\n")
                f.write(f"- 관련 규정(1줄): {d['rule_first_line']}\n")
                f.write(f"- 분석결과 JSON: {d['violation']}\n")
                f.write("\n")

    # 4) 파이널 코멘트 생성
    final_comment = generate_ai_comment(results)

    # 5) 파이널 코멘트 TXT 저장
    final_filename = f"{original_file_name}_final_comment.txt"
    final_path = os.path.join(save_dir, final_filename)
    with open(final_path, "w", encoding="utf-8") as f:
        f.write(final_comment["llm_comment"])

    print(f"🏁 분석 완료 → PDF_ID={pdf_id}")

    # 🔥 DB 업데이트는 main.py에서 수행하므로 반환만 한다
    return {
        "overall": final_comment["overall"],
        "danger_count": final_comment["danger_count"],
        "detail_path": detail_path,
        "final_comment_path": final_path
    }
