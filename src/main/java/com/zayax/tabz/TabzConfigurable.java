package com.zayax.tabz;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.function.Function;

public final class TabzConfigurable implements SearchableConfigurable {
    private JCheckBox tabzEnabledCheckbox;
    private JCheckBox hTabsActiveCheckbox;
    private JCheckBox reuseOpenSubtabGroupMainTabCheckbox;
    private JCheckBox sidetabsActiveCheckbox;
    private ComboBox<SidetabLayoutMode> sidetabLayoutModeCombo;
    private SidetabsSideSwitchButton sidetabsSideSwitch;
    private JCheckBox showCollapseButtonCheckbox;
    private JCheckBox projectViewGroupingCheckbox;
    private JCheckBox fitTabsToEditorWidthCheckbox;
    private ComboBox<SubtabOverflowMode> overflowModeCombo;
    private ComboBox<SubtabGroupTreeControlStyle> groupTreeControlStyleCombo;
    private JCheckBox invertGroupTreeControlFillCheckbox;
    private JCheckBox groupColorsEnabledCheckbox;
    private JCheckBox hoverViewEnabledCheckbox;
    private JCheckBox hoverViewProjectToEditorCheckbox;
    private JCheckBox showSubtabNameInMainTabCheckbox;
    private JSlider barHeightSlider;
    private JSlider textSizeSlider;
    private ComboBox<TabFontStyle> tabFontStyleCombo;
    private JLabel barHeightValueLabel;
    private JLabel textSizeValueLabel;
    private SubtabsRulesPanel rulesPanel;
    private SidetabsRulesPanel sidetabsRulesPanel;
    private JPanel rootPanel;
    private JTabbedPane mainTabs;
    private JPanel hTabsPanel;
    private JPanel vTabsPanel;
    private JPanel hoverSyncPanel;
    private JCheckBox splittabsEnabledCheckbox;
    private ComboBox<SplittabBehaviorMode> splittabBehaviorModeCombo;
    private ComboBox<SplittabDissolveMode> splittabDissolveModeCombo;
    private ComboBox<SplittabOtherPairFileMode> splittabOtherPairFileModeCombo;
    private JCheckBox restoreSwitchSplittabSessionCheckbox;
    private boolean rulesUiInitialized;

    @Override
    public @NotNull String getId() {
        return "com.zayax.tabz.settings";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "TabZ";
    }

    @Override
    public @Nullable JComponent createComponent() {
        tabzEnabledCheckbox = new JCheckBox("Enable TabZ");
        hTabsActiveCheckbox = new JCheckBox("Enable horizontal tabs");
        hTabsActiveCheckbox.setToolTipText("Horizontal tab strip directly above the editor");
        reuseOpenSubtabGroupMainTabCheckbox = new JCheckBox(
                "Reuse existing group main tab"
        );
        reuseOpenSubtabGroupMainTabCheckbox.setToolTipText(
                "When a file from the same tabz group is already open, switch tabz in that "
                        + "main tab instead of opening a second one. "
                        + "Not applied during an active split-pair session."
        );
        hTabsActiveCheckbox.addActionListener(event -> updateHTabsOptions());
        sidetabsActiveCheckbox = new JCheckBox("Enable vertical tabs");
        sidetabsActiveCheckbox.setToolTipText("Vertical tab strip at the editor edge");
        sidetabLayoutModeCombo = labeledEnumCombo(SidetabLayoutMode.values(), SidetabLayoutMode::label);
        sidetabLayoutModeCombo.addActionListener(event -> updateSidetabLayoutOptions());
        sidetabsSideSwitch = new SidetabsSideSwitchButton();
        showCollapseButtonCheckbox = new JCheckBox("Show collapse icons");
        showCollapseButtonCheckbox.setToolTipText(
                "Shows the collapse icon in the horizontal tab strip and vertical tabs"
        );
        projectViewGroupingCheckbox = new JCheckBox("Grouping");
        projectViewGroupingCheckbox.setToolTipText(
                "Shows the grouping control in the Project view; expand/collapse there is independent of this option"
        );
        overflowModeCombo = labeledEnumCombo(SubtabOverflowMode.values(), SubtabOverflowMode::label);
        groupTreeControlStyleCombo = labeledEnumCombo(
                SubtabGroupTreeControlStyle.values(),
                SubtabGroupTreeControlStyle::label
        );
        groupTreeControlStyleCombo.addActionListener(event -> updateGroupTreeControlOptions());

        invertGroupTreeControlFillCheckbox = new JCheckBox("Invert fill (filled when open)");

        groupColorsEnabledCheckbox = new JCheckBox("Group colors");
        groupColorsEnabledCheckbox.setToolTipText(
                "Tints editor main tabs and \"N files\" in the Project view with the group color"
        );

        hoverViewEnabledCheckbox = new JCheckBox("Enable Hover Sync");
        hoverViewEnabledCheckbox.setToolTipText(
                "Highlights related UI when hovering the Project view, horizontal tabs, main tabs, "
                        + "and split-pair bars (e.g. horizontal tabs and main tabs)"
        );
        hoverViewProjectToEditorCheckbox = new JCheckBox("Highlight editor");
        hoverViewProjectToEditorCheckbox.setToolTipText(
                "Highlights code in open editors when hovering files in the Project view or hover picker"
        );
        hoverViewEnabledCheckbox.addActionListener(event -> updateHoverSyncOptions());

        showSubtabNameInMainTabCheckbox = new JCheckBox(
                "Show tabz name in main tab title (in parentheses)"
        );

        barHeightSlider = createSlider(25, 100, 75);
        barHeightValueLabel = new JBLabel(formatPercent(barHeightSlider.getValue()));
        barHeightSlider.addChangeListener(event -> {
            barHeightValueLabel.setText(formatPercent(barHeightSlider.getValue()));
            previewTypography();
        });

        textSizeSlider = createSlider(50, 100, 75);
        textSizeValueLabel = new JBLabel(formatPercent(textSizeSlider.getValue()));
        textSizeSlider.addChangeListener(event -> {
            textSizeValueLabel.setText(formatPercent(textSizeSlider.getValue()));
            previewTypography();
        });

        tabFontStyleCombo = labeledEnumCombo(TabFontStyle.values(), TabFontStyle::label);
        tabFontStyleCombo.addActionListener(event -> previewTypography());

        fitTabsToEditorWidthCheckbox = new JCheckBox(
                "Fit tab and font size to editor width"
        );

        JPanel sidetabsFlagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
        sidetabsFlagRow.setOpaque(false);
        sidetabsFlagRow.add(sidetabsActiveCheckbox);
        sidetabsFlagRow.add(sidetabLayoutModeCombo);
        sidetabsFlagRow.add(sidetabsSideSwitch);

        JPanel ansichtPanel = FormBuilder.createFormBuilder()
                .addComponent(tabzEnabledCheckbox)
                .addComponent(showCollapseButtonCheckbox)
                .addComponent(groupColorsEnabledCheckbox)
                .addLabeledComponent("Project view grouping", groupTreeControlStyleCombo)
                .addComponent(invertGroupTreeControlFillCheckbox)
                .addLabeledComponent("Tab height", sliderRow(barHeightSlider, barHeightValueLabel))
                .addLabeledComponent("Font size", sliderRow(textSizeSlider, textSizeValueLabel))
                .addLabeledComponent("Font style", tabFontStyleCombo)
                .addComponent(fitTabsToEditorWidthCheckbox)
                .addLabeledComponent("Horizontal tabs overflow", overflowModeCombo)
                .getPanel();

        hoverSyncPanel = FormBuilder.createFormBuilder()
                .addComponent(hoverViewEnabledCheckbox)
                .addComponent(hoverViewProjectToEditorCheckbox)
                .getPanel();

        JPanel hTabsOptionsPanel = FormBuilder.createFormBuilder()
                .addComponent(hTabsActiveCheckbox)
                .addComponent(reuseOpenSubtabGroupMainTabCheckbox)
                .addComponent(projectViewGroupingCheckbox)
                .addComponent(showSubtabNameInMainTabCheckbox)
                .getPanel();
        hTabsPanel = new JPanel(new BorderLayout());
        hTabsPanel.add(hTabsOptionsPanel, BorderLayout.NORTH);

        JPanel vTabsOptionsPanel = FormBuilder.createFormBuilder()
                .addComponent(sidetabsFlagRow)
                .getPanel();
        vTabsPanel = new JPanel(new BorderLayout());
        vTabsPanel.add(vTabsOptionsPanel, BorderLayout.NORTH);

        splittabsEnabledCheckbox = new JCheckBox("Enable split pairs");
        splittabBehaviorModeCombo = labeledEnumCombo(
                SplittabBehaviorMode.values(),
                SplittabBehaviorMode::label
        );
        splittabDissolveModeCombo = labeledEnumCombo(
                SplittabDissolveMode.values(),
                SplittabDissolveMode::label
        );
        splittabOtherPairFileModeCombo = labeledEnumCombo(
                SplittabOtherPairFileMode.values(),
                SplittabOtherPairFileMode::label
        );
        splittabOtherPairFileModeCombo.setToolTipText(
                "Applies to Mixed and Switch when opening a file that belongs to another saved split pair "
                        + "while a split pair is active"
        );
        restoreSwitchSplittabSessionCheckbox = new JCheckBox("Restore Switch split pairs on project open");
        restoreSwitchSplittabSessionCheckbox.setToolTipText(
                "When Switch was active with split pairs in the foreground, reload that session on next project open. "
                        + "Off: restore previously saved normal editor tabs."
        );
        splittabsEnabledCheckbox.addActionListener(event -> updateSplittabBehaviorOptions());
        JPanel splitTabsPanel = FormBuilder.createFormBuilder()
                .addComponent(splittabsEnabledCheckbox)
                .addLabeledComponent("Behavior", splittabBehaviorModeCombo)
                .addLabeledComponent("On dissolve", splittabDissolveModeCombo)
                .addLabeledComponent("Open file from another split pair", splittabOtherPairFileModeCombo)
                .addComponent(restoreSwitchSplittabSessionCheckbox)
                .getPanel();

        JPanel aiPanel = new JPanel(new BorderLayout());
        aiPanel.setBorder(JBUI.Borders.empty(8, 0));
        aiPanel.add(new JBLabel("No settings yet."), BorderLayout.NORTH);

        rulesUiInitialized = false;

        mainTabs = new JTabbedPane();
        mainTabs.addTab("Appearance", ansichtPanel);
        mainTabs.addTab("Hover Sync", hoverSyncPanel);
        mainTabs.addTab("Horizontal tabs", hTabsPanel);
        mainTabs.addTab("Vertical tabs", vTabsPanel);
        mainTabs.addTab("Split Pairs", splitTabsPanel);
        mainTabs.addTab("AI", aiPanel);
        mainTabs.addChangeListener(event -> {
            Component selected = mainTabs.getSelectedComponent();
            if (selected == hTabsPanel || selected == vTabsPanel) {
                ensureRulesUiInitialized();
            }
        });

        JButton resetButton = new JButton("Reset all settings");
        resetButton.addActionListener(event -> confirmAndResetToDefaults());

        JPanel resetRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        resetRow.setOpaque(false);
        resetRow.setBorder(JBUI.Borders.emptyTop(8));
        resetRow.add(resetButton);

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.add(mainTabs, BorderLayout.CENTER);
        rootPanel.add(resetRow, BorderLayout.SOUTH);
        reset();
        restoreSettingsDialogTab();
        return rootPanel;
    }

    private void restoreSettingsDialogTab() {
        if (mainTabs == null) {
            return;
        }
        String savedTitle = migrateSettingsTabTitle(TabzSettings.getInstance().getSettingsDialogSelectedTabTitle());
        if (savedTitle == null) {
            return;
        }
        for (int index = 0; index < mainTabs.getTabCount(); index++) {
            if (savedTitle.equals(mainTabs.getTitleAt(index))) {
                mainTabs.setSelectedIndex(index);
                Component selected = mainTabs.getSelectedComponent();
                if (selected == hTabsPanel || selected == vTabsPanel) {
                    ensureRulesUiInitialized();
                }
                return;
            }
        }
    }

    private void rememberSettingsDialogTab() {
        if (mainTabs == null) {
            return;
        }
        int index = mainTabs.getSelectedIndex();
        if (index < 0 || index >= mainTabs.getTabCount()) {
            return;
        }
        TabzSettings.getInstance().setSettingsDialogSelectedTabTitle(mainTabs.getTitleAt(index));
    }

    private void ensureRulesUiInitialized() {
        if (rulesUiInitialized) {
            return;
        }
        rulesUiInitialized = true;
        rulesPanel = new SubtabsRulesPanel();
        sidetabsRulesPanel = new SidetabsRulesPanel();
        hTabsPanel.add(rulesPanel.createPanel(), BorderLayout.CENTER);
        vTabsPanel.add(sidetabsRulesPanel.createPanel(), BorderLayout.CENTER);
        TabzSettings settings = TabzSettings.getInstance();
        rulesPanel.reset(settings.getRules());
        sidetabsRulesPanel.reset(settings.getSidetabRules());
        hTabsPanel.revalidate();
        vTabsPanel.revalidate();
    }

    @Override
    public boolean isModified() {
        if (tabzEnabledCheckbox == null) {
            return false;
        }

        TabzSettings settings = TabzSettings.getInstance();
        boolean appearanceModified = tabzEnabledCheckbox.isSelected() != settings.isTabzEnabled()
                || hTabsActiveCheckbox.isSelected() != settings.isSubtabsActive()
                || reuseOpenSubtabGroupMainTabCheckbox.isSelected()
                        != settings.isReuseOpenSubtabGroupMainTab()
                || sidetabsActiveCheckbox.isSelected() != settings.isSidetabsActive()
                || sidetabLayoutModeCombo.getItem() != settings.getSidetabLayoutMode()
                || sidetabsSideSwitch.isSelected() != settings.isSidetabsOnRight()
                || showCollapseButtonCheckbox.isSelected() != settings.isShowCollapseButton()
                || projectViewGroupingCheckbox.isSelected() != settings.isProjectViewGroupingEnabled()
                || fitTabsToEditorWidthCheckbox.isSelected() != settings.isFitTabsToEditorWidth()
                || overflowModeCombo.getItem() != settings.getOverflowMode()
                || groupTreeControlStyleCombo.getItem() != settings.getGroupTreeControlStyle()
                || invertGroupTreeControlFillCheckbox.isSelected() != settings.isInvertGroupTreeControlFill()
                || groupColorsEnabledCheckbox.isSelected() != settings.isGroupColorsEnabled()
                || hoverViewEnabledCheckbox.isSelected() != settings.isHoverViewEnabled()
                || hoverViewProjectToEditorCheckbox.isSelected() != settings.isHoverViewProjectToEditorEnabled()
                || showSubtabNameInMainTabCheckbox.isSelected() != settings.isShowSubtabNameInMainTab()
                || barHeightSlider.getValue() != settings.getBarHeightPercent()
                || textSizeSlider.getValue() != settings.getTextSizePercent()
                || tabFontStyleCombo.getItem() != settings.getTabFontStyle()
                || splittabsEnabledCheckbox.isSelected() != settings.isSplittabsEnabled()
                || splittabBehaviorModeCombo.getItem() != settings.getSplittabBehaviorMode()
                || splittabDissolveModeCombo.getItem() != settings.getSplittabDissolveMode()
                || splittabOtherPairFileModeCombo.getItem() != settings.getSplittabOtherPairFileMode()
                || restoreSwitchSplittabSessionCheckbox.isSelected()
                        != settings.isRestoreSwitchSplittabSessionOnProjectOpen();
        if (appearanceModified) {
            return true;
        }
        if (!rulesUiInitialized || rulesPanel == null || sidetabsRulesPanel == null) {
            return false;
        }
        return !rulesPanel.isSameAs(settings.getRules())
                || !sidetabsRulesPanel.isSameAs(settings.getSidetabRules());
    }

    @Override
    public void apply() {
        if (tabzEnabledCheckbox == null) {
            return;
        }

        TypographyPreview.clear();
        TabzSettings settings = TabzSettings.getInstance();
        settings.setTabzEnabled(tabzEnabledCheckbox.isSelected());
        settings.setSubtabsActive(hTabsActiveCheckbox.isSelected());
        settings.setReuseOpenSubtabGroupMainTab(reuseOpenSubtabGroupMainTabCheckbox.isSelected());
        settings.setSidetabsActive(sidetabsActiveCheckbox.isSelected());
        settings.setSidetabLayoutMode(sidetabLayoutModeCombo.getItem());
        settings.setSidetabsOnRight(sidetabsSideSwitch.isSelected());
        settings.setShowCollapseButton(showCollapseButtonCheckbox.isSelected());
        settings.setProjectViewGroupingEnabled(projectViewGroupingCheckbox.isSelected());
        settings.setFitTabsToEditorWidth(fitTabsToEditorWidthCheckbox.isSelected());
        settings.setOverflowMode(overflowModeCombo.getItem());
        settings.setGroupTreeControlStyle(groupTreeControlStyleCombo.getItem());
        settings.setInvertGroupTreeControlFill(invertGroupTreeControlFillCheckbox.isSelected());
        SubtabGroupColors.setEnabled(groupColorsEnabledCheckbox.isSelected());
        settings.setHoverViewEnabled(hoverViewEnabledCheckbox.isSelected());
        settings.setHoverViewProjectToEditorEnabled(hoverViewProjectToEditorCheckbox.isSelected());
        settings.setShowSubtabNameInMainTab(showSubtabNameInMainTabCheckbox.isSelected());
        settings.setBarHeightPercent(barHeightSlider.getValue());
        settings.setTextSizePercent(textSizeSlider.getValue());
        settings.setTabFontStyle(tabFontStyleCombo.getItem());
        settings.setSplittabsEnabled(splittabsEnabledCheckbox.isSelected());
        settings.setSplittabBehaviorMode(splittabBehaviorModeCombo.getItem());
        settings.setSplittabDissolveMode(splittabDissolveModeCombo.getItem());
        settings.setSplittabOtherPairFileMode(splittabOtherPairFileModeCombo.getItem());
        settings.setRestoreSwitchSplittabSessionOnProjectOpen(restoreSwitchSplittabSessionCheckbox.isSelected());
        if (rulesUiInitialized && rulesPanel != null && sidetabsRulesPanel != null) {
            settings.setRules(rulesPanel.getRules());
            settings.setSidetabRules(sidetabsRulesPanel.getRules());
        }
        TabzPresentation.applySettingsChange();
    }

    @Override
    public void reset() {
        if (tabzEnabledCheckbox == null) {
            return;
        }

        TypographyPreview.clear();
        TabzSettings settings = TabzSettings.getInstance();
        tabzEnabledCheckbox.setSelected(settings.isTabzEnabled());
        hTabsActiveCheckbox.setSelected(settings.isSubtabsActive());
        reuseOpenSubtabGroupMainTabCheckbox.setSelected(settings.isReuseOpenSubtabGroupMainTab());
        updateHTabsOptions();
        sidetabsActiveCheckbox.setSelected(settings.isSidetabsActive());
        sidetabLayoutModeCombo.setItem(settings.getSidetabLayoutMode());
        sidetabsSideSwitch.setSelected(settings.isSidetabsOnRight());
        updateSidetabLayoutOptions();
        showCollapseButtonCheckbox.setSelected(settings.isShowCollapseButton());
        projectViewGroupingCheckbox.setSelected(settings.isProjectViewGroupingEnabled());
        fitTabsToEditorWidthCheckbox.setSelected(settings.isFitTabsToEditorWidth());
        overflowModeCombo.setItem(settings.getOverflowMode());
        groupTreeControlStyleCombo.setItem(settings.getGroupTreeControlStyle());
        invertGroupTreeControlFillCheckbox.setSelected(settings.isInvertGroupTreeControlFill());
        groupColorsEnabledCheckbox.setSelected(settings.isGroupColorsEnabled());
        hoverViewEnabledCheckbox.setSelected(settings.isHoverViewEnabled());
        hoverViewProjectToEditorCheckbox.setSelected(settings.isHoverViewProjectToEditorEnabled());
        updateHoverSyncOptions();
        showSubtabNameInMainTabCheckbox.setSelected(settings.isShowSubtabNameInMainTab());
        updateGroupTreeControlOptions();
        barHeightSlider.setValue(settings.getBarHeightPercent());
        textSizeSlider.setValue(settings.getTextSizePercent());
        tabFontStyleCombo.setItem(settings.getTabFontStyle());
        splittabsEnabledCheckbox.setSelected(settings.isSplittabsEnabled());
        splittabBehaviorModeCombo.setItem(settings.getSplittabBehaviorMode());
        splittabDissolveModeCombo.setItem(settings.getSplittabDissolveMode());
        splittabOtherPairFileModeCombo.setItem(settings.getSplittabOtherPairFileMode());
        restoreSwitchSplittabSessionCheckbox.setSelected(settings.isRestoreSwitchSplittabSessionOnProjectOpen());
        updateSplittabBehaviorOptions();
        barHeightValueLabel.setText(formatPercent(barHeightSlider.getValue()));
        textSizeValueLabel.setText(formatPercent(textSizeSlider.getValue()));
        if (rulesUiInitialized && rulesPanel != null && sidetabsRulesPanel != null) {
            rulesPanel.reset(settings.getRules());
            sidetabsRulesPanel.reset(settings.getSidetabRules());
        }
    }

    @Override
    public void disposeUIResources() {
        rememberSettingsDialogTab();
        TypographyPreview.clear();
        tabzEnabledCheckbox = null;
        hTabsActiveCheckbox = null;
        reuseOpenSubtabGroupMainTabCheckbox = null;
        sidetabsActiveCheckbox = null;
        sidetabLayoutModeCombo = null;
        sidetabsSideSwitch = null;
        showCollapseButtonCheckbox = null;
        projectViewGroupingCheckbox = null;
        fitTabsToEditorWidthCheckbox = null;
        overflowModeCombo = null;
        groupTreeControlStyleCombo = null;
        invertGroupTreeControlFillCheckbox = null;
        groupColorsEnabledCheckbox = null;
        hoverViewEnabledCheckbox = null;
        hoverViewProjectToEditorCheckbox = null;
        showSubtabNameInMainTabCheckbox = null;
        barHeightSlider = null;
        textSizeSlider = null;
        tabFontStyleCombo = null;
        barHeightValueLabel = null;
        textSizeValueLabel = null;
        rulesPanel = null;
        sidetabsRulesPanel = null;
        rootPanel = null;
        mainTabs = null;
        hTabsPanel = null;
        vTabsPanel = null;
        hoverSyncPanel = null;
        rulesUiInitialized = false;
    }

    private void updateHTabsOptions() {
        if (hTabsActiveCheckbox == null || reuseOpenSubtabGroupMainTabCheckbox == null) {
            return;
        }
        reuseOpenSubtabGroupMainTabCheckbox.setEnabled(hTabsActiveCheckbox.isSelected());
    }

    private void updateHoverSyncOptions() {
        if (hoverViewEnabledCheckbox == null || hoverViewProjectToEditorCheckbox == null) {
            return;
        }
        boolean enabled = hoverViewEnabledCheckbox.isSelected();
        hoverViewProjectToEditorCheckbox.setEnabled(enabled);
    }

    private void confirmAndResetToDefaults() {
        if (rootPanel == null) {
            return;
        }
        int answer = Messages.showYesNoDialog(
                rootPanel,
                "All TabZ settings (Appearance, horizontal/vertical tabs, split pairs, AI, and group colors) "
                        + "will be reset to defaults.\n\n"
                        + "Custom rules and tweaks will be lost.",
                "Reset settings?",
                Messages.getWarningIcon()
        );
        if (answer != Messages.YES) {
            return;
        }
        TabzSettings.getInstance().resetToDefaults();
        reset();
        apply();
    }

    private static @NotNull JSlider createSlider(int min, int max, int value) {
        JSlider slider = new JSlider(min, max, value);
        slider.setPaintTicks(false);
        slider.setPaintLabels(false);
        slider.setMaximumSize(new Dimension(Integer.MAX_VALUE, slider.getPreferredSize().height));
        return slider;
    }

    private static @NotNull JPanel sliderRow(@NotNull JSlider slider, @NotNull JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(JBUI.scale(8), 0));
        row.add(slider, BorderLayout.CENTER);
        valueLabel.setPreferredSize(new Dimension(JBUI.scale(48), valueLabel.getPreferredSize().height));
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    private static @Nullable String migrateSettingsTabTitle(@Nullable String title) {
        if (title == null) {
            return null;
        }
        return switch (title) {
            case "Ansicht" -> "Appearance";
            case "H-Tabs" -> "Horizontal tabs";
            case "V-Tabs" -> "Vertical tabs";
            default -> title;
        };
    }

    private static @NotNull String formatPercent(int value) {
        return value + " %";
    }

    private void updateSidetabLayoutOptions() {
        if (sidetabLayoutModeCombo == null || sidetabsSideSwitch == null) {
            return;
        }
        SidetabLayoutMode mode = sidetabLayoutModeCombo.getItem();
        sidetabsSideSwitch.setEnabled(mode != null);
    }

    private void updateGroupTreeControlOptions() {
        if (groupTreeControlStyleCombo == null || invertGroupTreeControlFillCheckbox == null) {
            return;
        }
        SubtabGroupTreeControlStyle style = groupTreeControlStyleCombo.getItem();
        invertGroupTreeControlFillCheckbox.setEnabled(style != null && style.allowsGroupExpansion());
    }

    private void updateSplittabBehaviorOptions() {
        if (splittabsEnabledCheckbox == null || splittabBehaviorModeCombo == null) {
            return;
        }
        boolean enabled = splittabsEnabledCheckbox.isSelected();
        splittabBehaviorModeCombo.setEnabled(enabled);
        if (splittabDissolveModeCombo != null) {
            splittabDissolveModeCombo.setEnabled(enabled);
        }
        if (splittabOtherPairFileModeCombo != null) {
            splittabOtherPairFileModeCombo.setEnabled(enabled);
        }
        if (restoreSwitchSplittabSessionCheckbox != null) {
            boolean switchMode = enabled
                    && splittabBehaviorModeCombo.getItem() == SplittabBehaviorMode.DEDICATED_VIEW;
            restoreSwitchSplittabSessionCheckbox.setEnabled(switchMode);
        }
    }

    private void previewTypography() {
        if (barHeightSlider == null || textSizeSlider == null || tabFontStyleCombo == null) {
            return;
        }
        TypographyPreview.set(
                barHeightSlider.getValue(),
                textSizeSlider.getValue(),
                tabFontStyleCombo.getItem()
        );
        TabzPresentation.refreshTypography();
    }

    private static <T> @NotNull ComboBox<T> labeledEnumCombo(
            T[] values,
            @NotNull Function<T, String> labeler
    ) {
        ComboBox<T> combo = new ComboBox<>(values);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                Component component = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                );
                if (value != null) {
                    @SuppressWarnings("unchecked")
                    T item = (T) value;
                    setText(labeler.apply(item));
                }
                return component;
            }
        });
        return combo;
    }
}
