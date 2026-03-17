package dd.pp.interparaiment.view.controls;

import java.awt.Component;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.concurrency.EdtExecutorService;

/**
 * @author dvhaus gmbh in cooperation with intechcore gmbh
 */
public class UIUpdater {
    private final Component component;
    private final Runnable preRevalidateTask;
    private ScheduledFuture<?> future;

    public UIUpdater(final Component component, final Runnable preRevalidateTask) {
        this.component = component;
        this.preRevalidateTask = preRevalidateTask;
    }

    public void start() {
        future = EdtExecutorService.getScheduledExecutorInstance()
                .scheduleWithFixedDelay(this::refreshUi, 0, 10000, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (future != null) {
            future.cancel(false);
            future = null;
        }
    }

    private void refreshUi() {
        ApplicationManager.getApplication().invokeLater(() -> {
            this.preRevalidateTask.run();
            component.revalidate();
            component.repaint();
        });
    }
}
