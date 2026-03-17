package dd.pp.interparaiment.view.actions;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

public class ConfigureTrackerAction extends AnAction {

    private final MessActionsContext state;

    public ConfigureTrackerAction(final MessActionsContext state) {
        super("Configure Tracking", "Opens Instance tacker config", AllIcons.General.Settings);
        this.state = state;
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent anActionEvent) {

    }
}
