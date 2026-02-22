package io.mrarm.irc.dialog;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.R;
import io.mrarm.irc.app.navigation.MainNavigator;
import io.mrarm.irc.connection.ServerConnectionSession;
import io.mrarm.irc.view.ListSearchView;

public class UserSearchDialog extends SearchDialog {

    private final ServerConnectionSession mConnection;
    private final ListSearchView.SimpleSuggestionsAdapter mAdapter;
    private final MainNavigator navigator;

    public UserSearchDialog(@NonNull Context context, ServerConnectionSession connection, MainNavigator navigator) {
        super(context);
        mConnection = connection;
        this.navigator = navigator;
        setQueryHint(context.getString(R.string.action_message_user));
        mAdapter = new ListSearchView.SimpleSuggestionsAdapter();
        mAdapter.setItemClickListener((int index, CharSequence value) -> {
            onQueryTextSubmit(value.toString());
        });
        setSuggestionsAdapter(mAdapter);
    }

    @Override
    public void onQueryTextSubmit(String query) {
        List<String> channels = new ArrayList<>();
        channels.add(query);
        mConnection.getApiInstance().joinChannels(channels, (Void v) -> {
            getOwnerActivity().runOnUiThread(() -> navigator.openServer(mConnection, query));
        }, null);
        cancel();
    }

    @Override
    public void onQueryTextChange(String newText) {
        if (newText.length() < 2) {
            mAdapter.setItems(null);
            return;
        }
        mConnection.getApiInstance().getUserInfoApi().findUsers(newText, (List<String> users) -> {
            List<CharSequence> suggestions = new ArrayList<>();
            for (String sug : users) {
                suggestions.add(sug);
            }
            mAdapter.setItems(suggestions);
        }, null);
    }

}
