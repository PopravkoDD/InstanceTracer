package dd.pp.interparaiment.view.service;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.FormBuilder;

/** IDE-owned project settings UI, independent of trace targets and execution options. */
public final class TrackerSettingsConfigurable implements Configurable {
    private final TrackerSettingsService settings;
    private final JCheckBox recordOnStart = new JCheckBox("Record automatically when an inspection connects");

    public TrackerSettingsConfigurable(Project project) {
        settings = project.getService(TrackerSettingsService.class);
    }

    @Override
    public String getDisplayName() { return "InstanceTracer"; }

    @Override
    public JComponent createComponent() {
        JPanel panel = FormBuilder.createFormBuilder().addComponent(recordOnStart).getPanel();
        reset();
        return panel;
    }

    @Override
    public boolean isModified() {
        return recordOnStart.isSelected() != settings.getClientConfiguration().recordOnStart();
    }

    @Override
    public void apply() {
        settings.updateClientConfiguration(new ClientConfiguration(recordOnStart.isSelected()));
    }

    @Override
    public void reset() {
        recordOnStart.setSelected(settings.getClientConfiguration().recordOnStart());
    }
}
