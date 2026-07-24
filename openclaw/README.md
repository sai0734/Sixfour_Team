# OpenClaw 연동 셋업 가이드

이 프로젝트의 관리자 대시보드는 [OpenClaw](https://github.com/openclaw/openclaw)라는 로컬 개인 자동화
에이전트를 이용해서 두 가지를 자동으로 처리한다:

- **일간 사이트 헬스체크**: 매일 새벽, 답례품 상품 이미지가 깨졌거나 없는지(상시, 전체 상품 대상), 그리고
  최근 24시간 이내 올라온 게시글/리뷰 중 스팸·도배·욕설·애매한 공격성 표현이 있는지 찾아서
  관리자 대시보드 알림 패널에 보고
- **주간 AI 매니저 브리핑**: 매주 월요일 새벽, 회원/매출/업체/상품 현황을 종합해서 PDF 리포트 생성

**중요**: OpenClaw는 "1인 1설치"가 기본인 개인용 로컬 도구다. git으로 코드를 받아도 이 자동화 자체는
따라오지 않는다 — 아래 절차를 새 컴퓨터마다 각자 진행해야 한다.

---

## 0. 사전 준비

- Node.js v22 이상
- Windows + Google Chrome (스킬이 Chrome 실행 파일 경로를 참조함)
- 백엔드 프로젝트가 정상적으로 켜지는 상태 (`wedding_back`, 기본 포트 8080)
- 프론트가 정상적으로 켜지는 상태 (`wedding_front`, 기본 포트 3000)

## 1. OpenClaw 설치

```powershell
npm install -g openclaw@latest
openclaw --version
```

`npm approve-scripts --allow-scripts-pending` 물어보면 승인.

## 2. 온보딩

```powershell
openclaw onboard --install-daemon
```

대화형으로 진행되며, 아래 항목들을 선택:
- Model/auth provider: OpenAI (또는 원하는 provider) — **이 프로젝트의 OpenAI 키와는 별개로 새로 발급받는 걸 권장**
  (같은 키를 쓰면 사용량/과금이 프로젝트 챗봇이랑 섞여서 구분이 안 됨)
- 채널: Telegram (온보딩상 필수로 뭔가 하나 선택해야 함 — BotFather에서 새 봇 토큰 발급받아 입력.
  실제로 이 채널을 통해 알림을 받거나 하지는 않지만, 온보딩 완료를 위해 필요함)
- Web search: 아무거나(key-free 옵션 추천, 예: DuckDuckGo) — 필수 기능은 아님

온보딩 끝나면 텔레그램 DM 정책을 잠그는 걸 권장:
```powershell
openclaw config set channels.telegram.dmPolicy "allowlist"
openclaw config set channels.telegram.allowFrom '["<본인 텔레그램 user id>"]'
```

## 3. 스킬 파일 복사

이 저장소의 `openclaw/` 폴더에 있는 파일 3개를 OpenClaw 워크스페이스로 그대로 복사:

```powershell
$ws = "$env:USERPROFILE\.openclaw\workspace"
Copy-Item "openclaw\WEDDING_DAILY_CHECK.md" "$ws\WEDDING_DAILY_CHECK.md"
Copy-Item "openclaw\WEDDING_WEEKLY_BRIEFING.md" "$ws\WEDDING_WEEKLY_BRIEFING.md"
Copy-Item "openclaw\WEEKLY_BRIEFING_TEMPLATE.html" "$ws\WEEKLY_BRIEFING_TEMPLATE.html"
```
(프로젝트 루트에서 실행한다고 가정. 경로 다르면 맞게 조정)

## 4. 인증키 설정 — **여기서 제일 많이 삽질함, 주의 깊게 읽을 것**

우리 백엔드와 OpenClaw가 서로 통신할 때 쓰는 고정 시크릿 키를 만든다.

```powershell
$bytes = New-Object byte[] 32
$rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
$rng.GetBytes($bytes)
$key = ($bytes | ForEach-Object { $_.ToString("x2") }) -join ""
Write-Output $key
setx OPENCLAW_INTERNAL_KEY $key
```

**⚠️ 매우 중요**: `setx`는 레지스트리에만 즉시 반영되고, **이미 실행 중인 프로그램(터미널, IDE 등)은 이 값을
모른다.** 아래 두 프로세스가 반드시 이 키를 알아야 하는데, 각각 새 값이 반영되도록 확실히 해야 함:

1. **백엔드(IntelliJ 등)**: IDE를 "재실행"만 하지 말고 **IDE 자체를 완전히 껐다가 다시 켤 것.**
   (재실행 버튼만 누르면 IDE가 자기 시작 시점의 옛날 환경변수를 그대로 물려줘서 절대 반영 안 됨 — 실제로 겪은 삽질)
2. **OpenClaw 게이트웨이**: **`openclaw daemon restart`로는 부족함 — 실제로 새 프로세스가 안 뜨고 옛날 환경변수를
   계속 들고 있는 경우가 있었음.** 대신 아래처럼 완전히 껐다가 켤 것:
   ```powershell
   openclaw gateway stop
   # 포트가 완전히 풀렸는지 확인 (아무 출력도 없어야 함)
   netstat -ano | findstr ":18789"
   openclaw gateway start
   ```

두 프로세스 다 새로 띄운 뒤, 아래로 직접 검증:
```powershell
$headers = @{ "X-OpenClaw-Key" = $env:OPENCLAW_INTERNAL_KEY; "Content-Type" = "application/json" }
$body = @{ pageUrl = "http://localhost:3000/test"; issueType = "PAGE_ERROR"; detail = "key test" } | ConvertTo-Json
Invoke-WebRequest -Uri "http://localhost:8080/api/openclaw/site-health-issues" -Method Post -Headers $headers -Body $body -UseBasicParsing
```
`200`이 나오면 성공 (테스트로 생성된 더미 항목은 대시보드에서 "처리 완료"로 지우면 됨). `403`이 나오면 위 1·2번을
다시 확인.

## 5. 크론 작업 등록

```powershell
openclaw cron add "wedding-daily-check" "웨딩 프로젝트 워크스페이스의 WEDDING_DAILY_CHECK.md 파일을 읽고, 거기 적힌 지시대로 오늘의 사이트 헬스체크와 게시글 점검을 수행해." --cron "0 3 * * *" --description "매일 새벽 3시 - 웨딩 플랫폼 사이트 헬스체크 + 게시글 이상 감지" --model "openai/gpt-5.4-mini" --no-deliver

openclaw cron add "wedding-weekly-briefing" "웨딩 프로젝트 워크스페이스의 WEDDING_WEEKLY_BRIEFING.md 파일을 읽고, 거기 적힌 지시대로 이번 주 종합 브리핑을 만들어서 PDF로 업로드해." --cron "0 4 * * 1" --description "매주 월요일 새벽 4시 - 웨딩 플랫폼 AI 매니저 주간 브리핑" --model "openai/gpt-5.4-mini" --no-deliver
```

**작업 이름은 정확히 `wedding-daily-check` / `wedding-weekly-briefing`으로 맞춰야 함** — 백엔드의
"지금 바로 진단하기" 버튼이 이 이름으로 작업을 찾아서 실행시키기 때문 (UUID는 컴퓨터마다 자동 생성되는 값이라
신경 안 써도 됨).

**`--model`은 꼭 `openclaw models list`에 실제로 등록된 모델로 지정할 것** (`models set`은 등록 안 된 모델명도
일단 받아주지만, 실행 시점에 `Unknown model` 에러로 실패함 — 실제로 겪은 문제). `openai/gpt-5.4-mini`는 이 프로젝트
기준으로 확인된 값이지만, 온보딩 시 고른 provider/계정에 따라 사용 가능한 모델이 다를 수 있으니 다르면
`openclaw models list`로 직접 확인해서 맞는 값으로 바꿀 것.

**`--no-deliver`는 꼭 넣을 것** — 기본 배송 설정(`announce -> last`)은 텔레그램 채팅방으로 결과를 보내려고 시도하는데,
이 자동화는 텔레그램으로 알림을 안 받게 만든 거라 배송 대상이 없어서 매번 실패하고, 그 실패가 크론 작업 자체의
"에러"로 잘못 집계됨 (실제 작업은 성공했는데도 `openclaw cron list`에 `error`로 나옴). `--no-deliver`를 빼먹었다면
나중에라도 `openclaw cron edit <이름 또는 id> --no-deliver`로 고칠 수 있음.

## 5-1. 대화 세션 기본 모델/thinking 맞추기 (선택)

크론(위 5번)에 준 `--model`은 크론 작업 전용이고, **사람이 직접 `openclaw chat`으로 대화할 때 쓰는 기본
모델/사고 깊이는 별도 설정**이라 이것도 컴퓨터마다 새로 맞춰야 함(마찬가지로 git에 안 따라옴):

```powershell
openclaw config set agents.defaults.model.primary "openai/gpt-5.4-mini"
openclaw config set agents.defaults.thinkingDefault "medium"
```

게이트웨이 재시작 불필요, 바로 적용됨. 단 **이미 열려있는 세션엔 소급 적용 안 됨** — 그 세션은 그 세션 안에서
`/model <provider/model>`, `/think <off|minimal|low|medium|high>`로 직접 바꾸거나, `/new`로 새 세션을 열어야
방금 바꾼 기본값을 물려받음. 터미널 UI에서는 `Ctrl+L`로 모델 선택기(화살표로 목록 선택)도 뜸.

## 6. 확인

1. `openclaw gateway status` → `Runtime: running` 확인
2. 백엔드 관리자 대시보드 → 우측 "OpenClaw 알림" 패널의 "지금 바로 진단하기" 클릭
3. AI 주간 브리핑 페이지 → "지금 바로 브리핑 생성하기" 클릭
4. 몇 분 뒤 새로고침해서 결과 확인 (오래 걸리면 `openclaw cron list`로 상태(`running`/`idle`) 확인 가능)

---

## 자주 겪는 문제

| 증상 | 원인 |
|---|---|
| 버튼 눌러도 계속 403 | `OPENCLAW_INTERNAL_KEY`를 백엔드/게이트웨이 둘 중 하나(또는 둘 다)가 아직 못 읽음. 4번 다시 확인 |
| "id not found" 에러 | 크론 작업 이름이 `wedding-daily-check`/`wedding-weekly-briefing`과 다름 |
| "already-running" | 이전 실행이 아직 안 끝남. 정상 상황이니 기다렸다가 재시도 |
| PDF에 한글이 깨짐/안 보임 | 이 PC에 맑은 고딕 폰트가 없는 경우 (윈도우 기본 폰트라 보통은 있음) |
| 실제로는 성공했는데 `openclaw cron list`엔 계속 `error`로 뜸 | `--no-deliver` 없이 등록한 경우. `openclaw cron edit <이름> --no-deliver`로 고치면 됨 |
| `Unknown model` 에러로 크론 실행 자체가 실패 | `--model`로 지정한 모델이 실제로 `openclaw models list`에 없는 값. 등록된 모델명으로 다시 `cron edit --model`로 변경 |
| 같은 주에 "지금 바로 브리핑 생성하기"를 여러 번 눌렀는데 두 번째부터 이전 실행 내용을 그대로 재업로드함 | 워크스페이스에 같은 날짜의 `briefing_YYYY-MM-DD.html/.pdf`가 이미 있어서 재사용된 것. 최신 `WEDDING_WEEKLY_BRIEFING.md`는 파일명에 시:분:초를 포함하도록 되어있어서 정상적으로는 안 겪는 문제지만, 혹시 발생하면 워크스페이스의 낡은 `briefing_*.html`/`.pdf`를 지우고 재시도 |
| 환경변수(API 키 등)를 바꿨는데 크론 실행에 옛날 값이 계속 쓰임 | `openclaw daemon restart`는 새 환경변수를 못 읽어올 때가 있음. 4번 설명대로 `gateway stop` → (포트 안 잡히는지 확인) → `gateway start`로 완전히 새로 띄울 것 |
