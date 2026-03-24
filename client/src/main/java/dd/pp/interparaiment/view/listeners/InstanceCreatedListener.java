package dd.pp.interparaiment.view.listeners;

import java.util.regex.Pattern;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.concurrency.AppExecutorUtil;

import dd.pp.interparaiment.event.IListener;
import dd.pp.interparaiment.event.requests.InstanceCreatedEvent;
import dd.pp.interparaiment.immodel.context.Path;
import dd.pp.interparaiment.view.controls.MessLogView;

public class InstanceCreatedListener implements IListener<InstanceCreatedEvent> {
    private static final Pattern CLASS_NAME_SPLITTER = Pattern.compile("\\.");
    private final MessLogView logView;
    private final Project project;
    private String previousCallerClass = null;

    public InstanceCreatedListener(final MessLogView logView, final Project project) {
        this.logView = logView;
        this.project = project;
    }

    @Override
    public void accept(final InstanceCreatedEvent data) {
        ReadAction.nonBlocking(() -> resolveNewInstance(data.getData()))
                .inSmartMode(project)
                .finishOnUiThread(ModalityState.defaultModalityState(), result -> logContext(result, data.getData()))
                .submit(AppExecutorUtil.getAppExecutorService());
    };

    private void logContext(final ConstructionLogContext context, final Path path) {
        if (!path.caller.equals(this.previousCallerClass)) {
            this.logView.logEmpty();
            this.logView.logStringWithYellowLabel(context.label, "--------------------------New Caller: " + context.callerName + "--------------------------");
            this.previousCallerClass = path.caller;
        }

        if (context.hyperlink == null) {
            this.logView.logStringWithRedLabel(context.label, context.message);
        } else {
            this.logView.logHyperlinkWithGreenLabel(context.label, context.message, context.hyperlink);
        }
    }

    private ConstructionLogContext resolveNewInstance(final Path path) {
        final PsiClass psiClass = JavaPsiFacade.getInstance(project)
                .findClass(path.caller, GlobalSearchScope.allScope(project));

        if (psiClass == null) {
            return createRawContext(path);
        }

        final VirtualFile file = PsiUtilCore.getVirtualFile(psiClass);

        if (file == null) {
            createRawContext(path);
            return null;
        }

        final OpenFileHyperlinkInfo linkToFile = new OpenFileHyperlinkInfo(this.project, file, path.line - 1);

        return createContextWithHyperlink(psiClass, path, linkToFile);
    }

    private ConstructionLogContext createRawContext(final Path path) {
        final String[] labelSplit = CLASS_NAME_SPLITTER.split(path.target);
        final String message = path.caller + "#" + path.method + "." + path.line + "(" + "(hash:" + path.instanceHash + ")";

        this.previousCallerClass = path.caller;

        return new ConstructionLogContext(labelSplit[labelSplit.length - 1], message, null, path.caller);
    }

    private ConstructionLogContext createContextWithHyperlink(PsiClass psiClass, final Path path, final OpenFileHyperlinkInfo linkToFile) {
        final String[] labelSplit = CLASS_NAME_SPLITTER.split(path.target);
        final String callerName = psiClass.getName();
        final String message = callerName + "." + path.method + "(hash:" + path.instanceHash + ")";

        this.previousCallerClass = path.caller;

        return new ConstructionLogContext(labelSplit[labelSplit.length - 1], message, linkToFile, callerName);
    }
}

class ConstructionLogContext {
    public final String label;
    public final String message;
    public final HyperlinkInfo hyperlink;
    public final String callerName;

    public ConstructionLogContext(final String label, final String message, final HyperlinkInfo hyperlink, final String callerName) {
        this.label = label;
        this.message = message;
        this.hyperlink = hyperlink;
        this.callerName = callerName;
    }
}
