package dd.pp.interparaiment.immodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import dd.pp.interparaiment.immodel.context.Path;

public class MessModel implements IMessNode {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<Integer, TracedTarget> targets = new LinkedHashMap<>();
    private final ArrayList<IMessNode> freshMeat = new ArrayList<>();

    public void put(final Path path) {
        lock.writeLock().lock();
        try {

            final TracedTarget tracedTarget = this.targets.get(path.target.hashCode());

            if (tracedTarget != null) {
                tracedTarget.put(path);
            } else {
                final TracedTarget newChild = new TracedTarget(path);
                this.targets.put(path.target.hashCode(), newChild);
                this.freshMeat.add(newChild);
                // creationTreeBus.createNode(path)
            }
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public ArrayList<IMessNode> getFreshMeat() {
        final ArrayList<IMessNode> freshMeat = new ArrayList<>(this.freshMeat);
        this.freshMeat.clear();

        return freshMeat;
    }

    @Override
    public String getValue() {
        return "Mess";
    }

    public Lock getReadLock() {
        return this.lock.writeLock();
    }
}
