package io.mrarm.irc.job;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.io.File;
import java.util.UUID;

import io.mrarm.chatlib.android.storage.SQLiteMessageStorageApi;
import io.mrarm.chatlib.irc.ServerConnectionApi;
import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionInfo;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.config.NotificationRuleManager;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.storage.StorageRepository;
import io.mrarm.irc.util.Async;
import io.mrarm.irc.util.StubMessageStorageApi;

/**
 * Performs background cleanup of logs, config files, and cached data.
 * Replaces the legacy AsyncTask-based version.
 */
public class RemoveDataTask {

    public interface OnRemoveDataFinishedListener {
        void onRemoveDataFinished();
    }

    private final Context context;
    private final boolean deleteConfig;
    private final UUID deleteServerLogs;
    private final StorageRepository repository;
    private final OnRemoveDataFinishedListener listener;
    private AlertDialog progressDialog;

    private RemoveDataTask(Context context, boolean deleteConfig, UUID deleteServerLogs,
                           StorageRepository repository, OnRemoveDataFinishedListener listener) {
        this.context = context;
        this.deleteConfig = deleteConfig;
        this.deleteServerLogs = deleteServerLogs;
        this.repository = repository;
        this.listener = listener;
    }

    public static void start(Context context, boolean deleteConfig, UUID deleteServerLogs,
                             StorageRepository repository, OnRemoveDataFinishedListener listener) {
        new RemoveDataTask(context, deleteConfig, deleteServerLogs, repository, listener).execute();
    }

    private void execute() {
        showProgressDialog();
        Async.io(this::performDeletion, () -> {
            Async.ui(() -> {
                new Handler(Looper.getMainLooper()).postDelayed(
                        this::onFinished,
                        300
                );
            });
        });
    }

    private void showProgressDialog() {
        if (context instanceof Activity a && !a.isFinishing() && !a.isDestroyed()) {
            progressDialog = new AlertDialog.Builder(a)
                    .setCancelable(false)
                    .setView(R.layout.dialog_please_wait)
                    .show();

            if (a instanceof ComponentActivity) {
                ((ComponentActivity) a)
                        .getLifecycle()
                        .addObserver(new DefaultLifecycleObserver() {

                            @Override
                            public void onDestroy(@NonNull LifecycleOwner owner) {
                                if (progressDialog != null && progressDialog.isShowing())
                                    progressDialog.dismiss();
                            }
                        });
            }
        }
    }

    private void performDeletion() {

        try {
            Context ctx = context;

            if (deleteConfig) {
                ServerConnectionManager mgr = ServerConnectionManager.getInstance(null);
                if (mgr != null)
                    mgr.disconnectAndRemoveAllConnections(true);
                else
                    //noinspection ResultOfMethodCallIgnored
                    new File(ctx.getFilesDir(),
                            ServerConnectionManager.CONNECTED_SERVERS_FILE_PATH).delete();

                ServerConfigManager.getInstance(ctx).deleteAllServers(true);
                NotificationRuleManager.getUserRules(ctx).clear();
                CommandAliasManager.getInstance(ctx).getUserAliases().clear();
                SettingsHelper.getInstance(ctx).clear();
                repository.closeNotificationCounts();

                File files = ctx.getFilesDir();
                File[] children = files.listFiles();
                if (children != null) {
                    for (File file : children) {
                        if ("cache".equals(file.getName()) || "lib".equals(file.getName()))
                            continue;
                        deleteRecursive(file);
                    }
                }

                repository.ensureNotificationCountsMigrated();
            }

            if (deleteServerLogs != null) {
                deleteChatLogDir(deleteServerLogs);
            } else {
                File[] logFiles = repository.getChatLogDir().listFiles();

                if (logFiles == null)
                    logFiles = new File[0];

                for (File f : logFiles) {
                    try {
                        deleteChatLogDir(UUID.fromString(f.getName()));
                    } catch (IllegalArgumentException ignored) {
                        deleteRecursive(f);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("RemoveDataTask", "Error deleting data", e);
        }
    }

    private void onFinished() {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();

        Toast.makeText(context,
                R.string.pref_storage_clear_all_chat_logs_done,
                Toast.LENGTH_SHORT
        ).show();

        if (listener != null)
            listener.onRemoveDataFinished();
    }

    private void deleteRecursive(File fileOrDir) {
        if (fileOrDir == null || !fileOrDir.exists())
            return;

        if (fileOrDir.isDirectory()) {
            File[] children = fileOrDir.listFiles();
            if (children != null) {
                for (File child : children)
                    deleteRecursive(child);

            }
        }

        //noinspection ResultOfMethodCallIgnored
        fileOrDir.delete();
    }


    private void deleteChatLogDir(UUID uuid) {
        ServerConnectionManager connectionManager = ServerConnectionManager.getInstance(null);
        if (connectionManager != null)
            connectionManager.killDisconnectingConnection(uuid);

        ServerConnectionInfo connection = connectionManager != null
                ? connectionManager.getConnection(uuid)
                : null;

        boolean hadMessageStorage = false;

        if (connection != null && connection.getApiInstance() instanceof ServerConnectionApi api) {
            if (api.getServerConnectionData().getMessageStorageApi() instanceof SQLiteMessageStorageApi) {
                hadMessageStorage = true;
                api.getServerConnectionData().setMessageStorageApi(new StubMessageStorageApi());
                repository.closeMessageStorage(uuid);
            }
            connection.resetMiscStorage();
        }

        repository.closeMessageLogs(uuid);

        File file = repository.getServerChatLogDir(uuid);
        deleteRecursive(file);

        if (hadMessageStorage && connection != null
                && connection.getApiInstance() instanceof ServerConnectionApi api) {
            Async.ui(() -> {
                SQLiteMessageStorageApi reopened = repository.getMessageStorageApi(uuid);
                api.getServerConnectionData().setMessageStorageApi(reopened);
                api.getServerConnectionData().setChannelDataStorage(repository.createChannelDataStorage(uuid));
            });
        }

    }
}
