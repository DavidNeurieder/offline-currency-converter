# Changelog

## 0.4.0

### Added
- Favorite currencies: star your most-used currencies for quick access
- Multi-currency view: see conversions to all your favorites at once
- Default favorites (USD, EUR, GBP, JPY, CNY) pre-selected on first install
- 90-day rate trend charts with gradient fill, cubic curves, dashed grid lines, and trend-based colors
- Touch-interactive charts: long-press for crosshair tooltip with date and rate
- Rate chart summary: current rate, change %, high, and low displayed inline below chart
- Date range tabs (7D / 30D / 90D) with persisted selection across restarts
- Historical chart toggle: enable/disable rate charts in settings (on by default)
- Home screen widget with Material You dynamic colors (Android 12+)
- Widget cross-rate computation: shows correct rate for any source/target pair
- Widget auto-updates after background sync and currency changes in the app
- Sync status badge: colored indicator shows how fresh your rates are
- Amount persistence: input amount survives app restarts (defaults to 1)
- Amount clear button
- Manual sync option: disable automatic background sync in settings
- Pull-to-refresh and Sync Now always work regardless of sync setting
- Settings page with back navigation (no bottom nav bar)
- Favorites management in Settings screen
- Copy button inline with converted amount
- Auto-sync on first install and app version update (no redundant syncs on every launch)

### Changed
- Charts now fetch 90 days of data (was 30) with UI-level filtering
- Initial amount defaults to 1 instead of empty
- Historical rates chart enabled by default for new installs
- Widget rate display rounded to 2 decimal places

## 0.3.0

### Added
- "Manual Only" sync option to disable automatic background sync
- App launch no longer syncs when set to manual only
- Pull-to-refresh and Sync Now button always work regardless of sync setting

## 0.2.0

### Added
- Privacy-first design: no accounts, no tracking, no analytics
- Multi-language support: 13 languages (ar, de, es, fr, hi, it, ja, ko, nl, pl, pt, ru, zh)
- F-Droid metadata and fastlane store listing

### Changed
- Removed unnecessary runtime permissions

## 0.1.0

### Added
- Offline currency conversion for 160+ ECB reference rates
- Material Design 3 UI with dynamic theming
- Background rate sync via WorkManager
- Copy conversion result to clipboard
- Country flag icons bundled with the app
