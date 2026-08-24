package de.sasbe.subtabs;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNestingRulesProvider;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class SubtabFileNestingGroups {
    private static final ExtensionPointName<ProjectViewNestingRulesProvider> NESTING_RULES_EP =
            ExtensionPointName.create("com.intellij.projectViewNestingRulesProvider");
    private static final String PROJECT_PANE_ID = "ProjectPane";

    record Rule(@NotNull String parentSuffix, @NotNull String childSuffix) {
    }

    record Group(@NotNull String parentFileName) {
        @NotNull String groupStem() {
            return CustomSubtabRuleMatcher.NESTING_GROUP_MARKER + parentFileName;
        }
    }

    private SubtabFileNestingGroups() {
    }

    static boolean isEnabled() {
        Application application = ApplicationManager.getApplication();
        if (application == null) {
            return true;
        }

        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) {
            return true;
        }

        for (Project project : projects) {
            if (!project.isDisposed() && isEnabled(project)) {
                return true;
            }
        }
        return false;
    }

    static boolean isEnabled(@NotNull Project project) {
        return ProjectView.getInstance(project).isUseFileNestingRules(PROJECT_PANE_ID);
    }

    static @Nullable Group findGroup(@NotNull String fileName) {
        if (ApplicationManager.getApplication() == null) {
            return null;
        }
        if (!isEnabled()) {
            return null;
        }

        for (Rule rule : rules()) {
            Group asChild = groupForChild(fileName, rule);
            if (asChild != null) {
                return asChild;
            }
        }

        for (Rule rule : rules()) {
            if (isParentFile(fileName, rule)) {
                return new Group(fileName);
            }
        }
        return null;
    }

    static @NotNull List<String> groupFileNames(
            @NotNull String parentFileName,
            @NotNull Iterable<String> siblingFileNames
    ) {
        List<String> members = new ArrayList<>();
        members.add(parentFileName);
        for (String sibling : siblingFileNames) {
            if (parentFileName.equals(sibling)) {
                continue;
            }
            Group group = findGroup(sibling);
            if (group != null && group.parentFileName().equals(parentFileName)) {
                members.add(sibling);
            }
        }
        return List.copyOf(members);
    }

    static @Nullable String parentFileName(@NotNull String groupStem) {
        if (!groupStem.startsWith(CustomSubtabRuleMatcher.NESTING_GROUP_MARKER)) {
            return null;
        }
        return groupStem.substring(CustomSubtabRuleMatcher.NESTING_GROUP_MARKER.length());
    }

    private static @NotNull List<Rule> rules() {
        if (ApplicationManager.getApplication() == null) {
            return List.of();
        }

        Set<Rule> rules = new LinkedHashSet<>();
        for (ProjectViewNestingRulesProvider provider : NESTING_RULES_EP.getExtensionList()) {
            provider.addFileNestingRules((parent, child) -> rules.add(new Rule(parent, child)));
        }
        rules.addAll(loadConfiguredRulesReflectively());
        return rules.stream()
                .sorted(Comparator.comparingInt((Rule rule) -> rule.childSuffix().length()).reversed())
                .toList();
    }

    private static @NotNull List<Rule> loadConfiguredRulesReflectively() {
        List<Rule> rules = new ArrayList<>();
        Application application = ApplicationManager.getApplication();
        if (application == null) {
            return rules;
        }

        collectRulesFromService(rules, "com.intellij.ide.fileNesting.FileNestingRules");
        collectRulesFromService(rules, "com.intellij.ide.fileNesting.FileNestingRulesService");
        return rules;
    }

    private static void collectRulesFromService(@NotNull List<Rule> rules, @NotNull String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Object instance = clazz.getMethod("getInstance").invoke(null);
            if (instance == null) {
                return;
            }
            collectRulesFromObject(rules, instance);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void collectRulesFromObject(@NotNull List<Rule> rules, @NotNull Object instance) {
        for (String methodName : List.of("getRules", "getNestingRules", "getUserRules")) {
            try {
                Method method = instance.getClass().getMethod(methodName);
                Object value = method.invoke(instance);
                if (value instanceof Iterable<?> iterable) {
                    for (Object entry : iterable) {
                        Rule parsed = parseRuleEntry(entry);
                        if (parsed != null) {
                            rules.add(parsed);
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static @Nullable Rule parseRuleEntry(@NotNull Object entry) {
        if (entry instanceof Rule rule) {
            return rule;
        }
        try {
            Method parentMethod = entry.getClass().getMethod("getParentSuffix");
            Method childMethod = entry.getClass().getMethod("getChildSuffix");
            Object parent = parentMethod.invoke(entry);
            Object child = childMethod.invoke(entry);
            if (parent instanceof String parentSuffix && child instanceof String childSuffix) {
                return new Rule(parentSuffix, childSuffix);
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static @Nullable Group groupForChild(@NotNull String fileName, @NotNull Rule rule) {
        if (!fileName.endsWith(rule.childSuffix()) || fileName.length() <= rule.childSuffix().length()) {
            return null;
        }
        String stem = fileName.substring(0, fileName.length() - rule.childSuffix().length());
        String parentFileName = stem + rule.parentSuffix();
        if (parentFileName.equals(fileName)) {
            return null;
        }
        return new Group(parentFileName);
    }

    private static boolean isParentFile(@NotNull String fileName, @NotNull Rule rule) {
        return fileName.endsWith(rule.parentSuffix()) && fileName.length() > rule.parentSuffix().length();
    }
}
