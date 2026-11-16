# Kotlin/Swift interop notes

Pitfalls found while building this project. File references point to the code where each one shows up.

## Objective-C export (what Swift sees)

- **Default arguments are not visible from Swift.** `AppContainer` has a no-argument constructor and explicit factory
  functions instead of relying on defaults.
- **Numbers and generics are re-typed.** `Int` becomes `Int32`, `Long` becomes `Int64`, a `Boolean` inside a generic
  becomes a boxed Kotlin wrapper type, and `data class.copy()` becomes `doCopy()`.
- **`kotlin.time.Instant` is opaque.** It arrives as a Kotlin class, so `CharacterUi` exposes epoch milliseconds and
  Swift builds a `Date` from them.
- **`enum` is a class in Swift.** Compare against its static instances (see `iosApp/Plain/CharactersModel.swift`).
  With SKIE it is a real Swift enum.
- **`sealed` is a protocol plus classes.** There is no exhaustive `switch` without SKIE, so a new case silently falls
  through the `as?` chain. With SKIE you get `onEnum(of:)`.
- **`Flow` loses its element type.** Without SKIE you implement `FlowCollector` yourself and cast `Any?`
  (`iosApp/Plain/FlowCollector.swift` wraps this in an `AsyncStream`). With SKIE it is an `AsyncSequence`.
- **Top-level functions live in a class named after the file plus `Kt`.** `MainViewController.kt` becomes
  `MainViewControllerKt`, so renaming such a file breaks Swift.
- **Name clashes with the Swift standard library.** The domain class `Character` was exported to Swift as `Character`
  and shadowed `Swift.Character`; it is now `CartoonCharacter`. Check the generated header for collisions.
- **Visibility defaults matter.** Everything is `public` unless marked otherwise, so the whole domain layer ended up in
  the header. Marking it `internal` roughly halved the header.
- **Types from other Kotlin modules** are visible to Swift only if re-exported with `export(...)`.
- **Kotlin code cannot be debugged from Xcode.**

## Lifecycle

- **Kotlin does not cancel anything for you on iOS.** The SwiftUI `.task` modifier cancels the subscription when a view
  disappears, and each Swift model closes its Kotlin ViewModel in `deinit`.
- **Do not close the ViewModel from `onDisappear`.** It fires on every tab switch, so a tab-based app ends up with a
  dead ViewModel behind a live view.
- **Do not hop to the main actor with a new `Task` per emitted value.** Unstructured tasks do not guarantee ordering.
  Collect into an `AsyncStream` and consume it with `for await`.

## Build

- **Keep the interop variants in separate build directories.** Sharing `shared/build` between the Plain and SKIE
  variants leaves stale SKIE files behind and breaks the plain Xcode build.
- **SKIE is tied to specific Kotlin versions**, which constrains upgrades. SKIE also adds Swift code to the framework,
  so check Swift and Xcode compatibility before handing a SKIE-built framework to another team.
- **Xcode runs Gradle on every build** through the Run Script phase, and user script sandboxing has to be disabled for
  it (`ENABLE_USER_SCRIPT_SANDBOXING = NO`).
- **Kotlin/Native downloads its toolchain on first use** (into `~/.konan`), so the first build is slow.

## Compose Multiplatform

- **Resources live in the shared module** (`composeResources`), not in the app's asset catalog.
- **`material3` has its own version number**, independent of the Compose Multiplatform version.
- **The package of the generated `Res` class must be set explicitly** (`compose.resources { packageOfResClass }`).
- **ProMotion iPhones need `CADisableMinimumFrameDurationOnPhone`** in `Info.plist` for Compose.
