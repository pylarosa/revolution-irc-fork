# IRC Android Client — Refactoring Strategy

> This document describes the full-picture modernization path.  
> It does not cover small bug fixes. It covers the structural moves  
> that will transform the architecture into a clean, testable,  
> maintainable modern Android codebase.

---

## Guiding Principle

The refactoring already in progress follows the right instinct: **decouple from the outside in**. Infrastructure (network, DB, delivery bus) was decoupled first. The next phase decouples the semantic layers (domain, UI state, presentation). The final phase introduces the full modern Android architecture stack.

Every phase below produces a **working app at its end**. No phase leaves the app in a broken intermediate state.

---

## The Four Phases

```
Phase 1 — Domain Layer          [conversation/, model/]
Phase 2 — UI Decoupling         [Fragment → ViewModel]
Phase 3 — chatlib Isolation     [protocol library boundary]
Phase 4 — Platform Hardening    [DI, testing, platform/]
```

---

## Phase 1 — Introduce the Domain / Conversation Layer

### Why this is first

Right now, `ChannelData` in `chatlib/irc/` is the convergence point for four different concerns: IRC protocol state, IRCv3 capability processing, message filtering, and pipeline delivery. It lives in the protocol package but it's not a protocol object — it's an application object that happens to know about IRC. This is the root cause of the most significant coupling in the system.

The empty `conversation/` and `model/` packages are placeholders for exactly what needs to go here.

### What to build

#### `model/` — Pure domain objects

Create a set of application-level domain objects that are completely independent of IRC wire protocol:

```
model/
├── Conversation.java        (id, type: CHANNEL/DM, displayName, topic)
├── ConversationMessage.java (id, senderId, text, type, timestamp, isPlayback)
├── ConversationParticipant.java (displayName, prefixes, isOnline)
└── ServerIdentity.java      (uuid, displayName, nickAtServer)
```

These are **not** `MessageInfo`, `ChannelData`, or any chatlib DTO. They have no knowledge of IRC prefixes, capability tags, or nick resolution UUIDs. They are what the UI should reason about. `MessageInfo` is a protocol object — it belongs in chatlib. `ConversationMessage` is what your app actually works with.

#### `conversation/` — Conversation repository and state

```
conversation/
├── ConversationRepository.java      (interface)
├── ConversationRepositoryImpl.java  (implementation)
├── ConversationSession.java         (live state for one open conversation)
└── ConversationEvent.java           (sealed class: MessageArrived, TopicChanged, MemberJoined, ...)
```

`ConversationRepository` becomes the single source of truth for conversation data. It wraps both the `MessageBus` (for live events) and `MessageStorageRepository` (for history). The `ChatMessagesFragment` should eventually talk to `ConversationRepository`, not to both sources separately.

`ConversationSession` replaces the role that `ChannelData` plays for the UI: it holds current member list, topic, and the live message stream. It is created and owned by `ServerConnectionSession`, not by the protocol layer.

### How the transition works

The key move is to **translate at the `MessageSink` boundary**. Instead of `ChannelData` calling `MessageSink.accept()` with a raw `MessageInfo`, introduce a thin translation step:

```
ChannelData.addMessage()
  → MessageSink.accept(channelName, messageInfo)     [existing, unchanged]
      ↓
  DefaultMessagePipeline
      → persist (unchanged)
      → translate MessageInfo → ConversationMessage
      → MessageBus.emit() with ConversationMessage    [or emit both]
```

This keeps the entire `chatlib` protocol layer untouched while the domain objects are introduced above the pipeline boundary.

### Outcome

After Phase 1:
- The UI talks to `ConversationRepository` and sees `ConversationMessage`, never raw IRC DTOs
- `ChannelData` is reduced to pure protocol state (members, modes, topic) — it no longer has any delivery responsibility
- The `conversation/` and `model/` packages are populated
- `chatlib/` is one step closer to being a true standalone library

---

## Phase 2 — UI Decoupling: Fragment → ViewModel

### Why this follows Phase 1

Phase 1 creates the `ConversationRepository` and `ConversationSession` that a `ViewModel` needs to observe. Without a clean data source for the ViewModel to wrap, the ViewModel would just replicate the same tangled dependencies. With Phase 1 done, the ViewModel has a clean API to program against.

### The core problem

`ChatMessagesFragment` currently does six different jobs:
1. Subscribes to `MessageBus` for live messages
2. Loads history from `MessageStorageRepository`
3. Manages scroll position and pagination
4. Tracks and renders the unread counter
5. Manages message multi-select state
6. Controls action mode (copy/share/delete)

`ChatFragment` directly casts to `MainActivity` for toolbar and drawer access.

`ChatMessagesAdapter` holds a reference to `ChatMessagesFragment` and calls back into it for action mode start/stop.

This is all normal for legacy Android code. The modern solution is ViewModels + state objects + callback interfaces.

### What to build

#### `ChatMessagesViewModel`

```java
class ChatMessagesViewModel extends ViewModel {
    // State — observed by fragment
    LiveData<List<ConversationMessage>> messages;
    LiveData<UnreadState> unreadState;
    LiveData<Boolean> isLoadingMore;

    // Actions — called by fragment
    void loadInitial(UUID serverId, String channel);
    void loadOlder();
    void loadNewer();
    void markRead(MessageId upTo);
    void deleteMessages(List<MessageId> ids);
}
```

The ViewModel owns the `ConversationRepository` subscription, the pagination state, and the unread tracking. The fragment becomes a pure rendering component: it observes state, binds to RecyclerView, and forwards user gestures to the ViewModel.

#### Callback interfaces to replace casts

Replace every `((MainActivity) getActivity()).method()` with a narrow interface:

```java
// Instead of casting to MainActivity for action mode:
interface MessageSelectionHost {
    ActionMode startMessageSelection(ActionMode.Callback callback);
}

// Instead of casting to ChatFragment for tab visibility:
interface TabVisibilityController {
    void setTabsHidden(boolean hidden);
}

// Instead of casting to ChatFragment for send helper:
interface SendMessageController {
    void setCurrentChannel(String channelName);
}
```

Fragments receive these via their host (activity/parent fragment implements the interface) or via a shared ViewModel scoped to the parent fragment.

#### Replace `ChatMessagesAdapter → ChatMessagesFragment` coupling

The adapter currently calls `mFragment.showMessagesActionMenu()`. Replace this with:

```java
// Adapter exposes events:
interface SelectionEventListener {
    void onSelectionStarted();
    void onSelectionCleared();
}
```
adapter.setSelectionEventListener(listener);

The fragment implements the listener and owns the action mode.

### Navigation hardening

`MainNavigator` already exists and is good. Complete its adoption:
- All fragment transactions go through `MainNavigator` — no fragment directly calls `fragmentManager.beginTransaction()`
- All back press handling goes through `MainNavigator.handleBackPressed()`
- `ChatFragment` gets its navigation needs expressed via `NavigationHost` rather than casting

### Outcome

After Phase 2:
- No `((MainActivity) getActivity())` casts anywhere in fragments
- No `((ChatFragment) getParentFragment())` casts in `ChatMessagesFragment`
- `ChatMessagesAdapter` has no reference to any fragment
- All UI state lives in ViewModels and survives rotation/recreation
- Every UI component is independently unit-testable

---

## Phase 3 — chatlib Isolation: Make the Protocol Library a Real Library

### The problem

`chatlib/` was designed as a standalone IRC library but it has accumulated app-level dependencies:
- `MessageCommandHandler` directly uses `DCCManager.getInstance(context)` (app singleton)
- `ChannelData` holds a `MessageSink` reference (app infrastructure)
- `ServerConnectionData` holds `MessageBus` and `MessageStorageRepository` (app infrastructure)
- Command handlers reference `ServerConnectionApi` (semi-internal to the library)

This means you cannot reuse, test, or evolve `chatlib/` independently of the app.

### What to do

#### Remove app infrastructure from `ServerConnectionData`

`ServerConnectionData` should contain only pure protocol state:

**Remove from `ServerConnectionData`:**
- `MessageSink messageSink`
- `MessageBus messageBus`
- `MessageStorageRepository messageStorageRepository`

These belong in `ServerConnectionSession` or in a new `SessionContext` object that lives in the `connection/` package.

The `ChannelData.addMessage()` pipeline boundary moves: instead of `ChannelData` reaching into `connection.getMessageSink()`, the `SessionInitializer` registers a `CommandHandler` (or listener) that intercepts `ChannelData` message events and feeds them to the pipeline. This keeps the flow but severs the dependency.

#### Remove DCC and notification coupling from command handlers

`MessageCommandHandler` currently receives `DCCServerManager` and `DCCClientManager` via setters — this is a reasonable injection pattern but the managers themselves are app singletons. The injection is fine; make the managers implement interfaces defined in `chatlib` rather than concrete app classes.

#### Define a clean `chatlib` API surface

After cleanup, `chatlib/` should depend on nothing outside itself except:
- Android SDK basics (no `Context` beyond what's needed for logging)
- The `chatlib/dto/` value objects
- Java standard library

Everything app-specific comes in via injection at `SessionInitializer.attach()` time.

#### Extract `chatlib` as a Gradle module (optional but recommended)

Once the dependencies are clean, move `chatlib/` into a separate Gradle `:chatlib` module. The compile boundary enforces the separation — if app code sneaks back into the library, the build breaks.

### Outcome

After Phase 3:
- `chatlib/` is a pure IRC protocol library with no app dependencies
- It can be unit-tested without Android instrumentation
- `ServerConnectionData` contains only IRC protocol state
- The pipeline boundary (`MessageSink.accept()`) is the only point where app infrastructure meets protocol

---

## Phase 4 — Platform Hardening: DI, Testing, Platform Abstractions

### Dependency Injection

The current codebase uses singletons for most shared state: `ServerConnectionManager.getInstance()`, `NotificationManager.getInstance()`, `MessageStorageRepository.getInstance()`, `ChatLogDatabase.getInstance()`. These work, but they make testing impossible and lifecycle management manual.

Introduce **Hilt** (or Dagger) for dependency injection:

```
@Module AppModule
    provides → MessageStorageRepository (singleton scoped)
    provides → ConversationRepository
    provides → ServerConnectionManager

@Module ConnectionModule
    provides → ServerConnectionFactory
    provides → SessionInitializer
    provides → DelayScheduler (from SchedulerProviderHolder)

@ViewModelModule
    provides → ChatMessagesViewModel
    provides → ServerListViewModel
```

With Hilt, ViewModels receive their dependencies through constructor injection. This removes all `getInstance()` calls from Fragment and Activity code. It also makes it trivial to swap implementations in tests.

`SchedulerProviderHolder.override()` is the existing test seam for scheduling — Hilt would replace this pattern with a proper binding.

### The `platform/` and `notification/` packages

These empty packages represent the final decoupling:

**`platform/`** should contain abstractions over Android system APIs:
```
platform/
├── NetworkMonitor.java          (wraps ConnectivityManager)
├── NotificationPoster.java      (wraps NotificationManagerCompat)
└── AppStorageProvider.java      (wraps file paths, DB location)
```

This makes the app layer testable without mocking Android system APIs.

**`notification/`** should contain the notification domain logic, extracted from the current `NotificationManager` singleton:
```
notification/
├── NotificationPolicy.java      (rule evaluation — pure logic, no Android)
├── NotificationDispatcher.java  (Android notification posting)
└── UnreadTracker.java           (unread count logic, drives ConversationState)
```

Splitting rule evaluation (pure logic) from notification posting (Android API) makes the rules unit-testable.

### Testing Strategy

With Phases 1–4 complete, the testing pyramid becomes achievable:

**Unit tests (JVM, no Android)**
- `ConversationRepository` logic
- `NotificationPolicy` rule evaluation
- `MessageEntity` deduplication key computation
- `ReconnectPolicy` delay algorithm
- All `chatlib/` command handlers (once isolated)

**Integration tests (JVM + Room in-memory)**
- `MessageStorageRepository` quota enforcement
- `ConversationStateRepository` read/unread transitions
- `DefaultMessagePipeline` persist-then-emit ordering

**UI tests (Android instrumentation)**
- `ChatMessagesFragment` renders messages from ViewModel state
- Navigation transitions via `MainNavigator`
- Notification tap → correct channel opens

### Kotlin Migration

The codebase is predominantly Java with Kotlin introduced in `IRCService` and `SchedulerProvider`. The migration direction is clear:

1. New code in `conversation/`, `model/`, `notification/`, `platform/` should be written in Kotlin from the start
2. ViewModels (Phase 2) should be Kotlin with `StateFlow`/`SharedFlow` instead of `LiveData`
3. The `MessageBus` subscriber pattern can be replaced with `Flow` where appropriate
4. Legacy Java classes migrate opportunistically — when a class is being significantly modified anyway, migrate it

Do not do a mass mechanical translation. Migrate file by file when there's a functional reason to touch the class.

---

## Execution Order Summary

```
Phase 1: Domain Layer
    1a. Define model/ objects (ConversationMessage, Conversation, etc.)
    1b. Build ConversationRepository interface + implementation
    1c. Build ConversationSession (live state per connection)
    1d. Wire translation at MessagePipeline boundary
    1e. Move ChannelData back toward pure protocol state

Phase 2: UI Decoupling
    2a. Define narrow callback interfaces (replace MainActivity casts)
    2b. Implement ChatMessagesViewModel
    2c. Refactor ChatMessagesFragment to observe ViewModel
    2d. Decouple ChatMessagesAdapter from fragment
    2e. Complete MainNavigator adoption (all fragment transactions)
    2f. Implement ChatFragment ViewModel for toolbar state

Phase 3: chatlib Isolation
    3a. Remove MessageSink/MessageBus/Repository from ServerConnectionData
    3b. Define SessionContext in connection/ to carry pipeline refs
    3c. Remove DCC/notification concrete types from chatlib handlers
    3d. Enforce boundary — optionally extract as Gradle module

Phase 4: Platform Hardening
    4a. Introduce Hilt, replace getInstance() singletons
    4b. Populate platform/ with system API abstractions
    4c. Populate notification/ with extracted domain logic
    4d. Write unit tests for domain and storage layers
    4e. Write UI tests for critical user flows
    4f. Migrate new code to Kotlin; opportunistic migration of modified files
```

---

## What Does Not Need to Change

Some parts of the codebase are already in good shape and should be left alone except for normal maintenance:

- `message/` pipeline — architecture is correct, no structural change needed
- `storage/db/` — schema design and DAO are solid
- `MessageStorageRepository` quota/cleanup — correct algorithm, good implementation
- `connection/ServerConnectionFactory` — does exactly one job, does it well
- `connection/SessionInitializer` — the compositor pattern is right; only needs updating as dependencies are injected via Hilt
- `infrastructure/threading/AppAsyncExecutor` — will be partially replaced by coroutines/Flow in Kotlin migration, but not urgently
- `app/navigation/MainNavigator` — good; just needs complete adoption across all fragments
- `ChatUIData` / `ChannelUIData` — correct scope and behavior, no changes needed
- `IRCService` — already a `LifecycleService` with clean responsibilities
- `DrawerHelper` — functional, well-scoped; can be improved later with ViewModel but not a priority

---

## The Destination Architecture

When all four phases are complete, the architecture looks like this:

```
┌─────────────────────────────────────────────┐
│                UI Layer                      │
│  Fragment (rendering only)                   │
│  ViewModel (UI state + user intent)          │
│  NavigationHost / MainNavigator              │
└────────────────────┬────────────────────────┘
                     │ observes StateFlow/LiveData
┌────────────────────▼────────────────────────┐
│           Domain / Conversation Layer        │
│  ConversationRepository                      │
│  ConversationSession                         │
│  ConversationMessage / model/                │
│  NotificationPolicy                          │
└────────────┬────────────────────────────────┘
             │ reads/writes
┌────────────▼──────────────┐  ┌──────────────────────────┐
│   Storage Layer            │  │  Delivery Layer           │
│  MessageStorageRepository  │  │  MessagePipeline          │
│  ConversationStateRepo     │  │  MessageBus               │
│  Room DB                   │  │  (pipeline executor)      │
└────────────────────────────┘  └──────────────────────────┘
                                          ▲
┌─────────────────────────────────────────┴────────────────┐
│          Protocol Layer  (:chatlib module)                │
│  IRCConnection · MessageHandler · CommandHandlers         │
│  ChannelData (protocol state only)                        │
│  CapabilityManager · MessageFilterList                    │
└──────────────────────────────────────────────────────────┘
```

Clean vertical slices. Each layer depends only on the layer below it. No horizontal dependencies between layers. No layer knows about Android UI (except the UI layer). No layer knows about IRC wire format (except the protocol layer). The domain layer is the translation zone between the two worlds, and it is where the application's business logic lives.
