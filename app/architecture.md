# IRC Android Client — Architecture Documentation

> Generated from full codebase review. Covers ~360 files across 42 packages.  
> Status: active large-scale refactoring in progress.

---

## Table of Contents

1. [System Overview](#1-system-overview)
2. [Package Structure](#2-package-structure)
3. [Module Breakdown](#3-module-breakdown)
4. [Data Flow](#4-data-flow)
5. [Key Design Patterns](#5-key-design-patterns)
6. [Threading Model](#6-threading-model)
7. [Component Interaction Map](#7-component-interaction-map)
8. [Storage Layer](#8-storage-layer)
9. [What Is Refactored vs Legacy](#9-what-is-refactored-vs-legacy)

---

## 1. System Overview

This is a full-featured Android IRC client. It maintains persistent background IRC connections via a foreground `Service`, renders live message streams in a `Fragment`-based UI, persists all messages to a Room database, and delivers notifications for mentions and unread messages.

The codebase is mid-refactor. The original architecture was a tightly coupled monolith where protocol handling, database writes, and UI updates were entangled in the same call stacks. The refactoring work has successfully decoupled the infrastructure layers (network, persistence, delivery bus) and is working toward a clean layered architecture. Several packages (`conversation/`, `model/`, `notification/`, `platform/`) exist as structural placeholders for the next phase.

### High-Level Layers

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer                          │
│  MainActivity · ChatFragment · ChatMessagesFragment  │
│  DrawerHelper · MainNavigator · ServerListFragment   │
└────────────────────────┬────────────────────────────┘
                         │ observes / commands
┌────────────────────────▼────────────────────────────┐
│               Session / Connection Layer             │
│  ServerConnectionManager · ServerConnectionSession   │
│  ServerConnectionFactory · SessionInitializer        │
└────────────────────────┬────────────────────────────┘
                         │ emits / receives
┌────────────────────────▼────────────────────────────┐
│              Message Pipeline Layer                  │
│  MessageSink · DefaultMessagePipeline · MessageBus   │
│  DefaultMessageBus                                   │
└──────────┬─────────────────────────┬────────────────┘
           │ persists                │ notifies
┌──────────▼──────────┐   ┌─────────▼──────────────────┐
│   Storage Layer     │   │   Notification Layer        │
│  MessageStorage     │   │  NotificationManager        │
│  Repository · Room  │   │  ChannelNotificationManager │
│  ConversationState  │   │  IRCService (foreground)    │
└─────────────────────┘   └────────────────────────────┘
           ▲
┌──────────┴──────────────────────────────────────────┐
│               Protocol Layer  (chatlib/)             │
│  IRCConnection · MessageHandler · ChannelData        │
│  CommandHandlerList · CapabilityManager              │
│  MessageFilterList · ServerConnectionData            │
└─────────────────────────────────────────────────────┘
```

---

## 2. Package Structure

```
io.mrarm.irc/
├── app/
│   ├── interaction/        ChatOptionsActionHandler
│   ├── menu/               ChatMenuApplier, ChatMenuState, ChatMenuStateResolver
│   └── navigation/         MainNavigator, NavigationHost, DrawerToolbarHost
├── chat/                   All chat UI fragments, adapters, helpers
├── chatlib/                Pure IRC protocol library (largely legacy, being wrapped)
│   ├── dto/                Protocol data objects (MessageInfo, ChannelInfo, etc.)
│   ├── irc/                Core IRC engine
│   │   ├── cap/            IRCv3 capability handlers
│   │   ├── dcc/            DCC file transfer
│   │   ├── filters/        Message filters (ZNC playback, ignore list)
│   │   └── handlers/       Command handlers (JOIN, PART, PRIVMSG, etc.)
│   ├── message/            MessageListener interface
│   └── user/               User identity resolution
├── config/                 All settings and configuration persistence
├── connection/             Session lifecycle management (refactored)
├── conversation/           [EMPTY — planned domain layer]
├── dialog/                 Reusable dialog components
├── drawer/                 Navigation drawer UI and adapter
├── infrastructure/
│   └── threading/          AppAsyncExecutor, DelayScheduler, SchedulerProvider
├── job/                    Background jobs (ping, cleanup)
├── legacy/                 [EMPTY — deprecated code staging area]
├── message/                Message pipeline interfaces and implementations (refactored)
├── model/                  [EMPTY — planned domain models]
├── notification/           [EMPTY — planned notification domain layer]
├── platform/               [EMPTY — planned platform abstraction]
├── setting/                Settings UI fragments and widgets
├── setup/                  First-run setup flow
├── storage/                Persistence repositories (refactored)
│   └── db/                 Room entities, DAOs, database class
├── ui/                     [EMPTY — planned shared UI components]
├── upnp/                   UPnP port mapping for DCC
├── util/                   Utilities, spans, theming, message formatting
└── view/                   Custom views
```

---

## 3. Module Breakdown

### 3.1 Application Bootstrap — `IRCApplication`, `IRCService`

**`IRCApplication`** is the `Application` subclass. It runs once per process and is responsible for:
- Initializing `SettingsHelper` (shared preferences wrapper)
- Creating Android notification channels via `NotificationManager.createDefaultChannels()`
- Tracking all live `Activity` instances via `ActivityLifecycleCallbacks`
- Coordinating graceful shutdown via `requestExit()`, which chains pre-exit vetoes, exit callbacks, activity finishing, `ServerConnectionManager` teardown, and service stop

**`IRCService`** is a `LifecycleService` (foreground). Its responsibilities are narrow and well-scoped:
- Displaying the persistent foreground notification summarizing connection state
- Registering the system `ConnectivityManager.NetworkCallback` and routing changes to `ServerConnectionManager.notifyConnectivityChanged()`
- Starting the `ServerPingScheduler`
- Handling the `ExitActionReceiver` broadcast (notification "Exit" button → `IRCApplication.requestExit()`)
- Handling device reboot via `BootReceiver`

These two classes together form the application shell. Neither owns business logic.

---

### 3.2 Connection Management — `connection/`

This package is one of the strongest results of the refactoring. It follows a clean separation of construction, lifecycle, and orchestration.

#### `ServerConnectionFactory`
Builds a `ServerConnectionSession` from a `ServerConfigData`. It handles all construction-time concerns: SSL context assembly, SASL options, nick/user/realname resolution from config vs defaults, `IRCConnectionRequest` population, and `ReconnectPolicy` creation. It instantiates a `SessionInitializer` and passes it into the session.

**Nothing else builds sessions.**

#### `ServerConnectionSession`
Represents one live IRC connection. Owns:
- **Lifecycle state**: `mConnected`, `mConnecting`, `mDisconnecting`, `mUserDisconnectRequest`
- **Reconnect logic**: works with `DelayScheduler` and `ReconnectPolicy` to schedule retries with backoff
- **Channel list**: maintains the sorted list of currently joined channels, persists it asynchronously via `ServerConnectionManager`
- **Notification data**: owns a `NotificationManager.ConnectionManager` which in turn owns per-channel `ChannelNotificationManager` instances
- **UI data**: owns `ChatUIData`, the map of per-channel draft text and send history

The session does **not** know about the IRC protocol. It delegates connection creation to `IRCConnection` and wiring to `SessionInitializer`.

#### `SessionInitializer`
The wiring script for a new connection. Called once per `connect()` on a fresh `IRCConnection`. It:
1. Creates `MessageBus` (new `DefaultMessageBus`)
2. Creates `MessagePipelineContext` (serverId + repository reference)
3. Creates `DefaultMessagePipeline` (wraps context + bus)
4. Injects pipeline and bus into `ServerConnectionData` via `setMessageSink()` and `setMessageBus()`
5. Subscribes a global bus listener that forwards non-playback messages to `NotificationManager`
6. Adds the `IgnoreListMessageFilter` and `ZNCPlaybackMessageFilter` to the filter chain
7. Wires DCC managers and CTCP version string into `MessageCommandHandler`
8. Attaches the disconnect listener that calls `session.notifyDisconnected()`

This is the single place where all subsystem wiring is visible. It acts as a **compositor**.

#### `ServerConnectionManager`
The application-level registry of all live sessions. Responsibilities:
- Creating sessions via `ServerConnectionFactory`
- Persisting and restoring the autoconnect list via `ServerConnectionRegistry`
- Broadcasting connection/disconnection events to global listeners (drawer, server list adapter, etc.)
- Forwarding connectivity change events to all sessions
- Managing the `IRCService` lifecycle (starts service when first connection is added)

It explicitly documents what it does **not** own: it does not control how connections behave, it does not own reconnect logic, and it does not know about IRC protocol.

#### `ServerConnectionRegistry`
A simple JSON file (`connected_servers.json`) that records which servers were connected and which channels were joined at last shutdown. It is explicitly non-authoritative — just a warm-start hint.

---

### 3.3 Protocol Layer — `chatlib/irc/`

This is the original IRC engine, partially wrapped but not yet fully replaced. It is responsible for everything that speaks IRC wire protocol.

#### `IRCConnection` extends `ServerConnectionApi`
The socket manager. Owns:
- Connecting/disconnecting to TCP/TLS sockets
- The network read loop (`handleInput()`) running on a dedicated thread
- Writing raw commands to the socket (`sendCommand()`, `sendMessage()`, etc.)
- Disconnect listener notification
- Self-message injection: when a PRIVMSG is sent, it re-routes the outgoing message back through the inbound pipeline to produce local echo (noted as having no echo-message capability deduplication)

#### `MessageHandler`
Parses raw IRC lines from the socket into structured form:
- Strips and parses IRCv3 message tags (`@key=value;...`)
- Extracts message prefix (`:nick!user@host`)
- Extracts command and parameter list
- Dispatches to `CommandHandlerList.getHandlerFor(command).handle(...)`

#### `ServerConnectionData`
The central session state object. Everything the command handlers read and write passes through here:
- Current nick, user, host
- Map of joined `ChannelData` objects (keyed by lowercase channel name)
- `CommandHandlerList`, `CapabilityManager`, `MessageFilterList`
- `ServerConnectionApi` reference (back-pointer to the connection)
- `WritableUserInfoApi` (user identity resolution)
- **`MessageSink`** — the hand-off point to the refactored pipeline
- **`MessageBus`** — the delivery bus for UI subscribers
- `MessageStorageRepository` — direct reference (used by ZNC filter)

#### `ChannelData`
Represents one joined channel or DM conversation at the protocol level. Key method:

```
addMessage(MessageInfo.Builder, Map<String, String> tags)
  → Capability.processMessage()    [IRCv3 caps annotate the message]
  → addMessage(MessageInfo)
      → MessageFilterList.filterMessage()   [drop/pass]
      → MessageSink.accept(channelName, message)   ← pipeline boundary
```

This is the most important architectural seam in the system. Everything above this call is protocol. Everything below is infrastructure.

#### Command Handlers (`handlers/`)
One handler per IRC command family. Notable:

- **`MessageCommandHandler`**: handles PRIVMSG/NOTICE. Resolves sender identity (blocking `Future.get()` on the network thread), handles CTCP (ACTION, PING, VERSION, DCC), and calls `channelData.addMessage()`. Also handles DCC negotiation.
- **`JoinCommandHandler`**: calls `connection.onChannelJoined()` → creates `ChannelData`
- **`PartCommandHandler` / `QuitCommandHandler`**: calls `onChannelLeft()` → removes `ChannelData`
- **`CapCommandHandler`**: negotiates IRCv3 capabilities with the server via `CapabilityManager`
- **`NickCommandHandler`**: updates user identity

#### Capabilities (`cap/`)
IRCv3 capability implementations. `Capability.processMessage()` is called on every message before filtering. Notable capabilities:
- **`ServerTimeCapability`**: overwrites the message timestamp from the `time` tag
- **`ZNCSelfMessageCapability`**: marks outgoing messages echoed by ZNC
- **`BatchCapability`**: groups messages into logical batches
- **`SASLCapability`**: handles SASL authentication exchange

#### Filters (`filters/`)
- **`ZNCPlaybackMessageFilter`**: detects ZNC history replays. Marks them as `isPlayback()`. The `MessageEntity` deduplication at the DB layer is the safety net for any that slip through.
- **`IgnoreListMessageFilter`** (in `util/`): drops messages matching the user's ignore list patterns.

---

### 3.4 Message Pipeline — `message/`

This is the cleanest, most self-contained result of the refactoring. It is a three-interface design:

```
MessageSink          ← accept(roomKey, MessageInfo)
    ↓
MessagePipeline      ← extends MessageSink, adds shutdown()
    ↓
DefaultMessagePipeline
    → persist to Room (on pipeline executor thread)
    → assign MessageId (RoomMessageId wrapping DB row id)
    → MessageBus.emit(channel, message, messageId)
```

#### `MessageSink`
Single method: `accept(roomKey, MessageInfo)`. Non-blocking contract. Must not throw on persistence failure. Ordering is pipeline-defined.

#### `MessagePipeline` / `DefaultMessagePipeline`
`DefaultMessagePipeline` owns a **single-threaded executor** named `"MessagePipeline"`. All persistence and bus emission happen on this thread, in order. This means:
- The network thread is never blocked by DB I/O
- Message ordering is guaranteed (single-threaded executor = FIFO)
- The bus always emits after persistence, so UI subscribers always see messages that are already in the DB

#### `MessageBus` / `DefaultMessageBus`
Pub/sub delivery. Subscribers register by channel name or `null` for all channels. `DefaultMessageBus` maintains two separate listener lists (`channelListeners` map + `globalListeners`) and takes defensive copies before iterating, making it safe for concurrent subscribe/unsubscribe during emission.

The global (`null`) subscription in `SessionInitializer` feeds `NotificationManager`. Per-channel subscriptions in `ChatMessagesFragment` feed the UI.

---

### 3.5 Storage Layer — `storage/` and `storage/db/`

#### `ChatLogDatabase`
Room database with two entities and WAL journal mode. A conditional unique index on `dedupe_key` (created in `onOpen`) provides deduplication for ZNC playback messages without requiring a schema migration.

#### `MessageEntity`
The persistence model for one message. Notable:
- **`dedupeKey`**: SHA-1 hash of `serverId|channel|sender|text|timeBucket(1min)`. Only set for playback messages. The unique index + `OnConflictStrategy.IGNORE` on insert means duplicate playback messages are silently dropped at the DB level.
- **`aproxRowSize`**: a careful UTF-8 byte estimate of the row's storage cost, used by the auto-cleanup and quota enforcement logic. Computed once at insert time and stored, avoiding expensive re-computation during cleanup scans.

#### `MessageDao`
Standard Room DAO. Key queries:
- `loadRecent()` — last N messages, for initial fragment load
- `loadBefore()` / `loadAfter()` — bidirectional pagination (scroll up/down)
- `findById()` — jump-to-message
- `selectOldestGlobal()` / `selectOldestForServer()` — oldest-first batches for quota enforcement
- `findIdByDedupeKey()` — deduplication fallback lookup

#### `MessageStorageRepository`
The application's only point of contact with `MessageDao`. Adds:
- **Insert with deduplication fallback**: if `insert()` returns -1 (conflict on dedupe_key), looks up the existing row's id and returns it, so the pipeline gets a valid `MessageId` regardless
- **Auto-cleanup**: every 500 inserts, checks global quota and trims oldest messages if over the 10% hysteresis threshold
- **Quota enforcement**: `enforceGlobalLimit()` and `enforceServerLimit()` batch-delete oldest rows by `aproxRowSize` until under quota
- **Async variants**: `loadOlderAsync()`, `loadNewerAsync()`, `loadRecentAsync()`, `loadNearAsync()` — all delegate to `AppAsyncExecutor.io()` to keep DB off the main thread

#### `ConversationStateEntity` / `ConversationStateDao` / `ConversationStateRepository`
Tracks per-channel read state: `lastReadId`, `firstUnreadId`, `lastNotifiedId`, `mutedUntilMs`. The DAO SQL is carefully written:
- `setFirstUnreadIfEmpty` uses `WHERE firstUnreadId = 0` — atomic compare-and-set without application-level locking
- `markRead` uses `CASE WHEN :lastReadId > lastReadId` — monotonic, safe to call multiple times

#### `MessageStorageHelper`
Serialization utilities for `MessageInfo` ↔ `MessageEntity`. Uses Gson JSON for `extraJson` (type-specific fields like new nick, kick target, mode entries) and a custom text format for `sender` (`"prefixes nick!user@host"`). These two formats coexist for historical reasons.

---

### 3.6 UI Layer — `chat/`

#### `ChatFragment`
The container fragment for one server connection. Manages:
- A `ViewPager` (or equivalent) of `ChatMessagesFragment` instances, one per channel plus a server status tab
- The toolbar and its title/subtitle (channel topic)
- The send message helper (`ChatFragmentSendMessageHelper`)
- Tab visibility (hidden during action mode)
- Channel switching by delegating to the pager adapter

Still has direct casts to `MainActivity` for action bar drawer toggle and toolbar access. This is the primary remaining UI coupling issue.

#### `ChatMessagesFragment`
The message list view for one channel. Responsibilities:
- Subscribing to `MessageBus` for live message delivery
- Loading message history from `MessageStorageRepository` (initial load, pagination on scroll)
- Managing the unread counter UI (badge, scroll-to-unread, dismiss)
- Managing message selection (long-press multi-select → action mode for copy/share/delete)
- Forwarding topic/member changes to `ChatFragment` via direct parent fragment cast

Subscribes to the bus in `onCreate()`, unsubscribes in `onDestroy()`. Receives `onMessage()` callbacks on whatever thread the bus emits from (the pipeline executor) and marshals to main thread via `RecyclerView.post()`.

#### `ChatMessagesAdapter`
`RecyclerView.Adapter` for the message list. Manages:
- Two lists: `mMessages` (append-end) and `mPrependedMessages` (prepend-top), merged in `getMessage(position)`
- Day separator injection between messages from different calendar days
- Stable IDs via a position-offset scheme (`mItemIdOffset`) that remains valid across prepend operations
- Multi-select state tracking across ViewHolder recycling
- "New messages" marker rendering (first unread message gets a different view type)

#### `ChatUIData` / `ChannelUIData`
Pure in-memory state for the send box. `ChatUIData` maps channel names to `ChannelUIData`. `ChannelUIData` holds the draft text and sent-message recall history (capped at 24). Cleaned up when the user leaves a channel (`onChannelLeft` listener in `attachToConnection()`).

---

### 3.7 Navigation — `app/navigation/`

#### `NavigationHost`
Single-method interface: `getNavigator()`. Implemented by `MainActivity`. Allows components that need to trigger navigation to depend on this interface rather than `MainActivity` directly.

#### `MainNavigator`
All fragment transaction logic lives here. Methods:
- `openServer(session, channel, messageId)` — reuses existing `ChatFragment` if same server, creates new one otherwise
- `openManageServers()` — replaces fragment with `ServerListFragment`
- `handleBackPressed()` — if currently in chat, goes to server list
- `handleIntent()` — processes launch intents (notification taps, IRC links)
- `onChannelSelected()` — called by drawer when user taps a channel
- `ensureValidConnection()` — called on connection removal to avoid showing a dead chat

#### `DrawerHelper`
Manages the navigation drawer. Implements `ServerConnectionManager.ConnectionsListener`, `ServerConnectionSession.InfoChangeListener`, `ServerConnectionSession.ChannelListChangeListener`, `NotificationManager.UnreadMessageCountCallback`, and `SharedPreferences.OnSharedPreferenceChangeListener`. All updates marshal to main thread via `mActivity.runOnUiThread()`.

Notable: implements swipe-to-close for DM conversations (swipe right on a direct message channel item to leave/close it). Uses `ItemTouchHelper` with type-checking to restrict swipe to non-`#` channels only.

---

### 3.8 Notification System — `NotificationManager`, `ChannelNotificationManager`

`NotificationManager` (app-level singleton, distinct from Android's system class) is the coordinator:
- `processMessage()` is called by `SessionInitializer`'s global bus subscriber for every non-playback message. It updates unread count, evaluates notification rules, and if a rule matches, triggers `ChannelNotificationManager.showNotification()`
- `findNotificationRule()` evaluates the three-tier rule chain: top defaults → user rules → bottom defaults
- `updateSummaryNotification()` builds/updates the group summary notification by iterating all connections and channels with pending notification messages
- `shouldMessageUseMentionFormatting()` — queried by the adapter to highlight messages that match a mention rule

`NotificationManager.ConnectionManager` (inner class) is a per-session container for per-channel `ChannelNotificationManager` instances and per-connection unread callbacks.

---

### 3.9 Infrastructure — `infrastructure/threading/`

#### `AppAsyncExecutor`
Static utility. Two thread pools:
- **`MAIN`**: Android main thread `Handler`
- **`IO`**: unbounded cached thread pool for background work

Patterns:
- `io(Runnable, Runnable)` — background work + optional UI callback
- `io(Supplier<T>, Consumer<T>)` — background computation + result delivery on main thread
- `ui(Runnable)` — post to main thread (no-op if already on main)

Used everywhere DB queries need to leave the main thread: `MessageStorageRepository` async variants, `DrawerHelper` updates, etc.

#### `DelayScheduler` / `HandlerDelayScheduler` / `SchedulerProvider`
Kotlin interface for schedulable delays, with a main-thread `Handler`-backed default implementation. Used by `ServerConnectionSession` for reconnect scheduling. `SchedulerProviderHolder` is a global holder that can be overridden in tests — this is the one proper dependency injection seam in the reconnect system.

---

## 4. Data Flow

### 4.1 Inbound Message — Socket to UI

```
[Network Thread]
IRCConnection.handleInput()
  reads line from BufferedReader
  → MessageHandler.handleLine()
      parses IRCv3 tags, prefix, command, params
      → CommandHandlerList.getHandlerFor(command)
          → MessageCommandHandler.handle()   [for PRIVMSG/NOTICE]
              resolveUser().get()             ← BLOCKING on network thread
              processCtcp()                  [if CTCP]
              → ChannelData.addMessage(Builder, tags)
                  → cap.processMessage()     [IRCv3 caps annotate builder]
                  → MessageFilterList.filterMessage()
                      → IgnoreListMessageFilter.filter()
                      → ZNCPlaybackMessageFilter.filter()
                          (may mark as playback, may query DB)
                  → MessageSink.accept(channelName, messageInfo)

[Pipeline Executor Thread — single-threaded]
DefaultMessagePipeline.accept()
  → DefaultMessagePipeline.persist()
      → MessageEntity.from(serverId, channel, messageInfo)
          computeDedupeKey() if playback
          serializeExtraData() via Gson
      → MessageStorageRepository.insertMessage(entity)
          dao.insert()
          if conflict on dedupe_key → dao.findIdByDedupeKey()
          every 500 inserts → considerAutoCleanup()
      returns RoomMessageId(rowId)
  → MessageBus.emit(channelName, messageInfo, messageId)
      copies listener lists (thread-safe)
      → ChatMessagesFragment.onMessage()    [per-channel subscriber]
      → SessionInitializer's lambda          [global subscriber]
          → NotificationManager.processMessage()

[Main Thread — via RecyclerView.post()]
ChatMessagesFragment.onMessage() continuation
  → ChatMessagesAdapter.appendMessage()
  → RecyclerView scrolls to bottom if near end
```

### 4.2 Outbound Message — User Input to Socket

```
[Main Thread]
User types in ChatAutoCompleteEditText
ChatFragmentSendMessageHelper.sendMessage()
  → ChatApi.sendMessage(channel, text, ...)
      → IRCConnection.sendMessage()
          → sendCommand("PRIVMSG channel :text")  [writes to socket]
          → sendMessageInternal()                  [local echo]
              constructs MessageInfo for self
              → ChannelData.addMessage()           [re-enters inbound path]
              [message persisted and delivered to UI via pipeline]
```

### 4.3 History Load — Fragment Initialization

```
[Main Thread]
ChatMessagesFragment.onCreate()
  → reloadMessages(nearMessageRoomId)
      → MessageStorageRepository.loadRecentAsync(serverId, channel, 100)
          [AppAsyncExecutor.IO thread]
          dao.loadRecent()
          toMessageListFromRoom()
              deserializeMessage() per entity
              sorts by Date
          [Main Thread via MAIN handler]
          → ChatMessagesAdapter.setMessages()
          → RecyclerView.scrollToPosition(last)

[On scroll to top — pagination]
ChatMessagesFragment scroll listener detects firstVisible < 10
  → MessageStorageRepository.loadOlderAsync(serverId, channel, firstId, 100)
      [IO thread] dao.loadBefore()
      [Main Thread]
      → ChatMessagesAdapter.addMessagesToTop()
```

### 4.4 Notification Flow

```
[Pipeline Executor Thread]
MessageBus.emit() → SessionInitializer global subscriber
  → NotificationManager.processMessage(context, session, channel, info, messageId)
      [Main Thread — called directly, NotificationManager does not post]
      → channelManager.addUnreadMessage(messageId)
      → findNotificationRule(session, channel, info)
          evaluates top defaults → user rules → bottom defaults
      → if rule matches and not suppressed:
          → channelManager.addNotificationMessage()
          → updateSummaryNotification()
          → channelManager.showNotification()
```

---

## 5. Key Design Patterns

### Pipeline / Chain of Responsibility
`MessageFilterList.filterMessage()` is a classic Chain of Responsibility. Each `MessageFilter` can drop a message by returning `false`. Filters are registered at session creation by `SessionInitializer`.

The message pipeline itself (Sink → Pipeline → Bus) is a linear processing pipeline where each stage has one job: accept, persist+assign-id, fan-out.

### Observer / Pub-Sub
Used pervasively:
- `MessageBus`: channel-scoped and global pub-sub for live messages
- `ServerConnectionManager.ConnectionsListener`: connection add/remove events
- `ServerConnectionSession.InfoChangeListener` / `ChannelListChangeListener`: per-session events
- `NotificationManager.UnreadMessageCountCallback`: unread badge updates
- `ChannelListListener` (chatlib): protocol-level channel join/part events

All listener lists use defensive copying before iteration. Most are guarded by `synchronized` blocks.

### Factory + Compositor
`ServerConnectionFactory` builds `ServerConnectionSession` from `ServerConfigData`. `SessionInitializer` composes the internal subsystems of the session. These two roles are correctly separated — the factory handles external configuration (SSL, SASL, nicks) while the initializer handles internal wiring (pipeline, bus, notifications).

### Repository
`MessageStorageRepository` and `ConversationStateRepository` follow the Repository pattern: they provide a domain-oriented API (load recent, load older, insert, enforce quota) that hides all Room DAO details. Callers never touch `MessageDao` or `ConversationStateDao` directly.

### Command pattern (IRC handlers)
`CommandHandler` is effectively a command object. `CommandHandlerList` maps command strings to handlers. New commands can be added without modifying the dispatch mechanism.

### Strategy
`ReconnectPolicy` encapsulates the reconnect delay algorithm. `NickPrefixParser` is a strategy for parsing nick prefixes (pluggable per server type). `MessageFilter` is a strategy for message filtering.

### Deduplication via content-addressed key
`MessageEntity.computeDedupeKey()` hashes `serverId|channel|sender|text|timeBucket` to produce a stable, content-derived identity for playback messages. Combined with a unique partial index (`WHERE dedupe_key IS NOT NULL`) and `OnConflictStrategy.IGNORE`, this provides idempotent inserts without application-level locking.

---

## 6. Threading Model

| Thread | What runs here |
|--------|---------------|
| **Main (UI)** | All UI work, adapter updates, navigation, notification posting |
| **Network (per connection)** | `IRCConnection.handleInput()` read loop, all command handlers, `ChannelData.addMessage()`, `MessageSink.accept()` call |
| **Pipeline Executor** | Single-threaded executor per session. DB persistence, `MessageBus.emit()`, notification processing |
| **AppAsyncExecutor.IO** | Cached pool. History loads, async DB reads, background saves |
| **Room internal** | Room uses its own executor for async queries (not used here — all Room calls are synchronous, called from appropriate non-main threads) |

**Critical notes:**
- `MessageCommandHandler` calls `resolveUser().get()` — a blocking `Future` — on the network thread. This is the main source of potential latency on the inbound path.
- `MessageBus.emit()` runs on the pipeline executor. Subscribers must not block. `ChatMessagesFragment.onMessage()` correctly posts to the main thread via `RecyclerView.post()`.
- `MessageStorageRepository.insertMessage()` is synchronized on `maintenanceLock` to protect insert + auto-cleanup atomicity. All callers are already off the main thread.

---

## 7. Component Interaction Map

```
IRCApplication
    └─ starts ──────────────────────────────→ IRCService
    └─ owns activity list ──────────────────→ [all Activities]

IRCService
    └─ creates/references ──────────────────→ ServerConnectionManager
    └─ listens to ──────────────────────────→ ConnectivityManager (system)

ServerConnectionManager
    └─ creates via ─────────────────────────→ ServerConnectionFactory
    └─ owns list of ────────────────────────→ ServerConnectionSession[]
    └─ notifies ────────────────────────────→ ConnectionsListener[] (DrawerHelper, ServerListAdapter)

ServerConnectionFactory
    └─ creates ─────────────────────────────→ ServerConnectionSession
    └─ creates ─────────────────────────────→ SessionInitializer

ServerConnectionSession
    └─ calls on connect ────────────────────→ SessionInitializer.attach(IRCConnection)
    └─ owns ────────────────────────────────→ ChatUIData
    └─ owns ────────────────────────────────→ NotificationManager.ConnectionManager

SessionInitializer
    └─ creates and wires ───────────────────→ DefaultMessagePipeline
    └─ creates and wires ───────────────────→ DefaultMessageBus
    └─ injects into ────────────────────────→ ServerConnectionData (sink + bus)
    └─ registers ───────────────────────────→ bus global subscriber → NotificationManager

IRCConnection (ServerConnectionApi)
    └─ reads from ──────────────────────────→ TCP/TLS socket
    └─ owns ────────────────────────────────→ ServerConnectionData
    └─ dispatches to ───────────────────────→ MessageHandler → CommandHandlerList

CommandHandlers
    └─ mutate ──────────────────────────────→ ServerConnectionData
    └─ call ────────────────────────────────→ ChannelData.addMessage()

ChannelData.addMessage()
    └─ calls ───────────────────────────────→ MessageFilterList
    └─ calls ───────────────────────────────→ MessageSink.accept()  ← PIPELINE BOUNDARY

DefaultMessagePipeline
    └─ calls ───────────────────────────────→ MessageStorageRepository.insertMessage()
    └─ calls ───────────────────────────────→ MessageBus.emit()

MessageBus.emit()
    └─ notifies ────────────────────────────→ ChatMessagesFragment (per-channel)
    └─ notifies ────────────────────────────→ NotificationManager.processMessage()

ChatFragment
    └─ hosts ───────────────────────────────→ ChatMessagesFragment[] (one per channel)
    └─ uses ────────────────────────────────→ ChatFragmentSendMessageHelper

ChatMessagesFragment
    └─ subscribes to ───────────────────────→ MessageBus
    └─ loads history from ──────────────────→ MessageStorageRepository
    └─ renders via ─────────────────────────→ ChatMessagesAdapter

MainActivity
    └─ owns ────────────────────────────────→ MainNavigator
    └─ owns ────────────────────────────────→ DrawerHelper
    └─ implements ──────────────────────────→ NavigationHost
```

---

## 8. Storage Layer

### Schema

**`messages_logs`**
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK autoincrement | Used as `RoomMessageId` |
| serverId | TEXT (UUID) | Indexed |
| channel | TEXT | Indexed with serverId+id |
| kind | TEXT | CHANNEL or PRIVATE |
| timestamp | INTEGER | Unix ms |
| type | INTEGER | `MessageInfo.MessageType.asInt()` |
| text | TEXT | Message body |
| sender | TEXT | Serialized nick!user@host with prefixes |
| extra_json | TEXT | Type-specific fields (Gson) |
| dedupe_key | TEXT NULLABLE | SHA-1 for playback; unique partial index |
| aprox_row_size | INTEGER | Estimated UTF-8 byte size for quota |

**`conversation_state`**
| Column | Type | Notes |
|--------|------|-------|
| serverId | TEXT (UUID) | Composite PK |
| channel | TEXT | Composite PK |
| lastReadId | INTEGER | Highest read message id |
| firstUnreadId | INTEGER | First unread id (0 = none) |
| lastNotifiedId | INTEGER | Highest notified id |
| mutedUntilMs | INTEGER | Future: mute until timestamp |

### Quota / Auto-cleanup Algorithm
1. Every 500 inserts, `considerAutoCleanup()` queries `SUM(aprox_row_size)` globally
2. If usage > limit × 1.10 (10% hysteresis), calls `enforceGlobalLimit()`
3. `enforceGlobalLimit()` queries oldest rows in batches of 500 (`selectOldestGlobal`), accumulates their sizes, and deletes each batch until the target freed bytes is reached
4. Per-server limits work identically but scoped to `serverId`

The `aprox_row_size` column makes this O(scan) rather than O(recount), since sizes are pre-computed at insert and stored.

---

## 9. What Is Refactored vs Legacy

### Fully Refactored ✓
- `message/` — `MessageSink`, `MessagePipeline`, `MessageBus` and their default implementations
- `connection/` — `ServerConnectionSession`, `ServerConnectionFactory`, `SessionInitializer`, `ServerConnectionRegistry`
- `storage/` — All Room entities, DAOs, repositories, `MessageStorageHelper`
- `infrastructure/threading/` — `AppAsyncExecutor`, `DelayScheduler`, `SchedulerProvider`
- `app/navigation/` — `MainNavigator`, `NavigationHost`
- `IRCService` — now a `LifecycleService`, network callback, clean start/stop
- `ChatUIData` / `ChannelUIData` — clean, scoped, no side effects

### Partially Refactored ⚠
- `chatlib/irc/ChannelData` — clean `addMessage()` boundary, but still lives in protocol package and holds protocol + delivery concerns together
- `chat/ChatFragment` — uses `NavigationHost` in some paths, still casts to `MainActivity` in others
- `chat/ChatMessagesFragment` — good bus subscription pattern, but accesses bus via 3-level cast through session, and casts to `MainActivity` for action mode
- `chatlib/irc/ServerConnectionData` — holds `MessageSink` and `MessageBus` (pipeline concerns) alongside pure protocol state
- `NotificationManager` — functional but structured as a flat singleton with no clear domain boundary

### Not Yet Addressed (Legacy) ✗
- `conversation/` — empty; the domain/conversation layer does not exist yet
- `model/` — empty; no domain models separate from protocol DTOs
- `notification/` — empty; notification logic lives directly in `NotificationManager` singleton
- `platform/` — empty; no platform abstractions
- `ui/` — empty; no shared UI component library
- `chatlib/irc/handlers/MessageCommandHandler` — blocking `resolveUser().get()` on network thread
- Full `chatlib/` package — designed as a standalone library but still deeply entangled with app-level concerns (`DCCManager`, `NotificationManager` references flow through it)
