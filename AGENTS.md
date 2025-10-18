This is the last output of the build process:

Executing tasks: [:app:assembleDebug] in project C:\Projects\PERSONAL\rev\revolution-irc-fork



> Task :ircSettingsPlugin:compileJava UP-TO-DATE
> Task :ircSettingsPlugin:compileGroovy UP-TO-DATE
> Task :ircSettingsPlugin:pluginDescriptors UP-TO-DATE
> Task :ircSettingsPlugin:processResources UP-TO-DATE
> Task :ircSettingsPlugin:classes UP-TO-DATE
> Task :ircSettingsPlugin:jar UP-TO-DATE

> Configure project :app
AGPBI: {"kind":"warning","text":"The option setting 'android.defaults.buildfeatures.buildconfig=true' is deprecated.\nThe current default is 'false'.\nIt will be removed in version 10.0 of the Android Gradle plugin.\nTo keep using this feature, add the following to your module-level build.gradle files:\n    android.buildFeatures.buildConfig = true\nor from Android Studio, click: `Refactor` > `Migrate BuildConfig to Gradle Build Files`.","sources":[{}]}

> Task :app:preBuild UP-TO-DATE
> Task :app:preDebugBuild UP-TO-DATE
> Task :app:mergeDebugNativeDebugMetadata NO-SOURCE
> Task :app:checkKotlinGradlePluginConfigurationErrors
> Task :app:generateDebugBuildConfig UP-TO-DATE
> Task :app:generateSettings UP-TO-DATE
> Task :app:checkDebugAarMetadata UP-TO-DATE
> Task :app:processDebugNavigationResources UP-TO-DATE
> Task :app:compileDebugNavigationResources UP-TO-DATE
> Task :app:generateDebugResValues UP-TO-DATE
> Task :app:mapDebugSourceSetPaths UP-TO-DATE
> Task :app:generateDebugResources UP-TO-DATE
> Task :app:mergeDebugResources UP-TO-DATE
> Task :app:createDebugCompatibleScreenManifests UP-TO-DATE
> Task :app:extractDeepLinksDebug UP-TO-DATE
> Task :app:processDebugMainManifest UP-TO-DATE
> Task :app:processDebugManifest UP-TO-DATE
> Task :app:processDebugManifestForPackage UP-TO-DATE
> Task :app:processDebugResources UP-TO-DATE
> Task :app:javaPreCompileDebug UP-TO-DATE
> Task :app:mergeDebugShaders UP-TO-DATE
> Task :app:compileDebugShaders NO-SOURCE
> Task :app:generateDebugAssets UP-TO-DATE
> Task :app:mergeDebugAssets UP-TO-DATE
> Task :app:compressDebugAssets UP-TO-DATE
> Task :app:checkDebugDuplicateClasses UP-TO-DATE
> Task :app:desugarDebugFileDependencies UP-TO-DATE
> Task :app:mergeLibDexDebug UP-TO-DATE
> Task :app:mergeDebugJniLibFolders UP-TO-DATE
> Task :app:mergeDebugNativeLibs NO-SOURCE
> Task :app:stripDebugDebugSymbols NO-SOURCE
> Task :app:validateSigningDebug UP-TO-DATE
> Task :app:writeDebugAppMetadata UP-TO-DATE
> Task :app:writeDebugSigningConfigVersions UP-TO-DATE
> Task :app:mergeExtDexDebug UP-TO-DATE

> Task :app:compileDebugKotlin
w: file:///C:/Projects/PERSONAL/rev/revolution-irc-fork/app/src/main/java/io/mrarm/irc/IRCService.kt:11:20 'NetworkInfo' is deprecated. Deprecated in Java

> Task :app:compileDebugJavaWithJavac
Java compiler version 21 has deprecated support for compiling with source/target version 8.
Try one of the following options:
1. [Recommended] Use Java toolchain with a lower language version
2. Set a higher source/target version
3. Use a lower version of the JDK running the build (if you're not using Java toolchain)
For more details on how to configure these settings, see https://developer.android.com/build/jdks.
To suppress this warning, set android.javaCompile.suppressSourceTargetDeprecationWarning=true in gradle.properties.
warning: [options] source value 8 is obsolete and will be removed in a future release
warning: [options] target value 8 is obsolete and will be removed in a future release
warning: [options] To suppress warnings about obsolete options, use -Xlint:-options.
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:3: warning: [deprecation] AsyncTask in android.os has been deprecated
import android.os.AsyncTask;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ServerConnectionManager.java:7: warning: [deprecation] NetworkInfo in android.net has been deprecated
import android.net.NetworkInfo;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatPagerAdapter.java:7: warning: [deprecation] FragmentPagerAdapter in androidx.fragment.app has been deprecated
import androidx.fragment.app.FragmentPagerAdapter;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatFragmentSendMessageHelper.java:8: warning: [deprecation] MarginLayoutParamsCompat in androidx.core.view has been deprecated
import androidx.core.view.MarginLayoutParamsCompat;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\config\BackupManager.java:6: warning: [deprecation] PreferenceManager in android.preference has been deprecated
import android.preference.PreferenceManager;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\config\SettingsHelper.java:8: warning: [deprecation] PreferenceManager in android.preference has been deprecated
import android.preference.PreferenceManager;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCManager.java:12: warning: [deprecation] AsyncTask in android.os has been deprecated
import android.os.AsyncTask;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCManager.java:19: warning: [deprecation] PreferenceManager in android.preference has been deprecated
import android.preference.PreferenceManager;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\dialog\StorageLimitsDialog.java:8: warning: [deprecation] PreferenceManager in android.preference has been deprecated
import android.preference.PreferenceManager;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\IRCChooserTargetService.java:9: warning: [deprecation] ChooserTarget in android.service.chooser has been deprecated
import android.service.chooser.ChooserTarget;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\IRCChooserTargetService.java:10: warning: [deprecation] ChooserTargetService in android.service.chooser has been deprecated
import android.service.chooser.ChooserTargetService;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\util\MessageBuilder.java:6: warning: [deprecation] PreferenceManager in android.preference has been deprecated
import android.preference.PreferenceManager;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\setting\fragment\AutocompletePreferenceFragment.java:4: warning: [deprecation] PreferenceManager in android.preference has been deprecated
import android.preference.PreferenceManager;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\setting\fragment\InterfaceSettingsFragment.java:10: warning: [deprecation] PreferenceManager in android.preference has been deprecated
import android.preference.PreferenceManager;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\setting\fragment\ReconnectSettingsFragment.java:6: warning: [deprecation] PreferenceManager in android.preference has been deprecated
import android.preference.PreferenceManager;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\setting\fragment\UserSettingsFragment.java:4: warning: [deprecation] PreferenceManager in android.preference has been deprecated
import android.preference.PreferenceManager;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\setup\BackupProgressActivity.java:6: warning: [deprecation] AsyncTask in android.os has been deprecated
import android.os.AsyncTask;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\StorageSettingsAdapter.java:6: warning: [deprecation] AsyncTask in android.os has been deprecated
import android.os.AsyncTask;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ThemeEditorActivity.java:9: warning: [deprecation] FragmentPagerAdapter in androidx.fragment.app has been deprecated
import androidx.fragment.app.FragmentPagerAdapter;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\util\PoolSerialExecutor.java:3: warning: [deprecation] AsyncTask in android.os has been deprecated
import android.os.AsyncTask;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\util\theme\ThemeManager.java:271: warning: [deprecation] Resources(AssetManager,DisplayMetrics,Configuration) in Resources has been deprecated
Resources r = new Resources(currentCustomThemePatcher != null ?
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:116: warning: [deprecation] execute(Params...) in AsyncTask has been deprecated
mUpdateListAsyncTask.execute();
^
where Params,Progress,Result are type-variables:
Params extends Object declared in class AsyncTask
Progress extends Object declared in class AsyncTask
Result extends Object declared in class AsyncTask
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:152: warning: [deprecation] onBackPressed() in ComponentActivity has been deprecated
public void onBackPressed() {
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:157: warning: [deprecation] onBackPressed() in ComponentActivity has been deprecated
super.onBackPressed();
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:166: warning: [deprecation] getColor(int) in Resources has been deprecated
getWindow().setStatusBarColor(getResources().getColor(R.color.searchColorPrimaryDark));
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:166: warning: [deprecation] setStatusBarColor(int) in Window has been deprecated
getWindow().setStatusBarColor(getResources().getColor(R.color.searchColorPrimaryDark));
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:168: warning: [deprecation] getColor(int) in Resources has been deprecated
getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:168: warning: [deprecation] setStatusBarColor(int) in Window has been deprecated
getWindow().setStatusBarColor(getResources().getColor(R.color.colorPrimaryDark));
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:173: warning: [deprecation] getSystemUiVisibility() in View has been deprecated
decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:173: warning: [deprecation] SYSTEM_UI_FLAG_LIGHT_STATUS_BAR in View has been deprecated
decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:173: warning: [deprecation] setSystemUiVisibility(int) in View has been deprecated
decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:175: warning: [deprecation] getSystemUiVisibility() in View has been deprecated
decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:175: warning: [deprecation] SYSTEM_UI_FLAG_LIGHT_STATUS_BAR in View has been deprecated
decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:175: warning: [deprecation] setSystemUiVisibility(int) in View has been deprecated
decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:249: warning: [deprecation] AsyncTask in android.os has been deprecated
private static class UpdateListAsyncTask extends AsyncTask<Void, Void, List<ChannelList.Entry>> {
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:249: warning: [deprecation] AsyncTask in android.os has been deprecated
private static class UpdateListAsyncTask extends AsyncTask<Void, Void, List<ChannelList.Entry>> {
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:262: warning: [deprecation] doInBackground(Params...) in AsyncTask has been deprecated
protected List<ChannelList.Entry> doInBackground(Void... voids) {
^
where Params,Result are type-variables:
Params extends Object declared in class AsyncTask
Result extends Object declared in class AsyncTask
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:255: warning: [deprecation] AsyncTask() in AsyncTask has been deprecated
public UpdateListAsyncTask(ChannelListActivity activity) {
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelListActivity.java:290: warning: [deprecation] onPostExecute(Result) in AsyncTask has been deprecated
protected void onPostExecute(List<ChannelList.Entry> ret) {
^
where Result is a type-variable:
Result extends Object declared in class AsyncTask
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\view\RecyclerViewScrollbar.java:228: warning: [unchecked] unchecked call to bindViewHolder(VH,int) as a member of the raw type Adapter
mRecyclerView.getAdapter().bindViewHolder(holder, i);
^
where VH is a type-variable:
VH extends ViewHolder declared in class Adapter
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ServerConnectionManager.java:432: warning: [deprecation] isConnected() in NetworkInfo has been deprecated
return info != null && info.isConnected();
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\NotificationManager.java:166: warning: [deprecation] getColor(int) in Resources has been deprecated
.setColor(context.getResources().getColor(R.color.colorNotificationMention))
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelNotificationManager.java:207: warning: [deprecation] getColor(int) in Resources has been deprecated
.setColor(context.getResources().getColor(R.color.colorNotificationMention))
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChannelNotificationManager.java:277: warning: [deprecation] USAGE_NOTIFICATION_COMMUNICATION_INSTANT in AudioAttributes has been deprecated
.setUsage(AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatPagerAdapter.java:17: warning: [deprecation] FragmentPagerAdapter in androidx.fragment.app has been deprecated
public class ChatPagerAdapter extends FragmentPagerAdapter {
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatPagerAdapter.java:26: warning: [deprecation] FragmentPagerAdapter(FragmentManager) in FragmentPagerAdapter has been deprecated
super(fm);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatPagerAdapter.java:33: warning: [deprecation] FragmentPagerAdapter(FragmentManager) in FragmentPagerAdapter has been deprecated
super(fm);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatFragmentSendMessageHelper.java:140: warning: [deprecation] setBottomSheetCallback(BottomSheetCallback) in BottomSheetBehavior has been deprecated
BottomSheetBehavior.from(mServerMessagesCard).setBottomSheetCallback(mServerMessagesBottomSheetCallback);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatFragmentSendMessageHelper.java:169: warning: [deprecation] MarginLayoutParamsCompat in androidx.core.view has been deprecated
MarginLayoutParamsCompat.setMarginStart(layoutParams, 0);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatFragmentSendMessageHelper.java:169: warning: [deprecation] setMarginStart(MarginLayoutParams,int) in MarginLayoutParamsCompat has been deprecated
MarginLayoutParamsCompat.setMarginStart(layoutParams, 0);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatFragmentSendMessageHelper.java:172: warning: [deprecation] MarginLayoutParamsCompat in androidx.core.view has been deprecated
MarginLayoutParamsCompat.setMarginStart(layoutParams, mContext.getResources()
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatFragmentSendMessageHelper.java:172: warning: [deprecation] setMarginStart(MarginLayoutParams,int) in MarginLayoutParamsCompat has been deprecated
MarginLayoutParamsCompat.setMarginStart(layoutParams, mContext.getResources()
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatSuggestionsAdapter.java:120: warning: [deprecation] getColor(int) in Resources has been deprecated
mText.setTextColor(mText.getContext().getResources().getColor(R.color.memberNormal));
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatMessagesFragment.java:114: warning: [deprecation] setUserVisibleHint(boolean) in Fragment has been deprecated
public void setUserVisibleHint(boolean isVisibleToUser) {
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatMessagesFragment.java:115: warning: [deprecation] setUserVisibleHint(boolean) in Fragment has been deprecated
super.setUserVisibleHint(isVisibleToUser);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatMessagesFragment.java:340: warning: [deprecation] getUserVisibleHint() in Fragment has been deprecated
if (getUserVisibleHint())
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatMessagesFragment.java:359: warning: [deprecation] getUserVisibleHint() in Fragment has been deprecated
if (mConnection != null && getUserVisibleHint() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatMessagesFragment.java:496: warning: [deprecation] getUserVisibleHint() in Fragment has been deprecated
if (getUserVisibleHint()) {
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatMessagesFragment.java:501: warning: [deprecation] getUserVisibleHint() in Fragment has been deprecated
if (mConnection != null && getUserVisibleHint() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatMessagesFragment.java:509: warning: [deprecation] getUserVisibleHint() in Fragment has been deprecated
if (getUserVisibleHint() && (activity == null || !activity.isAppExiting()))
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatMessagesFragment.java:511: warning: [deprecation] getUserVisibleHint() in Fragment has been deprecated
if (mConnection != null && getUserVisibleHint() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatMessagesFragment.java:593: warning: [deprecation] getUserVisibleHint() in Fragment has been deprecated
if (!getUserVisibleHint() && mAdapter.getNewMessagesStart() == null)
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatMessagesFragment.java:629: warning: [deprecation] getUserVisibleHint() in Fragment has been deprecated
if (getUserVisibleHint())
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\chat\ChatMessagesFragment.java:638: warning: [deprecation] getUserVisibleHint() in Fragment has been deprecated
if (getUserVisibleHint())
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\util\RecyclerViewScrollerRunnable.java:28: warning: [deprecation] postOnAnimation(View,Runnable) in ViewCompat has been deprecated
ViewCompat.postOnAnimation(mRecyclerView, this);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\util\RecyclerViewScrollerRunnable.java:45: warning: [deprecation] postOnAnimation(View,Runnable) in ViewCompat has been deprecated
ViewCompat.postOnAnimation(mRecyclerView, this);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\view\TextSelectionHandleView.java:20: warning: [deprecation] getDrawable(int) in Resources has been deprecated
return context.getResources().getDrawable(resId);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\ChatLogStorageManager.java:175: warning: [deprecation] getBlockSize() in StatFs has been deprecated
mBlockSize = statFs.getBlockSize();
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\util\AdvancedDividerItemDecoration.java:60: warning: [deprecation] getTranslationY(View) in ViewCompat has been deprecated
final int bottom = mBounds.bottom + Math.round(ViewCompat.getTranslationY(child));
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\util\EntryRecyclerViewAdapter.java:145: warning: [unchecked] unchecked assignment to variable mEntry as member of raw type EntryHolder
viewHolder.mEntry = null;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\setting\SettingsListAdapter.java:47: warning: [deprecation] startActivityForResult(Intent,int) in Fragment has been deprecated
mFragment.startActivityForResult(intent, requestId);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\config\BackupManager.java:264: warning: [deprecation] PreferenceManager in android.preference has been deprecated
SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\config\BackupManager.java:264: warning: [deprecation] getDefaultSharedPreferences(Context) in PreferenceManager has been deprecated
SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\config\BackupManager.java:271: warning: [deprecation] PreferenceManager in android.preference has been deprecated
SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\config\BackupManager.java:271: warning: [deprecation] getDefaultSharedPreferences(Context) in PreferenceManager has been deprecated
SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(context).edit();
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\config\SettingsHelper.java:64: warning: [deprecation] PreferenceManager in android.preference has been deprecated
mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\config\SettingsHelper.java:64: warning: [deprecation] getDefaultSharedPreferences(Context) in PreferenceManager has been deprecated
mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCActivity.java:87: warning: [deprecation] startActivityForResult(Intent,int) in ComponentActivity has been deprecated
startActivityForResult(intent, REQUEST_CODE_PICK_CUSTOM_DIRECTORY);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCActivity.java:91: warning: [deprecation] startActivityForResult(Intent,int) in ComponentActivity has been deprecated
startActivityForResult(intent, REQUEST_CODE_PICK_CUSTOM_DIRECTORY);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCManager.java:101: warning: [deprecation] PreferenceManager in android.preference has been deprecated
mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCManager.java:101: warning: [deprecation] getDefaultSharedPreferences(Context) in PreferenceManager has been deprecated
mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCManager.java:242: warning: [deprecation] AsyncTask in android.os has been deprecated
AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> deleteUploadPortMapping(mapping));
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCManager.java:242: warning: [deprecation] THREAD_POOL_EXECUTOR in AsyncTask has been deprecated
AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> deleteUploadPortMapping(mapping));
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCManager.java:421: warning: [deprecation] AsyncTask in android.os has been deprecated
AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCManager.java:421: warning: [deprecation] THREAD_POOL_EXECUTOR in AsyncTask has been deprecated
AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCManager.java:693: warning: [deprecation] AsyncTask in android.os has been deprecated
AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCManager.java:693: warning: [deprecation] THREAD_POOL_EXECUTOR in AsyncTask has been deprecated
AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\DCCManager.java:885: warning: [deprecation] createAccessIntent(String) in StorageVolume has been deprecated
Intent intent = volume.createAccessIntent(Environment.DIRECTORY_DOWNLOADS);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\dialog\SearchDialog.java:54: warning: [deprecation] FLAG_TRANSLUCENT_STATUS in LayoutParams has been deprecated
window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\dialog\SearchDialog.java:61: warning: [deprecation] SOFT_INPUT_ADJUST_RESIZE in LayoutParams has been deprecated
window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\dialog\ChannelSearchDialog.java:56: warning: [deprecation] getColor(int) in Resources has been deprecated
mHighlightTextColor = context.getResources().getColor(R.color.searchColorHighlight);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\view\ListSearchView.java:90: warning: [deprecation] getColor(int) in Resources has been deprecated
mStatusBarColor = getResources().getColor(R.color.searchColorPrimaryDark);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\view\ListSearchView.java:114: warning: [deprecation] getSystemUiVisibility() in View has been deprecated
int vis = decorView.getSystemUiVisibility();
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\view\ListSearchView.java:116: warning: [deprecation] SYSTEM_UI_FLAG_LIGHT_STATUS_BAR in View has been deprecated
vis |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\view\ListSearchView.java:118: warning: [deprecation] SYSTEM_UI_FLAG_LIGHT_STATUS_BAR in View has been deprecated
vis = vis & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\view\ListSearchView.java:119: warning: [deprecation] setSystemUiVisibility(int) in View has been deprecated
decorView.setSystemUiVisibility(vis);
^
C:\Projects\PERSONAL\rev\revolution-irc-fork\app\src\main\java\io\mrarm\irc\view\ListSearchView.java:122: warning: [deprecation] setStatusBarColor(int) in Window has been deprecated
mDialog.getWindow().setStatusBarColor(isVisible ? mStatusBarColor : 0);
^
Note: Some input files additionally use or override a deprecated API.
100 warnings

> Task :app:processDebugJavaRes UP-TO-DATE
> Task :app:mergeDebugJavaResource UP-TO-DATE
> Task :app:dexBuilderDebug
> Task :app:mergeProjectDexDebug
> Task :app:packageDebug
> Task :app:createDebugApkListingFileRedirect
> Task :app:assembleDebug

[Incubating] Problems report is available at: file:///C:/Projects/PERSONAL/rev/revolution-irc-fork/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.3/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 34s
41 actionable tasks: 7 executed, 34 up-to-date

Build Analyzer results available
