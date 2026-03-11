package dd.pp.interparaiment.immodel;

import java.util.ArrayList;
import java.util.Collection;

public interface IMessNode {
    ArrayList<? extends IMessNode> getChildrenIndexed();
    Collection<? extends IMessNode> getChildrenPure();
}
