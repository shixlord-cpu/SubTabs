package de.sasbe.subtabs;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
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

public final class SubtabsConfigurable implements SearchableConfigurable {
    private JCheckBox subtabsActiveCheckbox;
    private JCheckBox showCollapseButtonCheckbox;
    private JCheckBox scrollOnHoverCheckbox;
    private JCheckBox groupInProjectViewCheckbox;
    private JCheckBox fitTabsToEditorWidthCheckbox;
    private ComboBox<SubtabOverflowMode> overflowModeCombo;
    private ComboBox<SubtabGroupTreeControlStyle> groupTreeControlStyleCombo;
    private JCheckBox invertGroupTreeControlFillCheckbox;
    private JCheckBox groupColorsEnabledCheckbox;
    private JSlider barHeightSlider;
    private JSlider textSizeSlider;
    private JLabel barHeightValueLabel;
    private JLabel textSizeValueLabel;
    private SubtabsRulesPanel rulesPanel;

    @Override
    public @NotNull String getId() {
        return "de.sasbe.subtabs.settings";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "SubTabs";
    }

    @Override
    public @Nullable JComponent createComponent() {
        subtabsActiveCheckbox = new JCheckBox("SubTabs anzeigen");
        showCollapseButtonCheckbox = new JCheckBox("Einklappen-Symbol anzeigen");
        scrollOnHoverCheckbox = new JCheckBox(
                "Beim Hover über einen Subtab zur Datei im Projektbaum scrollen"
        );
        groupInProjectViewCheckbox = new JCheckBox(
                "Zugehörige Dateien im Projektbaum gruppieren"
        );
        fitTabsToEditorWidthCheckbox = new JCheckBox(
                "Tab- und Schriftgröße an die Breite des Editors anpassen"
        );
        overflowModeCombo = new ComboBox<>(SubtabOverflowMode.values());
        overflowModeCombo.setRenderer(new DefaultListCellRenderer() {
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
                if (value instanceof SubtabOverflowMode mode) {
                    setText(mode.label());
                }
                return component;
            }
        });
        groupTreeControlStyleCombo = new ComboBox<>(SubtabGroupTreeControlStyle.values());
        groupTreeControlStyleCombo.setRenderer(new DefaultListCellRenderer() {
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
                if (value instanceof SubtabGroupTreeControlStyle style) {
                    setText(style.label());
                }
                return component;
            }
        });
        groupTreeControlStyleCombo.addActionListener(event -> updateGroupTreeControlOptions());

        invertGroupTreeControlFillCheckbox = new JCheckBox("Füllung invertieren (gefüllt wenn geöffnet)");

        groupColorsEnabledCheckbox = new JCheckBox(
                "Subtabgruppen im Editor und Projektbaum farblich markieren"
        );
        groupColorsEnabledCheckbox.setToolTipText(
                "Weist jeder Subtabgruppe eine Farbe zu und zeigt sie als Rand im Editor und Projektbaum an."
        );

        barHeightSlider = createSlider(25, 100, 75);
        barHeightValueLabel = new JBLabel(formatPercent(barHeightSlider.getValue()));
        barHeightSlider.addChangeListener(event ->
                barHeightValueLabel.setText(formatPercent(barHeightSlider.getValue()))
        );

        textSizeSlider = createSlider(50, 100, 75);
        textSizeValueLabel = new JBLabel(formatPercent(textSizeSlider.getValue()));
        textSizeSlider.addChangeListener(event ->
                textSizeValueLabel.setText(formatPercent(textSizeSlider.getValue()))
        );

        rulesPanel = new SubtabsRulesPanel();

        JPanel generalPanel = FormBuilder.createFormBuilder()
                .addComponent(subtabsActiveCheckbox)
                .addComponent(showCollapseButtonCheckbox)
                .addComponent(scrollOnHoverCheckbox)
                .addComponent(groupInProjectViewCheckbox)
                .addComponent(fitTabsToEditorWidthCheckbox)
                .addLabeledComponent("Überlauf", overflowModeCombo)
                .addLabeledComponent("Gruppierung im Projektbaum", groupTreeControlStyleCombo)
                .addComponent(invertGroupTreeControlFillCheckbox)
                .addLabeledComponent("Tab-Höhe", sliderRow(barHeightSlider, barHeightValueLabel))
                .addLabeledComponent("Schriftgröße", sliderRow(textSizeSlider, textSizeValueLabel))
                .getPanel();

        JPanel groupColorsPanel = FormBuilder.createFormBuilder()
                .addComponent(groupColorsEnabledCheckbox)
                .addComponent(new JBLabel(
                        "Beim Aktivieren erhalten alle bestehenden Subtabgruppen automatisch unterschiedliche Farben. "
                                + "Neue Gruppen werden während der Aktivierung ebenfalls eingefärbt. "
                                + "Im Projektbaum kann die Farbe per Rechtsklick auf eine Gruppe geändert werden."
                ))
                .getPanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Allgemein", generalPanel);
        tabs.addTab("Gruppenfarben", groupColorsPanel);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.add(tabs, BorderLayout.NORTH);
        panel.add(rulesPanel.createPanel(), BorderLayout.CENTER);
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        if (subtabsActiveCheckbox == null || rulesPanel == null) {
            return false;
        }

        SubtabsSettings settings = SubtabsSettings.getInstance();
        return subtabsActiveCheckbox.isSelected() != settings.isSubtabsActive()
                || showCollapseButtonCheckbox.isSelected() != settings.isShowCollapseButton()
                || scrollOnHoverCheckbox.isSelected() != settings.isScrollProjectViewOnSubtabHover()
                || groupInProjectViewCheckbox.isSelected() != settings.isGroupRelatedFilesInProjectView()
                || fitTabsToEditorWidthCheckbox.isSelected() != settings.isFitTabsToEditorWidth()
                || overflowModeCombo.getItem() != settings.getOverflowMode()
                || groupTreeControlStyleCombo.getItem() != settings.getGroupTreeControlStyle()
                || invertGroupTreeControlFillCheckbox.isSelected() != settings.isInvertGroupTreeControlFill()
                || groupColorsEnabledCheckbox.isSelected() != settings.isGroupColorsEnabled()
                || barHeightSlider.getValue() != settings.getBarHeightPercent()
                || textSizeSlider.getValue() != settings.getTextSizePercent()
                || !rulesPanel.isSameAs(settings.getRules());
    }

    @Override
    public void apply() {
        if (subtabsActiveCheckbox == null || rulesPanel == null) {
            return;
        }

        SubtabsSettings settings = SubtabsSettings.getInstance();
        settings.setSubtabsActive(subtabsActiveCheckbox.isSelected());
        settings.setShowCollapseButton(showCollapseButtonCheckbox.isSelected());
        settings.setScrollProjectViewOnSubtabHover(scrollOnHoverCheckbox.isSelected());
        settings.setGroupRelatedFilesInProjectView(groupInProjectViewCheckbox.isSelected());
        settings.setFitTabsToEditorWidth(fitTabsToEditorWidthCheckbox.isSelected());
        settings.setOverflowMode(overflowModeCombo.getItem());
        settings.setGroupTreeControlStyle(groupTreeControlStyleCombo.getItem());
        settings.setInvertGroupTreeControlFill(invertGroupTreeControlFillCheckbox.isSelected());
        SubtabGroupColors.setEnabled(groupColorsEnabledCheckbox.isSelected());
        settings.setBarHeightPercent(barHeightSlider.getValue());
        settings.setTextSizePercent(textSizeSlider.getValue());
        settings.setRules(rulesPanel.getRules());
        SubtabsPresentation.applySettingsChange();
    }

    @Override
    public void reset() {
        if (subtabsActiveCheckbox == null || rulesPanel == null) {
            return;
        }

        SubtabsSettings settings = SubtabsSettings.getInstance();
        subtabsActiveCheckbox.setSelected(settings.isSubtabsActive());
        showCollapseButtonCheckbox.setSelected(settings.isShowCollapseButton());
        scrollOnHoverCheckbox.setSelected(settings.isScrollProjectViewOnSubtabHover());
        groupInProjectViewCheckbox.setSelected(settings.isGroupRelatedFilesInProjectView());
        fitTabsToEditorWidthCheckbox.setSelected(settings.isFitTabsToEditorWidth());
        overflowModeCombo.setItem(settings.getOverflowMode());
        groupTreeControlStyleCombo.setItem(settings.getGroupTreeControlStyle());
        invertGroupTreeControlFillCheckbox.setSelected(settings.isInvertGroupTreeControlFill());
        groupColorsEnabledCheckbox.setSelected(settings.isGroupColorsEnabled());
        updateGroupTreeControlOptions();
        barHeightSlider.setValue(settings.getBarHeightPercent());
        textSizeSlider.setValue(settings.getTextSizePercent());
        barHeightValueLabel.setText(formatPercent(barHeightSlider.getValue()));
        textSizeValueLabel.setText(formatPercent(textSizeSlider.getValue()));
        rulesPanel.reset(settings.getRules());
    }

    @Override
    public void disposeUIResources() {
        subtabsActiveCheckbox = null;
        showCollapseButtonCheckbox = null;
        scrollOnHoverCheckbox = null;
        groupInProjectViewCheckbox = null;
        fitTabsToEditorWidthCheckbox = null;
        overflowModeCombo = null;
        groupTreeControlStyleCombo = null;
        invertGroupTreeControlFillCheckbox = null;
        groupColorsEnabledCheckbox = null;
        barHeightSlider = null;
        textSizeSlider = null;
        barHeightValueLabel = null;
        textSizeValueLabel = null;
        rulesPanel = null;
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

    private void updateGroupTreeControlOptions() {
        if (groupTreeControlStyleCombo == null || invertGroupTreeControlFillCheckbox == null) {
            return;
        }
        SubtabGroupTreeControlStyle style = groupTreeControlStyleCombo.getItem();
        boolean expansionEnabled = style != null && style.allowsGroupExpansion();
        invertGroupTreeControlFillCheckbox.setEnabled(expansionEnabled);
    }
}
