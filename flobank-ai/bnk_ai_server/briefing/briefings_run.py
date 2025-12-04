import subprocess

def run(cmd):
    print(f"\n🚀 실행중: {cmd}")
    result = subprocess.run(cmd, shell=True)
    
    if result.returncode != 0:
        print(f"❌ 에러 발생! 중단됨: {cmd}")
        exit(1)
    else:
        print(f"✅ 완료: {cmd}")

# 1) 크롤러
run("python3 run_crawler.py")

# 2) 요약 수행
run("python3 summarize_articles_db.py")

# 3) 브리핑 생성
run("python3 generate_briefing.py")

print("\n🎉 모든 작업이 성공적으로 끝났습니다.")
