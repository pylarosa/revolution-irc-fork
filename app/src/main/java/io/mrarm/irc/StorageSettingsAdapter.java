package io.mrarm.irc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.StatFs;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.mrarm.irc.config.AppSettings;
import io.mrarm.irc.config.ServerConfigData;
import io.mrarm.irc.config.ServerConfigManager;
import io.mrarm.irc.dialog.MenuBottomSheetDialog;
import io.mrarm.irc.dialog.ServerStorageLimitDialog;
import io.mrarm.irc.dialog.StorageLimitsDialog;
import io.mrarm.irc.job.RemoveDataTask;
import io.mrarm.irc.storage.MessageStatsRepository;
import io.mrarm.irc.storage.MessageStorageRepository;
import io.mrarm.irc.util.Async;
import io.mrarm.irc.util.ColoredTextBuilder;
import io.mrarm.irc.util.StyledAttributesHelper;
import io.mrarm.irc.view.SimpleBarChart;

public class StorageSettingsAdapter extends RecyclerView.Adapter {

    public static final int TYPE_SERVER_LOGS_SUMMARY = 0;
    public static final int TYPE_SERVER_LOGS = 1;
    public static final int TYPE_CONFIGURATION_SUMMARY = 2;

    private List<ServerLogsEntry> mServerLogEntries = new ArrayList<>();
    private long mConfigurationSize = 0L;
    private int mSecondaryTextColor;
    private final MessageStorageRepository roomStorageRepository;

    private List<ServerLogsEntry> mDbServerLogEntries = new ArrayList<>();


    public StorageSettingsAdapter(Context context) {
        roomStorageRepository = MessageStorageRepository.getInstance(context);

        refreshServerLogsFromDb(context);
        refreshConfigurationSize(context);

        mSecondaryTextColor = StyledAttributesHelper.getColor(context, android.R.attr.textColorSecondary, Color.BLACK);
    }

    @SuppressLint("NotifyDataSetChanged")
    void refreshServerLogsFromDb(Context context) {

        Async.io(() -> {
            MessageStatsRepository stats = new MessageStatsRepository(context);

            // Aggregate per server
            List<MessageStatsRepository.ServerUsage> usageList = stats.getUsageForAllServers();
            List<ServerLogsEntry> newEntries = new ArrayList<>();

            for (MessageStatsRepository.ServerUsage usage : usageList) {
                UUID serverId = usage.serverId;
                long size = usage.size != null ? usage.size : 0;

                ServerConfigData server =
                        ServerConfigManager.getInstance(context).findServer(serverId);

                String name = server != null ? server.name : serverId.toString();

                newEntries.add(new ServerLogsEntry(name, serverId, size));
            }

            // push on UI thread
            Async.ui(() -> {
                mDbServerLogEntries.clear();
                mDbServerLogEntries.addAll(newEntries);
                mServerLogEntries.clear();
                mServerLogEntries.addAll(newEntries);

                notifyDataSetChanged();
            });


        });
    }

    void refreshConfigurationSize(Context context) {
        Async.io(() -> {
            long size = ConfigurationSizeCalculator.calculate(context);
            Async.ui(() -> {
                mConfigurationSize = size;
                notifyItemChanged(getItemCount() - 1);
            });
        });
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_SERVER_LOGS_SUMMARY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.settings_storage_chat_logs_summary, parent, false);
            return new ServerLogsSummaryHolder(view);
        } else if (viewType == TYPE_SERVER_LOGS) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.settings_storage_chat_logs_entry, parent, false);
            return new ServerLogsHolder(view);
        } else if (viewType == TYPE_CONFIGURATION_SUMMARY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.settings_storage_configuration_summary, parent, false);
            return new ConfigurationSummaryHolder(view);
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        int type = holder.getItemViewType();
        if (type == TYPE_SERVER_LOGS_SUMMARY) {
            ((ServerLogsSummaryHolder) holder).bind();
        } else if (type == TYPE_SERVER_LOGS) {
            ((ServerLogsHolder) holder).bind(mServerLogEntries.get(position - 1), position - 1);
        } else if (type == TYPE_CONFIGURATION_SUMMARY) {
            ((ConfigurationSummaryHolder) holder).bind();
        }
    }

    @Override
    public int getItemCount() {
        return 1 + mServerLogEntries.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return TYPE_SERVER_LOGS_SUMMARY;
        if (position >= 1 && position < 1 + mServerLogEntries.size())
            return TYPE_SERVER_LOGS;
        if (position == 1 + mServerLogEntries.size())
            return TYPE_CONFIGURATION_SUMMARY;
        return -1;
    }

    // Refactor to fit new size calculations
    public class ServerLogsSummaryHolder extends RecyclerView.ViewHolder {

        private SimpleBarChart mChart;
        private TextView mTotal;

        public ServerLogsSummaryHolder(View view) {
            super(view);
            mChart = view.findViewById(R.id.chart);
            mTotal = view.findViewById(R.id.total_value);
            view.findViewById(R.id.set_limits).setOnClickListener((View v) -> {
                StorageLimitsDialog dialog = new StorageLimitsDialog(v.getContext());
                dialog.setOnDismissListener((DialogInterface di) -> {
                    Async.io(() -> {
                        long globalLimit = AppSettings.getStorageLimitGlobal();
                        MessageStorageRepository.CleanupResult cleanupResult =  roomStorageRepository.enforceGlobalLimit(globalLimit);
                        Log.d("Enforce Global cleaning hit: ", cleanupResult.toString());

                    }, () -> refreshServerLogsFromDb(v.getContext()));
                });
                dialog.show();
            });
            view.findViewById(R.id.clear_chat_logs).setOnClickListener((View v) -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.pref_storage_clear_all_chat_logs)
                        .setMessage(R.string.pref_storage_clear_all_chat_logs_confirm)
                        .setPositiveButton(R.string.action_delete, (DialogInterface di, int i) -> {
                            RemoveDataTask.start(
                                    v.getContext(),
                                    false,
                                    null,
                                    roomStorageRepository,
                                    () -> refreshServerLogsFromDb(v.getContext()));
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            });
        }

        public void bind() {
            int count = Math.min(4, mServerLogEntries.size());
            long total = 0;
            if (count > 0) {
                float[] values = new float[count];
                int[] colors = new int[count];
                colors[0] = mChart.getResources().getColor(R.color.storageSettingsChartFirst);
                if (count > 1)
                    colors[1] = mChart.getResources().getColor(R.color.storageSettingsChartSecond);
                if (count > 2)
                    colors[2] = mChart.getResources().getColor(R.color.storageSettingsChartThird);
                if (count > 3)
                    colors[3] = mChart.getResources().getColor(R.color.storageSettingsChartOthers);
                for (int i = mServerLogEntries.size() - 1; i >= 0; --i) {
                    long val = mServerLogEntries.get(i).size;
                    total += val;
                    values[Math.min(i, count - 1)] += (float) (val / 1024.0 / 1024.0);
                }
                mChart.setData(values, colors);
                mChart.setVisibility(View.VISIBLE);
                itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), itemView.getResources().getDimensionPixelSize(R.dimen.storage_chat_logs_summary_padding_bottom));
            } else {
                mChart.setVisibility(View.GONE);
                itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), itemView.getResources().getDimensionPixelSize(R.dimen.storage_chat_logs_summary_padding_bottom_no_items));
            }
            mTotal.setText(formatFileSize(total));
        }

    }

    public static class ServerLogsEntry {
        String name;
        UUID uuid;
        long size;

        public ServerLogsEntry(String name, UUID uuid, long size) {
            this.name = name;
            this.uuid = uuid;
            this.size = size;
        }
    }

    public class ServerLogsHolder extends RecyclerView.ViewHolder {

        private TextView mText;

        public ServerLogsHolder(View view) {
            super(view);
            mText = view.findViewById(R.id.text);
            view.setOnClickListener((View v) -> showActionsMenu());
            view.setOnLongClickListener((View v) -> {
                showActionsMenu();
                return true;
            });
        }

        private void showActionsMenu() {
            MenuBottomSheetDialog menu = new MenuBottomSheetDialog(itemView.getContext());
            UUID serverId = (UUID) mText.getTag();
            if (serverId != null) {
                ServerConfigData server = ServerConfigManager.getInstance(itemView.getContext()).findServer(serverId);
                if (server != null && server.storageLimit == 0L) {
                    menu.addItem(R.string.pref_storage_set_server_limit, R.drawable.ic_storage, (MenuBottomSheetDialog.Item it) -> {
                        ServerStorageLimitDialog dialog = new ServerStorageLimitDialog(itemView.getContext(), server);
                        dialog.show();
                        return true;
                    });
                } else if (server != null) {
                    ColoredTextBuilder builder = new ColoredTextBuilder();
                    builder.append(mText.getContext().getString(R.string.pref_storage_change_server_limit));
                    builder.setSpan(new ForegroundColorSpan(mSecondaryTextColor));
                    builder.append(" (");
                    if (server.storageLimit == -1L)
                        builder.append(itemView.getContext().getString(R.string.pref_storage_no_limit));
                    else
                        builder.append((server.storageLimit / 1024L / 1024L) + " MB");
                    builder.append(")");
                    menu.addItem(builder.getSpannable(), R.drawable.ic_storage, (MenuBottomSheetDialog.Item it) -> {
                        ServerStorageLimitDialog dialog = new ServerStorageLimitDialog(itemView.getContext(), server);

                        dialog.setOnDismissListener(di -> {
                            Async.io(() -> {
                                long limit = server.storageLimit;
                                if (limit > 0) {
                                    MessageStorageRepository.CleanupResult cleanupResult = roomStorageRepository.enforceServerLimit(server.uuid, limit);
                                    Log.d("Cleanup performed: ", cleanupResult.toString());
                                }
                            }, () -> refreshServerLogsFromDb(itemView.getContext()));
                        });

                        dialog.show();
                        return true;
                    });
                    menu.addItem(R.string.pref_storage_remove_server_limit, 0, (MenuBottomSheetDialog.Item it) -> {
                        server.storageLimit = 0L;
                        try {
                            ServerConfigManager.getInstance(itemView.getContext()).saveServer(server);
                        } catch (IOException ignored) {
                        }
                        return true;
                    });
                }
            }

            menu.addItem(R.string.pref_storage_clear_server_chat_logs, R.drawable.ic_delete, (MenuBottomSheetDialog.Item it) -> {
                RemoveDataTask.start(
                        itemView.getContext(),
                        false,
                        serverId,
                        roomStorageRepository,
                        () -> refreshServerLogsFromDb(itemView.getContext()));
                return true;
            });
            menu.show();
        }

        public void bind(ServerLogsEntry entry, int pos) {
            int colorId = R.color.storageSettingsChartOthers;
            if (entry.size > 0L) {
                if (pos == 0)
                    colorId = R.color.storageSettingsChartFirst;
                else if (pos == 1)
                    colorId = R.color.storageSettingsChartSecond;
                else if (pos == 2)
                    colorId = R.color.storageSettingsChartThird;
            }
            ColoredTextBuilder builder = new ColoredTextBuilder();
            builder.append(entry.name, new ForegroundColorSpan(mText.getResources().getColor(colorId)));
            builder.append("  ");
            builder.append(formatFileSize(entry.size));

            long dbSize = findDbSizeForServer(entry.uuid);
            if (dbSize > 0) {
                builder.append("  (db: ");
                builder.append(formatFileSize(dbSize));
                builder.append(")");
            }

            mText.setText(builder.getSpannable());
            mText.setTag(entry.uuid);
        }

    }

    private long findDbSizeForServer(UUID uuid) {
        for (ServerLogsEntry e : mDbServerLogEntries) {
            if (e.uuid != null && e.uuid.equals(uuid))
                return e.size;
        }
        return -1;
    }


    public class ConfigurationSummaryHolder extends RecyclerView.ViewHolder {

        private TextView mTotal;

        public ConfigurationSummaryHolder(View view) {
            super(view);
            mTotal = view.findViewById(R.id.total_value);
            view.findViewById(R.id.reset).setOnClickListener((View v) -> {
                new AlertDialog.Builder(v.getContext())
                        .setTitle(R.string.pref_storage_reset_configuration)
                        .setMessage(R.string.pref_storage_reset_configuration_confirm)
                        .setPositiveButton(R.string.action_reset, (DialogInterface di, int i) -> {
                            RemoveDataTask.start(
                                    v.getContext(),
                                    true,
                                    null,
                                    roomStorageRepository,
                                    () -> {
                                        refreshServerLogsFromDb(v.getContext());
                                        refreshConfigurationSize(v.getContext());
                                    });
                        })
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
            });
        }

        public void bind() {
            mTotal.setText(formatFileSize(mConfigurationSize));
        }

    }

    public final class ConfigurationSizeCalculator {

        private ConfigurationSizeCalculator() {
        }

        public static long calculate(Context context) {
            File dataDir = new File(context.getApplicationInfo().dataDir);
            StatFs statFs = new StatFs(dataDir.getAbsolutePath());
            long blockSize = statFs.getBlockSizeLong();

            long total = 0L;
            File[] files = dataDir.listFiles();
            if (files == null) return 0L;

            for (File f : files) {
                if ("cache".equals(f.getName()) || "lib".equals(f.getName()))
                    continue;
                total += calculateDirectorySize(f, blockSize);
            }
            return total;
        }

        private static long calculateDirectorySize(File file, long blockSize) {
            File[] files = file.listFiles();
            if (files == null) return 0L;

            long size = blockSize;
            for (File f : files) {
                if (f.isDirectory())
                    size += calculateDirectorySize(f, blockSize);
                else
                    size += (f.length() + blockSize - 1) / blockSize * blockSize;
            }
            return size;
        }
    }

    private static String formatFileSize(long size) {
        if (size / 1024L >= 128)
            return String.format("%.2f MB", size / 1024.0 / 1024.0);
        else
            return String.format("%.2f KB", size / 1024.0);
    }

}
