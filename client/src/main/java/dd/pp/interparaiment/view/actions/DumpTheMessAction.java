package dd.pp.interparaiment.view.actions;

import org.jetbrains.annotations.NotNull;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import dd.pp.interparaiment.event.EventManager;

public class DumpTheMessAction extends AnAction {
    private final EventManager eventManager;
    private final MessActionsContext state;

    public DumpTheMessAction(final MessActionsContext state, final EventManager eventManager) {
        super("Dumps The Mess", "Shows all tracked instances", AllIcons.Actions.Dump);
        this.state = state;
        this.eventManager = eventManager;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        this.state.getUpdater().update();
    }
}
