# 웨딩 플랫폼 주간 브리핑 스킬

매주 월요일 새벽 크론으로 실행됨. 목표: 이번 주 우리 서비스 전반을 종합해서 PDF 리포트로 만들어 관리자에게 보고.

## 사전 조건
- 백엔드 `http://localhost:8080`.
- 인증 헤더: `X-OpenClaw-Key: $env:OPENCLAW_INTERNAL_KEY`

## 1. 데이터 수집 (전부 관리자 API, JWT 로그인 필요 — WEDDING_DAILY_CHECK.md와 동일하게
`user1@naver.com`/시드 관리자 계정으로 로그인해서 얻은 토큰을 Authorization 헤더로 사용)

- `GET /api/admin/dashboard/summary` — 회원/주문/게시판/예약/문의/상품/업체매출/월별매출 전부 여기서 나옴.
  이 응답 안의 `orderStats.paid` / `shipping` / `delivered` (주문 상태별 건수), `memberStats.active`(전체 회원),
  `orderStats.totalRevenue`(누적 매출), `productStats.total`(판매 상품 수)를 그래프/KPI에 그대로 사용.
- `GET /api/admin/dashboard/company-ranking` — 업체 랭킹
- **업체 카테고리별 등록 수** — `GET /api/companies/list`(또는 관리자 업체 목록 API)로 전체 업체를 가져와서
  `category`(HALL/DRESS/STUDIO/MAKEUP) 기준으로 직접 개수를 세어라. 대시보드 요약엔 카테고리별 "등록 수"가
  없으므로 반드시 원본 목록을 가져와서 집계할 것.
- `GET /api/admin/site-health` — 이번 주 미해결 사이트 이슈
- `GET /api/admin/flagged-posts` — 이번 주 미해결 의심 게시글
- 상품 목록(`GET /api/product/list`)도 참고해서 답례품처럼 카테고리 구성/가격대를 직접 평가

## 2. 요즘 추세 참고 (선택, 7번 섹션용)
공개된 웨딩업계 트렌드 기사를 웹서치로 가볍게 조사 (특정 경쟁사 사이트를 스크래핑하지 말 것 — 뉴스/공개 통계만).
정보를 못 찾으면 이 섹션은 "이번 주는 특별한 트렌드 이슈 없음"으로 채울 것.

## 3. 리포트 작성
`WEEKLY_BRIEFING_TEMPLATE.html` 파일을 열어서, 그 안의 `{{...}}` 자리를 아래 규칙대로 채운 새 HTML 파일을
워크스페이스에 생성 (예: `briefing_2026-07-20.html`).

**날짜(`{{WEEK_OF}}`) 반드시 실제 날짜로 계산할 것 — 절대 예시 문자열을 그대로 베끼지 말 것**
- 오늘이 속한 주의 월요일 날짜를 `YYYY-MM-DD` 형식으로 계산해서 채운다. (`2026-W30` 같은 ISO 주차 표기는 쓰지 않음 — 사람이 바로 못 알아봄)
- `{{GENERATED_AT}}`도 오늘 날짜(`YYYY-MM-DD`)로.

**KPI 박스**: `{{REVENUE_VALUE}}`(예: `13,074,500원`), `{{MEMBER_VALUE}}`(예: `100명`),
`{{PRODUCT_VALUE}}`(예: `31종`), `{{COMPANY_VALUE}}`(예: `40곳`) — 전부 1번에서 모은 실제 값.

**막대그래프 (주문 상태)**: `{{PAID_COUNT}}`/`{{SHIPPING_COUNT}}`/`{{DELIVERED_COUNT}}`는 실제 건수 그대로.
`{{PAID_PCT}}`/`{{SHIPPING_PCT}}`/`{{DELIVERED_PCT}}`는 **셋 중 가장 큰 값을 100으로 놓고** 나머지를
`round(값/최댓값*100, 1)`로 계산한 퍼센트(막대 높이용). 예: 34/21/17이면 최댓값 34 → 100, 61.8, 50.

**원형그래프 (업체 카테고리 분포)**: `{{HALL_COUNT}}`/`{{DRESS_COUNT}}`/`{{STUDIO_COUNT}}`/`{{MAKEUP_COUNT}}`는
실제 업체 수. `{{PIE_GRADIENT}}`는 CSS `conic-gradient()`에 그대로 들어갈 문자열이라 **누적 퍼센트**로 계산해야 함
(색상 순서 고정: 웨딩홀=#C06080, 드레스=#C5B3D3, 스튜디오=#7FB069, 메이크업=#D9A65B).
예: 웨딩홀 10, 드레스 6, 스튜디오 5, 메이크업 3 (합계 24)이면 각각 41.7% / 25% / 20.8% / 12.5%이고
누적 경계는 0, 41.7, 66.7, 87.5, 100 이므로:
```
#C06080 0% 41.7%, #C5B3D3 41.7% 66.7%, #7FB069 66.7% 87.5%, #D9A65B 87.5% 100%
```
이 문자열 형태 그대로 `{{PIE_GRADIENT}}`에 채울 것 (실제 숫자로 바꿔서).

**본문 9개 섹션**: 숫자만 나열하지 말고, 답례품 평가했을 때처럼 "그래서 어떻게 하면 좋을지"까지 문장으로 써줄 것.
`{{ACTION_ITEMS}}` 자리는 `<li>...</li>` 여러 개로 채울 것.

## 4. PDF로 변환
chrome-devtools-mcp 브라우저 도구로 방금 만든 HTML 파일을 열고, 인쇄(PDF로 저장) 기능으로 PDF 파일을 워크스페이스에 저장.
그래프/색상이 깨지지 않고 제대로 보이는지 확인 후 저장할 것.

## 5. 백엔드에 업로드
```powershell
$headers = @{ "X-OpenClaw-Key" = $env:OPENCLAW_INTERNAL_KEY }
$form = @{
  file = Get-Item "<방금 만든 PDF 경로>"
  weekOf = "<3번에서 계산한 실제 월요일 날짜, 예: 2026-07-20>"
  summaryText = "<2~3줄 요약>"
  lowStockCount = <숫자>
  flaggedPostCount = <숫자>
  siteIssueCount = <숫자>
}
Invoke-RestMethod -Uri "http://localhost:8080/api/openclaw/ai-briefing" -Method Post -Headers $headers -Form $form
```

## 마무리
업로드 성공했는지 확인하고, 무엇을 리포트에 담았는지 짧게 요약해서 남길 것.
