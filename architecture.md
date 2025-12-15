# Target architecture (messaging / IRC client)

## Goal
Organize code by domain boundaries so protocol, conversation logic, storage, notifications, and UI evolve independently.

## Top-level packages (under io.mrarm.irc)

- app/            Application wiring + Activities only (no business logic)
- connection/     IRC protocol + connection lifecycle (emits events)
- conversation/   Channels/privates + routing/state (decides where messages belong)
- message/        Message model + formatting + pipeline (UI-agnostic)
- storage/        Room + repositories + cleanup (persistence boundary)
- notification/   Unread + rules + dispatch (single source of truth)
- ui/             Fragments/adapters/dialogs (presentation only)
- infrastructure/ Threading, jobs, logging, upnp (boring glue)
- platform/       Android-specific helpers (theme/resources/permissions)
- legacy/         Quarantined old code; no new code here

## Dependency rules (must hold)
1) connection -> emits events only; must not depend on ui or storage
2) conversation -> must not depend on ui or Room entities/DAOs
3) storage -> must not depend on ui or protocol parsing
4) ui -> must not parse IRC protocol or compute domain rules
5) notification -> must not be split across unrelated packages

## Migration principle
Prefer moving one vertical slice at a time (one feature) over large renames.
