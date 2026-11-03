package dd.pp.interparaiment.view.run;

import java.awt.Component;
import java.util.Comparator;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import dd.pp.interparaiment.InspectorConfiguration;
import org.jetbrains.annotations.NotNull;

final class MessInspectionSettingsEditor extends SettingsEditor<MessInspectionConfiguration> {
    private final Project project;
    private final JComboBox<RunnerAndConfigurationSettings> configurations = new JComboBox<>();
    private final JTextField host = new JTextField();
    private final JTextField port = new JTextField();
    private final JBLabel selectionMessage = new JBLabel();
    private boolean resetting;

    MessInspectionSettingsEditor(Project project) {
        this.project = project;
        configurations.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                if (value instanceof RunnerAndConfigurationSettings settings) {
                    setText(settings.getName());
                    setIcon(settings.getType().getIcon());
                    setToolTipText(settings.getType().getDisplayName());
                } else {
                    setText("Select a Run Configuration");
                    setToolTipText(null);
                }
                return this;
            }
        });
        configurations.addActionListener(event -> {
            if (!resetting) {
                selectionMessage.setText("");
                fireEditorStateChanged();
            }
        });
        DocumentAdapter changes = new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent event) {
                if (!resetting) {
                    fireEditorStateChanged();
                }
            }
        };
        host.getDocument().addDocumentListener(changes);
        port.getDocument().addDocumentListener(changes);
    }

    @Override
    protected void resetEditorFrom(@NotNull MessInspectionConfiguration configuration) {
        resetting = true;
        try {
            configurations.removeAllItems();
            RunManager.getInstance(project).getAllSettings().stream()
                    .filter(settings -> !(settings.getConfiguration() instanceof MessInspectionConfiguration))
                    .sorted(Comparator.comparing(RunnerAndConfigurationSettings::getName, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(settings -> settings.getType().getDisplayName()))
                    .forEach(configurations::addItem);
            RunnerAndConfigurationSettings selected = configuration.findUnderlyingConfiguration();
            configurations.setSelectedItem(selected);
            host.setText(configuration.getHost());
            port.setText(Integer.toString(configuration.getPort()));
            selectionMessage.setText(selected == null && !configuration.getConfigurationId().isEmpty()
                    ? "Missing configuration: " + configuration.getConfigurationName() : "");
        } finally {
            resetting = false;
        }
    }

    @Override
    protected void applyEditorTo(@NotNull MessInspectionConfiguration configuration) throws ConfigurationException {
        RunnerAndConfigurationSettings selected = (RunnerAndConfigurationSettings) configurations.getSelectedItem();
        if (selected == null || !RunManager.getInstance(project).getAllSettings().contains(selected)
                || selected.getConfiguration() instanceof MessInspectionConfiguration) {
            throw new ConfigurationException("Select an existing Run Configuration");
        }
        final int enteredPort;
        try {
            enteredPort = Integer.parseInt(port.getText().trim());
            InspectorConfiguration.validateEndpoint(host.getText(), enteredPort);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Host must be a hostname/IP and port must be between 1 and 65535: " + e.getMessage());
        }
        configuration.setUnderlyingConfiguration(selected);
        configuration.setHost(host.getText().trim());
        configuration.setPort(enteredPort);
    }

    @Override
    protected @NotNull JComponent createEditor() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Run Configuration:", configurations)
                .addComponent(selectionMessage)
                .addComponent(new JBLabel("Create and configure the application in its own Run Configuration first."))
                .addLabeledComponent("Inspection host:", host)
                .addLabeledComponent("Inspection port:", port)
                .addComponent(new JBLabel("Trace targets are configured in the InstanceTracker tool window."))
                .getPanel();
    }
}
