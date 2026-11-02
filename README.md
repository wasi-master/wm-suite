# WM Suite

Android libraries shared by the WM apps. Consumed as a git submodule and an
included build, so an F-Droid or CI checkout has the source without a registry.

| Module | What it is |
|---|---|
| `icons` | Generated Material Symbols and `SymbolVector` |
| `ui-core` | `WmSlider`, `ScrollRail`, `DisabledReason`, live slider and state helpers, swipe actions |
| `theme` | Colours, type, fonts, TV focus, `SuiteTheme` |
| `settings-ui` | Settings rows, groups, screen frame, breadcrumbs, depth, search highlight, reorderable list |
| `sinks` | Backup destinations, `BackupCrypto`, `BackupLocation`, `BackupPipeline` |
| `kdeconnect` | KDE Connect engine |

## Using it

```kotlin
// settings.gradle.kts
includeBuild("vendor/wm-suite")

// a module's build.gradle.kts
implementation("com.wasimaster.wmsuite:settings-ui")
```

## Filling in the app's part

The kit knows nothing about any one app. Each app supplies:

- `SettingsTables`: route accents and icons, row icons, choice icons, the new-row test. Set once at startup.
- `LocalScreenExtras`: buttons above the screen's own, a bottom bar, an action in the top bar.
- `LocalSettingLinks`, `LocalOpenSettingLink`, `LocalRowFavourites`: row links and favourites. Without them, rows have neither.
- `LocalAdvancedFolds`: where open folds are remembered.
- `LocalCrumbGlyph`: a crumb's own glyph, when a table entry is not enough.
- `SinkNet`: internet gate, network log and debug log for the sinks.

`ActionRow` takes a `guard` that runs its action behind whatever check the app wants.

Package names of `icons`, `ui-core` and `sinks` are the ones the code had in WM
Keyboard, so its imports stay valid when it moves onto this repository.

MIT licensed.
