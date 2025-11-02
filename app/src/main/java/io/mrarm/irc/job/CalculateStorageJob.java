package io.mrarm.irc.job;

import android.content.Context;
import android.os.StatFs;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;

import io.mrarm.irc.StorageSettingsAdapter;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.storage.StorageRepository;
import io.mrarm.irc.util.Async;

public class CalculateStorageJob {
    public interface Callback {
        void onConfigSize(long size);

        void onServerLogsEntry(StorageSettingsAdapter.ServerLogsEntry entry);

        void onCompleted();
    }

    public static Future<?> run(Context context, Callback callback) {
        return Async.io(() -> {
            ServerConfigManager serverManager = ServerConfigManager.getInstance(context);
            StorageRepository storageRepo = StorageRepository.getInstance(context);
            File dataDir = new File(context.getApplicationInfo().dataDir);
            StatFs statFs = new StatFs(dataDir.getAbsolutePath());

            long dataBlockSize = getBlockSize(statFs, dataDir);
            long dataSize = 0L;
            File[] dataFiles = dataDir.listFiles();
            if (dataFiles != null) {
                for (File f : dataFiles) {
                    if (f.getName().equals("cache") || f.getName().equals("lib"))
                        continue;
                    dataSize += calculateDirectorySize(f, dataBlockSize);
                }
            }

            long finalDataSize = dataSize;
            Async.ui(() -> callback.onConfigSize(finalDataSize));

            List<File> processedDirs = new ArrayList<>();
            for (ServerConfigData data : serverManager.getServers()) {
                File file = storageRepo.getServerChatLogDir(data.uuid);
                processedDirs.add(file);
                if (!file.exists()) continue;
                long size = calculateDirectorySize(file, getBlockSize(statFs, file));
                if (size == 0L) continue;
                StorageSettingsAdapter.ServerLogsEntry entry = new StorageSettingsAdapter.ServerLogsEntry(data.name, data.uuid, size);
                Async.ui(() -> callback.onServerLogsEntry(entry));
            }

            File[] files = storageRepo.getChatLogDir().listFiles();
            if (files != null) {
                for (File f : files) {
                    if (processedDirs.contains(f)) continue;
                    long size = calculateDirectorySize(f, getBlockSize(statFs, f));
                    if (size == 0L) continue;
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(f.getName());
                    } catch (IllegalArgumentException ignored) {
                    }
                    StorageSettingsAdapter.ServerLogsEntry entry = new StorageSettingsAdapter.ServerLogsEntry(f.getName(), uuid, size);
                    Async.ui(() -> callback.onServerLogsEntry(entry));
                }
            }

            Async.ui(callback::onCompleted);
        });
    }

    private static long getBlockSize(StatFs statFs, File file) {
        statFs.restat(file.getAbsolutePath());
        return statFs.getBlockSizeLong();
    }

    private static long calculateDirectorySize(File file, long blockSize) {
        File[] files = file.listFiles();
        if (files == null) return 0L;
        long ret = blockSize;
        for (File f : files) {
            if (f.isDirectory())
                ret += calculateDirectorySize(f, blockSize);
            else
                ret += (f.length() + blockSize - 1) / blockSize * blockSize;
        }
        return ret;
    }
}
