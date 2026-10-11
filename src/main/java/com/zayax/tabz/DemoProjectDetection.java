package com.zayax.tabz;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

final class DemoProjectDetection {
    private DemoProjectDetection() {
    }

    static boolean isDemoProject(@NotNull Project project) {
        String basePath = project.getBasePath();
        if (basePath == null || basePath.isBlank()) {
            return false;
        }
        String normalized = basePath.replace('\\', '/');
        return normalized.endsWith("/demo-project") || normalized.endsWith("demo-project");
    }
}
