package com.zayax.tabz;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Real {@code runIde} check: open demo-project files and resolve Navigate-to-Declaration targets
 * the same way the editor does ({@link TargetElementUtil}). Enabled with
 * {@code -Dtabz.demo.verifyNavigation=<report dir>}.
 */
final class DemoNavigationVerifyHarness {
    private static final Logger LOG = Logger.getInstance(DemoNavigationVerifyHarness.class);
    private static final String PROPERTY = "tabz.demo.verifyNavigation";

    static void runIfEnabled() {
        String output = System.getProperty(PROPERTY, "").trim();
        if (output.isEmpty()) {
            return;
        }
        LOG.warn("[nav-verify] harness starting, output=" + output);
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            int exitCode = 1;
            Path reportDir = Path.of(output);
            StringBuilder report = new StringBuilder();
            try {
                Files.createDirectories(reportDir);
                Project project = awaitDemoProject();
                DumbService.getInstance(project).waitForSmartMode();
                Thread.sleep(12_000);
                boolean dumb = DumbService.getInstance(project).isDumb();
                line(report, "project=" + project.getName() + " dumbAfterWait=" + dumb);

                List<CaseResult> results = new ArrayList<>();
                results.add(verify(project, report,
                        "java-standalone",
                        "sidetabs-examples/java/NavigationHoverDemoStandalone.java",
                        "tabzNavigationDemoTarget();"));
                results.add(verify(project, report,
                        "ts-standalone",
                        "src/app/navigation-hover-demo/navigation-hover-demo.standalone.ts",
                        "tabzNavigationDemoTarget();"));
                results.add(verify(project, report,
                        "ts-control-user-card",
                        "src/app/user-card.component.ts",
                        "@Component"));

                long failed = results.stream().filter(result -> !result.passed()).count();
                line(report, "SUMMARY passed=" + (results.size() - failed) + " failed=" + failed);
                for (CaseResult result : results) {
                    line(report, "  " + result.id() + " passed=" + result.passed()
                            + " language=" + result.language() + " detail=" + result.detail());
                }
                Files.writeString(reportDir.resolve("report.txt"), report.toString(), StandardCharsets.UTF_8);
                boolean javaOk = results.stream().anyMatch(result -> "java-standalone".equals(result.id()) && result.passed());
                exitCode = javaOk ? 0 : 1;
                Files.writeString(reportDir.resolve("exit-code.txt"), Integer.toString(exitCode), StandardCharsets.UTF_8);
            } catch (Throwable throwable) {
                LOG.error("[nav-verify] failed", throwable);
                try {
                    line(report, "FATAL " + throwable);
                    Files.writeString(reportDir.resolve("report.txt"), report.toString(), StandardCharsets.UTF_8);
                    Files.writeString(reportDir.resolve("exit-code.txt"), "1", StandardCharsets.UTF_8);
                } catch (Exception ignored) {
                    // best effort
                }
            }
            if (Boolean.parseBoolean(System.getProperty("tabz.demo.verifyNavigation.exit", "true"))) {
                int code = exitCode;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                ApplicationManager.getApplication().invokeLater(
                        () -> ApplicationManager.getApplication().exit(code == 0, true, false),
                        ModalityState.any()
                );
            }
        });
    }

    private static @NotNull Project awaitDemoProject() throws InterruptedException {
        while (true) {
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                if (project.isInitialized() && !project.isDisposed() && DemoProjectDetection.isDemoProject(project)) {
                    return project;
                }
            }
            Thread.sleep(500);
        }
    }

    private record CaseResult(@NotNull String id, boolean passed, @Nullable String language, @NotNull String detail) {
    }

    private static @NotNull CaseResult verify(
            @NotNull Project project,
            @NotNull StringBuilder report,
            @NotNull String id,
            @NotNull String relativePath,
            @NotNull String needle
    ) throws Exception {
        CaseResult[] holder = new CaseResult[1];
        ApplicationManager.getApplication().invokeAndWait(() -> holder[0] = verifyOnEdt(
                project, report, id, relativePath, needle
        ), ModalityState.nonModal());
        Thread.sleep(400);
        return holder[0];
    }

    private static @NotNull CaseResult verifyOnEdt(
            @NotNull Project project,
            @NotNull StringBuilder report,
            @NotNull String id,
            @NotNull String relativePath,
            @NotNull String needle
    ) {
        VirtualFile base = ProjectUtil.guessProjectDir(project);
        if (base == null) {
            return fail(id, report, null, "no project dir");
        }
        VirtualFile file = base.findFileByRelativePath(relativePath.replace('\\', '/'));
        if (file == null) {
            return fail(id, report, null, "missing file " + relativePath);
        }
        new OpenFileDescriptor(project, file).navigate(true);

        return ApplicationManager.getApplication().runReadAction((Computable<CaseResult>) () -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile == null) {
                return fail(id, report, null, "no PsiFile");
            }
            Language language = psiFile.getLanguage();
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                return fail(id, report, language.getID(), "no Document");
            }
            int offset = document.getText().indexOf(needle);
            if (offset < 0) {
                return fail(id, report, language.getID(), "needle not found: " + needle);
            }
            int symbolOffset;
            if (needle.contains("tabzNavigationDemoTarget")) {
                symbolOffset = offset + needle.indexOf("tabzNavigationDemoTarget")
                        + "tabzNavigationDemoTarget".length() / 2;
            } else {
                symbolOffset = offset + 2;
            }

            Editor editor = EditorFactory.getInstance().createEditor(document, project);
            try {
                editor.getCaretModel().moveToOffset(symbolOffset);
                PsiElement target = TargetElementUtil.getInstance().findTargetElement(
                        editor,
                        TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED,
                        symbolOffset
                );
                if (target == null) {
                    return fail(id, report, language.getID(), "TargetElementUtil null at offset " + symbolOffset
                            + " (same as IDE: Cannot find declaration to go to)");
                }
                String targetFile = target.getContainingFile() == null
                        ? "?"
                        : target.getContainingFile().getVirtualFile().getPath();
                line(report, id + " OK -> " + target.getClass().getSimpleName() + " in " + targetFile);
                return new CaseResult(id, true, language.getID(), targetFile);
            } finally {
                EditorFactory.getInstance().releaseEditor(editor);
            }
        });
    }

    private static @NotNull CaseResult fail(
            @NotNull String id,
            @NotNull StringBuilder report,
            @Nullable String language,
            @NotNull String detail
    ) {
        line(report, id + " FAIL " + detail);
        return new CaseResult(id, false, language, detail);
    }

    private static void line(@NotNull StringBuilder report, @NotNull String text) {
        report.append(text).append('\n');
        LOG.warn("[nav-verify] " + text);
    }
}
