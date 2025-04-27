package io.mrarm.dataadapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DataAdapter extends RecyclerView.Adapter<ViewHolder> implements DataFragment.Listener {

    private DataFragment fragment;
    private int attachCount = 0;
    private boolean autoBindFragment = false;

    public void setSource(DataFragment fragment, boolean autoBindFragment) {
        if (this.fragment != null)
            fragment.removeListener(this);
        if (this.fragment != null && attachCount > 0 && this.autoBindFragment)
            this.fragment.unbind();
        this.fragment = fragment;
        this.autoBindFragment = autoBindFragment;
        if (attachCount > 0 && autoBindFragment)
            fragment.bind();
        fragment.addListener(this);
        notifyDataSetChanged();
    }

    public void setSource(DataFragment fragment) {
        setSource(fragment, true);
    }

    public DataFragment getSource() {
        return fragment;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        if (attachCount++ == 0) {
            if (fragment != null && autoBindFragment)
                fragment.bind();
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        if (--attachCount == 0) {
            if (fragment != null && autoBindFragment)
                fragment.unbind();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ViewHolderType.getTypeForId(viewType).createHolder(parent.getContext(), parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        //noinspection unchecked
        holder.bind(fragment.getItem(position), fragment.getContext(position));
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.unbind();
    }

    @Override
    public int getItemCount() {
        return fragment.getItemCount();
    }

    @Override
    public int getItemViewType(int position) {
        return fragment.getHolderTypeFor(position).getId();
    }

    @Override
    public void onItemRangeInserted(DataFragment fragment, int index, int count) {
        notifyItemRangeInserted(index, count);
    }

    @Override
    public void onItemRangeRemoved(DataFragment fragment, int index, int count) {
        notifyItemRangeRemoved(index, count);
    }

    @Override
    public void onItemRangeChanged(DataFragment fragment, int index, int count) {
        notifyItemRangeChanged(index, count);
    }

    @Override
    public void onItemRangeMoved(DataFragment fragment, int index, int toIndex, int count) {
        if (count == 1) {
            notifyItemMoved(index, toIndex);
        } else {
            notifyItemRangeRemoved(index, count);
            notifyItemRangeInserted(toIndex, count);
        }
    }

}
