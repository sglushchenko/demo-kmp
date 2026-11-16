# KMP iOS Integration Demo

A **test project that demonstrates how to integrate Kotlin Multiplatform (KMP) into an iOS app**.
It is a small Rick & Morty browser: the networking, business logic, error handling and state all live in a shared
Kotlin module, and the iOS app (SwiftUI) simply renders that state. A second tab renders the *same* state with
Compose Multiplatform to compare both UI approaches.

The goal is to show the real process and its pitfalls, not to be a production app.

## What it demonstrates

- A shared Kotlin module consumed by a native iOS app as a framework.
- **Two interop variants of the same app**, so you can compare them side by side:
  - `iosApp-Plain`: plain Kotlin/Native to Objective-C export, no extra tooling.
  - `iosApp-SKIE`: identical Kotlin code built with [SKIE](https://skie.touchlab.co/).
- Clean Architecture inside the shared module (domain / data / presentation).
- Typed error handling for every endpoint, covered by unit tests.
- Data that is independent of screens: one repository owns the data and several screens observe it.
- Common libraries: Ktor, kotlinx.serialization, coroutines and `StateFlow`, kotlinx-datetime, `kotlin.uuid.Uuid`.
- `expect`/`actual`, Kotlin enums and sealed types crossing into Swift, and Compose Multiplatform inside SwiftUI.

## Architecture

```
iOS app (SwiftUI)                    Compose Multiplatform tab
        \                                   /
         \--------- presentation -----------/      ViewModels + UI models (what the UI needs)
                         |
                       domain                      entities, DomainError, Outcome, use cases,
                         |                         repository interface (no framework imports)
                        data                       Ktor API, DTOs, mappers, error mapping,
                                                   repository implementation + in-memory cache
```

Dependencies point inwards: `presentation -> domain <- data`. The domain layer knows nothing about Ktor, JSON
or UI. The data layer is `internal`, so it never leaks into the Swift API.

```
shared/src/commonMain/kotlin/pet/shared/
├── domain/
│   ├── model/        Character, CharactersSnapshot, enums
│   ├── error/        DomainError (sealed), Outcome<T>
│   ├── repository/   CharacterRepository (interface)
│   └── usecase/      Observe / LoadNextPage / Refresh / GetCharacter
├── data/
│   ├── remote/       CharacterApi, DTOs, HttpClient setup, safeApiCall (exception -> DomainError)
│   ├── mapper/       DTO -> domain
│   └── repository/   CharacterRepositoryImpl (single source of truth)
├── presentation/
│   ├── characters/   CharactersViewModel + UI state
│   ├── detail/       CharacterDetailViewModel + UI state
│   ├── model/        CharacterUi, ErrorUi and mappers (display strings are prepared here)
│   └── compose/      Compose Multiplatform screen
├── di/               AppContainer (manual DI, the single entry point for Swift)
└── platform/         expect/actual example
```

Layers are packages inside one Gradle module on purpose: iOS consumes a single framework. Splitting into
`:domain`, `:data` and `:presentation` modules is possible, but every module that must be visible from Swift then
has to be re-exported from the umbrella framework.

### Data independent of screens

`CharacterRepositoryImpl` is a singleton that holds loaded characters in a `StateFlow`. ViewModels are thin and
own no data. Consequences you can see in the app:

- Open a character detail screen and it is served from the loaded list, with no extra request.
- Switch to the Compose tab and the list is already there; it does not reload.
- Recreating a ViewModel (or a whole screen) never loses data.

### Error handling

Every endpoint goes through one path: `safeApiCall` converts any exception into a typed `DomainError`, and the
repository returns `Outcome<T>` instead of throwing (exceptions are awkward to consume from Swift).

| Situation | DomainError | UI behaviour |
|---|---|---|
| No network, DNS or TLS failure | `NoConnection` | message + Retry |
| Timeout | `Timeout` | message + Retry |
| HTTP 404 | `NotFound` | message, no retry |
| Other HTTP 4xx | `Client(code)` | message, no retry |
| HTTP 5xx | `Server(code)` | message + Retry |
| Undecodable body | `InvalidResponse` | message, no retry |
| Anything else | `Unknown` | message + Retry |

Details worth noting:

- Every request carries a generated `X-Request-Id` (`kotlin.uuid.Uuid`). The id is part of each `DomainError` and
  shown on error screens, so a failure is traceable.
- A 404 for a page past the end of the list is treated as "no more data", not as an error.
- A failed "next page" keeps the loaded list on screen and shows a non-blocking error; a failed refresh keeps old data.
- Unknown enum values from the server fall back to `UNKNOWN` instead of failing the whole response.
- Cancellation is always rethrown so coroutine cancellation keeps working.

Endpoints: `GET /character?page={n}` and `GET /character/{id}` from <https://rickandmortyapi.com>.

## Running it

Requirements: macOS, Xcode, a JDK (17 or newer). Android Studio is optional; it is convenient for editing Kotlin.

```bash
open iosApp/iosApp.xcodeproj
```

Pick the `iosApp-Plain` or `iosApp-SKIE` scheme and run on a simulator. Xcode calls Gradle through a Run Script
phase (`embedAndSignAppleFrameworkForXcode`), which builds the Kotlin framework first. The first build is slow
because Kotlin/Native downloads its toolchain into `~/.konan`.

The Xcode project is generated from `iosApp/project.yml` with [XcodeGen](https://github.com/yonaskolb/XcodeGen).
If you change `project.yml`, run `xcodegen generate` inside `iosApp/`.

### Tests

```bash
./gradlew :shared:iosSimulatorArm64Test
```

The suite runs on the iOS simulator and covers every error type for both endpoints, repository behaviour
(pagination, de-duplication, refresh, caching, overlapping loads) and ViewModel states. HTTP is mocked with Ktor's
`MockEngine`; ViewModels are tested against a hand-written fake repository.

### Switching variants

The variant is a Gradle property (`-Pskie=true|false`) set by each target's Run Script. Each variant builds into its
own directory (`shared/build` and `shared/build-skie`); sharing one directory leaves stale files from the other
variant and breaks the Xcode build.

## Where to look to understand the interop

| Topic | File |
|---|---|
| How Kotlin becomes a framework (targets, framework name, SKIE switch) | `shared/build.gradle.kts` |
| What Swift actually sees | `shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework/Headers/Shared.h` (after a build) |
| Interop without SKIE | `iosApp/Plain/*.swift` |
| Interop with SKIE | `iosApp/Skie/*.swift` |
| How Xcode invokes Gradle | `iosApp/project.yml` (`preBuildScripts`) |
| Compose inside SwiftUI | `iosApp/Common/ComposeTab.swift`, `shared/src/iosMain/.../MainViewController.kt` |

## Pitfalls found along the way

Interop (Objective-C export):

- Kotlin default arguments are not visible from Swift. `AppContainer` exposes explicit factory functions instead.
- `data class.copy()` becomes `doCopy()`; `Int` becomes `Int32`; `Long` becomes `Int64`; a `Boolean` inside a
  generic becomes a boxed Kotlin wrapper type; `kotlin.time.Instant` becomes an opaque Kotlin type, so the UI model exposes epoch
  milliseconds instead.
- A Kotlin `enum` is a class in Swift; a `sealed` hierarchy is a protocol plus classes, so there is no exhaustive
  `switch`. With SKIE both become real Swift enums.
- A `Flow` arrives without its element type. Without SKIE you implement `FlowCollector` yourself and cast `Any?`
  (see `iosApp/Plain/FlowCollector.swift`); with SKIE it is an `AsyncSequence`.
- Top-level Kotlin functions are exposed as static methods of a class named after the file plus `Kt`
  (`MainViewController.kt` becomes `MainViewControllerKt`). Renaming such a file breaks Swift.
- Types from other Kotlin modules are visible to Swift only if re-exported with `export(...)`.
- Kotlin code cannot be debugged from Xcode.

Lifecycle and build:

- Kotlin never cancels anything for you on iOS. The SwiftUI `.task` modifier cancels the subscription when a view
  disappears, and each Swift model closes its Kotlin ViewModel in `deinit`. Closing it from `onDisappear` instead
  breaks tab-based apps, because `onDisappear` fires on every tab switch.
- SKIE is tied to specific Kotlin versions, which constrains upgrades.
- Compose resources live in the shared module (`composeResources`), not in the app's asset catalog.
- `material3` has its own version number, independent of the Compose Multiplatform version.
- The package of the generated `Res` class must be set explicitly (`compose.resources { packageOfResClass }`).
- iPhones with ProMotion displays need `CADisableMinimumFrameDurationOnPhone` in `Info.plist` for Compose.

## Versions

Kotlin 2.4.21, Gradle 9.9.1, Ktor 3.6.1, kotlinx.coroutines 1.11.0, Compose Multiplatform 1.12.2, SKIE 0.10.16.
Versions are pinned in `gradle/libs.versions.toml`.

## Not covered

Android and desktop targets, persistence (for example SQLDelight), a DI framework (Koin), publishing the shared module
as an XCFramework / Swift package, and CI.
