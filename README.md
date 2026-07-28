# 💍 Wedding All-in-One

> 업체 탐색/예약 · 쇼핑/커뮤니티 · AI 웨딩플랜 · 준비관리 · OpenClaw
> 결혼 준비의 모든 과정을 하나의 플랫폼에서 — 탐색부터 예약, 결제, 커뮤니티, AI 추천까지

**Notion**: [링크 추가 예정](#)
**Team GitHub**: https://github.com/sai0734/Sixfour_Team

<br>

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [문제 정의 및 해결 방향](#문제-정의-및-해결-방향)
- [기술 스택](#기술-스택)
- [팀원 소개](#팀원-소개)
- [ERD](#erd)
- [유스케이스 다이어그램](#유스케이스-다이어그램)
- [주요 기능](#주요-기능)
- [성능 최적화](#성능-최적화)
- [트러블슈팅](#트러블슈팅)
- [회고](#회고)

<br>

## 프로젝트 소개

### 기획 배경

웨딩홀 대관료·식대·스드메 가격 구조가 제각각이고, 정확한 견적을 받으려면 여러 업체를 직접 상담해야 합니다.

- 가격이 계약 후에야 공개되는 정보 비대칭
- 지역·업체별 비용 격차로 합리적 비교가 어려움
- 홀·스드메·예산·일정이 사이트마다 흩어져 있음
- 반복 탐색 비용이 만혼·비혼 부담으로 이어짐

→ **가격 투명성 + 준비 과정 통합**이 필요합니다.

**참고 근거**
- 조선일보(2023) — 서울 웨딩홀 90곳 중 약 8할 인상, 스드메 300만원대 → 500만원 안팎 상승 언급 ([기사](https://www.chosun.com/national/national_general/2023/02/14/JYSICKCUXBC5LOZOBU2YGZQLEQ/))
- 세계일보(2019) — 평균 결혼비용 약 2억 3,186만원, 예식장 가격 비공개·추가비로 비교 자체가 어려움 ([기사](https://www.segye.com/newsView/20190410517854))

**핵심 수치**

| 식대 평균 | 전국 대관료 평균 | 강남 대관료 평균 |
|:---:|:---:|:---:|
| 약 6.5만원 | 약 389만원 | 약 700만원 |

<br>

## 문제 정의 및 해결 방향

| # | 문제 | 해결 |
|---|------|------|
| 1 | **정보 분산** — 웨딩홀·스튜디오·드레스·메이크업 정보가 여러 사이트/블로그/카페에 흩어짐 | **원스톱 흐름** — 검색 → 비교 → 찜 → 예약 → 결제까지 한 플랫폼에서 완결 |
| 2 | **수기 관리 한계** — 예산·일정을 엑셀/메모앱으로 관리하며 누락·중복 발생 | **준비관리 통합** — 체크리스트 + 예산 + D-day 연동으로 진행상황을 한눈에 |
| 3 | **비교 번거로움** — 동일 조건으로 비교가 어려움 | **AI 웨딩플랜 자동 추천** — 예산·날짜·하객수·스타일 입력만으로 업체 조합 자동 추천 |
| 4 | **문의 채널 파편화** — 전화·카카오톡·인스타DM 등으로 분산 | **소통 통합** — 실시간 문의 채팅으로 업체-회원 커뮤니케이션을 플랫폼 내로 통합 |

<br>

## 기술 스택

### Frontend
![React](https://img.shields.io/badge/React_18.3-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Vite](https://img.shields.io/badge/Vite-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![Redux Toolkit](https://img.shields.io/badge/Redux_Toolkit-764ABC?style=for-the-badge&logo=redux&logoColor=white)
![React Router](https://img.shields.io/badge/React_Router_v7-CA4245?style=for-the-badge&logo=reactrouter&logoColor=white)
![Axios](https://img.shields.io/badge/Axios-5A29E4?style=for-the-badge&logo=axios&logoColor=white)
![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)

### Backend
![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Java](https://img.shields.io/badge/Java_21-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![JWT](https://img.shields.io/badge/JJWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![WebSocket](https://img.shields.io/badge/Spring_WebSocket-6DB33F?style=for-the-badge&logo=spring&logoColor=white)

### Data & Infra
![MariaDB](https://img.shields.io/badge/MariaDB-003545?style=for-the-badge&logo=mariadb&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)

### External API
![OpenAI](https://img.shields.io/badge/OpenAI_API-412991?style=for-the-badge&logo=openai&logoColor=white)
![Kakao](https://img.shields.io/badge/Kakao_Login-FFCD00?style=for-the-badge&logo=kakaotalk&logoColor=black)
![Toss](https://img.shields.io/badge/Toss_Payments-0064FF?style=for-the-badge&logo=toss&logoColor=white)

| 구분 | 스택 |
|---|---|
| Frontend | React 18.3(Vite) · Redux Toolkit · React Router v7 · Axios · Tailwind CSS · STOMP/SockJS · Recharts |
| Backend | Spring Boot 3.5 · Java 21 · Spring Security · Spring Data JPA · JJWT · WebSocket · Spring Mail/Validation · ModelMapper |
| Data | MariaDB(영속) · Redis(Refresh 토큰/블랙리스트 등 단기 상태) |
| AI / External | OpenAI API(gpt-4o-mini) · Google Vision OCR · CatVTON(이미지 합성) · Kakao Login · Toss Payments · Daum 우편번호 API |

<br>

## 팀원 소개

| 이름 | 역할 | 담당 및 주요 구현 기능 |
|---|---|---|
| 황용현 | 팀장 | (추가 예정) |
| 권용익 | 팀원 | (추가 예정) |
| 이재원 | 팀원 | (추가 예정) |
| 윤승진 | 팀원 | (추가 예정) |

<br>

## ERD

> 다이어그램 추가 예정

<br>

## 유스케이스 다이어그램

> 다이어그램 추가 예정

<br>

## 주요 기능

### 🔐 로그인 및 소셜로그인
`JWT` `Spring Security` `Redis`

> Flow 다이어그램 추가 예정

- 카카오 인증만으로는 로그인시키지 않고, 가입 완료 시점에만 진짜 로그인(JWT 발급) 처리
- Access/Refresh 만료시간을 상수로 표준화하고, refresh 토큰은 Redis에 TTL과 함께 저장해 자동 정리되도록 구성
- 로그인 성공/실패를 커스텀 핸들러로 가로채고, JWT 검증 필터를 앞단에 끼워 넣어 세션 없는 REST 인증 흐름을 구성

### 📝 회원가입
`Spring Security` `JWT`

> Flow 다이어그램 추가 예정

- 회원가입 요청 시점엔 Member 테이블에 아무것도 만들지 않고, 입력값 전체를 암호화된 비밀번호와 함께 JSON으로 Emailverify에 임시 저장
- 가입 신청과 이메일 인증 사이에 시간차가 있기 때문에, 그 사이 같은 이메일/휴대폰으로 다른 경로 가입이 먼저 끝났을 가능성을 인증 확정 직전에 한 번 더 검사

### 🏢 업체관리
`Spring Data JPA`

> Flow 다이어그램 추가 예정

- 목록/검색/유형/필터, 등록/보기/수정/삭제가 컨트롤러의 list/CRUD 엔드포인트와 연결
- 업체리스트에서 관리자/일반유저 이동 경로를 분리 — `roleNames`에 ADMIN 권한이 있으면 등록/수정/삭제 허용, `/admin/companies/list`는 관리자용, `/companies`는 일반유저용으로 구분

### 🎁 답례품
`REST CRUD` `Toss Payments`

> Flow 다이어그램 추가 예정

- 로그인 전엔 장바구니를 Redux 상태로만 유지하다가 로그인하는 순간 서버 DB로 이관 — 여러 상품을 한꺼번에 보내지 않고 `for...of` + `await`로 하나씩 순서대로 처리해 꼬임 방지
- 결제창을 띄우기 전 서버에서 먼저 주문을 만들어 실제 금액을 저장하고, 결제 승인 시점에 저장된 금액과 클라이언트가 보낸 금액을 대조 — 클라이언트 쪽 요청 조작으로 다른 금액이 결제되는 것을 방지

### 💬 커뮤니티 & AI 한줄요약
`Spring Data JPA` `OpenAI Chat Completions API`

> Flow 다이어그램 추가 예정

- 게시글마다 딱 한 번만 OpenAI를 호출하고 결과를 `Board.AISummary` 컬럼에 저장 → 이후 조회는 DB 캐시로 응답해 비용·응답속도 문제를 함께 해결. 글 수정 시엔 캐시를 비워 다음 조회 때 재생성
- 본문 150자 미만인 글은 요약 의미가 없다고 판단해 API 요청 자체를 생략, 이미 캐시된 요약은 재요청하지 않음 — 불필요한 OpenAI 호출을 프론트 단에서부터 축소
- 서버는 댓글을 시간순 평평한 목록으로만 내려주고, `parentId` 유무로 최상위 댓글/대댓글을 나눠 프론트에서 트리 구조로 렌더링 — 부모 댓글이 삭제돼도 소프트 삭제 처리라 대댓글은 그대로 유지

### 🤵 AI 웨딩플랜
`OpenAI Chat Completions(JSON)` `서버 슬롯 상태머신`

> Flow 다이어그램 추가 예정

- 담기(플랜 적용)까지 이어지지 않고 방치된 세션만 골라 일정 기간 지나면 매일 자정 자동 삭제 — 실제 사용 중인 세션은 건드리지 않음
- 사용자의 자연어 요청을 AI가 JSON으로 강제 응답시켜 카테고리별 CONFIRM/EXCLUDE/RECONSIDER로 분류 — 파싱 실패 시 조용히 규칙 기반으로 넘어가지 않고 세션을 그대로 유지
- 1차 배분 후 남은 예산이 있으면 취향이 반영 안 된 카테고리부터 더 비싼 옵션으로 업그레이드 — 사용자가 명확히 취향을 지정한 카테고리는 예산 때문에 임의로 바꾸지 않음

### 📩 업체 문의 (실시간 채팅)
`WebSocket(STOMP)` `SockJS` `JWT + STOMP 인증`

> Flow 다이어그램 추가 예정

- 채팅 기능 구현을 위해 단방향 프로토콜인 HTTP 대신, 웹-서버 간 지속 연결과 양방향 통신을 제공하는 WebSocket 사용
- 메시지 저장은 REST API로 처리하고, 저장이 끝난 후 STOMP로 새 메시지 신호를 채팅방 화면·회원 알림뱃지·업체 매니저함 3곳에 동시 전파 — 저장(REST API)과 알림(STOMP)의 책임 분리
- 구독(SUBSCRIBE) 요청이 올 때마다 요청자가 해당 방/업체/이메일 알림을 볼 권한이 있는지 검증
- 인터넷이 끊긴 경우 5초마다 재연결을 시도해 자동으로 알림을 복구

### 🤖 AI 챗봇
`OpenAI Function/Tool`

> Flow 다이어그램 추가 예정

- (핵심 로직 정리 예정)

### 👗 AI 드레스
`외부 CatVTON 이미지합성 API` `OpenAI`

> Flow 다이어그램 추가 예정

- 합성할 사진 선택, 드레스 선택, 합성 요청, 배경 적용, 결과와 기록을 표시
- Base64로 이미지를 PNG로 변환 — DB에는 저장하지 않고 사용자가 "저장하기" 버튼을 누르면 사용자 컴퓨터에만 저장

### 🧾 AI 견적서
`Google Vision OCR` `OpenAI Chat Completions(JSON)`

> Flow 다이어그램 추가 예정

- 홀-홀, 드레스-드레스처럼 같은 종류끼리만 비교를 허용 — AI가 서로 다른 종류의 견적서를 놓고 "어느 쪽이 낫다"는 억지 비교를 하지 않도록 원천 차단
- 가격 차이 문구는 AI가 생성한 텍스트가 아니라 저장된 금액으로 서버가 직접 계산 — AI가 숫자를 잘못 세거나 콤마를 빠뜨리는 실수를 원천 차단
- AI가 웨딩 업체 견적서가 아니라고 판단하거나(자동차·가전 등) 카테고리를 확신 못하면 업로드 자체를 거절하고, 구체적인 거절 사유를 그대로 사용자에게 표시

<br>

## 성능 최적화

### ① N+1 방지 — 목록과 이미지를 한 번에 조회
`@EntityGraph` `FetchType.LAZY` · 적용 화면: 홀/스드메 업체 목록

- `@EntityGraph(imageList)`로 업체+이미지를 한 번에 조회해 목록 N건 × 이미지 lazy 로딩(N+1)을 제거
- 연관 엔티티는 `FetchType.LAZY`로 필요할 때만 로드해 불필요한 조인·조회를 축소

### ② 서버 사이드 페이징 — Page 단위로만 조회
`Pageable` `PRODUCT_LIST_PAGE_SIZE` · 적용 화면: 답례품 상품 목록

- `searchProductList(..., pageable)`로 필터 + Page 단위만 조회해 전체 상품 일괄 로딩 방지
- 프론트 `PRODUCT_LIST_PAGE_SIZE=12` + `totalCount`/`pageNumList`로 페이지 네비게이션 구성

### ③ 라우트 단위 Code Splitting
`React.lazy` `Suspense`

- 페이지를 `React.lazy` + 동적 `import()`로 분리하고 `Suspense`로 감싸, 라우트 진입 시에만 청크를 로드하도록 구성 (Vite가 `import()` 기준으로 자동 청크 분리)

<br>

## 트러블슈팅

### ① 하드코딩된 서명 키로 관리자 토큰 위조 가능

| 문제 | 원인 | 해결 |
|---|---|---|
| 하드코딩된 서명 키로 관리자 토큰 위조 가능 | `JWTUtil.java`에 고정 문자열로 키 저장 | `JWT_SECRET` 환경변수로 분리하고 랜덤 키로 교체 |

> Postman 검증(BEFORE/AFTER) 캡처 추가 예정

### ② 결제 승인 API 중복 호출 시 오류 노출

| 문제 | 원인 | 해결 |
|---|---|---|
| 결제 승인 API 중복 호출 시 409 오류 노출 | PAID 확인 없이 토스 승인 API를 반복 호출 | 이미 PAID 상태면 기존 결제 결과를 즉시 반환 |

> Postman 검증(BEFORE/AFTER) 캡처 추가 예정

### ③ AI 웨딩플랜 "다시 찾기" 시 업체가 무작위로 바뀌는 문제

| 문제 | 원인 | 해결 |
|---|---|---|
| 제외(X)한 업체를 '다시 찾기'로 되돌리면, 원래 업체가 아닌 매번 다른 업체·추천 사유가 무작위로 표시됨 | 제외 처리 시 선택 업체·추천 사유를 함께 초기화(null)해, 복원할 정보가 없어 매번 새로 검색·재생성 | 제외 시 상태만 `EXCLUDED`로 바꾸고 선택 정보는 보존 → '다시 찾기'는 새로 검색하지 않고 원래 업체·사유를 그대로 복원 |

> BEFORE/AFTER 캡처 추가 예정

<br>

## 회고

> 팀/개인 회고 작성 예정

<br>

---

**Team GitHub**: https://github.com/sai0734/Sixfour_Team
