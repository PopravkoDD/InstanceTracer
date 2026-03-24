package dd.pp.interparaiment.view.controls;

import java.awt.Component;

import com.intellij.openapi.application.ApplicationManager;

public class UIUpdater {
    private final Component component;
    private final Runnable preRevalidateTask;

    public UIUpdater(final Component component, final Runnable preRevalidateTask) {
        this.component = component;
        this.preRevalidateTask = preRevalidateTask;
    }

    public void update() {
        ApplicationManager.getApplication().invokeLater(() -> {
            this.preRevalidateTask.run();
            component.revalidate();
            component.repaint();
        });
    }
}
