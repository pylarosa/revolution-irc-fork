package io.mrarm.dataadapter;

public class SingleItemData<T> extends BaseDataFragment<T> {

    private T value;
    private ViewHolderType<T> holderType;

    public SingleItemData(T value, ViewHolderType<T> holderType) {
        this.value = value;
        this.holderType = holderType;
    }

    public void setValue(T value) {
        this.value = value;
        notifyChanged();
    }

    public void setValue(T value, ViewHolderType<T> holderType) {
        this.value = value;
        this.holderType = holderType;
        notifyChanged();
    }

    public void setHolderType(ViewHolderType<T> holderType) {
        this.holderType = holderType;
        notifyChanged();
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @Override
    public T getItem(int index) {
        return value;
    }

    @Override
    public ViewHolderType<T> getHolderTypeFor(int index) {
        return holderType;
    }

    @Override
    public void buildElementPath(ElementPath.Builder builder, int index) {
    }

    public void notifyChanged() {
        notifyItemRangeChanged(0, 1);
    }

}
