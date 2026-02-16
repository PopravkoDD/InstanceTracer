package dd.pp.interparaiment.bbhooks;

import java.lang.instrument.Instrumentation;
import java.util.Optional;
import java.util.stream.Stream;

import dd.pp.interparaiment.BootstrapHookBridge;
import dd.pp.interparaiment.command.toolhandlers.FxUiClickConstructionChecker;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

public class ConstructionTracker {

    public static void attachTracker(final Instrumentation instrumentation, final String params) {
        final String targetName = extractTarget(params);
        new AgentBuilder.Default()
                .with(AgentBuilder.Listener.StreamWriting.toSystemError().withErrorsOnly())
                .type(ElementMatchers.named(targetName))
                .transform((builder, typeDesc, classLoader, module, pd) ->
                        builder.visit(
                                Advice.to(FxUiClickConstructionChecker.class)
                                        .on(ElementMatchers.isConstructor())
                        )
                )
                .installOn(instrumentation);

        System.out.println(targetName + " Transformed");
    }
    private static String extractTarget(String target) {
        String key = "ScanTarget=";
        int start = target.indexOf(key);
        if (start != -1) {
            start += key.length();
            int end = target.indexOf(' ', start);
            if (end == -1) end = target.length();
            return target.substring(start, end);
        }
        return null;
    }

    @Advice.OnMethodExit()
    public static void exit(@Advice.This Object self) {
        final BootstrapHookBridge bhb = BootstrapHookBridge.getInstance();
        final int constructionHash = System.identityHashCode(self);

        if (!bhb.getTrackedInstances().contains(constructionHash)) {
            bhb.getTrackedInstances().put(constructionHash, createMessage(self));
            bhb.notifyInstanceCreated(constructionHash);
        }
    }
    public static Optional<StackWalker.StackFrame> walk(Stream<StackWalker.StackFrame> s) {
        return s.skip(1).findFirst();
    }

    public static String format(StackWalker.StackFrame f) {
        return f.getClassName() + "." + f.getMethodName() +
                "(" + f.getFileName() + ":" + f.getLineNumber() + ")";
    }

    public static String createMessage(final Object self) {
        return self.getClass().getName() + ": " + StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(ConstructionTracker::walk)
                .map(ConstructionTracker::format).get();
    }
}
