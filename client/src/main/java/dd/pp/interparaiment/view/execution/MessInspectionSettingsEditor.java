package dd.pp.interparaiment.view.execution;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import dd.pp.interparaiment.view.dialogs.TrackerConfigurationDialog;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

final class MessInspectionSettingsEditor extends SettingsEditor<MessInspectionConfiguration> {
    private final Project project;
    private final JPanel panel = new JPanel(new GridBagLayout());
    private final JComboBox<RunnerAndConfigurationSettings> configurations = new JComboBox<>();
    private final JTextField host = new JTextField(20);
    private final JSpinner port = new JSpinner(new SpinnerNumberModel(35824, 1, 65535, 1));
    private final JCheckBox recordOnStart = new JCheckBox("Enable recording on start");
    private final JLabel targetCount = new JLabel();
    private final JLabel missingConfiguration = new JLabel();
    private List<String> traceTargets = List.of();

    MessInspectionSettingsEditor(Project project) {
        this.project = project;
        configurations.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                if (value instanceof RunnerAndConfigurationSettings settings) {
                    setText(settings.getName() + " (" + settings.getType().getDisplayName() + ")");
                    setIcon(settings.getType().getIcon());
                }
                return this;
            }
        });
        JButton targets = new JButton("Edit trace targets…");
        targets.addActionListener(event -> {
            TrackerConfigurationDialog dialog = new TrackerConfigurationDialog(project, traceTargets);
            if (dialog.showAndGet()) {
                traceTargets = List.copyOf(dialog.getTraceTargets());
                updateTargetCount();
                fireEditorStateChanged();
            }
        });
        JPanel targetEditor = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        targetEditor.add(targets);
        targetEditor.add(Box.createHorizontalStrut(8));
        targetEditor.add(targetCount);
        addRow(0, "Run Configuration:", configurations);
        addRow(1, "", missingConfiguration);
        addRow(2, "Receiver host:", host);
        addRow(3, "Receiver port:", port);
        addRow(4, "Trace targets:", targetEditor);
        addRow(5, "", recordOnStart);
    }

    private void addRow(int row, String text, JComponent component) {
        GridBagConstraints label = new GridBagConstraints();
        label.gridx = 0;
        label.gridy = row;
        label.anchor = GridBagConstraints.WEST;
        label.insets = new Insets(4, 4, 4, 8);
        panel.add(new JLabel(text), label);
        GridBagConstraints field = new GridBagConstraints();
        field.gridx = 1;
        field.gridy = row;
        field.weightx = 1;
        field.fill = GridBagConstraints.HORIZONTAL;
        field.insets = new Insets(4, 0, 4, 4);
        panel.add(component, field);
    }

    @Override
    protected void resetEditorFrom(@NotNull MessInspectionConfiguration configuration) {
        configurations.removeAllItems();
        for (RunnerAndConfigurationSettings candidate : RunManager.getInstance(project).getAllSettings()) {
            if (!(candidate.getConfiguration() instanceof MessInspectionConfiguration)) configurations.addItem(candidate);
        }
        RunnerAndConfigurationSettings selected = configuration.findUnderlyingConfiguration();
        configurations.setSelectedItem(selected);
        missingConfiguration.setText(selected == null && !configuration.getUnderlyingName().isBlank()
                ? "Missing configuration: " + configuration.getUnderlyingName() : "");
        host.setText(configuration.getHost());
        port.setValue(configuration.getPort());
        recordOnStart.setSelected(configuration.isRecordOnStart());
        traceTargets = configuration.getTraceTargets();
        updateTargetCount();
    }

    @Override
    protected void applyEditorTo(@NotNull MessInspectionConfiguration configuration) throws ConfigurationException {
        RunnerAndConfigurationSettings selected = (RunnerAndConfigurationSettings)configurations.getSelectedItem();
        if (selected == null) throw new ConfigurationException("Select an existing Run Configuration.");
        if (!RunManager.getInstance(project).getAllSettings().contains(selected)) {
            throw new ConfigurationException("The selected Run Configuration was removed. Reopen the editor and select an existing configuration.");
        }
        if (host.getText().isBlank() || host.getText().contains(",")) {
            throw new ConfigurationException("Enter a nonempty receiver host without commas.");
        }
        configuration.selectUnderlyingConfiguration(selected);
        configuration.setHost(host.getText());
        configuration.setPort(((Number)port.getValue()).intValue());
        configuration.setTraceTargets(traceTargets);
        configuration.setRecordOnStart(recordOnStart.isSelected());
    }

    private void updateTargetCount() {
        targetCount.setText(traceTargets.size() + " classes");
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return panel;
    }
}
