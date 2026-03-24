package dd.pp.interparaiment.view.listeners;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;

import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
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
    private final ExecutorService executor;
    private String previousCallerClass = "";

    public InstanceCreatedListener(final MessLogView logView, final Project project) {
        this.logView = logView;
        this.project = project;

        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ConstructionResolverExecutor");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void accept(final InstanceCreatedEvent data) {
        final Path path = data.getData();
        final boolean printNewCaller = !Objects.equals(this.previousCallerClass, path.caller);

        if (printNewCaller) {
            this.previousCallerClass = path.caller;
        }

        this.executor.submit(() -> {
            final ConstructionLogContext context = resolveNewInstance(path, printNewCaller);
            printInEDT(context);
        });
    }

    private void printInEDT(final ConstructionLogContext context) {
        ApplicationManager.getApplication().invokeLater(() -> logContext(context));
    }

    private void logContext(final ConstructionLogContext context) {
        if (context.printNewCaller) {
            this.logView.logEmpty();
            this.logView.logStringWithYellowLabel(context.label, "--------------------------New Caller: " + context.callerName + "--------------------------");
        }

        if (context.hyperlink == null) {
            this.logView.logStringWithRedLabel(context.label, context.message);
        } else {
            this.logView.logHyperlinkWithGreenLabel(context.label, context.message, context.hyperlink);
        }
    }

    @NotNull
    private ConstructionLogContext resolveNewInstance(final Path path, final boolean printNewCaller) {
        final PsiClass psiClass = ReadAction.compute(
                () -> JavaPsiFacade.getInstance(project)
                        .findClass(path.caller, GlobalSearchScope.allScope(project)));

        if (psiClass == null) {
            return createRawContext(path, printNewCaller);
        }

        final VirtualFile file = PsiUtilCore.getVirtualFile(psiClass);

        if (file == null) {
            return createRawContext(path, printNewCaller);
        }

        final OpenFileHyperlinkInfo linkToFile = new OpenFileHyperlinkInfo(this.project, file, path.line - 1);

        return createContextWithHyperlink(psiClass, path, linkToFile, printNewCaller);
    }

    @NotNull
    private ConstructionLogContext createRawContext(final Path path, final boolean printNewCaller) {
        final String[] labelSplit = CLASS_NAME_SPLITTER.split(path.target);
        final String message = path.caller + "#" + path.method + "." + path.line + "(" + "(hash:" + path.instanceHash + ")";

        this.previousCallerClass = path.caller;

        return new ConstructionLogContext(labelSplit[labelSplit.length - 1], message, null, path.caller, printNewCaller);
    }

    private ConstructionLogContext createContextWithHyperlink(PsiClass psiClass, final Path path, final OpenFileHyperlinkInfo linkToFile, final boolean printNewCaller) {
        final String[] labelSplit = CLASS_NAME_SPLITTER.split(path.target);
        final String callerName = psiClass.getName();
        final String message = callerName + "." + path.method + "(hash:" + path.instanceHash + ")";

        this.previousCallerClass = path.caller;

        return new ConstructionLogContext(labelSplit[labelSplit.length - 1], message, linkToFile, callerName, printNewCaller);
    }

    public void dispose() {
        this.executor.shutdown();
    }
}

class ConstructionLogContext {
    public final String label;
    public final String message;
    public final HyperlinkInfo hyperlink;
    public final String callerName;
    public boolean printNewCaller;

    public ConstructionLogContext(final String label, final String message, final HyperlinkInfo hyperlink, final String callerName, final boolean printNewCaller) {
        this.label = label;
        this.message = message;
        this.hyperlink = hyperlink;
        this.callerName = callerName;
        this.printNewCaller = printNewCaller;
    }
}
