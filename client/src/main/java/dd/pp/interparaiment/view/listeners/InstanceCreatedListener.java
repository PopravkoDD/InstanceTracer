package dd.pp.interparaiment.view.listeners;

import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilCore;

import dd.pp.interparaiment.event.IListener;
import dd.pp.interparaiment.event.requests.InstanceCreatedEvent;
import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.view.controls.MessLogView;

public class InstanceCreatedListener implements IListener<InstanceCreatedEvent> {
    private static final Pattern CLASS_NAME_SPLITTER = Pattern.compile("\\.");
    private final MessLogView logView;
    private final Project project;

    public InstanceCreatedListener(final MessLogView logView, final Project project) {
        this.logView = logView;
        this.project = project;
    }

    @Override
    public void accept(final InstanceCreatedEvent data) {
        SwingUtilities.invokeLater(() -> {
            final Path path = data.getData();

            final PsiClass psiClass = JavaPsiFacade.getInstance(project)
                    .findClass(path.caller, GlobalSearchScope.allScope(project));

            if (psiClass == null) {
                logRaw(path);
                return;
            }

            final VirtualFile file = PsiUtilCore.getVirtualFile(psiClass);

            if (file == null) {
                logRaw(path);
                return;
            }

            final OpenFileHyperlinkInfo linkToFile = new OpenFileHyperlinkInfo(this.project, file, path.line - 1);

            logWithHyperlink(psiClass, path, linkToFile);
        });

    }

    private void logRaw(final Path path) {
        this.logView.logStringWithRedLabel(path.target, path.caller + "#" + path.method + "." + path.line + "(" + path.instanceHash + ")");
    }

    private void logWithHyperlink(PsiClass psiClass, final Path path, final OpenFileHyperlinkInfo linkToFile) {
        final String message = psiClass.getName() + "." + path.method + "(hash:" + path.instanceHash + ")";
        final String[] split = CLASS_NAME_SPLITTER.split(path.target);
        this.logView.logHyperlinkWithGreenLabel(split[split.length - 1], message, linkToFile);
    }
}
