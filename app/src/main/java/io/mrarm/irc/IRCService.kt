package io.mrarm.irc

import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.mrarm.chatlib.dto.MessageId
import io.mrarm.chatlib.dto.MessageInfo
import io.mrarm.chatlib.message.MessageListener
import io.mrarm.irc.job.ServerPingScheduler
import io.mrarm.irc.util.WarningHelper
import kotlinx.coroutines.launch

class IRCService : LifecycleService(), ServerConnectionManager.ConnectionsListener {

    private var createdChannel = false
    private val messageListeners = mutableMapOf<ServerConnectionInfo, MessageListener>()
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun onCreate() {
        super.onCreate()

        WarningHelper.setAppContext(applicationContext)
        ChatLogStorageManager.getInstance(applicationContext)

        val manager = ServerConnectionManager.getInstance(this)
        manager.connections.forEach { onConnectionAdded(it) }
        manager.addListener(this)

        registerNetworkCallback()

        ServerPingScheduler.getInstance(this).startIfEnabled()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (ServerConnectionManager.hasInstance()) {
            val manager = ServerConnectionManager.getInstance(this)
            manager.connections.forEach { onConnectionRemoved(it) }
            manager.removeListener(this)
        }

        unregisterNetworkCallback()

        ServerPingScheduler.getInstance(this).stop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_STICKY
        if (action == ACTION_START_FOREGROUND) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !createdChannel) {
                createNotificationChannel(this)
                createdChannel = true
            }

            val builder = StringBuilder()
            var connectedCount = 0
            var connectingCount = 0
            var disconnectedCount = 0
            val manager = ServerConnectionManager.getInstance(this)
            manager.connections.forEach { connectionInfo ->
                when {
                    connectionInfo.isConnected -> connectedCount++
                    connectionInfo.isConnecting -> connectingCount++
                    else -> disconnectedCount++
                }
            }
            builder.append(resources.getQuantityString(R.plurals.service_status_connected, connectedCount, connectedCount))
            if (connectingCount > 0) {
                builder.append(getString(R.string.text_comma))
                builder.append(resources.getQuantityString(R.plurals.service_status_connecting, connectingCount, connectingCount))
            }
            if (disconnectedCount > 0) {
                builder.append(getString(R.string.text_comma))
                builder.append(resources.getQuantityString(R.plurals.service_status_disconnected, disconnectedCount, disconnectedCount))
            }

            val mainIntent = MainActivity.getLaunchIntent(this, null, null)
            val exitIntent = PendingIntent.getBroadcast(
                this,
                EXIT_ACTION_ID,
                ExitActionReceiver.getIntent(this),
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(this, IDLE_NOTIFICATION_CHANNEL)
                .setContentTitle(getString(R.string.service_title))
                .setContentText(builder.toString())
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOnlyAlertOnce(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        IDLE_NOTIFICATION_ID,
                        mainIntent,
                        PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .addAction(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        R.drawable.ic_close
                    else
                        R.drawable.ic_notification_close,
                    getString(R.string.action_exit),
                    exitIntent
                )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                notification.setSmallIcon(R.drawable.ic_server_connected)
            else
                notification.setSmallIcon(R.drawable.ic_notification_connected)

            startForeground(IDLE_NOTIFICATION_ID, notification.build())
        }
        return START_STICKY
    }

    private fun onMessage(connection: ServerConnectionInfo, channel: String?, info: MessageInfo, messageId: MessageId) {
        NotificationManager.getInstance().processMessage(this, connection, channel, info, messageId)
        ChatLogStorageManager.getInstance(this).onMessage(connection)
    }

    override fun onConnectionAdded(connection: ServerConnectionInfo) {
        val listener = MessageListener { channel, info, id ->
            onMessage(connection, channel, info, id)
        }
        messageListeners[connection] = listener
        connection.apiInstance?.messageStorageApi?.subscribeChannelMessages(null, listener, null, null)
    }

    override fun onConnectionRemoved(connection: ServerConnectionInfo) {
        val listener = messageListeners.remove(connection)
        if (listener != null) {
            connection.apiInstance?.messageStorageApi?.unsubscribeChannelMessages(null, listener, null, null)
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun registerNetworkCallback() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val manager = connectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handleNetworkUpdate()
            }

            override fun onLost(network: Network) {
                handleNetworkUpdate()
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                handleNetworkUpdate()
            }
        }
        networkCallback = callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            manager.registerNetworkCallback(request, callback)
            handleNetworkUpdate()
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to register network callback", e)
        }
    }

    private fun unregisterNetworkCallback() {
        val manager = connectivityManager ?: return
        val callback = networkCallback ?: return
        try {
            manager.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
        networkCallback = null
    }

    private fun handleNetworkUpdate() {
        lifecycleScope.launch {
            val manager = ServerConnectionManager.getInstance(this@IRCService)
            manager.notifyConnectivityChanged(hasAnyNetworkConnection())
            val wifiConnected = ServerConnectionManager.isWifiConnected(this@IRCService)
            ServerPingScheduler.getInstance(this@IRCService).onWifiStateChanged(wifiConnected)
        }
    }

    private fun hasAnyNetworkConnection(): Boolean {
        val manager = connectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = manager.activeNetwork ?: return false
            val capabilities = manager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val info: NetworkInfo? = manager.activeNetworkInfo
            @Suppress("DEPRECATION")
            info?.isConnected == true
        }
    }

    class BootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.i(TAG, "Device booted")
            start(context)
        }
    }

    class ExitActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            (context.applicationContext as IRCApplication).requestExit()
        }

        companion object {
            @JvmStatic
            fun getIntent(context: Context): Intent = Intent(context, ExitActionReceiver::class.java)
        }
    }

    companion object {
        private const val TAG = "IRCService"

        const val IDLE_NOTIFICATION_ID = 100
        const val EXIT_ACTION_ID = 102
        const val ACTION_START_FOREGROUND = "start_foreground"

        private const val IDLE_NOTIFICATION_CHANNEL = "IdleNotification"

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, IRCService::class.java).apply {
                action = ACTION_START_FOREGROUND
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        @JvmStatic
        fun stop(context: Context) {
            context.stopService(Intent(context, IRCService::class.java))
        }

        @JvmStatic
        fun createNotificationChannel(ctx: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                return
            val channel = NotificationChannel(
                IDLE_NOTIFICATION_CHANNEL,
                ctx.getString(R.string.notification_channel_idle),
                android.app.NotificationManager.IMPORTANCE_MIN
            ).apply {
                group = NotificationManager.getSystemNotificationChannelGroup(ctx)
                setShowBadge(false)
            }
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            mgr.createNotificationChannel(channel)
        }
    }
}
