package io.mrarm.irc;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SortedList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.chatlib.dto.ChannelList;
import io.mrarm.irc.util.Async;
import io.mrarm.irc.view.ProgressBar;
import io.mrarm.irc.view.RecyclerViewScrollbar;

public class ChannelListActivity extends ThemedActivity {

    public static final String ARG_SERVER_UUID = "server_uuid";

    public static final int SORT_UNSORTED = 0;
    public static final int SORT_NAME = 1;
    public static final int SORT_MEMBER_COUNT = 2;

    private ServerConnectionInfo mConnection;
    private View mMainAppBar;
    private View mSearchAppBar;
    private SearchView mSearchView;
    private RecyclerView mList;
    private ListAdapter mListAdapter;
    private ProgressBar mProgressBar;

    private String mFilterQuery;
    private int mSortMode = SORT_NAME;

    private final Object mLock = new Object();

    private SortedList<ChannelList.Entry> mSortedEntries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_channel_list);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Toolbar searchToolbar = findViewById(R.id.search_toolbar);
        searchToolbar.setNavigationOnClickListener(v -> setSearchMode(false));

        mMainAppBar = findViewById(R.id.appbar);
        mSearchAppBar = findViewById(R.id.search_appbar);
        mSearchView = findViewById(R.id.search_view);
        mProgressBar = findViewById(R.id.progress);
        mList = findViewById(R.id.list);

        UUID serverUUID = UUID.fromString(getIntent().getStringExtra(ARG_SERVER_UUID));
        mConnection = ServerConnectionManager.getInstance(this).getConnection(serverUUID);

        RecyclerView recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mListAdapter = new ListAdapter();
        recyclerView.setAdapter(mListAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        setupSortedList();

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mFilterQuery = newText.toLowerCase();
                refreshFilteredEntries();
                return true;
            }
        });

        showProgress(true);
        mConnection.getApiInstance().listChannels(list -> {
            Log.d("ChannelList", "Received full list of " + list.getEntries().size() + " channels");
            Async.io(() -> addChannels(list.getEntries()), () -> showProgress(false));
        }, entry -> {
            // Called per new entry during live fetching
            if (entry != null) {
                mList.post(() -> {
                    mSortedEntries.beginBatchedUpdates();
                    if (filterEntry(entry, mFilterQuery)) {
                        mSortedEntries.add(entry);
                    }
                    mSortedEntries.endBatchedUpdates();
                });
            }
        }, null);
    }

    private void setupSortedList() {
        mSortedEntries = new SortedList<>(ChannelList.Entry.class, new SortedList.Callback<ChannelList.Entry>() {
            @Override
            public int compare(ChannelList.Entry a, ChannelList.Entry b) {
                if (mSortMode == SORT_MEMBER_COUNT)
                    return Integer.compare(b.getMemberCount(), a.getMemberCount());
                if (mSortMode == SORT_NAME)
                    return a.getChannel().compareToIgnoreCase(b.getChannel());
                return 0; // Unsorted: preserve insertion order
            }

            @Override
            public void onInserted(int position, int count) {
                Log.d("ChannelList", "Inserted " + count + " item(s) at " + position);
                mListAdapter.notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                Log.d("ChannelList", "Removed " + count + " item(s) at " + position);
                mListAdapter.notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int from, int to) {
                mListAdapter.notifyItemMoved(from, to);
            }

            @Override
            public void onChanged(int position, int count) {
                mListAdapter.notifyItemRangeChanged(position, count);
            }

            @Override
            public boolean areContentsTheSame(ChannelList.Entry oldItem, ChannelList.Entry newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areItemsTheSame(ChannelList.Entry oldItem, ChannelList.Entry newItem) {
                return oldItem.getChannel().equals(newItem.getChannel());
            }
        });
    }


    private static boolean filterEntry(ChannelList.Entry entry, String query) {
        return query == null || query.isEmpty() ||
                entry.getChannel().toLowerCase().contains(query);
    }


    /**
     * Called by Async background job once the full list arrives
     */
    private void addChannels(List<ChannelList.Entry> entries) {
        runOnUiThread(() -> mList.post(() -> {
            mSortedEntries.beginBatchedUpdates();
            for (ChannelList.Entry e : entries) {
                if (filterEntry(e, mFilterQuery))
                    mSortedEntries.add(e);
            }
            mSortedEntries.endBatchedUpdates();
        }));
    }

    /**
     * Re-applies filter on already-fetched entries (search text changed)
     */
    private void refreshFilteredEntries() {
        showProgress(true);
        Async.io(() -> {
            List<ChannelList.Entry> all = new ArrayList<>();
            for (int i = 0; i < mSortedEntries.size(); i++) {
                all.add(mSortedEntries.get(i));
            }
            return all;
        }, list -> {
            mList.post(() -> {
                mSortedEntries.beginBatchedUpdates();
                mSortedEntries.clear();
                for (ChannelList.Entry e : list)
                    if (filterEntry(e, mFilterQuery))
                        mSortedEntries.add(e);
                mSortedEntries.endBatchedUpdates();
            });
            showProgress(false);
        });
    }

    private void showProgress(boolean show) {
        if (mProgressBar != null)
            mProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_channel_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.hideSoftInputFromWindow(getWindow().getDecorView().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            finish();
            return true;
        } else if (id == R.id.action_search) {
            setSearchMode(true);
            mSearchView.setIconified(false);
            return true;
        } else if (id == R.id.action_sort_none || id == R.id.action_sort_name || id == R.id.action_sort_member_count) {
            if (id == R.id.action_sort_name)
                mSortMode = SORT_NAME;
            else if (id == R.id.action_sort_member_count)
                mSortMode = SORT_MEMBER_COUNT;
            else
                mSortMode = SORT_UNSORTED;

            Log.d("ChannelList", "Sort mode changed → " + mSortMode);

            // 🛠️ Safe rebuild
            List<ChannelList.Entry> temp = new ArrayList<>();
            for (int i = 0; i < mSortedEntries.size(); i++)
                temp.add(mSortedEntries.get(i));

            // Temporarily detach adapter to avoid layout inconsistency
            mList.setAdapter(null);

            // Recreate the sorted list with new comparator
            setupSortedList();

            // Reattach adapter to new data source
            mList.setAdapter(mListAdapter);

            // Now repopulate safely
            mList.post(() -> {
                mSortedEntries.beginBatchedUpdates();
                for (ChannelList.Entry e : temp)
                    if (filterEntry(e, mFilterQuery))
                        mSortedEntries.add(e);
                mSortedEntries.endBatchedUpdates();
            });

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mSearchAppBar.getVisibility() == View.VISIBLE) {
            setSearchMode(false);
            return;
        }
        super.onBackPressed();
    }

    public void setSearchMode(boolean searchMode) {
        mMainAppBar.setVisibility(searchMode ? View.GONE : View.VISIBLE);
        mSearchAppBar.setVisibility(searchMode ? View.VISIBLE : View.GONE);

        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(getResources().getColor(
                    searchMode ? R.color.searchColorPrimaryDark : R.color.colorPrimaryDark));
        }
        View decorView = getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 23) {
            if (searchMode)
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            else
                decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
        if (!searchMode) {
            mFilterQuery = null;
            refreshFilteredEntries();
        }
    }

    // ------------------------------------------------------------
    // Adapter + ViewHolder
    // ------------------------------------------------------------
    public class ListAdapter extends RecyclerView.Adapter<ListEntry>
            implements RecyclerViewScrollbar.LetterAdapter {

        @Override
        public ListEntry onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.channel_list_item, parent, false);
            return new ListEntry(view);
        }

        @Override
        public void onBindViewHolder(ListEntry holder, int position) {
            holder.bind(mSortedEntries.get(position));
        }

        @Override
        public String getLetterFor(int position) {
            if (mSortMode != SORT_NAME) return null;
            String channel = mSortedEntries.get(position).getChannel();
            return channel.length() >= 2 ? channel.substring(1, 2).toUpperCase() : "?";
        }

        @Override
        public int getItemCount() {
            return mSortedEntries.size();
        }
    }

    public class ListEntry extends RecyclerView.ViewHolder {

        private final TextView mName;
        private final TextView mTopic;

        public ListEntry(View itemView) {
            super(itemView);
            mName = itemView.findViewById(R.id.name);
            mTopic = itemView.findViewById(R.id.topic);
            itemView.setOnClickListener(view -> {
                List<String> channels = new ArrayList<>();
                channels.add((String) mName.getTag());
                mConnection.getApiInstance().joinChannels(channels, v -> runOnUiThread(() -> {
                    finish();
                    startActivity(MainActivity.getLaunchIntent(ChannelListActivity.this,
                            mConnection, channels.get(0)));
                }), null);
            });
        }

        public void bind(ChannelList.Entry entry) {
            mName.setText(mName.getResources().getQuantityString(
                    R.plurals.channel_list_title_with_member_count, entry.getMemberCount(),
                    entry.getChannel(), entry.getMemberCount()));
            mName.setTag(entry.getChannel());
            mTopic.setText(entry.getTopic().trim());
            mTopic.setVisibility(mTopic.getText().length() > 0 ? View.VISIBLE : View.GONE);
        }
    }
}

