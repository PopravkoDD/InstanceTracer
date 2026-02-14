package dd.pp.interparaiment.command.toolhandlers;

import javafx.scene.Node;

import dd.pp.interparaiment.command.BaseHookHandler;
import dd.pp.interparaiment.command.context.HandlingContext;

public class PathLogger extends BaseHookHandler {
    @Override
    protected void handle(HandlingContext context, final Node handlingObject) {
        context.pathLogResult.insert(0, " -> " + handlingObject.getClass().getSimpleName() + "@" +  System.identityHashCode(handlingObject));
    }
}
