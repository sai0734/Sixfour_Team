package com.wedding.admin.dashboard.controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

// 관리자가 대시보드에서 "진단하기" 버튼을 눌러 OpenClaw 크론 작업을 즉시 실행시키는 API.
// 데모/즉시 확인용 - 실제로는 새벽 크론이 자동으로 돎. 같은 PC에 떠있는 OpenClaw CLI를 그대로 호출한다.
//
// 주의 1: "openclaw cron run"은 작업 이름이 아니라 UUID를 받는다(이름을 넣으면
// "id not found"로 조용히 실패함 - 실제로 겪은 버그). UUID는 컴퓨터마다 크론을 새로 등록할 때
// 랜덤 생성되므로 하드코딩하면 다른 컴퓨터에서 못 씀 - 그래서 매번 "openclaw cron list --json"으로
// 이름 기준 조회해서 사용한다 (이름만 똑같이 등록해두면 어느 컴퓨터에서든 동작).
// 주의 2: 이 CLI는 "이미 실행 중이라 이번엔 안 돌렸다"(ok:true, ran:false) 같은 정상적인
// 상황에서도 프로세스 exit code를 0이 아니게 반환한다. 그래서 exit code가 아니라 실제
// JSON 응답의 "ok" 필드를 봐야 진짜 실패인지 판단할 수 있다 - 이것도 실제로 겪은 버그.
@RestController
@Log4j2
@RequestMapping("/api/admin/openclaw")
@PreAuthorize("hasAnyRole('ADMIN')")
public class AdminOpenClawController {

    private static final String DAILY_CHECK_JOB_NAME = "wedding-daily-check";
    private static final String WEEKLY_BRIEFING_JOB_NAME = "wedding-weekly-briefing";

    private final Gson gson = new Gson();

    @PostMapping("/trigger-daily-check")
    public ResponseEntity<?> triggerDailyCheck() {
        return runCronJobByName(DAILY_CHECK_JOB_NAME);
    }

    @PostMapping("/trigger-weekly-briefing")
    public ResponseEntity<?> triggerWeeklyBriefing() {
        return runCronJobByName(WEEKLY_BRIEFING_JOB_NAME);
    }

    // 특정 작업 하나만 골라서 취소하는 CLI 명령이 없어서, 게이트웨이 자체를 재시작하는 방식으로
    // 처리한다. 지금 도는 모든 작업(일간/주간 둘 다 돌고 있었다면 둘 다)이 같이 중단되는
    // 다소 거친 방법이라 프론트에서 확인창을 띄우고 호출해야 함.
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelRunningJob() {
        try {
            String outputText = runCommand(30, "daemon", "restart");
            log.info("OpenClaw 게이트웨이 재시작(취소) 결과: {}", outputText);
            return ResponseEntity.ok().body(outputText);
        } catch (Exception e) {
            log.error("OpenClaw 게이트웨이 재시작 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ResponseEntity<?> runCronJobByName(String jobName) {
        String jobId;
        try {
            jobId = resolveJobId(jobName);
        } catch (Exception e) {
            log.error("OpenClaw 크론 작업 ID 조회 실패 [{}]: {}", jobName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("크론 작업을 찾을 수 없습니다: " + jobName);
        }

        try {
            String outputText = runCommand("cron", "run", jobId);
            log.info("OpenClaw 크론 트리거 결과 [{}]: {}", jobName, outputText);

            // exit code가 아니라 실제 응답 JSON의 "ok" 필드로 성공 여부를 판단
            Boolean ok = null;
            String reason = null;
            try {
                JsonObject json = gson.fromJson(outputText.trim(), JsonObject.class);
                if (json != null && json.has("ok")) {
                    ok = json.get("ok").getAsBoolean();
                }
                if (json != null && json.has("reason")) {
                    reason = json.get("reason").getAsString();
                }
            } catch (JsonSyntaxException ignored) {
                // 파싱 실패하면 아래에서 ok==null로 처리
            }

            if (Boolean.TRUE.equals(ok)) {
                return ResponseEntity.accepted().body(outputText);
            }

            log.error("OpenClaw 크론 트리거 실패 [{}]: {}", jobName, outputText);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(reason != null ? reason : outputText);

        } catch (Exception e) {
            log.error("OpenClaw 크론 작업 실행 실패: {} - {}", jobName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // "openclaw cron list --json" 결과에서 이름이 일치하는 작업의 UUID를 찾는다
    private String resolveJobId(String jobName) throws Exception {
        String output = runCommand("cron", "list", "--json");
        JsonObject json = gson.fromJson(output.trim(), JsonObject.class);
        JsonArray jobs = json.getAsJsonArray("jobs");

        for (JsonElement el : jobs) {
            JsonObject job = el.getAsJsonObject();
            if (jobName.equals(job.get("name").getAsString())) {
                return job.get("id").getAsString();
            }
        }

        throw new IllegalStateException("openclaw cron list에 '" + jobName + "' 작업이 없음");
    }

    private String runCommand(String... openclawArgs) throws Exception {
        return runCommand(10, openclawArgs);
    }

    private String runCommand(int timeoutSeconds, String... openclawArgs) throws Exception {
        String[] fullCommand = new String[openclawArgs.length + 3];
        fullCommand[0] = "cmd";
        fullCommand[1] = "/c";
        fullCommand[2] = "openclaw";
        System.arraycopy(openclawArgs, 0, fullCommand, 3, openclawArgs.length);

        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }

        process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        return output.toString();
    }
}
