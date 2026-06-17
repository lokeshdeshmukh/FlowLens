# FlowLens

FlowLens is an Android Studio plugin that analyzes Android navigation patterns and renders an interactive screen flow graph for Activities, Fragments, Compose destinations, bottom sheets, dialogs, and deep links.

## Features

- Toolbar action: `Analyze App Flow`
- Tool window: `Flow Graph`
- Static analysis for:
  - Navigation XML files in `res/navigation`
  - Jetpack Compose navigation via `NavHost`, `composable`, `dialog`, `bottomSheet`, and `navigate(...)`
  - Activity navigation via `startActivity(...)` and explicit `Intent(...)` launches
  - `findNavController().navigate(...)` calls
- Interactive graph with zoom, pan, auto-layout, search, selection highlighting, PNG export, Mermaid export, and JSON export
- Details panel with file path, incoming/outgoing routes, deep links, ViewModel references, and Repository references
- Background analysis with incremental caching for modified files

## Build

1. Ensure JDK 21 is active for Gradle.
2. FlowLens automatically uses `/Applications/Android Studio.app/Contents` when it exists.
3. If Android Studio is installed elsewhere, pass a custom path:

```bash
./gradlew -PandroidStudioLocalPath="/path/to/Android Studio.app/Contents" buildPlugin
```

4. Build the plugin:

```bash
./gradlew buildPlugin
```

5. Run an Android Studio sandbox with the plugin:

```bash
./gradlew runIde
```

6. The packaged plugin ZIP is generated under:

```text
build/distributions/
```

## Notes

- The project prefers a local Android Studio SDK for development and falls back to Android Studio `2025.3.4.6` only when no local path is available.
- The plugin declares compatibility from IntelliJ build `253+`.
- Bottom sheets and deep links use heuristic detection in V1, as requested.
- The analysis layer is optimized to reuse cached per-file results and only reprocess files whose modification stamp changed.
# FlowLens
