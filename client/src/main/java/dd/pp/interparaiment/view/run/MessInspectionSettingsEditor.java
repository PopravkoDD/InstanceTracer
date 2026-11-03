package dd.pp.interparaiment.view.run;

import java.awt.BorderLayout;
import java.util.EnumMap;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import dd.pp.interparaiment.InspectorConfiguration;
import org.jetbrains.annotations.NotNull;

final class MessInspectionSettingsEditor extends SettingsEditor<MessInspectionConfiguration> {
    private final JComboBox<MessInspectionConfiguration.Kind> kind = new JComboBox<>(MessInspectionConfiguration.Kind.values());
    private final JTextField host = new JTextField();
    private final JTextField port = new JTextField();
    private final JPanel nativePanel = new JPanel(new BorderLayout());
    private final JBLabel error = new JBLabel();
    private final EnumMap<MessInspectionConfiguration.Kind, RunConfiguration> drafts =
            new EnumMap<>(MessInspectionConfiguration.Kind.class);
    private MessInspectionConfiguration source;
    private MessInspectionConfiguration.Kind displayedKind;
    private SettingsEditor<RunConfiguration> nativeEditor;
    private boolean resetting;

    MessInspectionSettingsEditor() {
        kind.addActionListener(event -> switchEditor());
    }

    @Override
    public boolean isReadyForApply() {
        return nativeEditor != null && nativeEditor.isReadyForApply();
    }

    @Override
    public boolean isSpecificallyModified() {
        return nativeEditor != null && nativeEditor.isSpecificallyModified();
    }

    @Override
    protected void resetEditorFrom(@NotNull MessInspectionConfiguration configuration) {
        resetting = true;
        try {
            source = configuration;
            drafts.clear();
            // These are UI transaction drafts, not execution-time copies.
            drafts.put(configuration.getKind(), configuration.getPeer().clone());
            kind.setSelectedItem(configuration.getKind());
            host.setText(configuration.getHost());
            port.setText(Integer.toString(configuration.getPort()));
            showNativeEditor(configuration.getKind());
            error.setText("");
        } finally {
            resetting = false;
        }
    }

    private void switchEditor() {
        if (resetting || source == null) {
            return;
        }
        try {
            if (!nativeEditor.isReadyForApply()) {
                throw new ConfigurationException("Wait for the native configuration editor to finish loading");
            }
            nativeEditor.applyTo(drafts.get(displayedKind));
            showNativeEditor((MessInspectionConfiguration.Kind) kind.getSelectedItem());
            error.setText("");
            fireEditorStateChanged();
        } catch (ConfigurationException e) {
            error.setText(e.getMessage());
            resetting = true;
            kind.setSelectedItem(displayedKind);
            resetting = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void showNativeEditor(MessInspectionConfiguration.Kind selected) {
        if (nativeEditor != null) {
            Disposer.dispose(nativeEditor);
        }
        RunConfiguration draft = drafts.computeIfAbsent(selected, source::createPeer);
        nativeEditor = (SettingsEditor<RunConfiguration>) draft.getConfigurationEditor();
        Disposer.register(this, nativeEditor);
        nativeEditor.addSettingsEditorListener(editor -> fireEditorStateChanged());
        nativeEditor.resetFrom(draft);
        nativePanel.removeAll();
        nativePanel.add(nativeEditor.getComponent(), BorderLayout.CENTER);
        displayedKind = selected;
        nativePanel.revalidate();
        nativePanel.repaint();
    }

    @Override
    protected void applyEditorTo(@NotNull MessInspectionConfiguration configuration) throws ConfigurationException {
        final int enteredPort;
        try {
            enteredPort = Integer.parseInt(port.getText().trim());
            InspectorConfiguration.validateEndpoint(host.getText(), enteredPort);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Host must be a hostname/IP and port must be between 1 and 65535: " + e.getMessage());
        }
        configuration.setKind(displayedKind);
        nativeEditor.applyTo(configuration.getPeer());
        configuration.setHost(host.getText().trim());
        configuration.setPort(enteredPort);
    }

    @Override
    protected @NotNull JComponent createEditor() {
        JPanel options = FormBuilder.createFormBuilder()
                .addLabeledComponent("Underlying configuration:", kind)
                .addLabeledComponent("Inspection host:", host)
                .addLabeledComponent("Inspection port:", port)
                .addComponent(new JBLabel("Trace targets are configured in the InstanceTracker tool window."))
                .addComponent(error).getPanel();
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(options, BorderLayout.NORTH);
        panel.add(nativePanel, BorderLayout.CENTER);
        return panel;
    }
}
