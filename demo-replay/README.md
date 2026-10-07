# Demo replay (Familia / SubTabs)

When the sandbox demo starts via `start-demo.bat` or `record-demo.bat`, the plugin records
editor layout snapshots while you work in `demo-project`.

## Files

| File | Purpose |
|------|---------|
| `latest.json` | Updated during the session (debounced). Quick read for agents. |
| `last-session.txt` | Absolute path to the last full session file after IDE exit. |
| `sessions/<sessionId>.json` | Full timeline: triggers + state snapshots. |

## Typical workflow

1. Reproduce a bug in the demo IDE.
2. Close the IDE.
3. Open `latest.json` or the path from `last-session.txt`.
4. Inspect `lastState.editorWindows`, `splittabRegistry`, and per-tab `chrome`
   (subtab bar, Splittab header title/close visibility, sidetabs).

Recording uses a 400 ms debounce and is enabled only for the demo project
(`-Dsubtabs.demo.replay=record` from Gradle `runIde`).

To disable recording for one run:

```bat
set GRADLE_OPTS=-Dsubtabs.demo.replay=off
start-demo.bat
```
