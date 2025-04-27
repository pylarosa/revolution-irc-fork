package io.mrarm.dataadapter;

import android.content.Context;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.Map;

public abstract class ViewHolderType<T> {

    private static int nextId = 0;
    private static final Map<Integer, ViewHolderType> knownTypes = new HashMap<>(); // TODO: Make this have weak keys

    public static ViewHolderType getTypeForId(int id) {
        return knownTypes.get(id);
    }

    private final int id = nextId++;

    public ViewHolderType() {
        knownTypes.put(id, this);
    }

    public final int getId() {
        return id;
    }

    public abstract ViewHolder<T, ?> createHolder(Context context, ViewGroup parent);



    public static <T> ViewHolderType<T> from(final ViewHolderFactory<T> factory) {
        return new ViewHolderType<T>() {
            @Override
            public ViewHolder<T, ?> createHolder(Context context, ViewGroup parent) {
                return factory.createHolder(context, parent);
            }
        };
    }

    public static <T> DataBindingViewHolderType.Builder<T> fromDataBinding(int layoutId) {
        return new DataBindingViewHolderType.Builder<>(layoutId);
    }

    public interface ViewHolderFactory<T> {
        ViewHolder<T, ?> createHolder(Context context, ViewGroup parent);
    }

}
