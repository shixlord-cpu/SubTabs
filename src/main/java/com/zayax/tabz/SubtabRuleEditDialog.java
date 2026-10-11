package com.zayax.tabz;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

final class SubtabRuleEditDialog extends DialogWrapper {
    private final CustomSubtabRule rule;
    private final JBCheckBox enabledCheckBox = new JBCheckBox("Enabled");
    private final JBTextField nameField = new JBTextField();
    private final JBTextField groupSuffixField = new JBTextField();
    private final JBTextArea patternsArea = new JBTextArea(3, 40);
    private final JBTextField nameSegmentsField = new JBTextField();
    private final JBTextField groupNameSegmentsField = new JBTextField();
    private final ComboBox<String> searchComboBox = new ComboBox<>(new String[]{
            SubtabRulesDefaults.SEARCH_FOLDER_LABEL,
            SubtabRulesDefaults.SEARCH_NEIGHBORS_LABEL
    });
    private final JBTextArea excludePatternsArea = new JBTextArea(3, 40);

    SubtabRuleEditDialog(@Nullable JComponent parent, @NotNull CustomSubtabRule rule) {
        super(parent, true);
        this.rule = rule.copy();
        enabledCheckBox.setSelected(this.rule.enabled);
        nameField.setText(this.rule.name);
        groupSuffixField.setText(this.rule.groupSuffix);
        patternsArea.setText(this.rule.patterns);
        nameSegmentsField.setText(this.rule.nameSegments);
        groupNameSegmentsField.setText(this.rule.groupNameSegments);
        searchComboBox.setSelectedItem(SubtabRulesDefaults.searchScopeLabel(this.rule.searchNeighbors));
        excludePatternsArea.setText(this.rule.excludePatterns);
        setTitle("Edit TabZ rule");
        init();
    }

    @NotNull CustomSubtabRule rule() {
        return rule.copy();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        patternsArea.setLineWrap(true);
        patternsArea.setWrapStyleWord(true);
        excludePatternsArea.setLineWrap(true);
        excludePatternsArea.setWrapStyleWord(true);

        JBLabel help = new JBLabel("""
                <html><b>Tab</b> and <b>Group</b>: dot segments as in the table (e.g. <code>2</code> or <code>1, 2</code>).<br>\
                <b>Exclude</b>: files this rule should ignore (e.g. <code>.actions.ts</code>).</html>
                """);
        help.setBorder(JBUI.Borders.emptyBottom(8));

        FormBuilder builder = FormBuilder.createFormBuilder()
                .addComponent(enabledCheckBox)
                .addLabeledComponent(new JBLabel("Name:"), nameField, 1, false)
                .addLabeledComponent(new JBLabel("Suffix:"), groupSuffixField, 1, false)
                .addLabeledComponent(new JBLabel("Patterns:"), new JBScrollPane(patternsArea), 1, false)
                .addLabeledComponent(new JBLabel("Tab:"), nameSegmentsField, 1, false)
                .addLabeledComponent(new JBLabel("Group:"), groupNameSegmentsField, 1, false)
                .addLabeledComponent(new JBLabel("Search:"), searchComboBox, 1, false)
                .addLabeledComponent(new JBLabel("Exclude:"), new JBScrollPane(excludePatternsArea), 1, false)
                .addComponent(help);

        JPanel panel = builder.getPanel();
        panel.setBorder(JBUI.Borders.empty(4));

        boolean special = rule.isSpecial();
        groupSuffixField.setEnabled(!special);
        patternsArea.setEnabled(!special);
        nameSegmentsField.setEnabled(!special);
        groupNameSegmentsField.setEnabled(!special);
        searchComboBox.setEnabled(!special);
        excludePatternsArea.setEnabled(!special);
        if (special) {
            help.setText("<html>Folder and Custom groups are built-in rules with fixed patterns.</html>");
        }
        return panel;
    }

    @Override
    protected void doOKAction() {
        rule.enabled = enabledCheckBox.isSelected();
        rule.name = nameField.getText().trim();
        rule.groupSuffix = groupSuffixField.getText().trim();
        rule.patterns = patternsArea.getText().trim();
        rule.nameSegments = nameSegmentsField.getText().trim();
        rule.groupNameSegments = groupNameSegmentsField.getText().trim();
        rule.searchNeighbors = SubtabRulesDefaults.isNeighborsSearchLabel(String.valueOf(searchComboBox.getSelectedItem()));
        rule.excludePatterns = excludePatternsArea.getText().trim();
        super.doOKAction();
    }
}
