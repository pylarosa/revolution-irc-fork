package io.mrarm.dataadapter;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseDataFragment<T> implements DataFragment<T> {

    private final List<Listener> listeners = new ArrayList<>();
    private Object context;
    private int refCount = 0;

    @Override
    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    @Override
    public void bind() {
        if (refCount++ == 0)
            onBind();
    }

    @Override
    public void unbind() {
        if (--refCount == 0)
            onUnbind();
    }

    /**
     * Called when the first ref has been bound (after all refs have been unbound before or no refs
     * have been bound before at all).
     */
    protected void onBind() {
    }

    /**
     * Called when all refs have been unbound.
     */
    protected void onUnbind() {
    }

    /**
     * Checks whether we have at least a single ref (bind() has been called more times than unbind())
     * @return whether we have at least a single bind ref
     */
    protected final boolean isBound() {
        return refCount > 0;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    @Override
    public Object getContext(int index) {
        return context;
    }

    protected void notifyItemRangeInserted(int index, int count) {
        for (Listener l : listeners)
            l.onItemRangeInserted(this, index, count);
    }

    protected void notifyItemRangeRemoved(int index, int count) {
        for (Listener l : listeners)
            l.onItemRangeRemoved(this, index, count);
    }

    protected void notifyItemRangeChanged(int index, int count) {
        for (Listener l : listeners)
            l.onItemRangeChanged(this, index, count);
    }

    protected void notifyItemRangeMoved(int index, int toIndex, int count) {
        for (Listener l : listeners)
            l.onItemRangeMoved(this, index, toIndex, count);
    }

}
