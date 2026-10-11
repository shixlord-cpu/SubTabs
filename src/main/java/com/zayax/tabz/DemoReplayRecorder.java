package com.zayax.tabz;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service(Service.Level.PROJECT)
final class DemoReplayRecorder implements Disposable {
    private static final Logger LOG = Logger.getInstance(DemoReplayRecorder.class);
    private static final DateTimeFormatter SESSION_ID_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneId.systemDefault());

    private final Project project;
    private final Alarm debounceAlarm;
    private final String sessionId;
    private final long sessionStartedEpochMs = System.currentTimeMillis();
    private final List<RecordedEvent> events = new ArrayList<>();
    private @Nullable String pendingTrigger;
    private boolean flushed;

    DemoReplayRecorder(@NotNull Project project) {
        this.project = project;
        this.debounceAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, this);
        this.sessionId = SESSION_ID_FORMAT.format(Instant.now()) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    static @Nullable DemoReplayRecorder getInstance(@NotNull Project project) {
        if (!DemoReplayConfig.isRecordingEnabled()) {
            return null;
        }
        if (!DemoProjectDetection.isDemoProject(project) && !DemoReplayConfig.allowNonDemoProjectForTests) {
            return null;
        }
        return project.getService(DemoReplayRecorder.class);
    }

    void onEditorStateChanged(@NotNull String trigger) {
        if (project.isDisposed()) {
            return;
        }
        pendingTrigger = trigger;
        debounceAlarm.cancelAllRequests();
        debounceAlarm.addRequest(this::capturePendingSnapshot, DemoReplayConfig.DEBOUNCE_MS);
    }

    void captureNow(@NotNull String trigger) {
        pendingTrigger = trigger;
        debounceAlarm.cancelAllRequests();
        capturePendingSnapshot();
    }

    private void capturePendingSnapshot() {
        if (project.isDisposed()) {
            return;
        }
        String trigger = pendingTrigger == null ? "snapshot" : pendingTrigger;
        pendingTrigger = null;
        long elapsedMs = System.currentTimeMillis() - sessionStartedEpochMs;
        String stateJson = DemoReplaySnapshotBuilder.capture(project).build();
        events.add(new RecordedEvent(elapsedMs, trigger, stateJson));
        if (events.size() > DemoReplayConfig.MAX_EVENTS) {
            events.remove(0);
        }
        writeLatest(trigger, stateJson, false);
    }

    private void writeLatest(@NotNull String trigger, @NotNull String stateJson, boolean finalFlush) {
        try {
            Path outputDir = DemoReplayConfig.outputDirectory();
            Files.createDirectories(outputDir);
            String summary = buildSummary(stateJson);
            DemoReplayJson latest = DemoReplayJson.object()
                    .key("formatVersion").value(DemoReplayConfig.FORMAT_VERSION)
                    .key("sessionId").value(sessionId)
                    .key("sessionStartedEpochMs").value(sessionStartedEpochMs)
                    .key("lastUpdatedEpochMs").value(System.currentTimeMillis())
                    .key("finalFlush").value(finalFlush)
                    .key("lastTrigger").value(trigger)
                    .key("eventCount").value(events.size())
                    .key("summary").value(summary)
                    .key("lastState").rawValue(stateJson);
            Files.writeString(outputDir.resolve("latest.json"), latest.build(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOG.warn("Demo replay could not write latest.json", exception);
        }
    }

    private void flushSession() {
        if (flushed || events.isEmpty()) {
            return;
        }
        flushed = true;
        String lastState = events.get(events.size() - 1).stateJson();
        writeLatest("session_end", lastState, true);
        try {
            Path outputDir = DemoReplayConfig.outputDirectory();
            Files.createDirectories(outputDir);
            Path sessionsDir = outputDir.resolve("sessions");
            Files.createDirectories(sessionsDir);

            DemoReplayJson session = DemoReplayJson.object()
                    .key("formatVersion").value(DemoReplayConfig.FORMAT_VERSION)
                    .key("sessionId").value(sessionId)
                    .key("sessionStartedEpochMs").value(sessionStartedEpochMs)
                    .key("sessionEndedEpochMs").value(System.currentTimeMillis())
                    .key("summary").value(buildSummary(lastState))
                    .key("eventCount").value(events.size());
            session.arrayStart("events");
            for (RecordedEvent event : events) {
                session.element(DemoReplayJson.object()
                        .key("elapsedMs").value(event.elapsedMs())
                        .key("trigger").value(event.trigger())
                        .key("state").rawValue(event.stateJson())
                        .endObject());
            }
            session.arrayEnd();
            session.key("finalState").rawValue(lastState);

            Path sessionFile = sessionsDir.resolve(sessionId + ".json");
            Files.writeString(sessionFile, session.build(), StandardCharsets.UTF_8);
            Files.writeString(
                    outputDir.resolve("last-session.txt"),
                    sessionFile.toAbsolutePath().normalize().toString(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            LOG.warn("Demo replay could not write session file", exception);
        }
    }

    private @NotNull String buildSummary(@NotNull String stateJson) {
        String selected = extractJsonString(stateJson, "selectedFile");
        if (selected == null || selected.isBlank()) {
            return "Demo session without selected editor file";
        }
        return "Selected: " + selected;
    }

    private static @Nullable String extractJsonString(@NotNull String json, @NotNull String key) {
        String needle = "\"" + key + "\":";
        int start = json.indexOf(needle);
        if (start < 0) {
            return null;
        }
        start += needle.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length()) {
            return null;
        }
        if (json.startsWith("null", start)) {
            return null;
        }
        if (json.charAt(start) != '"') {
            return null;
        }
        start++;
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int index = start; index < json.length(); index++) {
            char ch = json.charAt(index);
            if (escaped) {
                value.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                return value.toString();
            }
            value.append(ch);
        }
        return null;
    }

    @Override
    public void dispose() {
        debounceAlarm.cancelAllRequests();
        if (!events.isEmpty()) {
            flushSession();
        } else if (pendingTrigger != null) {
            capturePendingSnapshot();
            flushSession();
        }
    }

    private record RecordedEvent(long elapsedMs, @NotNull String trigger, @NotNull String stateJson) {
    }
}
