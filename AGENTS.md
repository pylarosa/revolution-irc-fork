# AGENTS.md

## Project context
This is an Android IRC client undergoing a large refactor.
Legacy implementations are being removed in favor of a unified Room-based database.

Assume:
- Room is the single source of truth for persistence
- Legacy SQLite / file-based / per-channel DB code is deprecated unless explicitly referenced

## Architectural rules
- Do NOT reintroduce legacy storage paths
- Prefer deletion over adaptation of unused code
- Avoid adding abstractions unless strictly necessary
- No new AsyncTask usage (executor / coroutines only)

## Review focus
When reviewing changes, prioritize:
1. Dead code that can be deleted
2. Incorrect lifecycle or threading assumptions
3. Look for potential memory leaks
4. Room misuse (multiple instances, init races, WAL misuse)
5. Inconsistent responsibility boundaries

## What NOT to do
- Do not suggest large rewrites unless explicitly requested
- Do not “future-proof” with speculative abstractions
- Do not keep legacy code “just in case”

## Style
- Be direct and critical
- Prefer small diffs
- Flag uncertainty explicitly instead of guessing
