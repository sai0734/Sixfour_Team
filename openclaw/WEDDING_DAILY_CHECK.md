# 웨딩 플랫폼 일간 점검 스킬

매일 새벽 크론으로 실행됨. 목표: 사이트 상태와 게시글을 점검해서 결과를 관리자 백엔드에 보고.

## 사전 조건
- 백엔드가 `http://localhost:8080`에서 떠 있어야 함 (프론트는 `http://localhost:3000`, Vite/React CSR).
- 인증: 아래 두 보고용 API는 로그인 세션이 없으므로 헤더 `X-OpenClaw-Key: $env:OPENCLAW_INTERNAL_KEY` 를 붙여야 함.
  PowerShell에서는 `$env:OPENCLAW_INTERNAL_KEY` 로 값을 바로 읽을 수 있음 (이미 사용자 환경변수로 설정돼있음).

## 1. 사이트 헬스체크 (브라우저 도구 사용, chrome-devtools-mcp)
다음 페이지들을 순회하며 렌더링을 직접 확인 (단순 web_fetch는 CSR이라 안 됨, 반드시 브라우저로 열어서 볼 것):
- `http://localhost:3000/` (메인)
- `http://localhost:3000/product/list` 및 상품 목록에서 샘플 3~5개 상세 페이지
- `http://localhost:3000/companies` 및 업체 목록에서 샘플 3~5개 상세 페이지

각 페이지에서 확인할 것:
- 이미지가 진짜로 깨져서(브라우저의 깨진 이미지 아이콘, alt 텍스트만 보임) 나오는지.
  **주의**: 상품에 사진이 0장이어도 백엔드가 자동으로 기본 이미지(winter.jpg)를 대신 내려주기 때문에
  "이미지가 안 떴다"고 오해하면 안 됨 — 그림 자체는 정상적으로 로드되고 있으면 깨짐이 아님.
  진짜 깨진 경우(요청 자체가 실패하거나 브라우저가 깨진 아이콘을 그리는 경우)만 `IMAGE_BROKEN`으로 보고.
- **상품 사진 미등록(기본 이미지 노출) 확인**: 사진이 없는 상품은 `<img>`의 `src`에 실제 파일명 대신
  `undefined`가 들어간 걸로 알 수 있음 (예: `.../api/product/view/s_undefined`). 개발자도구의
  네트워크 탭이나 요소 검사로 `src`에 `undefined`가 포함된 상품 이미지를 발견하면, 깨짐이 아니라
  `IMAGE_MISSING`(이미지 미등록)으로 별도 보고. 상품명/번호를 detail에 포함할 것.
- 브라우저 콘솔에 에러가 찍히는지
- 페이지 자체가 에러 화면을 띄우거나 빈 화면인지

문제를 발견할 때마다 아래 API로 즉시 보고 (문제 없으면 아무것도 호출하지 않음):

```powershell
$headers = @{ "X-OpenClaw-Key" = $env:OPENCLAW_INTERNAL_KEY; "Content-Type" = "application/json" }
$body = @{ pageUrl = "<문제가 있었던 URL>"; issueType = "IMAGE_BROKEN"; detail = "<구체적으로 뭐가 문제였는지>" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/openclaw/site-health-issues" -Method Post -Headers $headers -Body $body
```
`issueType`은 `IMAGE_BROKEN` / `IMAGE_MISSING` / `CONSOLE_ERROR` / `PAGE_ERROR` 중 하나.

**CONSOLE_ERROR를 보고할 때 중요**: 관리자는 개발자가 아니고, 브라우저 개발자도구도 열어볼 수 없음.
`TypeError: Cannot read properties of undefined...` 같은 원문 에러 메시지를 그대로 보여줘도 관리자는
이해도 못 하고 아무것도 할 수 없음 — 즉 원문을 붙이든 안 붙이든 관리자 입장에서는 똑같이 쓸모없음.

그러니 콘솔 원문 에러는 **너 스스로 원인을 파악하는 용도로만** 쓰고, `detail`에는 그 에러 때문에
**실제로 무엇이 안 되는지(증상)를 관리자가 이해할 수 있는 평범한 말**로 바꿔서 적을 것.
- 나쁜 예: `detail = "TypeError: Cannot read properties of undefined (reading 'category') at ProductGridComponent.jsx:34"`
- 좋은 예: `detail = "업체 목록 페이지에서 일부 카드가 정상적으로 표시되지 않고 있습니다. 개발팀 확인이 필요합니다."`

핵심은 "어떤 페이지에서, 무엇이 이상하게 보이거나 안 되는지"를 사용자 관점에서 설명하는 것.
기술적인 원인/스택트레이스는 detail에 넣지 말 것.

## 2. 게시글/리뷰 이상 감지

이 사이트의 "게시글"은 두 가지 완전히 다른 도메인으로 나뉘어 있으니 **둘 다** 확인해야 함:

**(a) 커뮤니티 게시판 (자유게시판 + 후기게시판)**
`GET http://localhost:8080/api/boards/` (타입 파라미터 없이 호출하면 자유+후기 게시판 전체가 한번에 나옴, 공개 API, 인증 불필요)

**(b) 답례품(상품) 리뷰 — 커뮤니티 게시판과 별개 도메인이라 반드시 따로 확인**
`GET http://localhost:8080/api/product/list`로 상품 목록을 가져온 뒤, 각 상품마다
`GET http://localhost:8080/api/product/{pno}/reviews`를 호출해서 리뷰 내용을 확인 (둘 다 공개 API).
상품이 많으면 최근 등록/리뷰 많은 상품 위주로 샘플링해도 됨 (전 상품 전수조사는 비효율적).

(a), (b) 각각의 글/리뷰 내용이 아래에 해당하는지 판단:
- 광고/스팸성 홍보 글로 의심됨
- 도배(동일/유사 글 반복)로 보임
- 명확한 욕설은 아니지만 다른 회원에게 불쾌감을 줄 수 있는 애매한 표현

**주의**: 명백한 금칙어/욕설은 백엔드가 작성 즉시 별도로 실시간 처리하므로 여기서 다시 잡을 필요 없음. 애매해서 사람 판단이 필요한 것만 보고.

의심되는 글/리뷰를 발견하면 (리뷰는 boardId 대신 review의 rno와 소속 상품명을 reason에 명시):
```powershell
$headers = @{ "X-OpenClaw-Key" = $env:OPENCLAW_INTERNAL_KEY; "Content-Type" = "application/json" }
$body = @{ boardId = <해당 boardId>; reason = "<왜 의심되는지 구체적으로>" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/openclaw/flagged-posts" -Method Post -Headers $headers -Body $body
```

## 마무리
작업 끝나면 무엇을 확인했고 무엇을 보고했는지 짧게 요약해서 남길 것 (터미널 출력이면 충분, 별도 파일 저장 불필요).
