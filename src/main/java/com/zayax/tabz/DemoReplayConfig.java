package com.zayax.tabz;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.nio.file.Path;
import java.nio.file.Paths;

final class DemoReplayConfig {
    @TestOnly
    static boolean allowNonDemoProjectForTests;
    static final int FORMAT_VERSION = 1;
    static final int DEBOUNCE_MS = 400;
    static final int MAX_EVENTS = 500;

    private DemoReplayConfig() {
    }

    static boolean isRecordingEnabled() {
        return "record".equalsIgnoreCase(System.getProperty("tabz.demo.replay", "").trim());
    }

    static @NotNull Path outputDirectory() {
        String configured = System.getProperty("tabz.demo.replay.dir", "").trim();
        if (!configured.isEmpty()) {
            return Paths.get(configured);
        }
        return Paths.get("demo-replay");
    }
}
