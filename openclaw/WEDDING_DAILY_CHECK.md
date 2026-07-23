# 웨딩 플랫폼 일간 점검 스킬

매일 새벽 크론으로 실행됨. 목표: 사이트 상태와 게시글을 점검해서 결과를 관리자 백엔드에 보고.

## 사전 조건
- 백엔드가 `http://localhost:8080`에서 떠 있어야 함 (프론트는 `http://localhost:3000`, Vite/React CSR).
- 인증: 아래 두 보고용 API는 로그인 세션이 없으므로 헤더 `X-OpenClaw-Key: $env:OPENCLAW_INTERNAL_KEY` 를 붙여야 함.
  PowerShell에서는 `$env:OPENCLAW_INTERNAL_KEY` 로 값을 바로 읽을 수 있음 (이미 사용자 환경변수로 설정돼있음).
- **한글 인코딩 주의**: 한글이 들어간 `reason`/`detail` 텍스트를 PowerShell 커맨드 문자열 안에 직접 타이핑해서 넘기면, 이 실행 환경의 콘솔 코드페이지 문제로 한글이 전부 `?`로 깨져서 서버에 저장됨 (실제로 이 문제가 발생한 적 있음). 그래서 아래 두 보고 API 모두, 한글이 들어가는 JSON 바디는 **반드시 파일쓰기 도구로 UTF-8 파일에 먼저 저장한 뒤, 그 파일을 바이트 그대로 읽어서 전송**할 것 (PowerShell 커맨드 문자열에 한글을 직접 넣지 말 것). 아래 각 섹션의 예시가 이 방식을 따름.

## 1. 사이트 이상 징후 — 상품 이미지 깨짐만 확인 (상시, 전체 상품 대상)

**범위가 이것 하나로 단순화됨 — 콘솔 에러/페이지 오류는 더 이상 확인하지 않음.**

**주의: 상품마다 브라우저를 열어서 하나씩 확인하면 매번 페이지 로딩 시간이 쌓여서 전체 실행이 크게 느려짐(실제로 이것 때문에 6분 이상 걸린 적 있음) — 브라우저로 하나씩 열어보지 말고, 아래처럼 스크립트 하나로 전체 상품을 한 번에 판정할 것.**

`/api/product/list` 응답의 `dtoList`에는 상품마다 `uploadFileNames` 배열이 이미 들어있어서, 이 배열이 비어있으면("이미지 미등록") 그것만으로 바로 판정 가능하고, 파일명이 있으면 그 파일이 실제로 로드되는지(`/api/product/view/{파일명}`이 200을 반환하는지, 즉 "이미지 깨짐")만 확인하면 됨 — 상품 상세 페이지를 브라우저로 열어볼 필요가 없음. 아래 스크립트가 이 판정을 전체 상품에 대해 한 번에 끝내고 결과를 파일로 저장함:
```powershell
@'
const fs = require("fs");
const headers = { "X-OpenClaw-Key": process.env.OPENCLAW_INTERNAL_KEY || "" };
(async () => {
  const res = await fetch("http://localhost:8080/api/product/list?size=100", { headers });
  const data = await res.json();
  const products = data.dtoList || [];
  const issues = [];
  for (const p of products) {
    if (!p.uploadFileNames || p.uploadFileNames.length === 0) {
      issues.push({ pno: p.pno, pname: p.pname, type: "MISSING" });
      continue;
    }
    const fileName = p.uploadFileNames[0];
    // 주의: method를 "HEAD"로 바꾸지 말 것 — 이 API는 HEAD를 401로 거부함(보안 설정이 GET만 허용). 반드시 GET으로 확인.
    const check = await fetch(`http://localhost:8080/api/product/view/${fileName}`, { method: "GET", headers });
    if (!check.ok) {
      issues.push({ pno: p.pno, pname: p.pname, type: "BROKEN" });
    }
  }
  fs.writeFileSync("C:/Users/hjc13/.openclaw/workspace/_tmp_image_issues.json", JSON.stringify({ totalChecked: products.length, issues }, null, 2), "utf8");
  console.log(JSON.stringify({ totalChecked: products.length, issueCount: issues.length, issues }, null, 2));
})();
'@ | node -
```
그 다음 `_tmp_image_issues.json`(또는 위 콘솔 출력)을 보고 `issues` 배열의 각 항목에 대해 아래처럼 보고할 것. `type`이 `MISSING`이든 `BROKEN`이든 **구분하지 않고 전부 `IMAGE_BROKEN`으로 보고**할 것.
- **detail은 개발자가 아니라 일반 관리자가 읽는 문구다.** 파일명, API 경로, HTTP 상태 코드, `undefined` 같은 개발자 전용 용어/기술 디테일을 절대 넣지 말 것 — **실제로 이 규칙을 어기고 이렇게 잘못 쓴 적이 있음**: `` 상품 '교동복' (pno 4)의 대표 이미지 `imgi_19_1515_temp_...png`를 `/api/product/view/...`로 요청하면 404가 반환됩니다. `` **(절대 이렇게 쓰지 말 것)** — 오직 "상품명(상품번호)에 실제 사진이 없어 기본 이미지가 대신 표시되고 있습니다"(`MISSING`) 또는 "상품명(상품번호)의 이미지가 깨져서 표시되지 않습니다"(`BROKEN`)처럼, 상품명/상품번호와 상황 설명만 담은 한 문장으로 적을 것.
- **pageUrl은 항상 프론트엔드 상품 상세 페이지** `http://localhost:3000/product/read/{pno}` 형식으로 적을 것. `/api/product/view/...`(백엔드 이미지 API 경로)나 다른 형태를 넣지 말 것 — 실제로 이걸 잘못 넣은 적 있음.

문제를 발견할 때마다 아래처럼 즉시 보고 (문제 없으면 아무것도 호출하지 않음):

1. 파일쓰기 도구로 `C:\Users\hjc13\.openclaw\workspace\_tmp_site_health.json`에 아래 형태의 JSON을 UTF-8로 저장 (한글 그대로 넣을 것):
```json
{ "pageUrl": "http://localhost:3000/product/read/<pno>", "issueType": "IMAGE_BROKEN", "detail": "<구체적으로 뭐가 문제였는지, 한글, 개발자 용어 없이>" }
```
2. 그 파일을 바이트 그대로 읽어서 전송:
```powershell
$headers = @{ "X-OpenClaw-Key" = $env:OPENCLAW_INTERNAL_KEY; "Content-Type" = "application/json; charset=utf-8" }
$bodyBytes = [System.IO.File]::ReadAllBytes("C:\Users\hjc13\.openclaw\workspace\_tmp_site_health.json")
Invoke-RestMethod -Uri "http://localhost:8080/api/openclaw/site-health-issues" -Method Post -Headers $headers -Body $bodyBytes
```
`issueType`은 항상 `IMAGE_BROKEN`.

## 2. 게시글/리뷰 이상 감지 — **어제(최근 1일) 올라온 것만** 확인

**전체를 훑지 말고, regDate가 최근 24시간(어제) 이내인 것만 대상으로 함** — 매일 도는 점검이라 이미 확인한 옛날 글을 매번 다시 볼 필요 없음.

**한글 읽기 주의 (GET 응답도 인코딩이 깨짐)**: `Invoke-RestMethod ... | ConvertTo-Json`을 실행해서 나온 결과를 터미널 출력으로 바로 읽으면, 이 실행 환경의 콘솔 코드페이지 문제로 한글 내용(제목/본문/닉네임 등)이 전부 깨진 문자로 보임 (실제로 이 문제가 발생한 적 있음 — 예: `ë¶ììë¬093` 처럼 보임). **터미널 출력을 직접 읽지 말고, 항상 결과를 UTF-8 파일로 저장한 뒤 그 파일을 파일읽기 도구로 읽을 것.** 아래 (a), (b) 모두 이 방식으로 되어있음.

이 사이트의 "게시글"은 두 가지 완전히 다른 도메인으로 나뉘어 있으니 **둘 다** 확인해야 함:

**(a) 커뮤니티 게시판 (자유게시판 + 후기게시판)**
```powershell
$headers = @{ "X-OpenClaw-Key" = $env:OPENCLAW_INTERNAL_KEY }
Invoke-RestMethod -Uri "http://localhost:8080/api/boards/" -Headers $headers | ConvertTo-Json -Depth 6 |
  Out-File -FilePath "C:\Users\hjc13\.openclaw\workspace\_tmp_boards.json" -Encoding utf8
```
(타입 파라미터 없이 호출하면 자유+후기 게시판 전체가 나옴, 공개 API라 헤더 없어도 되지만 있어도 무해함)
그 다음 `_tmp_boards.json`을 파일읽기 도구로 열어서, 각 게시글의 `regDate`가 **최근 24시간 이내인 것만** 골라내서 검사.

**(b) 답례품(상품) 리뷰 — 커뮤니티 게시판과 별개 도메인이라 반드시 따로 확인**

**주의 1: 상품이 여러 개라서 하나씩 따로따로 호출하면(에이전트 턴을 상품 수만큼 반복) 중간에 지쳐서 몇 개만 확인하고 끝내버리기 쉽다 — 실제로 이 문제가 발생해서 리뷰 하나가 통째로 누락된 적 있음.** 그러니 **반드시 아래처럼 상품 전체를 순회하는 로직을 PowerShell 스크립트 하나(한 번의 명령 실행)에 다 담아서**, 최근 24시간 이내 리뷰만 걸러 한 파일에 모아 저장할 것 — 상품 개수만큼 따로 호출하지 말 것.
**주의 2: `/api/product/list`는 페이징 API라 파라미터 없이 부르면 10개만 나옴 — 아래처럼 `size=100`을 반드시 붙일 것** (1번 섹션과 동일한 문제, 실제로 이것 때문에 리뷰 스팸이 있는 상품이 목록에서 아예 빠진 적 있음):
```powershell
$headers = @{ "X-OpenClaw-Key" = $env:OPENCLAW_INTERNAL_KEY }
$products = Invoke-RestMethod -Uri "http://localhost:8080/api/product/list?size=100" -Headers $headers
$cutoff = (Get-Date).AddHours(-24)
$recentReviews = @()
foreach ($p in $products.dtoList) {
  $reviews = Invoke-RestMethod -Uri "http://localhost:8080/api/product/$($p.pno)/reviews" -Headers $headers
  foreach ($r in $reviews) {
    if ([datetime]$r.regDate -gt $cutoff) {
      $recentReviews += $r
    }
  }
}
$recentReviews | ConvertTo-Json -Depth 6 |
  Out-File -FilePath "C:\Users\hjc13\.openclaw\workspace\_tmp_recent_reviews.json" -Encoding utf8
```
(상품 목록 응답 형태가 `dtoList`가 아니라 배열 자체일 수도 있으니, 위 스크립트 실행 전에 `$products`의 실제 구조를 먼저 한 번 확인해서 맞게 조정할 것.)
그 다음 `_tmp_recent_reviews.json`을 파일읽기 도구로 열어서 검사 — 이미 24시간 필터링과 전체 상품 순회가 끝난 상태라 바로 판단만 하면 됨.

(a), (b)에서 골라낸(최근 24시간 이내) 글/리뷰만 아래에 해당하는지 판단:
- 광고/스팸성 홍보 글로 의심됨
- 도배(동일/유사 글 반복)로 보임
- 다른 회원에게 불쾌감을 줄 수 있는 애매한 표현
- 명확한 욕설/비하 표현 (이런 걸 걸러주는 별도 실시간 필터는 아직 없으므로, 이것도 반드시 이 점검에서 직접 잡아서 보고할 것)

의심되는 글/리뷰를 발견하면 (리뷰는 boardId 대신 review의 rno와 소속 상품명을 reason에 명시):

1. 파일쓰기 도구로 `C:\Users\hjc13\.openclaw\workspace\_tmp_flagged_post.json`에 아래 형태의 JSON을 UTF-8로 저장 (한글 그대로 넣을 것):
```json
{ "boardId": <해당 boardId>, "reason": "<왜 의심되는지 구체적으로, 한글>" }
```
2. 그 파일을 바이트 그대로 읽어서 전송:
```powershell
$headers = @{ "X-OpenClaw-Key" = $env:OPENCLAW_INTERNAL_KEY; "Content-Type" = "application/json; charset=utf-8" }
$bodyBytes = [System.IO.File]::ReadAllBytes("C:\Users\hjc13\.openclaw\workspace\_tmp_flagged_post.json")
Invoke-RestMethod -Uri "http://localhost:8080/api/openclaw/flagged-posts" -Method Post -Headers $headers -Body $bodyBytes
```

## 마무리
작업 끝나면 무엇을 확인했고(어제 날짜 기준 게시글/리뷰 몇 건을 검사 대상으로 골랐는지 포함) 무엇을 보고했는지 짧게 요약해서 남길 것 (터미널 출력이면 충분, 별도 파일 저장 불필요).
