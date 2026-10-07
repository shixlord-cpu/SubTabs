package de.sasbe.subtabs;

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

public final class SubtabsConfigurable implements SearchableConfigurable {
    private JCheckBox familiaEnabledCheckbox;
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
        return "de.sasbe.subtabs.settings";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Familia";
    }

    @Override
    public @Nullable JComponent createComponent() {
        familiaEnabledCheckbox = new JCheckBox("Familia aktivieren");
        hTabsActiveCheckbox = new JCheckBox("H-Tabs aktivieren");
        hTabsActiveCheckbox.setToolTipText("Horizontale Tab-Leiste direkt über dem Editor");
        reuseOpenSubtabGroupMainTabCheckbox = new JCheckBox(
                "Bestehenden Gruppen-Haupttab wiederverwenden"
        );
        reuseOpenSubtabGroupMainTabCheckbox.setToolTipText(
                "Ist bereits eine Datei derselben Subtab-Gruppe geöffnet, wird keine zweite "
                        + "Haupttab-Leiste geöffnet, sondern in diesem Tab per Subtab gewechselt. "
                        + "Gilt nicht während einer aktiven Split-Pair-Sitzung."
        );
        hTabsActiveCheckbox.addActionListener(event -> updateHTabsOptions());
        sidetabsActiveCheckbox = new JCheckBox("V-Tabs aktivieren");
        sidetabsActiveCheckbox.setToolTipText("Vertikale Tab-Leiste am Editorrand");
        sidetabLayoutModeCombo = labeledEnumCombo(SidetabLayoutMode.values(), SidetabLayoutMode::label);
        sidetabLayoutModeCombo.addActionListener(event -> updateSidetabLayoutOptions());
        sidetabsSideSwitch = new SidetabsSideSwitchButton();
        showCollapseButtonCheckbox = new JCheckBox("Einklappen-Symbole anzeigen");
        showCollapseButtonCheckbox.setToolTipText(
                "Zeigt das Einklappen-Symbol in der H-Tab-Leiste und bei V-Tabs"
        );
        projectViewGroupingCheckbox = new JCheckBox("Gruppierung");
        projectViewGroupingCheckbox.setToolTipText(
                "Zeigt die Gruppierungsschaltfläche im Projektbaum; Ein- und Ausblenden dort unabhängig von dieser Option"
        );
        overflowModeCombo = labeledEnumCombo(SubtabOverflowMode.values(), SubtabOverflowMode::label);
        groupTreeControlStyleCombo = labeledEnumCombo(
                SubtabGroupTreeControlStyle.values(),
                SubtabGroupTreeControlStyle::label
        );
        groupTreeControlStyleCombo.addActionListener(event -> updateGroupTreeControlOptions());

        invertGroupTreeControlFillCheckbox = new JCheckBox("Füllung invertieren (gefüllt wenn geöffnet)");

        groupColorsEnabledCheckbox = new JCheckBox("Gruppenfarben");
        groupColorsEnabledCheckbox.setToolTipText(
                "Färbt Editor-Haupttabs und „X Dateien“ im Projektbaum in der Gruppenfarbe ein"
        );

        hoverViewEnabledCheckbox = new JCheckBox("Hover Sync aktivieren");
        hoverViewEnabledCheckbox.setToolTipText(
                "Hebt beim Hover über Projektbaum, H-Tabs, Haupttabs und Split-Pair-Leisten die "
                        + "zugehörigen Partner-UI hervor (z. B. H-Tabs und Haupttabs)"
        );
        hoverViewProjectToEditorCheckbox = new JCheckBox("Editor highlighten");
        hoverViewProjectToEditorCheckbox.setToolTipText(
                "Hebt beim Hover über Dateien im Projektbaum oder in der Hover-Selectbox den "
                        + "Code-Editor offener Dateien im Vordergrund-Fenster hervor"
        );
        hoverViewEnabledCheckbox.addActionListener(event -> updateHoverSyncOptions());

        showSubtabNameInMainTabCheckbox = new JCheckBox(
                "Subtab-Namen im Haupttab in Klammern anzeigen"
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
                "Tab- und Schriftgröße an die Breite des Editors anpassen"
        );

        JPanel sidetabsFlagRow = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0));
        sidetabsFlagRow.setOpaque(false);
        sidetabsFlagRow.add(sidetabsActiveCheckbox);
        sidetabsFlagRow.add(sidetabLayoutModeCombo);
        sidetabsFlagRow.add(sidetabsSideSwitch);

        JPanel ansichtPanel = FormBuilder.createFormBuilder()
                .addComponent(familiaEnabledCheckbox)
                .addComponent(showCollapseButtonCheckbox)
                .addComponent(groupColorsEnabledCheckbox)
                .addLabeledComponent("Gruppierung im Projektbaum", groupTreeControlStyleCombo)
                .addComponent(invertGroupTreeControlFillCheckbox)
                .addLabeledComponent("Tab-Höhe", sliderRow(barHeightSlider, barHeightValueLabel))
                .addLabeledComponent("Schriftgröße", sliderRow(textSizeSlider, textSizeValueLabel))
                .addLabeledComponent("Schriftstil", tabFontStyleCombo)
                .addComponent(fitTabsToEditorWidthCheckbox)
                .addLabeledComponent("H-Tabs bei Überlauf", overflowModeCombo)
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

        splittabsEnabledCheckbox = new JCheckBox("Split Pairs aktivieren");
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
                "Gilt für Mixed und Switch, wenn bei offenem Split Pair eine Datei geöffnet wird, "
                        + "die zu einem anderen gespeicherten Split Pair gehört"
        );
        restoreSwitchSplittabSessionCheckbox = new JCheckBox("Switch-Split-Pairs beim Projektstart wiederherstellen");
        restoreSwitchSplittabSessionCheckbox.setToolTipText(
                "Wenn Switch aktiv war und Split Pairs im Vordergrund, beim nächsten Öffnen des Projekts "
                        + "dieselbe Switch-Sitzung laden. Aus: zuvor gespeicherte normale Editor-Tabs laden."
        );
        splittabsEnabledCheckbox.addActionListener(event -> updateSplittabBehaviorOptions());
        JPanel splitTabsPanel = FormBuilder.createFormBuilder()
                .addComponent(splittabsEnabledCheckbox)
                .addLabeledComponent("Verhalten", splittabBehaviorModeCombo)
                .addLabeledComponent("Verhalten bei Auflösung", splittabDissolveModeCombo)
                .addLabeledComponent("Datei eines anderen Split Pairs öffnen", splittabOtherPairFileModeCombo)
                .addComponent(restoreSwitchSplittabSessionCheckbox)
                .getPanel();

        JPanel aiPanel = new JPanel(new BorderLayout());
        aiPanel.setBorder(JBUI.Borders.empty(8, 0));
        aiPanel.add(new JBLabel("Noch keine Einstellungen."), BorderLayout.NORTH);

        rulesUiInitialized = false;

        mainTabs = new JTabbedPane();
        mainTabs.addTab("Ansicht", ansichtPanel);
        mainTabs.addTab("Hover Sync", hoverSyncPanel);
        mainTabs.addTab("H-Tabs", hTabsPanel);
        mainTabs.addTab("V-Tabs", vTabsPanel);
        mainTabs.addTab("Split Pairs", splitTabsPanel);
        mainTabs.addTab("AI", aiPanel);
        mainTabs.addChangeListener(event -> {
            Component selected = mainTabs.getSelectedComponent();
            if (selected == hTabsPanel || selected == vTabsPanel) {
                ensureRulesUiInitialized();
            }
        });

        JButton resetButton = new JButton("Alle Einstellungen zurücksetzen");
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
        String savedTitle = SubtabsSettings.getInstance().getSettingsDialogSelectedTabTitle();
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
        SubtabsSettings.getInstance().setSettingsDialogSelectedTabTitle(mainTabs.getTitleAt(index));
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
        SubtabsSettings settings = SubtabsSettings.getInstance();
        rulesPanel.reset(settings.getRules());
        sidetabsRulesPanel.reset(settings.getSidetabRules());
        hTabsPanel.revalidate();
        vTabsPanel.revalidate();
    }

    @Override
    public boolean isModified() {
        if (familiaEnabledCheckbox == null) {
            return false;
        }

        SubtabsSettings settings = SubtabsSettings.getInstance();
        boolean appearanceModified = familiaEnabledCheckbox.isSelected() != settings.isFamiliaEnabled()
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
        if (familiaEnabledCheckbox == null) {
            return;
        }

        TypographyPreview.clear();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setFamiliaEnabled(familiaEnabledCheckbox.isSelected());
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
        SubtabsPresentation.applySettingsChange();
    }

    @Override
    public void reset() {
        if (familiaEnabledCheckbox == null) {
            return;
        }

        TypographyPreview.clear();
        SubtabsSettings settings = SubtabsSettings.getInstance();
        familiaEnabledCheckbox.setSelected(settings.isFamiliaEnabled());
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
        familiaEnabledCheckbox = null;
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
                "Alle Familia-Einstellungen (Ansicht, H-Tabs, V-Tabs, SplitTabs, AI und Gruppenfarben) "
                        + "werden auf die Standardwerte zurückgesetzt.\n\n"
                        + "Eigene Regeln und Anpassungen gehen dabei verloren.",
                "Einstellungen zurücksetzen?",
                Messages.getWarningIcon()
        );
        if (answer != Messages.YES) {
            return;
        }
        SubtabsSettings.getInstance().resetToDefaults();
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
        SubtabsPresentation.refreshTypography();
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
