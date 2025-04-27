package io.mrarm.dataadapter;

public interface ViewHolderTypeResolver<T> {

    ViewHolderType<T> resolveType(T data);

}
