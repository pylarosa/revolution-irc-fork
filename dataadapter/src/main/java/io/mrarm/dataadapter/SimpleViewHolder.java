package io.mrarm.dataadapter;

import android.view.View;

import androidx.annotation.NonNull;

public abstract class SimpleViewHolder<T> extends ViewHolder<T, Object> {

    public SimpleViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    public abstract void bind(T value);

    @Override
    public void bind(T value, Object context) {
        bind(value);
    }

}
