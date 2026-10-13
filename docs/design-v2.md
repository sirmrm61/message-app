# Design v2 — minimal iOS-inspired UI

## Decisions

- **Design system location**: `core` module, package `core.designsystem` (files `AppColors.kt`, `Motion.kt`, `Components.kt`). The spec suggested a `core:designsystem` module; placing it inside the existing `:core` avoids build-graph changes since every feature module already depends on `:core`.
- **Palette**: fixed minimal iOS-like palette (`AppPalette`): system blue `#007AFF` / `#0A84FF` dark, green deposit, red withdrawal, amber warning, grouped background `#F2F2F7`, white cards, 12%-alpha hairline separators. Dynamic color removed — single accent for a consistent brand look. Dark equivalents provided; AMOLED (pure black) variant **not** added.
- **Typography**: Vazirmatn family, iOS-like scale (34/28/22/20/17/15/13/11 sp), hierarchy by weight, line-height ~1.5 for FA body text.
- **Components**: `Hairline`, `AppSectionHeader`, `AppListGroup` (inset grouped card), `AppListRow`, `AppIconTile`, `AppSegmentedControl`, `AppSearchBar` (36dp gray fill), `AppPrimaryButton`, `AppSwitch`/`appSwitchColors`, `AppBadge`, `UnreadDot`, `AppAvatar`, `AppEmptyState`, `AppShimmerRow`, `AppSheet` (bottom sheet + grabber).
- **Motion** (`AppMotion`): springs dampingRatio 0.85–0.9, tweens 200/300/350 ms; `Modifier.pressable` — press = scale 0.97 + alpha fade, **no ripple** anywhere in redesigned surfaces.
- **Screens**: conversations list (collapsing large title, pull-down search, quiet underline folder tabs, swipe archive/delete, selection circles), chat (rounded bubbles blue/gray, day separators, OTP copy card, waveform TTS indicator, capsule composer + segmented SIM), transactions (monthly summary + mini bar chart, subtype text filters, sticky sender groups, ✓/✗ confirm card), promotions (summary + suggestions group), onboarding (3 pages, dots, one primary button), settings (inset groups with colored icon tiles, segmented pickers).
- **Icons**: existing Material Icons (autoMirrored where direction matters). No new icon library pulled in.
- **Blur/translucent bottom tab bar**: not applicable — the app has no bottom navigation bar.

## Versions / dependencies

- No new dependencies added. Kotlin 2.0.21, Compose BOM 2024.10.01, Material3 unchanged.
- `INTERNET` permission still absent; no logic/DB/classifier/TTS changes.

## Unverified / not done

- `material-3` skill (hamen/material-3-skill) SKILL.md could not be fetched ("No content retrieved") — design was implemented directly against Compose Material3 APIs.
- Screenshot tests (Paparazzi/Roborazzi) not added — would require a new Gradle plugin; existing unit tests remain green.
- Tablet/foldable two-pane layout not implemented (single-pane adaptive).
- AMOLED theme variant not implemented (light + dark only).
- Before screenshots: `~/shots/before/`; after: `~/shots/after/` (FA light/dark on emulator API 35).
