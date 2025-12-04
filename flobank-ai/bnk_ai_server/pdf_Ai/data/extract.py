import os
import fitz

OUTPUT_DIR = "./txt"


def extract_blocks(pdf_path):
    doc = fitz.open(pdf_path)
    blocks_all = []

    for page in doc:
        blocks = page.get_text("blocks")  # (x0, y0, x1, y1, text, block_no, ...)
        for b in blocks:
            text = b[4].strip()
            if text:
                blocks_all.append(text)

    return "\n\n".join(blocks_all)  # 문단 단위


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    for filename in os.listdir("./"):
        if filename.lower().endswith(".pdf"):
            full_path = os.path.join("./", filename)
            print(f"📄 처리 중: {filename}")

            text = extract_blocks(full_path)

            save_path = os.path.join(OUTPUT_DIR, filename.replace(".pdf", ".txt"))
            with open(save_path, "w", encoding="utf-8") as f:
                f.write(text)

            print(f"✔ 저장됨: {save_path}")


if __name__ == "__main__":
    main()
