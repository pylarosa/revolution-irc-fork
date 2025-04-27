package io.mrarm.dataadapter;

import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;

import io.mrarm.observabletransform.Bindable;

public class ConditionalDataFragment<T> extends BaseDataFragment<T> {

    private ObservableBoolean condition;
    private boolean oldCondition;
    private DataFragment<T> wrapped;
    private final ObservableListener listener = new ObservableListener();

    public ConditionalDataFragment(DataFragment<T> wrapped, ObservableBoolean condition) {
        this.wrapped = wrapped;
        this.condition = condition;
        oldCondition = condition.get();
    }

    @Override
    protected void onBind() {
        wrapped.addListener(listener);
        wrapped.bind();
        condition.addOnPropertyChangedCallback(listener);
        if (condition instanceof Bindable)
            ((Bindable) condition).bind();
        onChanged();
    }

    @Override
    protected void onUnbind() {
        wrapped.unbind();
        wrapped.removeListener(listener);
        condition.removeOnPropertyChangedCallback(listener);
        if (condition instanceof Bindable)
            ((Bindable) condition).unbind();
    }

    private void onChanged() {
        boolean newCondition = condition.get();
        if (newCondition == oldCondition)
            return;
        if (newCondition)
            notifyItemRangeInserted(0, wrapped.getItemCount());
        else
            notifyItemRangeRemoved(0, wrapped.getItemCount());
        this.oldCondition = newCondition;
    }

    @Override
    public int getItemCount() {
        return oldCondition ? wrapped.getItemCount() : 0;
    }

    @Override
    public T getItem(int index) {
        return wrapped.getItem(index);
    }

    @Override
    public Object getContext(int index) {
        return wrapped.getContext(index);
    }

    @Override
    public ViewHolderType<T> getHolderTypeFor(int index) {
        return wrapped.getHolderTypeFor(index);
    }

    @Override
    public void buildElementPath(ElementPath.Builder builder, int index) {
        wrapped.buildElementPath(builder, index);
    }

    private class ObservableListener extends Observable.OnPropertyChangedCallback
            implements DataFragment.Listener {

        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            onChanged();
        }

        @Override
        public void onItemRangeInserted(DataFragment fragment, int index, int count) {
            if (oldCondition)
                notifyItemRangeInserted(index, count);
        }

        @Override
        public void onItemRangeRemoved(DataFragment fragment, int index, int count) {
            if (oldCondition)
                notifyItemRangeRemoved(index, count);
        }

        @Override
        public void onItemRangeChanged(DataFragment fragment, int index, int count) {
            if (oldCondition)
                notifyItemRangeChanged(index, count);
        }

        @Override
        public void onItemRangeMoved(DataFragment fragment, int index, int toIndex, int count) {
            if (oldCondition)
                notifyItemRangeMoved(index, toIndex, count);
        }
    }

}
