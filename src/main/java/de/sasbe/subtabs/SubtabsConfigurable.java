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
    private JCheckBox sidetabsActiveCheckbox;
    private ComboBox<SidetabLayoutMode> sidetabLayoutModeCombo;
    private SidetabsSideSwitchButton sidetabsSideSwitch;
    private JCheckBox showCollapseButtonCheckbox;
    private JCheckBox groupInProjectViewCheckbox;
    private JCheckBox fitTabsToEditorWidthCheckbox;
    private ComboBox<SubtabOverflowMode> overflowModeCombo;
    private ComboBox<SubtabGroupTreeControlStyle> groupTreeControlStyleCombo;
    private JCheckBox invertGroupTreeControlFillCheckbox;
    private JCheckBox groupColorsEnabledCheckbox;
    private JCheckBox hoverViewEnabledCheckbox;
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
        sidetabsActiveCheckbox = new JCheckBox("V-Tabs aktivieren");
        sidetabsActiveCheckbox.setToolTipText("Vertikale Tab-Leiste am Editorrand");
        sidetabLayoutModeCombo = labeledEnumCombo(SidetabLayoutMode.values(), SidetabLayoutMode::label);
        sidetabLayoutModeCombo.addActionListener(event -> updateSidetabLayoutOptions());
        sidetabsSideSwitch = new SidetabsSideSwitchButton();
        showCollapseButtonCheckbox = new JCheckBox("Einklappen-Symbole anzeigen");
        showCollapseButtonCheckbox.setToolTipText(
                "Zeigt das Einklappen-Symbol in der H-Tab-Leiste, bei V-Tabs und im Projektbaum"
        );
        groupInProjectViewCheckbox = new JCheckBox(
                "Zugehörige Dateien im Projektbaum gruppieren"
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

        hoverViewEnabledCheckbox = new JCheckBox("Hover Sync");
        hoverViewEnabledCheckbox.setToolTipText(
                "Hebt beim Hover über H-Tabs, Dateien und Haupttabs die zugehörigen Tabs, H-Tabs "
                        + "und Projektbaum-Einträge hervor"
        );

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
                .addComponent(groupInProjectViewCheckbox)
                .addComponent(groupColorsEnabledCheckbox)
                .addComponent(hoverViewEnabledCheckbox)
                .addLabeledComponent("Gruppierung im Projektbaum", groupTreeControlStyleCombo)
                .addComponent(invertGroupTreeControlFillCheckbox)
                .addLabeledComponent("Tab-Höhe", sliderRow(barHeightSlider, barHeightValueLabel))
                .addLabeledComponent("Schriftgröße", sliderRow(textSizeSlider, textSizeValueLabel))
                .addLabeledComponent("Schriftstil", tabFontStyleCombo)
                .addComponent(fitTabsToEditorWidthCheckbox)
                .getPanel();

        JPanel hTabsOptionsPanel = FormBuilder.createFormBuilder()
                .addComponent(hTabsActiveCheckbox)
                .addComponent(showSubtabNameInMainTabCheckbox)
                .addLabeledComponent("Überlauf", overflowModeCombo)
                .getPanel();
        hTabsPanel = new JPanel(new BorderLayout());
        hTabsPanel.add(hTabsOptionsPanel, BorderLayout.NORTH);

        JPanel vTabsOptionsPanel = FormBuilder.createFormBuilder()
                .addComponent(sidetabsFlagRow)
                .getPanel();
        vTabsPanel = new JPanel(new BorderLayout());
        vTabsPanel.add(vTabsOptionsPanel, BorderLayout.NORTH);

        JPanel splitTabsPanel = new JPanel(new BorderLayout());
        splitTabsPanel.setBorder(JBUI.Borders.empty(8, 0));
        splitTabsPanel.add(new JBLabel("Noch keine Einstellungen."), BorderLayout.NORTH);

        JPanel aiPanel = new JPanel(new BorderLayout());
        aiPanel.setBorder(JBUI.Borders.empty(8, 0));
        aiPanel.add(new JBLabel("Noch keine Einstellungen."), BorderLayout.NORTH);

        rulesUiInitialized = false;

        mainTabs = new JTabbedPane();
        mainTabs.addTab("Ansicht", ansichtPanel);
        mainTabs.addTab("H-Tabs", hTabsPanel);
        mainTabs.addTab("V-Tabs", vTabsPanel);
        mainTabs.addTab("SplitTabs", splitTabsPanel);
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
        return rootPanel;
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
                || sidetabsActiveCheckbox.isSelected() != settings.isSidetabsActive()
                || sidetabLayoutModeCombo.getItem() != settings.getSidetabLayoutMode()
                || sidetabsSideSwitch.isSelected() != settings.isSidetabsOnRight()
                || showCollapseButtonCheckbox.isSelected() != settings.isShowCollapseButton()
                || groupInProjectViewCheckbox.isSelected() != settings.isGroupRelatedFilesInProjectView()
                || fitTabsToEditorWidthCheckbox.isSelected() != settings.isFitTabsToEditorWidth()
                || overflowModeCombo.getItem() != settings.getOverflowMode()
                || groupTreeControlStyleCombo.getItem() != settings.getGroupTreeControlStyle()
                || invertGroupTreeControlFillCheckbox.isSelected() != settings.isInvertGroupTreeControlFill()
                || groupColorsEnabledCheckbox.isSelected() != settings.isGroupColorsEnabled()
                || hoverViewEnabledCheckbox.isSelected() != settings.isHoverViewEnabled()
                || showSubtabNameInMainTabCheckbox.isSelected() != settings.isShowSubtabNameInMainTab()
                || barHeightSlider.getValue() != settings.getBarHeightPercent()
                || textSizeSlider.getValue() != settings.getTextSizePercent()
                || tabFontStyleCombo.getItem() != settings.getTabFontStyle();
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
        settings.setSidetabsActive(sidetabsActiveCheckbox.isSelected());
        settings.setSidetabLayoutMode(sidetabLayoutModeCombo.getItem());
        settings.setSidetabsOnRight(sidetabsSideSwitch.isSelected());
        settings.setShowCollapseButton(showCollapseButtonCheckbox.isSelected());
        settings.setGroupRelatedFilesInProjectView(groupInProjectViewCheckbox.isSelected());
        settings.setFitTabsToEditorWidth(fitTabsToEditorWidthCheckbox.isSelected());
        settings.setOverflowMode(overflowModeCombo.getItem());
        settings.setGroupTreeControlStyle(groupTreeControlStyleCombo.getItem());
        settings.setInvertGroupTreeControlFill(invertGroupTreeControlFillCheckbox.isSelected());
        SubtabGroupColors.setEnabled(groupColorsEnabledCheckbox.isSelected());
        settings.setHoverViewEnabled(hoverViewEnabledCheckbox.isSelected());
        settings.setShowSubtabNameInMainTab(showSubtabNameInMainTabCheckbox.isSelected());
        settings.setBarHeightPercent(barHeightSlider.getValue());
        settings.setTextSizePercent(textSizeSlider.getValue());
        settings.setTabFontStyle(tabFontStyleCombo.getItem());
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
        sidetabsActiveCheckbox.setSelected(settings.isSidetabsActive());
        sidetabLayoutModeCombo.setItem(settings.getSidetabLayoutMode());
        sidetabsSideSwitch.setSelected(settings.isSidetabsOnRight());
        updateSidetabLayoutOptions();
        showCollapseButtonCheckbox.setSelected(settings.isShowCollapseButton());
        groupInProjectViewCheckbox.setSelected(settings.isGroupRelatedFilesInProjectView());
        fitTabsToEditorWidthCheckbox.setSelected(settings.isFitTabsToEditorWidth());
        overflowModeCombo.setItem(settings.getOverflowMode());
        groupTreeControlStyleCombo.setItem(settings.getGroupTreeControlStyle());
        invertGroupTreeControlFillCheckbox.setSelected(settings.isInvertGroupTreeControlFill());
        groupColorsEnabledCheckbox.setSelected(settings.isGroupColorsEnabled());
        hoverViewEnabledCheckbox.setSelected(settings.isHoverViewEnabled());
        showSubtabNameInMainTabCheckbox.setSelected(settings.isShowSubtabNameInMainTab());
        updateGroupTreeControlOptions();
        barHeightSlider.setValue(settings.getBarHeightPercent());
        textSizeSlider.setValue(settings.getTextSizePercent());
        tabFontStyleCombo.setItem(settings.getTabFontStyle());
        barHeightValueLabel.setText(formatPercent(barHeightSlider.getValue()));
        textSizeValueLabel.setText(formatPercent(textSizeSlider.getValue()));
        if (rulesUiInitialized && rulesPanel != null && sidetabsRulesPanel != null) {
            rulesPanel.reset(settings.getRules());
            sidetabsRulesPanel.reset(settings.getSidetabRules());
        }
    }

    @Override
    public void disposeUIResources() {
        TypographyPreview.clear();
        familiaEnabledCheckbox = null;
        hTabsActiveCheckbox = null;
        sidetabsActiveCheckbox = null;
        sidetabLayoutModeCombo = null;
        sidetabsSideSwitch = null;
        showCollapseButtonCheckbox = null;
        groupInProjectViewCheckbox = null;
        fitTabsToEditorWidthCheckbox = null;
        overflowModeCombo = null;
        groupTreeControlStyleCombo = null;
        invertGroupTreeControlFillCheckbox = null;
        groupColorsEnabledCheckbox = null;
        hoverViewEnabledCheckbox = null;
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
        rulesUiInitialized = false;
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
