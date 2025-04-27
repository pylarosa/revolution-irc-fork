package io.mrarm.dataadapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;

public final class DataBindingViewHolderType<T> extends ViewHolderType<T> {

    private int layoutId = -1;
    private int brValue = -1;
    private int brContext = -1;
    private BindCallback<T, ?, ?> bindCallback;

    private DataBindingViewHolderType() {
    }

    public int getValueVarId() {
        return brValue;
    }

    public int getContextVarId() {
        return brContext;
    }

    public BindCallback<T, ?, ?> getBindCallback() {
        return bindCallback;
    }

    @Override
    public ViewHolder<T, ?> createHolder(Context context, ViewGroup parent) {
        ViewDataBinding viewBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context), layoutId, parent, false);
        return new DataBindingViewHolder<>(viewBinding, this);
    }


    public final static class Builder<T> {

        private DataBindingViewHolderType<T> type = new DataBindingViewHolderType<>();

        Builder(int layout) {
            type.layoutId = layout;
        }

        public Builder<T> setValueVarId(int varId) {
            type.brValue = varId;
            return this;
        }

        public Builder<T> setContextVarId(int varId) {
            type.brContext = varId;
            return this;
        }

        public <CT, VT extends ViewDataBinding> Builder<T>
        onBind(BindCallback<T, CT, VT> callback) {
            type.bindCallback = callback;
            return this;
        }

        public <VT extends ViewDataBinding> Builder<T>
        onBind(SimpleBindCallback<T, VT> callback) {
            return this.<Object, VT>onBind(
                    (holder, binding, data, context) -> callback.onBind(holder, binding, data));
        }

        public DataBindingViewHolderType<T> build() {
            return type;
        }

    }

    public interface SimpleBindCallback<T, VT extends ViewDataBinding> {
        void onBind(DataBindingViewHolder<T, ?> holder, VT binding, T data);
    }

    public interface BindCallback<T, CT, VT extends ViewDataBinding> {
        void onBind(DataBindingViewHolder<T, CT> holder, VT binding, T data, CT context);
    }

}
