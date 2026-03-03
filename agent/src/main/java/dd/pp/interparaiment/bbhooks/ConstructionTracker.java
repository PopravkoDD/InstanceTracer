package dd.pp.interparaiment.bbhooks;

import java.lang.instrument.Instrumentation;
import java.util.Optional;
import java.util.stream.Stream;

import dd.pp.interparaiment.BootstrapHookBridge;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

public class ConstructionTracker {
    public static String SPLITTER = ",";

    public static void attachTracker(final Instrumentation instrumentation, final String targetName) {
        new AgentBuilder.Default()
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
                .type(ElementMatchers.named(targetName))
                .transform((builder, typeDesc, classLoader, module, pd) ->
                        builder.visit(
                                Advice.to(ConstructionTracker.class)
                                        .on(ElementMatchers.isConstructor())
                        )
                )
                .installOn(instrumentation);

        System.out.println(targetName + " Transformed");
    }

    @Advice.OnMethodExit()
    public static void exit(@Advice.This Object self) {
        final BootstrapHookBridge bhb = BootstrapHookBridge.getInstance();
        final int constructionHash = System.identityHashCode(self);

        final StackWalker.StackFrame callerFrame = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(ConstructionTracker::walk)
                .get();

        final String callerClass = callerFrame.getClassName();
        final String callerMethod = callerFrame.getMethodName();
        final int lineNumber = callerFrame.getLineNumber();

        final String message = new StringBuilder()
                .append(constructionHash)
                .append(ConstructionTracker.SPLITTER)
                .append(self.getClass().getName())
                .append(ConstructionTracker.SPLITTER)
                .append(callerClass)
                .append(ConstructionTracker.SPLITTER)
                .append(callerMethod)
                .append(ConstructionTracker.SPLITTER)
                .append(lineNumber)
                .toString();

        bhb.getTrackedInstances().put(constructionHash, message);
        bhb.notifyInstanceCreated(constructionHash);
        System.out.println("Instance tracked");
    }
    public static Optional<StackWalker.StackFrame> walk(Stream<StackWalker.StackFrame> stackStream) {
        return stackStream.skip(1).findFirst();
    }

    public static String format(StackWalker.StackFrame frame) {
        return frame.getClassName() + "." + frame.getMethodName() +
                "(" + frame.getFileName() + ":" + frame.getLineNumber() + ")";
    }
}
