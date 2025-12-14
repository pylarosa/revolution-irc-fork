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

import io.mrarm.irc.R;
import io.mrarm.irc.ServerConnectionManager;
import io.mrarm.irc.config.CommandAliasManager;
import io.mrarm.irc.config.NotificationRuleManager;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.config.SettingsHelper;
import io.mrarm.irc.storage.MessageStorageRepository;
import io.mrarm.irc.util.Async;

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
    private final MessageStorageRepository roomRepository;
    private final OnRemoveDataFinishedListener listener;

    private AlertDialog progressDialog;

    private RemoveDataTask(Context context, boolean deleteConfig, UUID deleteServerLogs,
                           MessageStorageRepository roomRepository,
                           OnRemoveDataFinishedListener listener) {
        this.context = context;
        this.deleteConfig = deleteConfig;
        this.deleteServerLogs = deleteServerLogs;
        this.roomRepository = roomRepository;
        this.listener = listener;
    }

    public static void start(Context context, boolean deleteConfig, UUID deleteServerLogs, MessageStorageRepository roomRepository,
                             OnRemoveDataFinishedListener listener) {

        new RemoveDataTask(context, deleteConfig, deleteServerLogs, roomRepository, listener)
                .execute();
    }

    private void execute() {
        showProgressDialog();
        Async.io(this::performDeletion, () ->
                Async.ui(() -> new Handler(Looper.getMainLooper()).postDelayed(
                        this::onFinished, 300)
                ));
    }

    private void performDeletion() {

        try {
            if (deleteConfig) {
                clearAllConfigData();
            }

            if (deleteServerLogs != null) {
                roomRepository.deleteLogsForServer(deleteServerLogs);

            } else {
                roomRepository.deleteAllLogs();
            }
        } catch (Exception e) {
            Log.e("RemoveDataTask", "Error deleting data", e);
        }
    }

    private void clearAllConfigData() {
        Context ctx = context;

        // Disconnect and drop connections
        ServerConnectionManager mgr = ServerConnectionManager.getInstance(null);
        if (mgr != null) {
            mgr.disconnectAndRemoveAllConnections(true);
        } else {
            //noinspection ResultOfMethodCallIgnored
            new File(ctx.getFilesDir(),
                    ServerConnectionManager.CONNECTED_SERVERS_FILE_PATH).delete();
        }

        // Clear app config
        ServerConfigManager.getInstance(ctx).deleteAllServers();
        NotificationRuleManager.getUserRules(ctx).clear();
        CommandAliasManager.getInstance(ctx).getUserAliases().clear();
        SettingsHelper.getInstance(ctx).clear();


        // Remove all leftover files EXCEPT cache and lib
        File[] children = ctx.getFilesDir().listFiles();
        if (children != null) {
            for (File f : children) {
                if (!"cache".equals(f.getName()) && !"lib".equals(f.getName())) {
                    deleteRecursive(f);
                }
            }
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
}
