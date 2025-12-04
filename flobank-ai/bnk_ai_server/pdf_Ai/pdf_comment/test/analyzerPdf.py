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
# 2) 라인 기반 청킹
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


# -------------------------------------------------------
# 3) 규정 리랭크
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
# 4) 위규 판단 (Danger / Safe 2단계)
# -------------------------------------------------------
def check_violation(chunk: str, related_rules: str):
    prompt = f"""
너는 금융상품 설명 문구의 규제 위험성을 판단하는 심사관이다.

이 문장은 Danger 또는 Safe 중 하나로만 분류한다.
추측·과잉 해석 금지. 문구 자체만 평가한다.

-----------------------------------------
[ Danger — 아래 상황이 직접 명시된 경우만 ]
-----------------------------------------
- 소비자의 청약철회권·해지권·자료열람권 등 법적 권리를 포기하도록 유도하거나 제한한다고 직접 명시함
- 정보를 숨기거나 은폐·누락한다고 직접 표현됨
- 사실과 다름을 직접적으로 말함
- 소비자에게 불이익을 강요·기망·속임으로 유도하는 표현이 직접 등장

※ 단, 아래는 Danger 아님 (무조건 Safe):
- 환율변동·수수료·이율·환차손 등 외화예금 필수 안내 문구
- 압류, 휴면예금, 현찰수수료 등 은행이 반드시 안내해야 하는 의무 고지
- 절차 안내 또는 정상적인 제한 설명
- 상품 이해를 위한 일반적 주의 문구


-----------------------------------------
[ Safe ]
-----------------------------------------
위 Danger 기준에 해당하지 않는 모든 문구는 Safe.

=========================================
JSON만 출력:
- 절대 백틱(```) 또는 markdown 코드블록을 사용하지 말아라.
- JSON은 순수 JSON만 출력하라.
{{
  "violation": true/false,
  "safe": true/false,
  "rule": "관련 규정 또는 null",
  "reason": "핵심 근거",
  "risky_words": []
}}

=========================================
[분석대상]
{chunk}

[관련 규정]
{related_rules}

"""

    return llm.invoke(prompt).content


# -------------------------------------------------------
# 5) 전체 TXT 분석
# -------------------------------------------------------
def analyze(lines, retriever, rules_per_chunk=10, top_k_after_rerank=5):
    chunks = chunk_by_lines(lines)
    print(f"🔍 총 {len(chunks)}개 청크 분석 시작")

    results = []

    for idx, chunk in enumerate(chunks):
        print(f"\n📌 청크 분석 중: {idx+1}/{len(chunks)}")

        retrieved_docs = retriever.invoke(chunk)[:rules_per_chunk]
        reranked = rerank_rules(chunk, retrieved_docs)
        selected_rules = "\n\n".join(reranked[:top_k_after_rerank])
        violation = check_violation(chunk, selected_rules)

        results.append({
            "chunk_index": idx,
            "chunk": chunk,
            "related_rules": selected_rules,
            "violation": violation
        })

    return results


# -------------------------------------------------------
# 6) 저장
# -------------------------------------------------------
def save_results(result, txt_path):
    base_name = os.path.basename(txt_path).replace(".txt", "")
    out_dir = "analysis_result"
    os.makedirs(out_dir, exist_ok=True)

    json_path = os.path.join(out_dir, f"{base_name}_result.json")
    txt_path2 = os.path.join(out_dir, f"{base_name}_result.txt")

    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=4)

    with open(txt_path2, "w", encoding="utf-8") as f:
        for r in result:
            f.write(f"[Chunk {r['chunk_index']}]\n")
            f.write("==== Chunk 내용 ====\n")
            f.write(r["chunk"] + "\n\n")
            f.write("==== 관련 규정 ====\n")
            f.write(r["related_rules"] + "\n\n")
            f.write("==== 위규 판단 ====\n")
            f.write(r["violation"] + "\n")
            f.write("\n" + "="*80 + "\n\n")

    print(f"💾 JSON 저장 완료 → {json_path}")
    print(f"💾 TXT 저장 완료 → {txt_path2}")

    return json_path, txt_path2


# -------------------------------------------------------
# 7) 최종 PDF AI 코멘트 생성 (Danger / Safe)
# -------------------------------------------------------
def generate_pdf_comment(results):
    llm_local = ChatOpenAI(model="gpt-4o-mini", temperature=0)

    dangers = []
    safes = []

    for r in results:
        try:
            v = json.loads(r["violation"])
        except:
            continue

        if v.get("violation"):
            dangers.append({
                "chunk_index": r["chunk_index"],
                "chunk": r["chunk"],
                "details": v
            })
        else:
            safes.append({
                "chunk_index": r["chunk_index"],
                "chunk": r["chunk"],
                "details": v
            })

    danger_count = len(dangers)
    safe_count = len(safes)

    overall = "Danger" if danger_count > 0 else "Safe"

    prompt = f"""
다음은 분석 결과이다.

[Danger]
{json.dumps(dangers, ensure_ascii=False, indent=2)}

[Safe]
{json.dumps(safes, ensure_ascii=False, indent=2)}

전체 등급: {overall}

-----------------------------------------------
코멘트 생성 규칙
-----------------------------------------------
- Danger 존재: 문제 문구 인용 + 문제가 되는 이유 + 개선방향
- Danger 없음(Safe만 존재):  
  “위반 또는 오해 가능성이 나타나지 않았으며 설명이 명확합니다.”  
  같은 긍정 코멘트 최소 1개 포함
- 절대 백틱(```) 또는 markdown 코드블록을 사용하지 말아라.
- JSON은 순수 JSON만 출력하라.

출력(JSON):
{{
  "overall_risk": "{overall}",
  "comments": [
    {{"type": "Danger", "text": "..."}},
    {{"type": "Safe", "text": "..."}}
  ]
}}

주의:
- Danger 없으면 Danger 항목 빼기
- Safe 항목은 최소 1개 포함
- JSON만 출력
"""

    llm_comment = llm_local.invoke(prompt).content

    return {
        "llm_comment": llm_comment,
        "danger_count": danger_count,
        "safe_count": safe_count,
        "overall_risk": overall
    }


# -------------------------------------------------------
# 8) 실행부
# -------------------------------------------------------
if __name__ == "__main__":
    print("📚 규정 벡터DB 로드 중...")

    FAISS_PATH = "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/test/faiss_db"
    embed = OpenAIEmbeddings(model="text-embedding-3-large")
    db = FAISS.load_local(FAISS_PATH, embed, allow_dangerous_deserialization=True)
    retriever = db.as_retriever(search_kwargs={"k": 15})

    target_txt = "/home/g5223sho/bnk_ai_server/pdf_Ai/pdf_comment/test/pdf_temp/가짜설명서.txt"

    lines = load_lines(target_txt)
    result = analyze(lines, retriever)
    save_results(result, target_txt)

    print("\n🧠 PDF AI 최종 코멘트 생성 중...\n")
    final_comment = generate_pdf_comment(result)

    out_path = "analysis_result/final_comment.json"
    with open(out_path, "w", encoding="utf-8") as f:
        f.write(final_comment["llm_comment"])

    print(f"💬 PDF AI 코멘트 저장 완료 → {out_path}")
    print("🔢 위험 카운트:", final_comment["danger_count"])
    print("🏷 최종 등급:", final_comment["overall_risk"])
