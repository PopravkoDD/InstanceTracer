package dd.pp.interparaiment.command;

import javafx.scene.Node;

public class ByTextAnchorHandler implements INodeAnchorHandler {
    private final INodeAnchorHandler next;
    public ByTextAnchorHandler(final INodeAnchorHandler next) {
        this.next = next;
    }
    @Override
    public String handle(Node node) {
//        if (node instanceof TextInputControl) {
//            return extractAnchor(node);
//        }

        return next.handle(node);
    }

//    private static String extractAnchor(Node node) {
//        Document doc = ((JTextComponent) component).getDocument();
//        if (doc.getLength() == 0) {
//            return AnchorlessComponentHandler.getComponentIndexInModel(component, component.getParent());
//        }
//        return  "text:::" + doc.getText(0, doc.getLength());
//    }
}
