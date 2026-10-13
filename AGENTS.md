# EasyMoney Agent Rules

## Purpose

This file provides repository-level instructions for AI coding agents and
developers working on EasyMoney.

## Android Architecture

- Compose communicates with ViewModels through intents and callbacks.
- ViewModels expose immutable StateFlow-based UI state.
- SharedFlow is used for transient UI events such as snackbars and navigation.
- Repositories abstract local and remote data sources.
- Room stores persistent structured data.
- DataStore stores small user preferences.
- Hilt provides dependencies.
- Business logic must not be placed inside composables.
- GlobalScope must not be used.

## Backend Architecture

- The Android application communicates with the EasyMoney backend through
  repository abstractions.
- LLM provider secrets must remain on the backend.
- Backend routes should delegate business logic to services.
- Provider-specific code should be isolated from domain logic.
- Requests and responses must be validated.

## AI Rules

- AI responses must use supplied evidence only.
- AI responses must not invent market events, numbers, news, earnings, or
  analyst activity.
- AI output must be validated before being returned to Android.
- Invalid structured output must not be displayed.
- When evidence is insufficient, the response must clearly say so.
- AI output must be presented as informational content, not individualized
  financial advice.
- Prompt or model changes must include updated evaluation cases.

## Security Rules

- Do not place secrets or provider keys in Android source code.
- Do not commit environment files containing real secrets.
- Validate input before sending it to an LLM.
- Validate structured output before returning it to Android.
- Do not log secret values or sensitive user information.

## Testing Requirements

- Add or update tests for behavior changes.
- Run affected Android unit tests.
- Run relevant Android instrumented tests.
- Run backend tests for changed services.
- Run AI evaluations when prompts, models, evidence rules, or output schemas
  change.

## Before Completing a Task

1. Review the existing architecture.
2. Make the smallest focused change.
3. Build affected modules.
4. Run relevant tests.
5. Inspect the generated diff.
6. Review error handling.
7. Review security and secret management.
8. Update documentation when behavior or architecture changes.

