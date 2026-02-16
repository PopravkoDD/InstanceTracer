package dd.pp.interparaiment.command.toolhandlers;

import javafx.scene.Node;

import dd.pp.interparaiment.BootstrapHookBridge;
import dd.pp.interparaiment.command.BaseHookHandler;
import dd.pp.interparaiment.command.context.HandlingContext;

public class FxUiClickConstructionChecker extends BaseHookHandler {

    @Override
    protected void handle(final HandlingContext context, final Node handlingObject) {
        final Object o = BootstrapHookBridge.getInstance().getTrackedInstances().get(System.identityHashCode(handlingObject));
        if (o != null) {
            System.out.println("Target Tracked: " + o);
        }
    }
}
