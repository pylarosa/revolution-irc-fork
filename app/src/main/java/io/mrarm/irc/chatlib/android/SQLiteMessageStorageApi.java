package io.mrarm.irc.chatlib.android;

import android.os.Handler;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Future;

import io.mrarm.irc.chatlib.ResponseCallback;
import io.mrarm.irc.chatlib.ResponseErrorCallback;
import io.mrarm.irc.chatlib.dto.MessageFilterOptions;
import io.mrarm.irc.chatlib.dto.MessageId;
import io.mrarm.irc.chatlib.dto.MessageInfo;
import io.mrarm.irc.chatlib.dto.MessageList;
import io.mrarm.irc.chatlib.dto.MessageListAfterIdentifier;
import io.mrarm.irc.chatlib.message.MessageListener;
import io.mrarm.irc.chatlib.message.WritableMessageStorageApi;
import io.mrarm.irc.chatlib.util.SimpleRequestExecutor;

public class SQLiteMessageStorageApi implements WritableMessageStorageApi {

    private static final MyMessageIdParser MESSAGE_ID_PARSER = new MyMessageIdParser();

    private static final SimpleDateFormat fileNameFormat = new SimpleDateFormat("'messages-'yyyy-MM-dd'.db'", Locale.getDefault());

    private final Handler handler = new Handler();
    private final SimpleRequestExecutor executor = new SimpleRequestExecutor();
    private final List<MessageListener> globalListeners = new ArrayList<>();
    private final Map<String, List<MessageListener>> listeners = new HashMap<>();
    final Map<Long, SQLiteMessageStorageFile> files = new HashMap<>();
    private final SortedSet<Long> availableFilesAsc = new TreeSet<>();
    private final SortedSet<Long> availableFilesDesc = new TreeSet<>(Collections.reverseOrder());
    private final File directory;
    private SQLiteMessageStorageFile currentFile;

    public SQLiteMessageStorageApi(File directory) {
        this.directory = directory;
        open();
    }

    Handler getHandler() {
        return handler;
    }

    private long getDateIdentifier(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.DATE) + c.get(Calendar.MONTH) * 32 + c.get(Calendar.YEAR) * 32 * 12;
    }

    private File getFilePathFor(long dateId) {
        Calendar c = Calendar.getInstance();
        c.set((int) (dateId / 32 / 12), (int) ((dateId / 32) % 12), (int) (dateId % 32));
        return new File(directory, fileNameFormat.format(c.getTime()));
    }

    private long getDateIdFromFileName(String fileName) throws ParseException {
        return getDateIdentifier(fileNameFormat.parse(fileName));
    }

    private SQLiteMessageStorageFile openFileFor(long dateId, boolean readOnly) {
        synchronized (files) {
            SQLiteMessageStorageFile file = files.get(dateId);
            if (file != null && file.addReference())
                return file;
            if (!readOnly) {
                availableFilesAsc.add(dateId);
                availableFilesDesc.add(dateId);
            }
            file = new SQLiteMessageStorageFile(this, dateId, getFilePathFor(dateId), readOnly);
            file.addReference();
            files.put(dateId, file);
            return file;
        }
    }

    private SQLiteMessageStorageFile openFileFor(Date date, boolean readOnly) {
        return openFileFor(getDateIdentifier(date), readOnly);
    }

    public void open() {
        synchronized (files) {
            directory.mkdirs();
            availableFilesAsc.clear();
            availableFilesDesc.clear();
            for (File child : directory.listFiles()) {
                if (child.isFile()) {
                    try {
                        long dateId = getDateIdFromFileName(child.getName());
                        availableFilesAsc.add(dateId);
                        availableFilesDesc.add(dateId);
                    } catch (ParseException ignored) {
                    }
                }
            }
        }
    }

    public void close() {
        synchronized (files) {
            for (SQLiteMessageStorageFile file : files.values()) {
                file.close(false);
            }
            files.clear();
        }
    }

    @Override
    public Future<Void> addMessage(String channel, MessageInfo messageInfo, ResponseCallback<Void> callback, ResponseErrorCallback errorCallback) {
        return executor.queue(() -> {
            Date d = new Date();
            SQLiteMessageStorageFile file = openFileFor(d, false);
            long msgId = file.addMessage(channel, messageInfo);
            MessageId mgsIdObj = new MyMessageId(getDateIdentifier(d), (int) msgId);
            file.removeReference();
            synchronized (listeners) {
                for (MessageListener listener : globalListeners)
                    listener.onMessage(channel, messageInfo, mgsIdObj);
                if (listeners.containsKey(channel)) {
                    for (MessageListener listener : listeners.get(channel))
                        listener.onMessage(channel, messageInfo, mgsIdObj);
                }
            }
            return null;
        }, callback, errorCallback);
    }

    private MessageList getMessagesImpl(String channel, int count, MessageFilterOptions options, MessageListAfterIdentifier after) {
        MyMessageListOlderIdentifier a = (MyMessageListOlderIdentifier) after;
        boolean isNewer = a instanceof MyMessageListNewerIdentifier;
        MyMessageListOlderIdentifier otherId = null;
        if (a != null) {
            if (isNewer)
                otherId = new MyMessageListOlderIdentifier(a.fileDateId, a.afterId, a.offset);
            else
                otherId = new MyMessageListNewerIdentifier(a.fileDateId, a.afterId, a.offset);
        }
        long fileDateId = (a == null ? getDateIdentifier(new Date()) : a.fileDateId);
        SQLiteMessageStorageFile file = openFileFor(fileDateId, true);
        MessageQueryResult result = file.getMessages(channel, (a == null ? -1 : a.afterId), (a == null ? 0 : a.offset), count, isNewer, options);
        file.removeReference();
        List<MessageInfo> ret = new ArrayList<>();
        List<MessageId> retIds = new ArrayList<>();
        if (result != null) {
            int afterId = result.getAfterId();
            if (result.getMessages().size() == count) {
                if (isNewer)
                    return new MessageList(result.getMessages(), result.getMessageIds(), new MyMessageListNewerIdentifier(fileDateId, afterId, 0), otherId);
                else
                    return new MessageList(result.getMessages(), result.getMessageIds(), otherId, afterId == -1 ? null : new MyMessageListOlderIdentifier(fileDateId, afterId, 0));
            }
            ret.addAll(result.getMessages());
            retIds.addAll(result.getMessageIds());
        }

        for (long i : (isNewer ? availableFilesAsc.tailSet(fileDateId + 1) : availableFilesDesc.tailSet(fileDateId - 1))) {
            file = openFileFor(i, true);
            result = file.getMessages(channel, -1, 0, count - ret.size(), isNewer, options);
            file.removeReference();
            if (result != null) {
                if (isNewer) {
                    ret.addAll(result.getMessages());
                    retIds.addAll(result.getMessageIds());
                } else {
                    ret.addAll(0, result.getMessages());
                    retIds.addAll(0, result.getMessageIds());
                }
                int afterId = result.getAfterId();
                if (ret.size() == count) {
                    if (isNewer)
                        return new MessageList(ret, retIds, afterId == -1 ? null : new MyMessageListNewerIdentifier(i, afterId, 0), otherId);
                    else
                        return new MessageList(ret, retIds, otherId, afterId == -1 ? null : new MyMessageListOlderIdentifier(i, afterId, 0));
                }
            }
        }

        if (isNewer)
            return new MessageList(ret, retIds, null, otherId);
        else
            return new MessageList(ret, retIds, otherId, null);
    }

    @Override
    public Future<MessageList> getMessages(String channel, int count, MessageFilterOptions options, MessageListAfterIdentifier after, ResponseCallback<MessageList> callback, ResponseErrorCallback errorCallback) {
        return executor.queue(() -> getMessagesImpl(channel, count, options, after), callback, errorCallback);
    }

    @Override
    public Future<MessageList> getMessagesNear(String s, MessageId messageId, MessageFilterOptions filter, ResponseCallback<MessageList> callback, ResponseErrorCallback errorCallback) {
        return executor.queue(() -> {
            if (!(messageId instanceof MyMessageId))
                throw new RuntimeException("Invalid message id type");
            MyMessageId m = (MyMessageId) messageId;
            MessageList older = getMessagesImpl(s, 50, filter, new MyMessageListOlderIdentifier(m.fileDateId, m.id, 0));
            MessageList newer = getMessagesImpl(s, 50, filter, new MyMessageListNewerIdentifier(m.fileDateId, m.id - 1 /* include the current message */, 0));
            List<MessageInfo> ret = older.getMessages(); // we can mutate it just fine, as we control the object
            List<MessageId> retIds = older.getMessageIds();
            ret.addAll(newer.getMessages());
            retIds.addAll(newer.getMessageIds());
            return new MessageList(ret, retIds, newer.getNewer(), older.getOlder());
        }, callback, errorCallback);
    }

    @Override
    public Future<Void> deleteMessages(String channel, List<MessageId> list, ResponseCallback<Void> callback, ResponseErrorCallback errorCallback) {
        return executor.queue(() -> {
            long previousFileId = -1;
            long previousFirstRowId = -1;
            long previousLastRowId = -1;
            for (MessageId messageId : list) {
                if (!(messageId instanceof MyMessageId))
                    throw new RuntimeException("Invalid message id type");
                MyMessageId mmid = (MyMessageId) messageId;
                if (mmid.fileDateId == previousFileId && previousFirstRowId == mmid.id + 1) {
                    previousFirstRowId = mmid.id;
                } else if (mmid.fileDateId == previousFileId && previousLastRowId == mmid.id - 1) {
                    previousLastRowId = mmid.id;
                } else {
                    if (previousFileId != -1) {
                        SQLiteMessageStorageFile file = openFileFor(previousFileId, true);
                        file.removeMessageRange(channel, previousFirstRowId, previousLastRowId);
                        file.removeReference();
                    }
                    previousFileId = mmid.fileDateId;
                    previousFirstRowId = previousLastRowId = mmid.id;
                }
            }
            if (previousFileId != -1) {
                SQLiteMessageStorageFile file = openFileFor(previousFileId, true);
                file.removeMessageRange(channel, previousFirstRowId, previousLastRowId);
                file.removeReference();
            }
            return null;
        }, callback, errorCallback);
    }

    @Override
    public Future<Void> subscribeChannelMessages(String channel, MessageListener messageListener, ResponseCallback<Void> callback, ResponseErrorCallback errorCallback) {
        synchronized (listeners) {
            if (channel != null) {
                if (!listeners.containsKey(channel))
                    listeners.put(channel, new ArrayList<>());
                listeners.get(channel).add(messageListener);
            } else
                globalListeners.add(messageListener);
        }
        return SimpleRequestExecutor.run(() -> null, callback, errorCallback);
    }

    @Override
    public Future<Void> unsubscribeChannelMessages(String channel, MessageListener messageListener, ResponseCallback<Void> callback, ResponseErrorCallback errorCallback) {
        synchronized (listeners) {
            if (channel != null) {
                if (listeners.containsKey(channel))
                    listeners.get(channel).remove(messageListener);
            } else
                globalListeners.remove(messageListener);
        }
        return SimpleRequestExecutor.run(() -> null, callback, errorCallback);
    }

    @Override
    public MessageId.Parser getMessageIdParser() {
        return getMessageIdParserInstance();
    }

    static class MyMessageId implements MessageId {

        long fileDateId;
        int id;

        MyMessageId(long fileDateId, int id) {
            this.fileDateId = fileDateId;
            this.id = id;
        }

        @Override
        public String toString() {
            return fileDateId + ":" + id;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof MyMessageId)
                return fileDateId == ((MyMessageId) obj).fileDateId && id == ((MyMessageId) obj).id;
            return false;
        }
    }

    static class MyMessageIdParser implements MessageId.Parser {

        @Override
        public MessageId parse(String str) {
            int i = str.indexOf(':');
            long fileDateId = Long.parseLong(str.substring(0, i));
            long id = Long.parseLong(str.substring(i + 1));
            return new MyMessageId(fileDateId, (int) id);
        }

    }

    static class MyMessageListOlderIdentifier implements MessageListAfterIdentifier {

        long fileDateId;
        int afterId = 1;
        int offset = 0;

        MyMessageListOlderIdentifier(long fileDateId, int afterId, int offset) {
            this.fileDateId = fileDateId;
            this.afterId = afterId;
            this.offset = offset;
        }

    }


    static class MyMessageListNewerIdentifier extends MyMessageListOlderIdentifier {
        MyMessageListNewerIdentifier(long fileDateId, int afterId, int offset) {
            super(fileDateId, afterId, offset);
        }
    }

    public static MyMessageIdParser getMessageIdParserInstance() {
        return MESSAGE_ID_PARSER;
    }

}
