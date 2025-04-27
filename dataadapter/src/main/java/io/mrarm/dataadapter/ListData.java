package io.mrarm.dataadapter;

import androidx.databinding.ObservableList;

import java.util.List;

import io.mrarm.observabletransform.Bindable;

public class ListData<T> extends BaseDataFragment<T> {

    private List<? extends T> list;
    private int oldListItemCount = 0;
    private ListViewHolderTypeResolver<T> viewHolderTypeResolver;
    private ObservableListListener listListener;

    public ListData(List<? extends T> list, ListViewHolderTypeResolver<T> typeResolver) {
        setSource(list, typeResolver);
    }

    public ListData(List<? extends T> list, ViewHolderTypeResolver<T> typeResolver) {
        setSource(list, typeResolver);
    }

    public ListData(List<? extends T> list, ViewHolderType<T> type) {
        setSource(list, type);
    }

    public ListData(ObservableList<? extends T> list, ListViewHolderTypeResolver<T> typeResolver) {
        setSource(list, typeResolver);
    }

    public ListData(ObservableList<? extends T> list, ViewHolderTypeResolver<T> typeResolver) {
        setSource(list, typeResolver);
    }

    public ListData(ObservableList<? extends T> list, ViewHolderType<T> type) {
        setSource(list, type);
    }

    private void updateItemCounts() {
        if (oldListItemCount > 0)
            super.notifyItemRangeRemoved(0, oldListItemCount);
        oldListItemCount = list.size();
        super.notifyItemRangeInserted(0, oldListItemCount);
    }

    public void setSource(List<? extends T> list, ListViewHolderTypeResolver<T> typeResolver) {
        if (listListener != null) {
            //noinspection unchecked
            ((ObservableList<? extends T>) this.list).removeOnListChangedCallback(listListener);
            listListener = null;
            if (this.list instanceof Bindable)
                ((Bindable) this.list).unbind();
        }
        this.list = list;
        this.viewHolderTypeResolver = typeResolver;
        updateItemCounts();
    }

    public void setSource(List<? extends T> list, ViewHolderTypeResolver<T> typeResolver) {
        setSource(list, (i, value) -> typeResolver.resolveType(value));
    }

    public void setSource(List<? extends T> list, ViewHolderType<T> type) {
        setSource(list, (i, value) -> type);
    }


    public void setSource(ObservableList<? extends T> list, ListViewHolderTypeResolver<T> typeResolver) {
        setSource((List<? extends T>) list, typeResolver);
        listListener = new ObservableListListener();
        if (isBound()) {
            //noinspection unchecked
            list.addOnListChangedCallback(listListener);

            if (list instanceof Bindable)
                ((Bindable) list).bind();
        }
    }

    public void setSource(ObservableList<? extends T> list, ViewHolderTypeResolver<T> typeResolver) {
        setSource(list, (i, value) -> typeResolver.resolveType(value));
    }

    public void setSource(ObservableList<? extends T> list, ViewHolderType<T> type) {
        setSource(list, (i, value) -> type);
    }


    @Override
    public T getItem(int index) {
        return list.get(index);
    }

    @Override
    public int getItemCount() {
        return oldListItemCount;
    }

    @Override
    public ViewHolderType<T> getHolderTypeFor(int index) {
        return viewHolderTypeResolver.resolveType(index, list.get(index));
    }

    @Override
    public void buildElementPath(ElementPath.Builder builder, int index) {
        // This might be a weird quirk to the ElementPath - however, because the ElementPath is
        // going to be used to get relative positions of items, it makes sense to have the actual
        // item in there, so it can be easily fetched.
        builder.add(new ElementPath.SimpleElement(null, index, index));
    }

    @Override
    protected void onBind() {
        if (listListener != null) {
            //noinspection unchecked
            ((ObservableList<? extends T>) this.list).addOnListChangedCallback(listListener);

            if (list instanceof Bindable)
                ((Bindable) list).bind();
        }
        updateItemCounts();
    }

    @Override
    protected void onUnbind() {
        if (listListener != null) {
            //noinspection unchecked
            ((ObservableList<? extends T>) this.list).removeOnListChangedCallback(listListener);
            if (list instanceof Bindable)
                ((Bindable) list).unbind();
        }
    }

    // export the notify methods

    public void notifyItemRangeChanged(int index, int count) {
        super.notifyItemRangeChanged(index, count);
    }

    public void notifyItemRangeInserted(int index, int count) {
        oldListItemCount += count;
        super.notifyItemRangeInserted(index, count);
    }

    public void notifyItemRangeRemoved(int index, int count) {
        oldListItemCount -= count;
        super.notifyItemRangeRemoved(index, count);
    }

    public void notifyItemRangeMoved(int index, int toIndex, int count) {
        super.notifyItemRangeMoved(index, toIndex, count);
    }

    private class ObservableListListener extends ObservableList.OnListChangedCallback {

        @Override
        public void onChanged(ObservableList sender) {
            int newItemCount = list.size();
            notifyItemRangeChanged(0, Math.min(oldListItemCount, newItemCount));
            if (newItemCount > oldListItemCount)
                ListData.super.notifyItemRangeInserted(oldListItemCount, newItemCount - oldListItemCount);
            else if (newItemCount < oldListItemCount)
                ListData.super.notifyItemRangeRemoved(newItemCount, oldListItemCount - newItemCount);
            oldListItemCount = newItemCount;
        }

        @Override
        public void onItemRangeChanged(ObservableList sender, int positionStart, int itemCount) {
            notifyItemRangeChanged(positionStart, itemCount);
        }

        @Override
        public void onItemRangeInserted(ObservableList sender, int positionStart, int itemCount) {
            notifyItemRangeInserted(positionStart, itemCount);
        }

        @Override
        public void onItemRangeMoved(ObservableList sender, int fromPosition, int toPosition, int itemCount) {
            notifyItemRangeMoved(fromPosition, toPosition, itemCount);
        }

        @Override
        public void onItemRangeRemoved(ObservableList sender, int positionStart, int itemCount) {
            notifyItemRangeRemoved(positionStart, itemCount);
        }

    }

}
