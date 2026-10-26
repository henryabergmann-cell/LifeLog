# Life Log — Implementation Plan

Status: **Approved 2026-10-07 — M1 and M2 done, M3 next**
Last updated: 2026-10-09

Native Android calendar/appointment app with a free tier and a Premium subscription (Google Play subscriptions, handled through RevenueCat, see D9).

---

## 0. Decisions

### 0.1 Setup (resolved 2026-10-07)

| # | Item | Outcome |
|---|------|---------|
| B1 | Build toolchain | Portable install in your user folder, no admin rights: Microsoft OpenJDK 21 (`%USERPROFILE%\.jdks`), Android SDK (`%LOCALAPPDATA%\Android\Sdk`: platform 36, build-tools 36.1.0, platform-tools), MinGit (`%LOCALAPPDATA%\Programs\MinGit`). You accepted the Android SDK license. |
| B2 | Gradle wrapper | Gradle 9.8.0. The distribution and wrapper-jar checksums were verified against `services.gradle.org`. |
| B3 | Application ID | **Decided (2026-10-26):** `io.github.henryabergmann.lifelog`, set once in `gradle.properties` (`lifelog.applicationId`). It can't change after the first Play upload. The code namespace is `app.lifelog` and doesn't depend on the store ID. |
| B4 | Project location | `%USERPROFILE%\AndroidStudioProjects\LifeLog` |

**SDK levels (changed 2026-10-07):** `compileSdk 37` with `targetSdk 36` and `minSdk 26`. The current stable AndroidX releases require compileSdk 37. Runtime behaviour is still governed by targetSdk 36, as specified.

**Milestone gate** (changed because of D7): `./gradlew assembleDebug test assembleDebugAndroidTest`. The last task compiles the instrumented tests. Running them needs an emulator or device: `./gradlew connectedDebugAndroidTest`.

### 0.2 Design decisions: the ★ options were accepted, except D7

| # | Decision | Options |
|---|----------|---------|
| D1 | **Device-calendar sync scope** | ★ **Show + push:** device calendars you pick are shown read-only in all Life Log views. Life Log events can also be mirrored one way into one writable device calendar you pick. Life Log stays the source of truth for its own events. — *Alt:* full two-way sync with conflict resolution. That is far more work and risk, so I suggest it after 1.0. |
| D2 | **What happens when Premium lapses** | ★ **No data is ever deleted.** Categories above 3 stay on existing events, but you can't create new ones. Extra reminders are kept but only the first one fires. The journal becomes read-only. Widgets show a "Premium" placeholder. Sync pauses (copies already in the device calendar stay). Backup/restore is locked. |
| D3 | **Event time model** | ★ **Timed events are anchored to a time zone** (the device zone at creation). The zone is shown in the editor only when it differs from the current device zone. **All-day events are floating** (same date everywhere). This maps cleanly onto CalendarContract and handles DST correctly. — *Alt:* everything floating. Simpler, but events shift when you travel and the mapping to the device calendar gets lossy. |
| D4 | **Android Auto Backup** (Google's own backup) | ★ **Cloud backup off, device-to-device transfer on** (`dataExtractionRules`). Data never leaves the device through Google backup, but it does move to a new phone during setup. The entitlement cache is always excluded. — *Alt:* allow encrypted Google cloud backup. |
| D5 | **Exact alarms** | ★ `SCHEDULE_EXACT_ALARM` with an explanation screen, falling back to inexact alarms (a few minutes late) when it's denied. This is what your spec says. — *Alt:* `USE_EXACT_ALARM`. Calendar apps qualify for it and the user is never prompted, but it needs a Play Console declaration and policy review. |
| D6 | **Recurring edit scopes** | ★ "This event" and "All events", as specified. "This and following events" is left out of scope and can be added later. |
| D7 | **Where UI tests run** | ✔ **Instrumented `androidTest`** (Robolectric was declined). Room DAO tests and Compose UI tests live in `androidTest`. The gate compiles them, and they run on an emulator or device. Pure logic (domain, ViewModels, DataStore, billing state machine) stays in JVM unit tests under `./gradlew test`. |
| D8 | **Language setting on Android 8–12** | ★ Use **AppCompat**'s per-app locale support (`AppCompatDelegate.setApplicationLocales`), which works on all API levels. — *Alt:* show the in-app language setting only on Android 13+ (system per-app language) and hide it below. |
| D9 | **Billing backend** (decided 2026-10-07) | ✔ **RevenueCat** instead of calling Play Billing directly. RevenueCat's servers validate purchases with Google and receive Google's Real-time Developer Notifications, which replaces the "own backend later" TODO. The app gains the `INTERNET` permission, used for purchase checks only. Purchase data and an anonymous RevenueCat user ID leave the device; appointments, journal and settings never do. The RevenueCat SDK stays inside `:billing`, behind `PremiumManager`. |

### 0.3 Dependencies

**Approved 2026-10-07:**
- Group A
- From Group B: kotlinx-serialization, appcompat and material3-adaptive. Robolectric was declined.
- RevenueCat Android SDK `com.revenuecat.purchases:purchases` (D9). It brings Play Billing with it, so `billing-ktx` from Group A is no longer added directly.

Notes:
- `hiltViewModel()` now ships in `androidx.hilt:hilt-lifecycle-viewmodel-compose`, which was split out of `hilt-navigation-compose`. That artifact is used instead.
- `kotlin-parcelize` isn't needed, because kotlinx-serialization covers saved state.

**Pinned in M1:**

| Tool / library | Version | Tool / library | Version |
|---|---|---|---|
| Gradle | 9.8.0 | AGP (built-in Kotlin) | 9.4.1 |
| Kotlin | 2.4.20 | KSP | 2.3.12 |
| Hilt / androidx.hilt | 2.60.1 / 1.4.0 | Compose BOM | 2026.09.00 |
| Navigation Compose | 2.10.2 | Room | 2.8.5 |
| DataStore | 1.2.1 | Lifecycle | 2.11.0 |
| Activity | 1.13.0 | Core KTX | 1.19.1 |
| AppCompat | 1.8.0 | Coroutines / Serialization | 1.11.0 / 1.11.0 |
| WorkManager (M2) | 2.12.0 | *Later:* Glance | 1.2.0 |
| *Later:* RevenueCat SDK | 10.25.0 (brings Play Billing 8.3.0) | | |

The original request is kept below for reference.

**Group A: implied by your stack:**

- Build plugins: KSP (Room/Hilt code generation), Kotlin Compose compiler plugin, `kotlin-parcelize`
- `androidx.core:core-ktx`, `androidx.activity:activity-compose`, Compose BOM (`ui`, `foundation`, `material3`, `ui-tooling`)
- `androidx.lifecycle`: `lifecycle-runtime-compose` and `lifecycle-viewmodel-compose`, for `collectAsStateWithLifecycle` and ViewModels in Compose
- `androidx.hilt:hilt-navigation-compose` (`hiltViewModel()`) and `androidx.hilt:hilt-work` (Hilt-injected workers)
- `javax.inject:javax.inject`: JSR-330 `@Inject` in the pure-Kotlin domain module (no Android/Hilt there)
- `androidx.glance:glance-appwidget` + `glance-material3`, the artifacts of Glance
- `com.android.billingclient:billing-ktx`, the Kotlin artifact of Play Billing
- Test only: `kotlinx-coroutines-test`, Compose `ui-test-junit4` / `ui-test-manifest`, `androidx.room:room-testing`, `androidx.work:work-testing`, `androidx.test` (core, ext-junit, runner), `hilt-android-testing`

**Group B: new libraries, each needs a yes or no:**

| Library | Used for | Without it |
|---|---|---|
| `kotlinx-serialization` (plugin + `-json`) | Type-safe Navigation Compose routes, backup JSON, `SavedStateHandle` drafts | String routes + Android's built-in `org.json` (more boilerplate, weaker typing) |
| `androidx.appcompat` | Per-app language on API 26–32 (D8) | Language setting only on Android 13+ |
| `org.robolectric:robolectric` (test only) | D7 | Instrumented tests only |
| `androidx.compose.material3.adaptive` (navigation-suite) | Bottom bar ↔ navigation rail and two-pane layouts on tablets | Hand-rolled breakpoints (works fine, a bit more code) |

**What I will deliberately not add:** calendar-UI libraries and chart libraries (built with Compose and Canvas instead), Paging, Timber, Accompanist, an icon library (Material Symbols are shipped as vector drawables), and no analytics or crash reporting. Crash and ANR data comes from Play Console *Android vitals*, which needs no SDK. I'll use `java.time` rather than kotlinx-datetime because it's native at minSdk 26, so no desugaring is needed.

Exact versions are pinned in `libs.versions.toml` in M1, using the latest stable release of each library that works with the others. Play Billing Library will be the latest stable, at least 8.x.

---

## 1. Architecture

MVVM with clean layers and unidirectional data flow. Each screen has:
- an immutable `UiState`, exposed as `StateFlow<UiState>` from a `@HiltViewModel`
- a single `onAction(UiAction)` entry point
- one-off effects (snackbar, navigation) modeled as state that the UI consumes, with no fire-and-forget events that can be lost

```
            ┌────────────────────────────── :app ──────────────────────────────┐
            │ Compose UI · ViewModels · Navigation · Glance widgets · DI root  │
            └───────┬──────────────────────────┬───────────────────────┬───────┘
                    │                          │ (Hilt aggregation)    │
                    ▼                          ▼                       ▼
     ┌──────── :domain ────────┐   ┌──────── :data ────────┐   ┌──── :billing ────┐
     │ pure Kotlin/JVM          │◄──│ Room, DataStore,       │   │ RevenueCat SDK,  │
     │ models, use cases,       │   │ AlarmManager, notifs,  │   │ entitlement      │
     │ repository interfaces,   │◄──│ WorkManager, Calendar- │   │ cache,           │
     │ recurrence, reminders,   │   │ Contract, backup (SAF) │   │ PurchaseVerifier │
     │ PremiumManager interface │◄──┴────────────────────────┘   └────────┬─────────┘
     └──────────────────────────┘◄─────────────────────────────────────────┘
```

**Boundaries the build enforces**
- `:domain` is a plain Kotlin/JVM module. It can't use Android APIs, and its tests run fast on the JVM.
- The RevenueCat SDK, which brings Play Billing with it, is an `implementation` dependency of `:billing` only. That keeps `com.revenuecat.*` and `com.android.billingclient.*` off `:app`'s compile classpath, so **UI code physically cannot call billing code.** The UI talks only to `PremiumManager` (a `:domain` interface).
- Implementation classes in `:data` and `:billing` are `internal`. Their public API is just their Hilt modules.
- Starting a purchase needs an `Activity`. The paywall passes it as an opaque `PurchaseHost` wrapper through `PremiumManager.purchase(host, plan)`, so no billing types leak out.

**Cross-cutting**
- `java.time.Clock` and coroutine dispatchers are injected through qualifiers, so tests are deterministic.
- Process death: ViewModels keep their inputs (selected date, view mode, editor draft, search query) in `SavedStateHandle`.
- Configuration and secrets are read from `local.properties` (environment variables are the fallback for CI) and passed in through `BuildConfig`. A committed `local.properties.example` documents every key. The build fails with a clear message when a required key is missing:
  ```
  lifelog.revenuecat.apiKey=goog_…          # RevenueCat public Android SDK key; required for release.
                                            # Debug builds without it run with billing off (fake-Premium toggle still works).
  lifelog.revenuecat.entitlementId=premium
  lifelog.url.privacy=https://…
  lifelog.url.terms=https://…
  # release only
  lifelog.signing.storeFile=…  lifelog.signing.storePassword=…
  lifelog.signing.keyAlias=…   lifelog.signing.keyPassword=…
  ```
  The product ID `life_log_premium`, the base plans `monthly`/`yearly` and the trial offer are configured in Play Console and attached to a RevenueCat *offering*. The app reads the offering's monthly and annual packages, so product IDs never appear in the app at all. (M1 still has the earlier `lifelog.billing.*` keys; M5 replaces them with the keys above.)

---

## 2. Module and package layout

Code namespace: `app.lifelog`. The Play application ID is configured separately (see B3).

```
LifeLog/
├─ settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml
├─ local.properties.example          # committed; local.properties is git-ignored
├─ PLAN.md, RELEASE.md (M7)
│
├─ domain/            (kotlin-jvm)    …lifelog.domain
│   ├─ model/         Event, Occurrence, Category, Reminder, RecurrenceRule, JournalEntry, Mood,
│   │                 AppSettings, Entitlement, PremiumStatus, SubscriptionPlan, PremiumFeature
│   ├─ repository/    EventRepository, CategoryRepository, JournalRepository, SettingsRepository,
│   │                 DeviceCalendarRepository, BackupRepository
│   ├─ recurrence/    RecurrenceEngine (expansion), RRuleCodec (RFC 5545 subset)
│   ├─ reminder/      ReminderPlanner (pure: events + now + horizon → triggers), ReminderScheduler (interface)
│   ├─ premium/       PremiumManager, EntitlementRepository, FeatureGate, PurchaseVerifier,
│   │                 PurchaseRecord, VerificationResult, PurchaseHost, SavingsCalculator
│   ├─ stats/         StatisticsCalculator
│   └─ usecase/       GetOccurrencesInRange, SaveEvent(scope), DeleteEvent(scope), DuplicateEvent,
│                     SearchEvents, …
│
├─ data/              (android-library) …lifelog.data
│   ├─ db/            LifeLogDatabase, entity/, dao/, converter/, migration/   (schemas/ exported to repo)
│   ├─ repository/    *RepositoryImpl
│   ├─ settings/      SettingsDataStore
│   ├─ reminder/      AlarmReminderScheduler, ReminderReceiver, SystemEventsReceiver,
│   │                 ReminderNotifier (channels), RescheduleRemindersWorker
│   ├─ calendarsync/  DeviceCalendarDataSource (CalendarContract), CalendarPushWorker
│   ├─ backup/        BackupDto (versioned), BackupSerializer, BackupRepositoryImpl (SAF streams)
│   └─ di/
│
├─ billing/           (android-library) …lifelog.billing
│   ├─ RevenueCatClient                 # thin, mockable wrapper around Purchases (configure, offerings,
│   │                                   # purchase, restore, customer info, update listener)
│   ├─ RevenueCatEntitlementRepository  # CustomerInfo → Entitlement; refresh on start and resume
│   ├─ EntitlementCache (DataStore)     # offline fallback
│   ├─ RevenueCatPurchaseVerifier       # accepts only server-validated CustomerInfo; swap point for an own backend
│   ├─ PremiumManagerImpl               # combines entitlement + debug override
│   └─ di/BillingModule
│
└─ app/               (android-application) …lifelog
    ├─ LifeLogApp (@HiltAndroidApp, WorkManager Configuration.Provider), MainActivity
    ├─ navigation/    @Serializable routes, LifeLogNavHost, adaptive nav scaffold
    ├─ designsystem/  theme/ (Color, Type, Theme, dynamic color), component/ (TopBar, ColorSwatch,
    │                 EmptyState, LockedFeatureCard, ScopeDialog, …)
    ├─ feature/
    │   ├─ calendar/  CalendarScreen + VM; month/, week/, day/, agenda/; layout/EventColumnLayout
    │   ├─ event/     detail/, editor/ (SavedStateHandle draft), recurrence/ (rule picker)
    │   ├─ search/   categories/   journal/   stats/   paywall/
    │   ├─ settings/  (+ backup/, sync/, about/, language/)
    │   └─ onboarding/ (+ permissions/)
    ├─ widget/        TodayAgendaWidget, MonthWidget, receivers, GlanceWidgetUpdater
    ├─ src/debug/     DebugPremiumOverride + Developer options screen (fake Premium)
    └─ src/release/   NoPremiumOverride (always null). No toggle code exists in release.
```

---

## 3. Data model

### 3.1 Room (database `life_log.db`, schema version 1 at launch, schema exported, migrations tested)

**`categories`**: `id` PK · `name` · `colorArgb` · `isCustomColor` · `sortOrder` · `createdAt`

**`events`**: one row per one-off event, recurring-series master, or overridden occurrence

| Column | Notes |
|---|---|
| `id` PK, `uid` (UUID) | `uid` stays stable across backup/restore and sync |
| `title`, `location?`, `notes?`, `categoryId?` (FK → SET NULL) | |
| `allDay` | |
| `startDate`, `endDate` (epoch day) | Local dates in the event's zone (inclusive end for all-day) |
| `startTime?`, `endTime?` (second of day) | null when `allDay` |
| `zoneId?` | IANA zone for timed events; null = floating (all-day), see D3 |
| `startUtc`, `endUtc` | **Derived** index columns for range queries; recomputed on save |
| `rrule?`, `seriesEndUtc?` | RFC 5545 subset (FREQ, INTERVAL, BYDAY, BYMONTHDAY, BYSETPOS, UNTIL, COUNT); `seriesEndUtc` null = infinite |
| `seriesMasterId?`, `originalStart?` | Set on override rows ("this event" edits), mirroring CalendarContract `ORIGINAL_ID` / `ORIGINAL_INSTANCE_TIME` |
| `deletedAt?` | Soft delete for the undo snackbar; purged by a worker |
| `createdAt`, `updatedAt` | |

Indexes: `(startUtc)`, `(endUtc)`, `(seriesMasterId)`, `(categoryId)`.

**`event_exdates`**: `eventId` FK CASCADE · `originalStart`. Records occurrences of a series deleted with "this event".

**`reminders`**: `id` · `eventId` FK CASCADE · `minutesBefore`. For all-day events the offset is measured from the all-day reminder time set in Settings (default 09:00). Free tier: 1 per event. Premium: up to 5.

**`scheduled_alarms`**: `requestCode` PK · `eventId` · `reminderId` · `occurrenceStart` · `triggerAtUtc`. Lets the scheduler cancel stale alarms precisely and restore state after boot.

**`events_fts`** (FTS4, external content = `events`): `title`, `location`, `notes`, used for search.

**`journal_entries`** (Premium): `date` (epoch day) PK · `mood?` (1–5) · `note` (≤ 1 000 chars) · `createdAt` · `updatedAt`

**`device_calendars`** (Premium, D1): `calendarId` PK · `displayName` · `accountName` · `color` · `visible` · `isPushTarget`

**`device_event_links`** (Premium, D1): `eventUid` · `deviceEventId` · `deviceCalendarId` · `contentHash`

### 3.2 DataStore

- **`settings`**: theme (system/light/dark) · dynamicColor · firstDayOfWeek (system/Mon/Sat/Sun) · defaultReminderMinutes · allDayReminderTime · defaultEventDurationMinutes · lastCalendarView · onboardingDone · exactAlarmExplained · upgradeBannerDismissedUntil
- **`entitlement`** (owned by `:billing`, excluded from every backup): state · plan (monthly/yearly) · willRenew · expiresAt · billingIssueDetected · lastVerifiedAt
- Language is not stored here. It lives in the per-app locale store (D8).

### 3.3 Time rules

- Recurrences expand in the event's own zone, so a weekly 09:00 meeting stays at 09:00 across DST. Local times that fall into a DST gap shift forward, following `ZonedDateTime` rules.
- Recurrence edge cases follow RFC 5545: "monthly on the 31st" skips shorter months and "yearly on Feb 29" skips non-leap years. "Last day of month" and "nth weekday" are offered as explicit options.
- Range queries use the derived UTC columns, padded by ±1 day. The domain layer then filters precisely, which also covers floating all-day events.
- Editing "all events" after the time or recurrence rule changed resets that series' overrides and exdates, and the UI warns before doing so. Edits to other fields keep the overrides.

---

## 4. Premium and billing design

**`PremiumManager`** (`:domain` interface) is the only gate the UI ever sees:
```kotlin
interface PremiumManager {
    val isPremium: StateFlow<Boolean>
    val status: StateFlow<PremiumStatus>          // Free, Pending, Active(plan, autoRenewing), Unknown
    val plans: StateFlow<PlansState>              // Loading / Loaded(List<SubscriptionPlan>) / Unavailable
    suspend fun purchase(host: PurchaseHost, plan: PlanId): PurchaseOutcome
    suspend fun refresh(): RefreshOutcome          // also used for "Restore purchases"
}
```
`FeatureGate` (domain) turns `isPremium` into limits, such as `maxCategories`, `maxRemindersPerEvent` and `canUse(PremiumFeature)`. ViewModels ask `FeatureGate` and never check product IDs themselves.

**Purchase flow (`:billing`, RevenueCat, D9)**
1. **Configure.** `Purchases.configure(...)` runs in `Application.onCreate` with the API key from `BuildConfig`.
   - Users get anonymous RevenueCat IDs; there is no login.
   - `collectDeviceIdentifiers()` is never called and no attribution integrations are enabled.
   - Google Play's own in-app messages for billing problems stay on, which is the SDK default.
2. **Load plans.** The current RevenueCat offering provides the **monthly** and **annual** packages (base plans `monthly` / `yearly` of `life_log_premium`).
   - Prices, periods and the free trial come from each package's `StoreProduct`, which RevenueCat loads from Google Play. Nothing is hard-coded.
   - The trial is shown only when Play offers it, meaning the user is eligible.
3. **Yearly saving %** = `floor((1 − yearlyMicros / (12 × monthlyMicros)) × 100)`.
   - It uses the recurring price, not the trial phase.
   - It's shown only when both plans share a currency and the result is > 0. Rounding down means the saving is never overstated.
4. **Purchase.** `purchase(PurchaseParams(activity, package))` starts the Google Play purchase screen.
   - Switching between monthly and yearly passes the old product with a Google replacement mode: prorated charge when upgrading, deferred when downgrading.
   - RevenueCat validates the purchase with Google on its servers and acknowledges it, so Play's 3-day refund rule is handled.
5. **Purchase results:**
   - success → `PurchaseVerifier.verify(customerInfo)` → grant
   - payment pending → status `Pending`, no access
   - cancelled by the user → nothing shown
   - already owned → refresh
   - network or store errors → message plus retry
6. **Refresh.** `getCustomerInfo()` runs on app start and on every `MainActivity` resume, and the SDK's customer-info listener pushes changes in between. **Restore purchases** calls `restorePurchases()`.
7. **Subscription states.** RevenueCat's servers receive Google's Real-time Developer Notifications, so the app sees the exact state:

   | State | Premium entitlement | Life Log behavior |
   |---|---|---|
   | Active / in trial | active, `willRenew` | Access granted; Settings shows the plan and renewal date (trial end during a trial) |
   | Cancelled, not yet expired | active, `willRenew = false` | Access granted; Settings shows "Ends on …" |
   | Grace period | active, billing issue detected | Access granted; Settings shows "Payment problem: update your payment method in Google Play" |
   | Account hold / paused / expired | inactive | Access removed (data kept, D2) |
   | Pending purchase | inactive | "Payment pending" shown on the paywall and in Settings |

8. **Offline.** The SDK caches the last customer info. In addition, `EntitlementCache` keeps the last verified state with a timestamp in DataStore. A network or store error keeps the cached state; only a successful server answer changes it.
9. **`PurchaseVerifier`.** This interface stays as the spec requires.
   - `RevenueCatPurchaseVerifier` grants access only from customer info that RevenueCat's servers validated against Google, and only for the configured entitlement ID.
   - If you ever replace RevenueCat with your own backend, this is the swap point. The earlier "own backend + RTDN" TODO is now covered by RevenueCat. Connecting Google Play to RevenueCat (service-account key, notification topic) is a dashboard step for you, listed in M5 and `RELEASE.md`.
10. **Manage subscription** opens the `managementURL` from customer info, falling back to `https://play.google.com/store/account/subscriptions?package=<appId>`.
11. **Debug fake Premium.** `PremiumManagerImpl` combines the real entitlement with a `PremiumOverride`. The debug source set binds a toggle stored in DataStore and shows it in Developer options. The release source set binds a no-op, so neither the toggle UI nor its code is in release builds. M7 adds a check that `assembleRelease` contains no `DebugPremiumOverride` class.

**Paywall content:** feature list, both plans with Play prices, trial terms ("7 days free, then {price}/year"), the saving badge, the CTA, "Restore purchases", Terms and Privacy links, and the required auto-renew notice: price and period, automatic renewal until cancelled, how to cancel in Google Play, and that you're charged when the trial ends unless you cancel before.

**Upgrade entry points (free users only):** a Settings row, locked feature screens and rows, locked widgets, and one dismissible banner on the calendar screen that stays hidden for 14 days after dismissal. Premium users see none of these.

---

## 5. Reminders and notifications

- `ReminderPlanner` (pure) computes trigger instants for all reminders within the next 48 hours, capped at 100 alarms because AlarmManager has a per-app limit. `AlarmReminderScheduler` makes `scheduled_alarms` match that plan: it cancels removed alarms and schedules new ones.
- When exact alarms are allowed it uses `setExactAndAllowWhileIdle`. When they're denied, `setAndAllowWhileIdle` (inexact) and a one-time hint explains that reminders may arrive a few minutes late.
- **Rescheduling triggers:**
  - any change to events or reminders
  - `BOOT_COMPLETED`
  - `TIMEZONE_CHANGED` and `TIME_SET`
  - `MY_PACKAGE_REPLACED`
  - `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`
  - after each alarm fires
  - a periodic `RescheduleRemindersWorker` (about every 12 h), as a safety net that extends the 48 h window
- Receivers do minimal work: `goAsync()` plus a coroutine, or they enqueue a worker.
- The notification channel "Reminders" has localized names, recreated on locale change. Tapping a notification deep-links to the occurrence's detail screen.
- The `POST_NOTIFICATIONS` request on Android 13+ comes after onboarding. If it's denied, a non-blocking hint appears in Settings and in the event editor.

---

## 6. Screens

| Screen | Tier | Notes |
|---|---|---|
| Onboarding (3-page pager) | Free | Skip and Next, page indicator readable by TalkBack |
| Notification permission explainer | Free | Android 13+ only |
| Exact-alarm explainer | Free | Only shown if `canScheduleExactAlarms()` is false; opens the system settings page |
| **Calendar host** | Free | View switcher (Month / Week / Day / Agenda), swipe between periods (`HorizontalPager`), Today button, Search action, New-event FAB. On tablets: month grid and day list side by side |
| ├ Month view | Free | Event chips; dots instead of chips at large font scales; tap a day → Day view |
| ├ Week view | Free | Time grid, all-day row, overlap layout (columns), current-time line |
| ├ Day view | Free | Same time grid as Week, one day |
| └ Agenda | Free | Lazy list grouped by day, infinite in both directions (windowed loading) |
| Event detail | Free | Edit, duplicate, delete (scope dialog for series, undo snackbar) |
| Event editor | Free | Title, all-day, date/time pickers (M3), location, notes, category, recurrence, reminder(s); process-death safe |
| Recurrence picker | Free | Presets plus custom: interval, weekdays, monthly mode, ends never / on date / after N times |
| Search | Free | FTS across title, location and notes; recurring events show their next occurrence |
| Categories | Free (≤ 3) / Premium | Preset palette (12 colors) for free, custom color picker for Premium |
| Settings | Free | Theme, dynamic color, first day of week, default reminder, all-day reminder time, default event length, language, Premium status and Manage subscription, Backup & restore, Calendar sync, Privacy policy, Terms, version; Developer options (debug builds only) |
| Paywall | — | See §4 |
| Journal: today and timeline | Premium | Mood (5 levels, labelled for TalkBack) and short note; reverse-chronological timeline; open any day from the calendar |
| Statistics | Premium | Range (4 weeks / 12 weeks / 12 months): appointments per week and month, busiest weekdays and hours, category breakdown. Canvas charts, each with an accessible text summary |
| Calendar sync | Premium | Permission rationale, pick calendars to show, pick the push target (D1) |
| Backup & restore | Premium | Export (`CreateDocument`, `life-log-backup-YYYY-MM-DD.json`) and import (`OpenDocument`), with validation and "replace all data" confirmation |
| Widgets (Glance) | Premium | Today's agenda and Month. Locked placeholder for free users; updated on data change, at midnight, and on time-zone or locale change |

**Navigation:** Calendar · Journal · Insights · Settings. This is a bottom bar on phones and a navigation rail on tablets and in landscape. Journal and Insights show a locked teaser for free users. Predictive back is enabled, since targetSdk 36 turns it on by default.

---

## 7. Quality and platform

- **Accessibility:**
  - every icon has a content description, and calendar cells read like "Tuesday, 6 October, 3 appointments"
  - all touch targets are at least 48dp
  - the layout is verified at 200 % font scale
  - mood and color choices never depend on color alone
  - charts have text equivalents
  - custom components get correct semantics: headings, `stateDescription`, traversal order
- **Layout:** edge-to-edge with correct inset handling. Rotation and large screens are supported; on Android 16, large screens ignore orientation locks anyway.
- **Localization:** 18 languages (added 2026-10-25):
  - English (default), German, French, Spanish, Italian, Portuguese (Brazilian wording), Dutch, Polish, Czech, Swedish, Danish, Norwegian Bokmål, Finnish, Japanese, Korean, Chinese (Simplified), Indonesian and Hindi
  - all text in `strings.xml`, with each language's plural rules (one/few/many/other)
  - dates are formatted with locale-aware `DateTimeFormatter`, and weekday names use standalone forms
  - the locale config is generated by AGP, so Android 13+ lists the languages in system settings
  - the translations other than English and German are machine-quality drafts: have native speakers review them before the store launch (see `RELEASE.md`)
- **Privacy:**
  - no analytics or tracking SDKs
  - **network use is limited to RevenueCat for purchases (D9)**: the `INTERNET` permission comes from the RevenueCat SDK, and no other code makes network calls (checked in M7)
  - what leaves the device: purchase tokens and receipts, an anonymous RevenueCat user ID, and basic app and SDK info. No device identifiers or advertising ID.
  - appointments, journal, categories and settings never leave the device
  - RevenueCat must be named in the privacy policy, and purchase history declared in Play's data-safety form (see `RELEASE.md`)
  - Auto Backup per D4. RevenueCat's local cache is also excluded from device transfer; the subscription is restored from the Google account instead.
  - links open in the user's browser
  - calendar permission is requested only when sync is enabled
- **Release build:** R8 full mode and resource shrinking. Keep rules are added only where libraries don't ship their own consumer rules (Room, RevenueCat, Hilt and kotlinx-serialization do). Release signing is configured from `local.properties` / environment, for upload with Play App Signing.
- **Process death:** ViewModels use `SavedStateHandle`. This is verified with ViewModel tests and manually with "Don't keep activities".

---

## 8. Milestones

Every milestone ends with `./gradlew assembleDebug test assembleDebugAndroidTest` passing (see §0.1), this checklist updated, and a short summary to you.

### M1 — Project setup, theme, navigation skeleton, Room ✅ (2026-10-07)
- [x] Gradle wrapper, `settings.gradle.kts`, `libs.versions.toml`, the four modules, KSP, Hilt, Compose
- [x] `local.properties.example` and `BuildConfig` wiring with fail-fast validation; `.gitignore`; `git init` (no commits yet: waiting for your Git name and email)
- [x] Material 3 theme with dynamic color and light/dark, edge-to-edge `MainActivity`
- [x] Type-safe navigation skeleton: 4 top-level destinations (placeholders), adaptive bar/rail
- [x] Room database v1 with **all** tables from §3 (premium tables included, to avoid early migrations), DAOs, FTS, exported schema
- [x] DataStore settings repository (read/write, defaults)
- [x] `strings.xml` en and de baseline, generated locale config
- [x] Tests: settings repository (unit, 5), first-day-of-week (unit, 2), MainViewModel (unit, 1); DAO tests (instrumented, 6: range query, series range, soft delete/restore, FTS with diacritics, reminder cascade, category SET NULL). The DAO tests compile but haven't been run on a device yet.

### M2 — Appointment CRUD and calendar views ✅ (2026-10-09)
- [x] Domain models, `EventRepository`, occurrence queries (non-recurring)
- [x] Event editor (create, edit, duplicate) with validation (end after start, title required) and a `SavedStateHandle` draft
- [x] Event detail; delete with undo snackbar (soft delete and purge worker)
- [x] Categories CRUD with a free limit of 3 through `FeatureGate` (a stub `PremiumManager` returns false until M5)
- [x] Month, Week, Day and Agenda views; swipe; Today; view switcher; last view remembered
- [x] Overlap layout algorithm for the Week and Day time grid
- [x] Search screen (FTS)
- [x] Tests: 59 JVM unit tests (occurrence expansion, use cases, overlap layout, period/pager math, FTS query building, entity mapping, calendar/editor/search/categories ViewModels incl. process death); 6 instrumented DAO tests **run and passing on the API 36 emulator**
- [x] Manual check on the emulator: create, month/week/day/agenda views, detail, delete + undo, search, categories dialog
- Added (Group A / Hilt family): `androidx.work:work-runtime`, `androidx.hilt:hilt-work` + `hilt-compiler`, `com.google.dagger:dagger` + `dagger-compiler` (generates factories for `:domain` use cases)

### M3 — Recurring events, reminders and notifications
- [ ] `RecurrenceRule` model, `RRuleCodec`, `RecurrenceEngine` (exdates, overrides)
- [ ] Recurrence picker UI; scope dialogs for edit and delete ("this event" / "all events")
- [ ] `ReminderPlanner`, `AlarmReminderScheduler` (exact with inexact fallback), notification channel and notifier, deep link
- [ ] `SystemEventsReceiver` (boot, time zone, time set, package replaced, exact-alarm permission change) and `RescheduleRemindersWorker`
- [ ] Tests: recurrence (all frequencies, interval, until/count, 31st and Feb 29, nth weekday, DST in Europe/Berlin and America/New_York, exdates, overrides), reminder planning (horizon, cap, all-day, time-zone change), scheduler reconciliation (mocked AlarmManager wrapper)

### M4 — Settings, onboarding and permissions
- [ ] Settings screen with all items in §6 (Premium, backup and sync rows locked or placeholder until M5/M6)
- [ ] Theme and first-day-of-week applied app-wide; default reminder and event length used by the editor
- [x] In-app language picker (D8): Settings → Language, AppCompat per-app locales (done early, 2026-10-25)
- [ ] 3-page onboarding → notification explainer and request → exact-alarm explainer → calendar
- [ ] Graceful handling of denied permissions, plus re-entry points from Settings
- [ ] Tests: settings and onboarding ViewModels, first-day-of-week week math

### M5 — Billing, PremiumManager, paywall, feature gating
- [ ] Config switch: `lifelog.revenuecat.*` keys replace `lifelog.billing.*` in `local.properties(.example)` and `BuildConfig`
- [ ] `:billing` module with the RevenueCat SDK: configure, offerings (monthly/annual), purchase, pending, plan switch, restore, refresh on start and resume, customer-info listener
- [ ] `EntitlementCache` (offline), `PurchaseVerifier` with `RevenueCatPurchaseVerifier`
- [ ] **RevenueCat dashboard, done by me in the browser after you OK each step:** create the "Life Log" project and Android app (needs the final application ID, B3); entitlement `premium`; products `life_log_premium:monthly` / `:yearly`; default offering with monthly and annual packages
- [ ] **Done by you:** upload the Google Play service-account key in RevenueCat and enable Real-time Developer Notifications. I don't handle credentials. Then copy the public `goog_` SDK key into `local.properties`.
- [ ] Real `PremiumManagerImpl` and `FeatureGate`; locked-feature UI; upgrade entry points hidden for Premium
- [ ] Paywall screen (prices from Play, saving %, trial text, restore, Terms/Privacy, auto-renew notice)
- [ ] Settings: Premium status and Manage subscription
- [ ] Debug-only fake-Premium toggle (debug source set)
- [ ] Tests: entitlement mapping (active / trial / cancelled-not-expired / grace period / account hold / expired / pending / error keeps cache / wrong entitlement rejected), savings calculation, paywall ViewModel. RevenueCat is mocked through `RevenueCatClient`.

### M6 — Premium features
- [ ] Unlimited categories and custom color picker; multiple reminders (up to 5)
- [ ] Journal: day entry and mood, timeline, entry point from the calendar day
- [ ] Glance widgets: today's agenda and month; update triggers; locked state
- [ ] Device-calendar sync per D1 (permission flow, calendar picker, read-only overlay, one-way push)
- [ ] Backup & restore (versioned JSON, validation, transactional replace, reschedule reminders afterwards)
- [ ] Statistics screen with Canvas charts and text summaries
- [ ] Downgrade behavior per D2
- [ ] Tests: statistics calculator, backup round-trip and invalid-file rejection, sync mapping and hash, gate behavior on downgrade

### M7 — Tests, accessibility, release
- [ ] Compose UI tests: create an appointment end to end; paywall (plans shown, saving %, trial text, restore, error state)
- [ ] Accessibility pass (semantics audit, 200 % font scale, touch targets, contrast) and a manual TalkBack checklist for you
- [ ] Tablet and rotation pass
- [ ] R8 release config, keep rules, `assembleRelease` succeeds
- [x] Release build checked for debug-only code: `verifyReleaseHasNoDebugCode` runs before every release package and fails if developer options or fake Premium are in it (done early, 2026-10-26)
- [ ] Merged-manifest permission review (`INTERNET` only via RevenueCat; no other network code)
- [ ] `RELEASE.md`: signing and Play App Signing, subscription product, base plan and offer setup, RevenueCat ↔ Play connection (service account, RTDN), license testers and sandbox purchases, data-safety form (incl. RevenueCat), content rating, store listing (en/de), privacy policy, closed and open test tracks, pre-launch report, rollout

---

## 9. What I can verify, and what you need to check on a device

**I verify:** builds, JVM unit tests, that the instrumented tests compile (D7), lint, release minification, and the merged manifest. With your OK, I can also set up the RevenueCat dashboard in the browser pane.

**You need a device or emulator for:**
- real notifications and alarms across reboot and time-zone changes
- TalkBack
- widgets on the home screen
- device-calendar sync
- **real purchases**, which only work once the app is uploaded to a Play test track and your account is added as a license tester (covered step by step in `RELEASE.md`)

I'll include a short manual test checklist with each milestone summary where it applies.
