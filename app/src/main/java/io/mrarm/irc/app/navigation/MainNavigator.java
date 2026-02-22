package io.mrarm.irc.app.navigation;

import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import io.mrarm.irc.ServerListFragment;
import io.mrarm.irc.chat.ChatFragment;
import io.mrarm.irc.connection.ServerConnectionManager;
import io.mrarm.irc.connection.ServerConnectionSession;
import io.mrarm.irc.drawer.DrawerHelper;

public class MainNavigator {
    public interface Host {

        void dismissFragmentDialog();

        void setChannelInfoDrawerVisible(boolean visible);

    }
    public interface ServerResolver {

        ServerConnectionSession resolve(String uuid);
    }
    private final FragmentManager fragmentManager;

    private final int containerId;
    private final DrawerHelper drawerHelper;
    private final Host host;
    public MainNavigator(FragmentManager fragmentManager, int containerId, DrawerHelper drawerHelper, Host host) {
        this.fragmentManager = fragmentManager;
        this.containerId = containerId;
        this.drawerHelper = drawerHelper;
        this.host = host;
    }

    private Fragment getCurrentFragment() {
        return fragmentManager.findFragmentById(containerId);
    }

    public ChatFragment openServer(ServerConnectionSession server,
                                   String channel,
                                   String messageId) {

        host.dismissFragmentDialog();
        host.setChannelInfoDrawerVisible(false);
        ChatFragment fragment;
        Fragment current = getCurrentFragment();

        if (current instanceof ChatFragment &&
                ((ChatFragment) current).getConnectionInfo() == server) {

            fragment = (ChatFragment) current;
            fragment.setCurrentChannel(channel, messageId);
            host.setChannelInfoDrawerVisible(false);

        } else {
            fragment = ChatFragment.newInstance(server, channel, messageId);
            fragmentManager.beginTransaction()
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    .replace(containerId, fragment)
                    .commit();
        }

        drawerHelper.setSelectedChannel(server, channel);
        return fragment;
    }

    public void openServer(ServerConnectionSession server, String channel) {
        openServer(server, channel, null);
    }

    public void openManageServers() {
        host.dismissFragmentDialog();
        host.setChannelInfoDrawerVisible(false);

        fragmentManager.beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(containerId, ServerListFragment.newInstance())
                .commit();

        drawerHelper.setSelectedMenuItem(drawerHelper.getManageServersItem());
    }

    public void openManageServersSelected() {
        openManageServers();
    }

    public boolean handleBackPressed() {
        Fragment current = getCurrentFragment();

        if (current instanceof ChatFragment) {
            openManageServers();
            return true;
        }

        return false;
    }

    public ChatFragment handleIntent(Intent intent,
                                     ServerResolver resolver,
                                     Fragment currentFragment,
                                     String argServerUuid,
                                     String argChannel,
                                     String argMessageId,
                                     String argManageServers) {

        String serverUUID = intent.getStringExtra(argServerUuid);
        ServerConnectionSession server = null;

        if (serverUUID != null) {
            server = resolver.resolve(serverUUID);
        }

        if (server != null) {

            return openServer(
                    server,
                    intent.getStringExtra(argChannel),
                    intent.getStringExtra(argMessageId)
            );

        } else if (intent.getBooleanExtra(argManageServers, false)
                || currentFragment == null) {

            openManageServers();
        }

        return null;
    }

    public void onChannelSelected(ServerConnectionSession server, String channel) {
        Fragment current = getCurrentFragment();

        if (current instanceof ChatFragment &&
                ((ChatFragment) current).getConnectionInfo() == server) {

            ((ChatFragment) current).setCurrentChannel(channel, null);

        } else {

            openServer(server, channel);
        }
    }

    public void ensureValidConnection(ServerConnectionManager manager) {
        Fragment current = getCurrentFragment();

        if (current instanceof ChatFragment) {
            ServerConnectionSession session =
                    ((ChatFragment) current).getConnectionInfo();

            if (!manager.hasConnection(session.getUUID())) {
                openManageServers();
            }
        }
    }
}
