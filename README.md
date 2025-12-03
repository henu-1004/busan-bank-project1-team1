# busan-bank-project1-team1

# 🏦 FLOBANK (Busan Bank Project 1 Team 1)

**FloBank**는 차세대 뱅킹 시스템을 모사한 핀테크 웹 애플리케이션 프로젝트입니다.
채널계(API Server)와 계정계(Core Banking Server)를 분리하여 **TCP/IP 소켓 통신**으로 연동하는 실제 금융권 아키텍처를 기반으로 설계되었습니다. 수신, 여신, 외환 등 핵심 금융 업무와 함께 OpenAI, Elasticsearch, Vector DB를 활용한 AI 기반의 혁신적인 금융 서비스를 제공합니다.

-----

## 📅 프로젝트 개요

  * **프로젝트 명**: FloBank (플로뱅크)
  * **개발 기간**: 2025.11.05 \~ 2025.12.05
  * **주요 목표**:
      * **외환 상품 설계**: 기존에 없던 외화 상품 설계
      * **AI 금융 서비스**: OpenAI 및 Vector DB(Pinecone)를 활용한 챗봇 및 문서 분석
      * **검색 최적화**: Elasticsearch를 도입하여 상품 및 게시글 검색 속도 향상
      * **데이터 수집 자동화**: Python(Selenium)을 이용한 금리 및 환율 정보 크롤링

-----

## 👥 팀원 소개 (Team 1)

| 역할 | 이름 | 담당 업무 | GitHub |
| :--- | :--- | :--- | :--- |
| **Team Leader** | **(이름)** | 프로젝트 총괄, 아키텍처 설계, (주요 담당 기능) | [@user](https://github.com/user) |
| **Front/Back** | **(이름)** | 수신/여신 기능 구현, UI 개발 | [@user](https://github.com/user) |
| **Front/Back** | **(이름)** | 외환/송금 서비스, 관리자 페이지 | [@user](https://github.com/user) |
| **AI/Data** | **(이름)** | AI 챗봇 연동, 검색 엔진 최적화, 데이터 크롤링 | [@user](https://github.com/user) |
| **Infra/Sec** | **(이름)** | CI/CD 파이프라인 구축, 보안 설정, 서버 배포 | [@user](https://github.com/user) |

-----

## 🌟 주요 기능 (Key Features)

### 1\. 📡 코어 뱅킹 & 통신 (Core Banking & Network)

  * **TCP/IP 통신 (Spring Integration)**:
      * `flobank-api`(채널계)에서 JSON 데이터를 직렬화하여 TCP 소켓으로 송신.
      * `flobank-ap`(계정계)에서 수신 후 비즈니스 로직 처리(DB 반영) 및 결과 응답.
  * **분산 아키텍처**: 웹 서버 부하와 핵심 금융 트랜잭션 처리를 물리적/논리적으로 분리하여 안정성 확보.

### 2\. 💰 뱅킹 서비스 (Banking Services)

  * **수신 (Deposit)**:
      * 비대면 입출금 통장 개설.
      * 예/적금 상품 가입 및 해지 (이자 계산 로직 포함).
      * 계좌 조회 및 이체 내역 관리.
  * **이체 (Transfer)**:
      * 당행/타행 이체, 자동 이체 설정.
      * 1일/1회 이체 한도 관리 및 검증.
  * **외환 (Exchange & Remittance)**:
      * **실시간 환율 정보**: Python 크롤러가 수집한 최신 환율 제공.
      * **환전 지갑**: 외화 매입/매도 및 모바일 환전 신청.
      * **해외 송금**: SWIFT 코드 기반 해외 송금 신청 및 진행 상황 조회.

### 3\. 🤖 AI & 스마트 서비스 (AI & Smart Features)

  * **AI 챗봇 (Flo-Bot)**:
      * **RAG (Retrieval-Augmented Generation)**: Pinecone(Vector DB)에 저장된 금융 지식 데이터와 DB 화이트리스트 조회를 기반으로 정확한 답변 제공.
      * 자연어 처리를 통한 메뉴 이동 및 상품 추천.
  * **PDF 문서 AI 분석**:
      * 복잡한 금융 상품 약관이나 설명서(PDF)를 업로드하면 AI가 핵심 내용을 요약 및 분석해 주는 서비스.
  * **다국어 번역**:
      * 외국인 고객을 위한 실시간 텍스트 번역 지원.

### 4\. 🔍 검색 및 데이터 (Search & Data)

  * **통합 검색 (Elasticsearch)**:
      * 금융 상품, 공지사항, FAQ, 이벤트 등을 한 번에 검색.
      * 형태소 분석을 통한 유사어 검색 및 자동 완성 기능.
  * **데이터 크롤링**:
      * 시중 은행의 예금 금리 및 실시간 환율 정보를 주기적으로 수집하여 DB 동기화.

### 5\. 🛡️ 보안 및 인증 (Security & Auth)

  * **Spring Security & JWT**: Stateless한 인증 관리 및 Role 기반 접근 제어.
  * **본인 인증**: CoolSMS(Solapi) API를 이용한 휴대폰 본인 인증 및 SMTP 이메일 인증.
  * **암호화**: 개인정보(주민번호 등) 양방향 암호화(AES-256) 및 비밀번호 단방향 암호화(SHA/Bcrypt).

### 6\. 👨‍💼 관리자 모드 (Admin Dashboard)

  * **대시보드**: 신규 가입자 추이, 일일 거래량, 예치금 현황 등을 차트(Chart.js 등)로 시각화.
  * **상품 관리**: 예/적금 금리 수정, 신규 상품 등록/수정/삭제.
  * **회원 및 Q\&A 관리**: 블랙리스트 관리, 1:1 문의 답변 처리.

-----

## 📂 디렉토리 구조 (Directory Structure)

```bash
busan-bank-project1-team1
├── 📂 flobank-api               # [Channel Server] 웹/API 서버 (사용자 접점)
│   ├── 📂 crawl                 # Python 크롤링 스크립트 (환율/금리)
│   ├── 📂 src/main/java/kr/co/api/flobankapi
│   │   ├── 📂 config            # 설정 (Security, Swagger, Redis, AI, WebClient)
│   │   ├── 📂 controller        # Controller (View/API 분리)
│   │   │   ├── 📂 admin         # 관리자용 컨트롤러
│   │   │   └── ...              # 일반 사용자용 (Chatbot, Deposit, Remit 등)
│   │   ├── 📂 service           # 비즈니스 로직 (AI 연동, 메일 발송 등)
│   │   ├── 📂 tcp               # TCP Client 설정 및 Gateway
│   │   ├── 📂 document          # Elasticsearch Document 클래스
│   │   ├── 📂 dto               # 데이터 전송 객체
│   │   └── 📂 jwt               # JWT 인증 필터 및 프로바이더
│   ├── 📂 src/main/resources
│   │   ├── 📂 elastic           # Elasticsearch 인덱스 매핑 설정
│   │   ├── 📂 mappers           # MyBatis XML Mapper (DB 쿼리)
│   │   ├── 📂 static            # 정적 자원 (CSS, JS, Images)
│   │   └── 📂 templates         # Thymeleaf HTML 템플릿
│   └── 📄 build.gradle          # 의존성 설정
│
├── 📂 flobank-ap                # [Core Server] 계정계 서버 (금융 트랜잭션 처리)
│   ├── 📂 src/main/java/kr/co/ap/flobankap
│   │   ├── 📂 service           # 핵심 뱅킹 서비스 (계좌개설, 이체, 잔액조회 등)
│   │   ├── 📂 tcp               # TCP Server 설정 및 메시지 핸들러
│   │   └── 📂 dto               # 전문 통신용 DTO
│   ├── 📂 src/main/resources
│   │   └── 📂 mappers           # Oracle DB 연동 매퍼
│   └── 📄 build.gradle
│
├── 📂 .github/workflows         # CI/CD (GitHub Actions) 설정
├── 📂 terms                     # 약관 데이터 (AI 학습/분석용 텍스트)
└── 📄 docker-compose.yml        # 전체 서비스(Web, DB, Redis, ELK 등) 오케스트레이션
```

-----

## 🛠️ 기술 스택 (Tech Stack)

### Backend

  * **Language**: Java 21
  * **Framework**: Spring Boot 3.5.7
  * **Database**: Oracle Database (Main), Redis (Cache/Session), Pinecone (Vector DB)
  * **Search Engine**: Elasticsearch 8.x
  * **Communication**: Spring Integration (TCP/IP)
  * **ORM**: MyBatis 3.0.5

### Frontend

  * **Template Engine**: Thymeleaf
  * **Language**: HTML5, CSS3, JavaScript (ES6+)
  * **Library**: Bootstrap, jQuery, Chart.js

### DevOps & Tools

  * **Infra**: Docker, Docker Compose
  * **CI/CD**: GitHub Actions
  * **VCS**: Git, GitHub
  * **Collaboration**: Notion, Discord

-----

## 🚀 실행 방법 (How to Run)

1.  **레포지토리 클론**

    ```bash
    git clone https://github.com/greenbnk2/busan-bank-project1-team1.git
    ```

2.  **환경 변수 설정**

      * `application.yml` 내 DB 정보 및 OpenAI API Key 설정이 필요합니다.

3.  **Docker Compose 실행**

    ```bash
    docker-compose up -d --build
    ```

4.  **접속**

      * Web: `http://localhost:8080`
