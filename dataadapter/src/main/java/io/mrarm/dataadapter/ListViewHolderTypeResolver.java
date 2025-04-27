package io.mrarm.dataadapter;

public interface ListViewHolderTypeResolver<T> {

    ViewHolderType<T> resolveType(int index, T data);

}
