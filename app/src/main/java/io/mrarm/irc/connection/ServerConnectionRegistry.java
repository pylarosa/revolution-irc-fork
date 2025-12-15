package io.mrarm.irc.connection;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.config.SettingsHelper;

/**
 * Persisted registry of server connections managed by the application.
 * <p>
 * Acts as a lightweight inventory used to:  <br>
 * - restore managed connections on app restart <br>
 * - track which server connections should exist <br>
 * - be cleared when server configurations are removed
 * <p>
 * This registry mirrors runtime state but is NOT authoritative. <br>
 * Safe to delete; contents are derived from live connections.
 */

public class ServerConnectionRegistry {
    private static final String FILE_NAME = "connected_servers.json";
    private final File file;

    public ServerConnectionRegistry(Context context) {
        this.file = new File(context.getFilesDir(), FILE_NAME);
    }

    public static class ConnectedServerEntry {
        public UUID uuid;
        public List<String> channels;
    }

    private static class ConnectedServers {
        public List<ConnectedServerEntry> servers;
    }

    public List<ConnectedServerEntry> loadConnectedServers() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            ConnectedServers data = SettingsHelper.getGson()
                    .fromJson(reader, ConnectedServers.class);
            reader.close();

            if (data == null || data.servers == null)
                return new ArrayList<>();

            return data.servers;

        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void saveConnectedServers(List<ConnectedServerEntry> entries) {
        ConnectedServers data = new ConnectedServers();
        data.servers = entries;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            SettingsHelper.getGson().toJson(data, writer);
            writer.close();

        } catch (Exception ignored) {
        }
    }

    public void clearConnectedServersRegistry() {
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

}
