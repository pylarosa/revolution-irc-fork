package io.mrarm.irc;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Keep;

import java.util.ArrayList;
import java.util.List;

import io.mrarm.irc.config.SettingsHelper;

/**
 * <b>IRCApplication</b>
 * <p>This class represents the global entry point of the IRC client.
 *
 * <p><b>General concept:</b>
 * In Android, every app process has exactly one instance of
 * {@link android.app.Application}.  It is created by the system
 * before any Activity, Service, or Receiver and stays alive for the
 * whole lifetime of the app process.  Developers can subclass it to
 * run global initialization code and to keep shared state or singletons
 * accessible throughout the app.
 *
 * <p><b>This implementation:</b>
 * <ul>
 *   <li>Extends {@link android.app.Application} so that it runs once
 *       when the process starts.</li>
 *   <li>Implements {@link android.app.Application.ActivityLifecycleCallbacks}
 *       to monitor creation and destruction of all Activities and keep
 *       track of them in a list.</li>
 *   <li>Initializes core app services in {@link #onCreate()}:
 *       loads {@link io.mrarm.irc.config.SettingsHelper} and creates
 *       default notification channels used by {@link io.mrarm.irc.IRCService}.</li>
 *   <li>Provides a global exit routine through {@link #requestExit()} that:
 *       <ol>
 *         <li>Runs all registered {@link PreExitCallback}s to check
 *             if exit is safe.</li>
 *         <li>Notifies all {@link ExitCallback}s to perform cleanup.</li>
 *         <li>Finishes every tracked Activity to close the UI.</li>
 *         <li>Destroys the {@link ServerConnectionManager} singleton
 *             (terminating IRC connections).</li>
 *         <li>Stops the background {@link IRCService}.</li>
 *       </ol>
 *   </li>
 * </ul>
 *
 * <p><b>Role in the app:</b>
 * Acts as a global singleton that coordinates lifecycle-wide state,
 * initialization, and graceful shutdown.  Any component can obtain it
 * through {@code (IRCApplication) context.getApplicationContext()}.
 *
 * <p>Created automatically by the Android framework according to the
 * {@code android:name=".IRCApplication"} entry in AndroidManifest.xml.
 */

@Keep
public class IRCApplication extends Application implements Application.ActivityLifecycleCallbacks {
    /**
     * Holds application-wide runtime references used for graceful shutdown.
     *
     * <ul>
     *   <li><b>mActivities</b> – List of all currently created {@link Activity}
     *       instances, tracked through {@link #onActivityCreated(Activity, Bundle)}
     *       and {@link #onActivityDestroyed(Activity)}. Used by
     *       {@link #requestExit()} to close every open screen.</li>
     *
     *   <li><b>mPreExitCallbacks</b> – Registered listeners that can veto app exit
     *       (return {@code false} from {@link PreExitCallback#onAppPreExit()})
     *       when, for example, there are unsaved changes or active connections.</li>
     *
     *   <li><b>mExitCallbacks</b> – Registered listeners invoked during the actual
     *       shutdown phase to perform cleanup (saveConnectedServers state, release resources, etc.).</li>
     * </ul>
     *
     * These lists are created once and never reassigned, so they could be marked
     * {@code final};
     * <br> their contents remain mutable throughout the app’s lifetime.
     */
    private final List<Activity> mActivities = new ArrayList<>();
    private final List<PreExitCallback> mPreExitCallbacks = new ArrayList<>();
    private final List<ExitCallback> mExitCallbacks = new ArrayList<>();

    /**
     * Initializes global app components when the process is first created.
     *
     * <p>Called automatically by the Android framework before any Activity or
     * Service. This method sets up global configuration and lifecycle tracking:
     * <ul>
     *   <li>Initializes {@link SettingsHelper} singleton to loadConnectedServers stored preferences
     *       and connection settings.</li>
     *   <li>Creates default {@link NotificationManager} channels used by
     *       {@link IRCService} and other parts of the app.</li>
     *   <li>Registers this instance as a global {@link ActivityLifecycleCallbacks}
     *       listener to monitor activity creation and destruction.</li>
     * </ul>
     */

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("[FLOW]", ">>> Application starting, IRCApplication.onCreated() called");
        SettingsHelper.getInstance(this);
        NotificationManager.createDefaultChannels(this);
        registerActivityLifecycleCallbacks(this);
    }

    /**
     * ─────────────────────────────────────────────────────────────────────────────
     * 📱  APPLICATION LIFECYCLE OVERVIEW
     * ─────────────────────────────────────────────────────────────────────────────
     * <p>
     *  Android process startup sequence:
     * <p>
     *      1. System reads AndroidManifest.xml
     *         → sees:  android:name=".IRCApplication"
     * <p>
     *      2. System (ActivityThread) creates one instance of IRCApplication
     *         → calls {@link #onCreate()} once per process.
     *         → global initialization happens here:
     *             - SettingsHelper singleton
     *             - Notification channels
     *             - Activity lifecycle tracking
     * <p>
     *      3. System launches the first Activity with intent-filter
     *         (action=MAIN, category=LAUNCHER) → {@code MainActivity}
     *             ├─ onCreate()
     *             ├─ onStart()   → Activity becomes visible
     *             └─ onResume()  → Activity gets focus / user can interact
     * <p>
     *      At this point the app is fully running.
     * <p>
     *  ─────────────────────────────────────────────────────────────────────────────
     * <p>
     *  Application shutdown sequence (via {@link #requestExit()} or system kill):
     * <p>
     *      1. {@link #requestExit()} invoked
     *             ├─ Run all registered {@link PreExitCallback}s
     *             │      → can cancel exit if return false
     *             ├─ Notify {@link ExitCallback}s for cleanup
     *             ├─ Finish all tracked Activities
     *             ├─ Destroy {@link ServerConnectionManager}
     *             └─ Stop {@link IRCService}
     *
     *      2. Each Activity follows reverse order:
     *             onPause() → onStop() → onDestroy()
     * <p>
     *      3. When all Activities are finished and services stopped,
     *         the process eventually terminates; IRCApplication is destroyed.
     * <p>
     *  ─────────────────────────────────────────────────────────────────────────────
     * <p>
     *  Summary:
     *      • {@code onCreate()} = process boot
     *      • {@code onStart()}  = Activity visible
     *      • {@code onResume()} = Activity interactive
     *      • {@code requestExit()} = graceful shutdown of the whole app
     */


    public void addPreExitCallback(PreExitCallback c) {
        Log.i("[FLOW]", ">>> Adding PreExitCallback, IRCApplication.addPreExitCallback() called");
        mPreExitCallbacks.add(c);
    }

    public void removePreExitCallback(PreExitCallback c) {
        Log.i("[FLOW]", ">>> Removing PreExitCallback, IRCApplication.removePreExitCallback() called");
        mPreExitCallbacks.remove(c);
    }

    public void addExitCallback(ExitCallback c) {
        Log.i("[FLOW]", ">>> Adding ExitCallback, IRCApplication.addExitCallback() called");
        mExitCallbacks.add(c);
    }

    public void removeExitCallback(ExitCallback c) {
        Log.i("[FLOW]", ">>> Removing ExitCallback, IRCApplication.removeExitCallback() called");
        mExitCallbacks.remove(c);
    }

    public boolean requestExit() {
        Log.i("[FLOW]", ">>> Application exit requested, IRCApplication.requestExit() called");
        for (PreExitCallback exitCallback : mPreExitCallbacks) {
            if (!exitCallback.onAppPreExit())
                exitCallback.logPreExitCallbak();
            return false;
        }
        for (ExitCallback exitCallback : mExitCallbacks){
            exitCallback.logExitCallbak();
            exitCallback.onAppExiting();
        }
        for (Activity activity : mActivities)
            activity.finish();
        ServerConnectionManager.destroyInstance();
        IRCService.stop(this);
        return true;
    }


    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.i("[FLOW]", ">>> Adding Activity, IRCApplication.onActivityCreated() created "
                + activity.getClass().getSimpleName());
        mActivities.add(activity);
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.i("[FLOW]", ">>> Destroying Activity, IRCApplication.onActivityDestroyed() destroyed "
                + activity.getClass().getSimpleName());
        mActivities.remove(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.i("[FLOW]", ">>> Activity started, IRCApplication.onActivityStarted() started "
                + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.i("[FLOW]", ">>> Activity resumed, IRCApplication.onActivityResumed() resumed "
                + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.i("[FLOW]", ">>> Activity paused, IRCApplication.onActivityPaused() paused "
                + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.i("[FLOW]", ">>> Activity stopped, IRCApplication.onActivityStopped() stopped "
                + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Log.i("[FLOW]", ">>> Activity state saved, IRCApplication.onActivitySaveInstanceState() performed"
                + activity.getClass().getSimpleName());
    }


    public interface PreExitCallback {

        default void logPreExitCallbak() {
            Log.i("[FLOW]", ">>> Interface IRCApplication.PreExitCallback() called ");
        }

        boolean onAppPreExit();
    }


    public interface ExitCallback {

        default void logExitCallbak() {
            Log.i("[FLOW]", ">>> Interface IRCApplication.ExitCallback() called ");
        }

        void onAppExiting();
    }
}