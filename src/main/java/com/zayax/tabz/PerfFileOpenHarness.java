package com.zayax.tabz;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.RepaintManager;
import javax.swing.SwingConstants;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Measures file opens in the real IDE window. Only runs with {@code -Dtabz.perf.harness=<output dir>}.
 */
final class PerfFileOpenHarness {
    private static final Logger LOG = Logger.getInstance(PerfFileOpenHarness.class);
    private static final String PROPERTY = "tabz.perf.harness";
    private static final int OPENS_PER_SCENARIO = 12;
    private static final long QUIET_WINDOW_MS = 150;
    private static final long PROBE_LATENCY_MS = 12;
    private static final long TIMEOUT_MS = 15_000;

    static void runIfEnabled() {
        String output = System.getProperty(PROPERTY, "").trim();
        if (output.isEmpty()) {
            return;
        }
        LOG.warn("[perf] harness requested");
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Project project = awaitDemoProject();
                new Run(project, Path.of(output)).run();
            } catch (Throwable throwable) {
                LOG.error("Perf harness failed", throwable);
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

    private record Config(String name, boolean hTabs, boolean grouping) {
    }

    private record Sample(String file, int openTabs, long callMs, long shownMs, long settledMs, long maxStallMs) {
    }

    private static final class Run {
        private final Project project;
        private final Path outputDir;
        private final StringBuilder report = new StringBuilder();
        private final Map<String, Integer> inclusiveFrames = new HashMap<>();
        private final Map<String, Integer> leafFrames = new HashMap<>();
        private final Map<String, Integer> waitInclusiveFrames = new HashMap<>();
        private final Map<String, Integer> waitEventSignatures = new HashMap<>();
        private int stackSamples;
        private int waitStackSamples;
        private Thread edt;
        private final TracingRepaintManager tracer = new TracingRepaintManager();

        private static final class TracingRepaintManager extends RepaintManager {
            volatile boolean tracing;
            final Map<String, Integer> repaintCallers = new java.util.concurrent.ConcurrentHashMap<>();
            final Map<String, Integer> revalidateCallers = new java.util.concurrent.ConcurrentHashMap<>();

            @Override
            public void addDirtyRegion(JComponent component, int x, int y, int w, int h) {
                if (tracing) {
                    record(repaintCallers, component);
                }
                super.addDirtyRegion(component, x, y, w, h);
            }

            @Override
            public synchronized void addInvalidComponent(JComponent invalidComponent) {
                if (tracing) {
                    record(revalidateCallers, invalidComponent);
                }
                super.addInvalidComponent(invalidComponent);
            }

            private static void record(@NotNull Map<String, Integer> target, @NotNull JComponent component) {
                StackTraceElement[] stack = new Throwable().getStackTrace();
                StringBuilder key = new StringBuilder(component.getClass().getName()).append(" <- ");
                int taken = 0;
                for (StackTraceElement frame : stack) {
                    String className = frame.getClassName();
                    if (className.startsWith("java.") || className.startsWith("javax.")
                            || className.startsWith("sun.") || className.contains("TracingRepaintManager")
                            || className.contains("$$Lambda") || className.startsWith("jdk.")) {
                        continue;
                    }
                    String simple = className.substring(className.lastIndexOf('.') + 1);
                    key.append(taken == 0 ? "" : " < ").append(simple).append('.').append(frame.getMethodName());
                    if (++taken >= 5) {
                        break;
                    }
                }
                target.merge(key.toString(), 1, Integer::sum);
            }
        }

        Run(@NotNull Project project, @NotNull Path outputDir) {
            this.project = project;
            this.outputDir = outputDir;
        }

        void run() throws Exception {
            Files.createDirectories(outputDir);
            DumbService.getInstance(project).waitForSmartMode();
            Thread.sleep(8_000);
            edt(() -> edt = Thread.currentThread());
            edt(() -> {
                ToolWindow projectView = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW);
                if (projectView != null) {
                    projectView.show();
                }
            });
            List<VirtualFile> files = collectFiles();
            line("files available: " + files.size());

            TabzSettings settings = TabzSettings.getInstance();
            boolean originalHTabs = settings.isSubtabsActive();
            boolean originalGroupingEnabled = settings.isProjectViewGroupingEnabled();
            boolean originalGroupingActive = settings.isProjectViewGroupingActive();
            boolean originalSidetabs = settings.isSidetabsActive();
            boolean originalSplittabs = settings.isSplittabsEnabled();
            SplittabBehaviorMode originalSplittabMode = settings.getSplittabBehaviorMode();
            SplittabOtherPairFileMode originalOtherPairMode = settings.getSplittabOtherPairFileMode();
            line("original settings: hTabs=" + originalHTabs + " groupingEnabled=" + originalGroupingEnabled
                    + " groupingActive=" + originalGroupingActive + " vTabs=" + originalSidetabs
                    + " tabz=" + settings.isTabzEnabled());

            try {
                List<String> configFilter = List.of(System.getProperty("tabz.perf.configs", "on,off").split(","));
                List<Config> configs = new ArrayList<>();
                if (configFilter.contains("on")) {
                    configs.add(new Config("H-Tabs+Gruppierung AN", true, true));
                }
                if (configFilter.contains("off")) {
                    configs.add(new Config("H-Tabs+Gruppierung AUS", false, false));
                }
                if (configFilter.contains("htabs")) {
                    configs.add(new Config("nur H-Tabs", true, false));
                }
                if (configFilter.contains("grouping")) {
                    configs.add(new Config("nur Gruppierung", false, true));
                }

                if (Boolean.getBoolean("tabz.perf.repaintTrace")) {
                    edt(() -> RepaintManager.setCurrentManager(tracer));
                    line("repaint tracing enabled");
                }
                applyConfig(configs.get(0));
                setupSplits(files, 1, 0);
                for (VirtualFile file : files) {
                    measureOpen(file, false);
                }
                line("warmup done");

                int rotation = 0;
                for (Config config : configs) {
                    applyConfig(config);
                    inclusiveFrames.clear();
                    leafFrames.clear();
                    waitInclusiveFrames.clear();
                    waitEventSignatures.clear();
                    stackSamples = 0;
                    waitStackSamples = 0;
                    for (String splitsText : System.getProperty("tabz.perf.splits", "1,2,3,4").split(",")) {
                        if (splitsText.isBlank()) {
                            continue;
                        }
                        int splits = Integer.parseInt(splitsText.trim());
                        rotation += 3;
                        List<VirtualFile> pool = setupSplits(files, splits, rotation);
                        List<Sample> samples = new ArrayList<>();
                        for (int index = 0; index < OPENS_PER_SCENARIO && index < pool.size(); index++) {
                            samples.add(measureOpen(pool.get(index), true));
                            Thread.sleep(250);
                        }
                        writeScenario(config, splits, samples);
                        screenshot(config.name().replaceAll("[^A-Za-z]+", "_") + "_" + splits + "splits");
                    }
                    for (String modeText : System.getProperty("tabz.perf.splittab", "").split(",")) {
                        if (modeText.isBlank()) {
                            continue;
                        }
                        SplittabBehaviorMode mode = "switch".equalsIgnoreCase(modeText.trim())
                                ? SplittabBehaviorMode.DEDICATED_VIEW
                                : SplittabBehaviorMode.INTEGRATED;
                        if (!Boolean.getBoolean("tabz.perf.skipSplittabOpen")) {
                            runSplittabScenario(config, mode, files);
                        }
                        for (String otherText : System.getProperty("tabz.perf.otherPair", "").split(",")) {
                            if (otherText.isBlank()) {
                                continue;
                            }
                            runOtherPairScenario(config, mode, files, "switch".equalsIgnoreCase(otherText.trim())
                                    ? SplittabOtherPairFileMode.SWITCH_TO_PAIR
                                    : SplittabOtherPairFileMode.OPEN_NORMALLY);
                        }
                    }
                    writeProfile(config);
                    flush();
                }
            } finally {
                edt(() -> {
                    settings.setSubtabsActive(originalHTabs);
                    settings.setProjectViewGroupingEnabled(originalGroupingEnabled);
                    settings.setProjectViewGroupingActive(originalGroupingActive);
                    settings.setSidetabsActive(originalSidetabs);
                    settings.setSplittabsEnabled(originalSplittabs);
                    settings.setSplittabBehaviorMode(originalSplittabMode);
                    settings.setSplittabOtherPairFileMode(originalOtherPairMode);
                    TabzPresentation.applySettingsChange();
                });
                line("DONE");
                flush();
                if (Boolean.getBoolean("tabz.perf.exit")) {
                    Thread.sleep(1_000);
                    ApplicationManager.getApplication().invokeLater(
                            () -> ApplicationManager.getApplication().exit(true, true, false),
                            ModalityState.any()
                    );
                }
            }
        }

        private void applyConfig(@NotNull Config config) throws Exception {
            line("");
            line("=== " + config.name() + " ===");
            edt(() -> {
                TabzSettings settings = TabzSettings.getInstance();
                settings.setSubtabsActive(config.hTabs());
                settings.setProjectViewGroupingEnabled(config.grouping());
                settings.setProjectViewGroupingActive(config.grouping());
                TabzPresentation.applySettingsChange();
            });
            waitQuiet(3_000);
        }

        private @NotNull List<VirtualFile> collectFiles() throws Exception {
            List<VirtualFile> result = new ArrayList<>();
            edt(() -> {
                VirtualFile base = ProjectUtil.guessProjectDir(project);
                VirtualFile app = base == null ? null : base.findFileByRelativePath("src/app");
                if (app == null) {
                    return;
                }
                VfsUtilCore.iterateChildrenRecursively(app, file -> true, file -> {
                    if (!file.isDirectory()) {
                        result.add(file);
                    }
                    return true;
                });
            });
            result.sort(Comparator.comparing(VirtualFile::getPath));
            return result;
        }

        /** Builds {@code splits} native panes with two files each and returns the remaining files to open. */
        private @NotNull List<VirtualFile> setupSplits(@NotNull List<VirtualFile> files, int splits, int rotation)
                throws Exception {
            List<VirtualFile> rotated = new ArrayList<>(files.size());
            for (int index = 0; index < files.size(); index++) {
                rotated.add(files.get((index + rotation) % files.size()));
            }
            edt(() -> {
                FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
                manager.closeAllFiles();
                while (manager.getWindows().length > 1) {
                    manager.unsplitAllWindow();
                }
            });
            waitQuiet(3_000);
            int next = 0;
            for (int pane = 0; pane < splits; pane++) {
                VirtualFile first = rotated.get(next++);
                VirtualFile second = rotated.get(next++);
                int paneIndex = pane;
                edt(() -> {
                    FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
                    EditorWindow window;
                    if (paneIndex == 0) {
                        window = manager.getCurrentWindow();
                        if (window == null) {
                            manager.openFile(first, true);
                            window = manager.getCurrentWindow();
                        } else {
                            InternalPlatformBridge.openFileWithProviders(manager, first, true, window);
                        }
                    } else {
                        EditorWindow[] windows = manager.getWindows();
                        EditorWindow last = windows[windows.length - 1];
                        window = last.split(SwingConstants.VERTICAL, true, first, true);
                    }
                    if (window != null) {
                        manager.setCurrentWindow(window);
                        InternalPlatformBridge.openFileWithProviders(manager, second, true, window);
                    }
                });
                waitQuiet(3_000);
            }
            int[] windowCount = new int[1];
            edt(() -> windowCount[0] = FileEditorManagerEx.getInstanceEx(project).getWindows().length);
            line("setup: " + splits + " requested panes -> " + windowCount[0] + " editor windows");
            return new ArrayList<>(rotated.subList(next, rotated.size()));
        }

        /**
         * Shows an html+scss split pair of one component (next to a few normal tabs), then opens a file of
         * another component and right after that one more file.
         */
        private void runSplittabScenario(
                @NotNull Config config,
                @NotNull SplittabBehaviorMode mode,
                @NotNull List<VirtualFile> files
        ) throws Exception {
            edt(() -> {
                TabzSettings settings = TabzSettings.getInstance();
                settings.setSplittabsEnabled(true);
                settings.setSplittabBehaviorMode(mode);
            });
            List<VirtualFile[]> components = new ArrayList<>();
            for (VirtualFile file : files) {
                if (file.getName().endsWith(".component.html") && file.getParent() != null) {
                    VirtualFile scss = file.getParent().findChild(file.getName().replace(".html", ".scss"));
                    VirtualFile ts = file.getParent().findChild(file.getName().replace(".html", ".ts"));
                    if (scss != null && ts != null) {
                        components.add(new VirtualFile[]{file, scss, ts});
                    }
                }
            }
            line("splittab [" + mode + "] components=" + components.size()
                    + " dissolve=" + TabzSettings.getInstance().getSplittabDissolveMode());
            List<Sample> first = new ArrayList<>();
            List<Sample> followUp = new ArrayList<>();
            for (int index = 0; index < OPENS_PER_SCENARIO; index++) {
                VirtualFile[] pair = components.get(index % components.size());
                VirtualFile foreign = components.get((index + 1) % components.size())[2];
                VirtualFile next = files.get((index * 7 + 3) % files.size());
                if (next.equals(foreign) || next.equals(pair[0]) || next.equals(pair[1])) {
                    next = files.get((index * 7 + 4) % files.size());
                }
                edt(() -> {
                    FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
                    manager.closeAllFiles();
                    while (manager.getWindows().length > 1) {
                        manager.unsplitAllWindow();
                    }
                });
                waitQuiet(1_000);
                for (int background = 0; background < 3; background++) {
                    VirtualFile file = files.get((index * 5 + background * 11) % files.size());
                    edt(() -> FileEditorManagerEx.getInstanceEx(project).openFile(file, true));
                }
                edt(() -> FileEditorManagerEx.getInstanceEx(project).openFile(pair[0], true));
                waitQuiet(1_000);
                edt(() -> ComponentSubtabEditorSplitNavigation.createSplit(project, pair[0], pair[1]));
                long chromeDeadline = System.currentTimeMillis() + 10_000;
                boolean[] engaged = new boolean[1];
                while (System.currentTimeMillis() < chromeDeadline) {
                    edt(() -> engaged[0] = ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project));
                    if (engaged[0]) {
                        break;
                    }
                    Thread.sleep(20);
                }
                waitQuiet(1_500);
                if (!engaged[0]) {
                    line("  pair " + pair[0].getName() + " + " + pair[1].getName() + ": splittab chrome NOT shown");
                }
                if (index == 0) {
                    screenshot(config.name().replaceAll("[^A-Za-z]+", "_") + "_splittab_" + mode + "_pair");
                }
                line("  pair " + pair[0].getName() + " + " + pair[1].getName() + " -> open " + foreign.getName());
                first.add(measureOpen(foreign, true));
                if (index == 0) {
                    screenshot(config.name().replaceAll("[^A-Za-z]+", "_") + "_splittab_" + mode + "_foreign");
                }
                Thread.sleep(250);
                followUp.add(measureOpen(next, true));
                Thread.sleep(250);
            }
            writeSamples("splittab " + mode + " first open", config, first);
            writeSamples("splittab " + mode + " follow-up open", config, followUp);
        }

        /** Pair A is shown, then the scss file of the saved pair B is opened. */
        private void runOtherPairScenario(
                @NotNull Config config,
                @NotNull SplittabBehaviorMode mode,
                @NotNull List<VirtualFile> files,
                @NotNull SplittabOtherPairFileMode otherMode
        ) throws Exception {
            edt(() -> {
                TabzSettings settings = TabzSettings.getInstance();
                settings.setSplittabsEnabled(true);
                settings.setSplittabBehaviorMode(mode);
                settings.setSplittabOtherPairFileMode(otherMode);
            });
            List<VirtualFile[]> components = componentsOf(files);
            String label = "other pair " + mode + " / " + otherMode;
            line(label);
            List<Sample> samples = new ArrayList<>();
            for (int index = 0; index < components.size(); index++) {
                VirtualFile[] pairA = components.get(index);
                VirtualFile[] pairB = components.get((index + 1) % components.size());
                resetEditors();
                edt(() -> FileEditorManagerEx.getInstanceEx(project).openFile(pairB[0], true));
                waitQuiet(800);
                createPairAndWait(pairB);
                resetEditors();
                edt(() -> FileEditorManagerEx.getInstanceEx(project).openFile(files.get(0), true));
                edt(() -> FileEditorManagerEx.getInstanceEx(project).openFile(pairA[0], true));
                waitQuiet(800);
                createPairAndWait(pairA);
                line("  pair " + pairA[0].getName() + " shown -> open " + pairB[1].getName());
                samples.add(measureOpen(pairB[1], true));
                waitQuiet(500);
                edt(() -> {
                    ComponentSubtabEditorSplitRegistry.SplittabPair active =
                            ComponentSubtabEditorSplitRegistry.getInstance(project).activePair();
                    FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
                    EditorWindow current = manager.getCurrentWindow();
                    line("    after: active=" + (active == null ? null
                            : active.leftFile().getName() + "+" + active.rightFile().getName())
                            + " splitPairUi=" + ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project)
                            + " dedicated=" + SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()
                            + " selected=" + (current == null || current.getSelectedFile() == null
                            ? null : current.getSelectedFile().getName())
                            + " windows=" + manager.getWindows().length
                            + " open=" + manager.getOpenFiles().length);
                });
                if (index == 0) {
                    screenshot(config.name().replaceAll("[^A-Za-z]+", "_") + "_otherpair_" + mode + "_" + otherMode);
                }
            }
            writeSamples(label, config, samples);
        }

        private @NotNull List<VirtualFile[]> componentsOf(@NotNull List<VirtualFile> files) {
            List<VirtualFile[]> components = new ArrayList<>();
            for (VirtualFile file : files) {
                if (file.getName().endsWith(".component.html") && file.getParent() != null) {
                    VirtualFile scss = file.getParent().findChild(file.getName().replace(".html", ".scss"));
                    VirtualFile ts = file.getParent().findChild(file.getName().replace(".html", ".ts"));
                    if (scss != null && ts != null) {
                        components.add(new VirtualFile[]{file, scss, ts});
                    }
                }
            }
            return components;
        }

        private void resetEditors() throws Exception {
            edt(() -> {
                FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
                if (SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive()) {
                    SplittabDedicatedViewService.getInstance(project).exitDedicatedView(false);
                }
                manager.closeAllFiles();
                while (manager.getWindows().length > 1) {
                    manager.unsplitAllWindow();
                }
            });
            waitQuiet(800);
        }

        private void createPairAndWait(@NotNull VirtualFile[] pair) throws Exception {
            edt(() -> ComponentSubtabEditorSplitNavigation.createSplit(project, pair[0], pair[1]));
            long deadline = System.currentTimeMillis() + 10_000;
            boolean[] engaged = new boolean[1];
            while (System.currentTimeMillis() < deadline) {
                edt(() -> engaged[0] = ComponentSubtabEditorSplitNavigation.editorSplittabUiEngaged(project));
                if (engaged[0]) {
                    break;
                }
                Thread.sleep(20);
            }
            if (!engaged[0]) {
                line("  pair " + pair[0].getName() + ": splittab chrome NOT shown");
            }
            waitQuiet(1_000);
        }

        private void writeSamples(@NotNull String label, @NotNull Config config, @NotNull List<Sample> samples) {
            List<Long> settled = new ArrayList<>();
            List<Long> shown = new ArrayList<>();
            for (Sample sample : samples) {
                settled.add(sample.settledMs());
                shown.add(sample.shownMs());
            }
            line(String.format("SUMMARY [%s] %s  shown median=%dms max=%dms | settled median=%dms max=%dms | >1000ms: %d/%d",
                    config.name(), label, median(shown), max(shown), median(settled), max(settled),
                    shown.stream().filter(value -> value < 0 || value > 1000).count(), samples.size()));
        }

        private @NotNull Sample measureOpen(@NotNull VirtualFile file, boolean sampleStacks) throws Exception {
            long[] callNs = new long[1];
            int[] openTabs = new int[1];
            AtomicLong startNs = new AtomicLong();
            StackSampler sampler = sampleStacks ? new StackSampler() : null;
            if (sampler != null) {
                sampler.start();
                tracer.tracing = true;
            }
            com.intellij.openapi.Disposable eventScope = com.intellij.openapi.util.Disposer.newDisposable();
            List<String> editorEvents = java.util.Collections.synchronizedList(new ArrayList<>());
            project.getMessageBus().connect(eventScope).subscribe(
                    com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER,
                    new com.intellij.openapi.fileEditor.FileEditorManagerListener() {
                        @Override
                        public void fileOpened(@NotNull com.intellij.openapi.fileEditor.FileEditorManager source,
                                               @NotNull VirtualFile opened) {
                            editorEvents.add("opened " + opened.getName() + pluginStack());
                        }

                        @Override
                        public void fileClosed(@NotNull com.intellij.openapi.fileEditor.FileEditorManager source,
                                               @NotNull VirtualFile closed) {
                            editorEvents.add("closed " + closed.getName() + pluginStack());
                        }
                    }
            );
            edt(() -> {
                startNs.set(System.nanoTime());
                new OpenFileDescriptor(project, file).navigate(true);
                callNs[0] = System.nanoTime() - startNs.get();
            });
            long shownNs = -1;
            long deadline = System.nanoTime() + TIMEOUT_MS * 1_000_000;
            while (System.nanoTime() < deadline) {
                boolean[] shown = new boolean[1];
                edt(() -> {
                    shown[0] = isShownInCurrentWindow(file);
                    openTabs[0] = FileEditorManagerEx.getInstanceEx(project).getOpenFiles().length;
                });
                if (shown[0]) {
                    shownNs = System.nanoTime() - startNs.get();
                    tracer.tracing = false;
                    if (sampler != null) {
                        sampler.waitingForOpen = false;
                    }
                    break;
                }
                Thread.sleep(2);
            }
            tracer.tracing = false;
            if (shownNs < 0) {
                edt(() -> {
                    FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
                    EditorWindow current = manager.getCurrentWindow();
                    StringBuilder windows = new StringBuilder();
                    for (EditorWindow window : manager.getWindows()) {
                        windows.append(' ').append(java.util.Arrays.toString(
                                EditorWindowFiles.files(window).stream().map(VirtualFile::getName).toArray()))
                                .append("->").append(window.getSelectedFile() == null ? null : window.getSelectedFile().getName());
                    }
                    line("  NOT SHOWN " + file.getName() + ": isOpen=" + manager.isFileOpen(file)
                            + " current=" + (current == null || current.getSelectedFile() == null
                            ? null : current.getSelectedFile().getName())
                            + " windows=" + windows
                            + " dedicated=" + SplittabDedicatedViewService.getInstance(project).isDedicatedViewActive());
                });
                screenshot("not_shown_" + file.getName().replaceAll("[^A-Za-z]+", "_"));
                synchronized (editorEvents) {
                    for (String event : editorEvents) {
                        line("    editor event: " + event);
                    }
                }
            }
            com.intellij.openapi.util.Disposer.dispose(eventScope);
            long[] quiet = waitQuietFrom(startNs.get());
            if (sampler != null) {
                sampler.stopAndMerge();
            }
            Sample sample = new Sample(
                    file.getName(),
                    openTabs[0],
                    callNs[0] / 1_000_000,
                    shownNs < 0 ? -1 : shownNs / 1_000_000,
                    quiet[0],
                    quiet[1]
            );
            if (sampleStacks) {
                line(String.format("  open %-34s tabs=%2d call=%5dms shown=%5dms settled=%5dms maxStall=%5dms",
                        sample.file(), sample.openTabs(), sample.callMs(), sample.shownMs(),
                        sample.settledMs(), sample.maxStallMs()));
            }
            return sample;
        }

        private static @NotNull String pluginStack() {
            StringBuilder result = new StringBuilder();
            for (StackTraceElement frame : new Throwable().getStackTrace()) {
                String className = frame.getClassName();
                if (className.startsWith("com.zayax.tabz") && !className.contains("PerfFileOpenHarness")) {
                    result.append(" < ").append(className.substring("com.zayax.tabz.".length()))
                            .append('.').append(frame.getMethodName());
                }
            }
            return result.toString();
        }

        private boolean isShownInCurrentWindow(@NotNull VirtualFile file) {
            FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
            EditorWindow current = manager.getCurrentWindow();
            if (current == null || !file.equals(current.getSelectedFile())) {
                return false;
            }
            FileEditor editor = manager.getSelectedEditor(file);
            if (editor == null) {
                return false;
            }
            JComponent component = editor.getComponent();
            return component.isShowing() && component.getWidth() > 0 && component.getHeight() > 0;
        }

        /** Returns {settledMs since start, max EDT stall ms}. */
        private long[] waitQuietFrom(long startNs) throws Exception {
            long maxStall = 0;
            long quietSinceNs = -1;
            long deadline = System.nanoTime() + TIMEOUT_MS * 1_000_000;
            while (System.nanoTime() < deadline) {
                long postedNs = System.nanoTime();
                edt(() -> {
                });
                long latencyMs = (System.nanoTime() - postedNs) / 1_000_000;
                maxStall = Math.max(maxStall, latencyMs);
                if (latencyMs <= PROBE_LATENCY_MS) {
                    if (quietSinceNs < 0) {
                        quietSinceNs = postedNs;
                    }
                    if ((System.nanoTime() - quietSinceNs) / 1_000_000 >= QUIET_WINDOW_MS) {
                        return new long[]{(quietSinceNs - startNs) / 1_000_000, maxStall};
                    }
                } else {
                    quietSinceNs = -1;
                }
                Thread.sleep(4);
            }
            return new long[]{-1, maxStall};
        }

        private void waitQuiet(long minimumMs) throws Exception {
            Thread.sleep(minimumMs);
            waitQuietFrom(System.nanoTime());
        }

        private void writeScenario(@NotNull Config config, int splits, @NotNull List<Sample> samples) {
            List<Long> settled = new ArrayList<>();
            List<Long> shown = new ArrayList<>();
            for (Sample sample : samples) {
                settled.add(sample.settledMs());
                shown.add(sample.shownMs());
            }
            line(String.format("SUMMARY [%s] splits=%d  shown median=%dms max=%dms | settled median=%dms max=%dms | >1000ms: %d/%d",
                    config.name(), splits, median(shown), max(shown), median(settled), max(settled),
                    settled.stream().filter(value -> value < 0 || value > 1000).count(), samples.size()));
        }

        private void writeProfile(@NotNull Config config) {
            line("PROFILE [" + config.name() + "] EDT samples=" + stackSamples
                    + " (plugin frames, inclusive count; 1 sample ~ 5ms)");
            inclusiveFrames.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(40)
                    .forEach(entry -> line(String.format("   %6d  %s", entry.getValue(), entry.getKey())));
            line("PROFILE-LEAF [" + config.name() + "] (deepest frame of EDT samples inside plugin calls)");
            leafFrames.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(25)
                    .forEach(entry -> line(String.format("   %6d  %s", entry.getValue(), entry.getKey())));
            line("WAIT-EVENTS [" + config.name() + "] EDT samples until file shown=" + waitStackSamples
                    + " (events dispatched while the open waits)");
            waitEventSignatures.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(30)
                    .forEach(entry -> line(String.format("   %6d  %s", entry.getValue(), entry.getKey())));
            if (!tracer.repaintCallers.isEmpty() || !tracer.revalidateCallers.isEmpty()) {
                line("REPAINT-CALLERS [" + config.name() + "] while the open waits");
                tracer.repaintCallers.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(30)
                        .forEach(entry -> line(String.format("   %7d  %s", entry.getValue(), entry.getKey())));
                line("REVALIDATE-CALLERS [" + config.name() + "] while the open waits");
                tracer.revalidateCallers.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .limit(30)
                        .forEach(entry -> line(String.format("   %7d  %s", entry.getValue(), entry.getKey())));
                tracer.repaintCallers.clear();
                tracer.revalidateCallers.clear();
            }
            line("WAIT-FRAMES [" + config.name() + "] (inclusive, all packages)");
            waitInclusiveFrames.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(90)
                    .forEach(entry -> line(String.format("   %6d  %s", entry.getValue(), entry.getKey())));
        }

        private static long median(@NotNull List<Long> values) {
            List<Long> sorted = new ArrayList<>(values);
            sorted.sort(Long::compare);
            return sorted.isEmpty() ? -1 : sorted.get(sorted.size() / 2);
        }

        private static long max(@NotNull List<Long> values) {
            return values.stream().mapToLong(Long::longValue).max().orElse(-1);
        }

        private void screenshot(@NotNull String name) {
            try {
                edt(() -> {
                    JFrame frame = WindowManager.getInstance().getFrame(project);
                    if (frame == null || frame.getWidth() <= 0) {
                        return;
                    }
                    BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D graphics = image.createGraphics();
                    frame.getRootPane().paint(graphics);
                    graphics.dispose();
                    try {
                        ImageIO.write(image, "png", outputDir.resolve(name + ".png").toFile());
                    } catch (IOException exception) {
                        LOG.warn(exception);
                    }
                });
            } catch (Exception exception) {
                LOG.warn(exception);
            }
        }

        private void line(@NotNull String text) {
            report.append(text).append('\n');
            LOG.info("[perf] " + text);
        }

        private void flush() {
            try {
                Files.writeString(outputDir.resolve("report.txt"), report.toString(), StandardCharsets.UTF_8);
            } catch (IOException exception) {
                LOG.warn(exception);
            }
        }

        private void edt(@NotNull Runnable runnable) throws Exception {
            ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.nonModal());
        }

        private final class StackSampler extends Thread {
            private volatile boolean running = true;
            volatile boolean waitingForOpen = true;
            private final Map<String, Integer> inclusive = new HashMap<>();
            private final Map<String, Integer> leaf = new HashMap<>();
            private final Map<String, Integer> waitInclusive = new HashMap<>();
            private final Map<String, Integer> waitEvents = new HashMap<>();
            private int samples;
            private int waitSamples;

            private void recordWaitSample(@NotNull StackTraceElement[] stack) {
                waitSamples++;
                java.util.Set<String> seen = new java.util.HashSet<>();
                int nestedDispatch = -1;
                for (int index = 0; index < stack.length; index++) {
                    StackTraceElement frame = stack[index];
                    String key = frame.getClassName() + "." + frame.getMethodName();
                    if (key.contains("$$Lambda") || frame.getClassName().startsWith("com.zayax.tabz.PerfFileOpenHarness")) {
                        continue;
                    }
                    if (seen.add(key)) {
                        waitInclusive.merge(key, 1, Integer::sum);
                    }
                    if (nestedDispatch < 0 && key.equals("com.intellij.ide.IdeEventQueue.dispatchEvent")) {
                        nestedDispatch = index;
                    }
                }
                if (nestedDispatch < 0) {
                    waitEvents.merge("<no nested event>", 1, Integer::sum);
                    return;
                }
                StringBuilder signature = new StringBuilder();
                int taken = 0;
                for (int index = nestedDispatch - 1; index >= 0 && taken < 7; index--) {
                    StackTraceElement frame = stack[index];
                    String className = frame.getClassName();
                    if (className.contains("$$Lambda") || className.startsWith("java.lang.reflect")
                            || className.startsWith("jdk.internal") || className.startsWith("kotlin.")
                            || className.startsWith("kotlinx.") || className.contains("IdeEventQueue")
                            || className.contains("TransactionGuard") || className.contains("CoreProgressManager")
                            || className.contains("ThreadContext") || className.contains("ContextRunnable")
                            || className.contains("AccessController") || className.contains("ProtectionDomain")) {
                        continue;
                    }
                    String simple = className.substring(className.lastIndexOf('.') + 1);
                    signature.append(taken == 0 ? "" : " > ").append(simple).append('.').append(frame.getMethodName());
                    taken++;
                }
                waitEvents.merge(signature.toString(), 1, Integer::sum);
            }

            StackSampler() {
                super("tabz-perf-sampler");
                setDaemon(true);
            }

            @Override
            public void run() {
                while (running) {
                    StackTraceElement[] stack = edt.getStackTrace();
                    if (waitingForOpen) {
                        recordWaitSample(stack);
                    }
                    boolean inPlugin = false;
                    java.util.Set<String> seen = new java.util.HashSet<>();
                    for (StackTraceElement frame : stack) {
                        if (frame.getClassName().startsWith("com.zayax.tabz.PerfFileOpenHarness")) {
                            continue;
                        }
                        if (frame.getClassName().startsWith("com.zayax.tabz")) {
                            String key = frame.getClassName().substring("com.zayax.tabz.".length())
                                    + "." + frame.getMethodName();
                            if (!inPlugin) {
                                leaf.merge(key, 1, Integer::sum);
                                inPlugin = true;
                            }
                            if (seen.add(key)) {
                                inclusive.merge(key, 1, Integer::sum);
                            }
                        }
                    }
                    if (inPlugin) {
                        String top = stack.length == 0 ? "?" : stack[0].getClassName() + "." + stack[0].getMethodName();
                        leaf.merge("  top: " + top, 1, Integer::sum);
                    }
                    samples++;
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException interrupted) {
                        return;
                    }
                }
            }

            void stopAndMerge() throws InterruptedException {
                running = false;
                join();
                inclusive.forEach((key, value) -> inclusiveFrames.merge(key, value, Integer::sum));
                leaf.forEach((key, value) -> leafFrames.merge(key, value, Integer::sum));
                waitInclusive.forEach((key, value) -> waitInclusiveFrames.merge(key, value, Integer::sum));
                waitEvents.forEach((key, value) -> waitEventSignatures.merge(key, value, Integer::sum));
                stackSamples += samples;
                waitStackSamples += waitSamples;
            }
        }
    }
}
