# BV TV UI Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle BV's TV app into a YouTube-style home (featured header + rows), creator-first cards, a Netflix-style show page, and YouTube-style player controls, without changing the app's structure.

**Architecture:**
- Shared design tokens (colors, focus) live in `app/shared/ui/theme`.
- Pure presentation logic lives in `app/shared` and `player/tv` as plain Kotlin functions with JVM unit tests: time text, card lines, show-page labels, and the player key reducer.
- Compose screens in `app/tv` and `player/tv` consume that logic.
- Data gaps (avatar, publish time, rating, region, wide art, UP info in the player) are filled by small optional fields with defaults, so every API mode and `app/mobile` keep compiling.

**Tech Stack:** Kotlin, Jetpack Compose with `androidx.tv.material3`, Coil (`AsyncImage`), Koin, kotlinx.serialization, JUnit via `kotlin.test`.

**Spec:** `docs/superpowers/specs/2026-10-21-tv-ui-refresh-design.md` (read it before starting. The plan argues from it and does not repeat its rationale).

## Global Constraints

- **Environment for every Gradle command:**
  `export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home ANDROID_HOME=/opt/homebrew/share/android-commandlinetools`
- **Compile check (run at the end of every task):**
  `./gradlew :app:tv:compileDebugKotlin :app:mobile:compileDebugKotlin :player:tv:compileDebugKotlin`
  → `BUILD SUCCESSFUL`. `app/mobile` must keep compiling, and its behaviour must not change.
- **Colors (copy exactly):**

  | Token | Value |
  |---|---|
  | background | `#0F0F10` |
  | primary text | `#F1F1F1` |
  | secondary text | `#AAAAAA` |
  | accent | `#FB7299` |

  Use the accent only for selected and highlight details, never for large areas.
- **Side menu labels:** `UGC` → `分区`, `PGC` → `番剧影视`.
- **Time text:**
  - Under 1 minute: `刚刚`.
  - Under 1 hour: `N分钟前`.
  - Under 1 day: `N小时前`.
  - Under 7 days: `N天前`.
  - Otherwise `MM-dd` in the current year, `yyyy-MM-dd` in other years.
  - Never a raw timestamp.
- **Counts:** keep the existing 万/亿 formatting.
- **Timings:** the home header changes after **300ms** of resting focus, with a crossfade. The player controls layer auto-hides after **5s** without input while playing, and stays visible while paused.
- **Episode ranges:** shows with more than **20** episodes get chips of 20 (`1–20`, `21–40`, …).
- **Player hint line (exact):** `←/→ 快退/快进 · ↓ 更多操作 · ↑ 播放列表 · Menu 设置`
- **Show Play button text (exact):**
  - `继续播放 第N话 · 剩 M 分钟`
  - `播放 第1话`
  - `正片均需大会员或付费观看` (disabled)
- **No new network requests per card, per header change, or per player open.** Use only data already loaded.
- **Both API modes (Web and App) must load every screen.** Any field may be missing, so render without it.
- **TV layout density:** the layout is 960dp wide (`Theme.kt` sets density = widthPixels / 960), so 1dp = 2px on the onn.
- **On-device check:** adb at `192.168.68.70:5555`, using the optimized build:
  1. Build: `./gradlew assembleDefaultR8Test`
  2. Align and sign:
     ```
     BT=$ANDROID_HOME/build-tools/36.0.0
     IN=app/build/outputs/apk/default/r8Test/BV_894_0.3.0.r894.0e454c06.r8Test_default_universal.apk
     OUT=<scratch>/BV_r8Test_signed.apk
     $BT/zipalign -f -p 4 $IN $OUT.a
     $BT/apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android --ks-key-alias androiddebugkey --out $OUT $OUT.a
     ```
  3. Install and compile:
     ```
     adb -s 192.168.68.70:5555 install -r $OUT
     adb -s 192.168.68.70:5555 shell cmd package compile -m speed -f dev.aaa1115910.bv.r8test
     ```
  4. Screenshot: `adb -s 192.168.68.70:5555 shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png`. If the screen is black, the box is asleep: send `input keyevent KEYCODE_WAKEUP` first.
- **Commits:** follow the repo's style (short Chinese subject lines are fine) and end each one with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Missing card or header data** (App-API feeds have no avatar; some feeds have no `pubdate`, `dimension` or description). The card shows the UP icon and leaves out the missing parts, with no stray `·` and no `0小时前`. Tests: Task 2 `metaLine` and Task 5 `statsLine`.
2. **Logged out, or an empty follow feed.** The 关注的 UP 主 row is absent, not an empty strip. Test: Task 5 `homeRows`.
3. **A show with no visible episodes, or history pointing at a hidden members-only episode.** The Play button is disabled with the exact text, or falls back to `播放 第1话`. Tests: Task 6.
4. **The player opened without UP data** (from history, dynamics or search), and shows (no UP at all). The header shows the title only, or the show title over `第N话`. Test: Task 8 `playerHeader`.
5. **Back while the controls layer is visible versus hidden,** while playing and while paused. Back hides the layer first and only exits from the hidden state, as today. Tests: Task 8 reducer.

---

### Task 1: Design tokens and TV dark theme

**Files:**
- Create: `app/shared/src/main/kotlin/dev/aaa1115910/bv/ui/theme/BvColors.kt`
- Modify: `app/shared/src/main/kotlin/dev/aaa1115910/bv/ui/theme/Theme.kt` (the `tvDarkColorScheme` declaration, about line 46)

**Interfaces:**
- Produces: `object BvColors { val Background: Color; val TextPrimary: Color; val TextSecondary: Color; val Accent: Color; val ScrimStrong: Color /* #E6000000 */; val ChipFill: Color /* white @ 12% */ }`. Every later UI task uses these names.

- [ ] **Step 1:** Create `BvColors` with the Global Constraints values (`0xFF0F0F10`, `0xFFF1F1F1`, `0xFFAAAAAA`, `0xFFFB7299`), plus `ScrimStrong = Color(0xE6000000)` and `ChipFill = Color(0x1FFFFFFF)`.
- [ ] **Step 2:** In `BVTheme`, set these on `tvDarkColorScheme`:
  - `background` and `surface` = `BvColors.Background`
  - `onBackground` and `onSurface` = `BvColors.TextPrimary`
  - `onSurfaceVariant` = `BvColors.TextSecondary`
  - keep `border = Color.White`

  Leave `primary`, the light scheme, and the mobile (`androidx.compose.material3`) schemes untouched.
- [ ] **Step 3:** Run the compile check. Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 4:** Do the on-device check. Expected: home and settings have a near-black background, and text is still readable.
- [ ] **Step 5:** Commit with the message `界面：新增配色并调整 TV 深色主题`.

### Task 2: Card data: avatar, publish time, time text, card lines

**Files:**
- Modify: `bili-api/src/main/kotlin/dev/aaa1115910/biliapi/entity/ugc/UgcItem.kt`
- Create: `app/shared/src/main/kotlin/dev/aaa1115910/bv/util/RelativeTime.kt`
- Modify: `app/shared/src/main/kotlin/dev/aaa1115910/bv/entity/carddata/VideoCardData.kt`
- Test: `bili-api/src/test/kotlin/dev/aaa1115910/biliapi/entity/ugc/UgcItemFieldsTest.kt`
- Test: `app/shared/src/test/kotlin/dev/aaa1115910/bv/util/RelativeTimeTest.kt`
- Test: `app/shared/src/test/kotlin/dev/aaa1115910/bv/entity/carddata/VideoCardDataTest.kt`

**Interfaces:**
- Produces:
  - `UgcItem.authorFace: String = ""`
  - `UgcItem.pubDate: Long = 0L` (epoch seconds)
  - `fun formatRelativeTime(epochSeconds: Long, nowMillis: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String` (returns `""` when `epochSeconds <= 0`)
  - `VideoCardData.upFace: String = ""`
  - `fun VideoCardData.metaLine(): String`
- `VideoCardData.pubTimeString` is now computed with `formatRelativeTime`.

- [ ] **Step 1: Write the failing tests.**

  `RelativeTimeTest`, with `now = 2026-10-21T12:00:00+08:00` and `zone = Asia/Shanghai`:
  ```kotlin
  assertEquals("刚刚", formatRelativeTime(now - 30))
  assertEquals("5分钟前", formatRelativeTime(now - 5 * 60))
  assertEquals("7小时前", formatRelativeTime(now - 7 * 3600))
  assertEquals("2天前", formatRelativeTime(now - 2 * 86400))
  assertEquals("10-01", formatRelativeTime(/* 2026-10-01 */))
  assertEquals("2025-12-31", formatRelativeTime(/* 2025-12-31 */))
  assertEquals("", formatRelativeTime(0))
  ```
  `VideoCardDataTest`:
  ```kotlin
  assertEquals("影视飓风 · 103万播放 · 7小时前", card(upName = "影视飓风", play = 1035185, pubTime = now - 7 * 3600).metaLine())
  assertEquals("影视飓风", card(upName = "影视飓风", play = null, pubTime = null).metaLine())
  assertEquals("103万播放", card(upName = "", play = 1035185, pubTime = 0).metaLine())
  ```
  Pass `now` through to the time computation for testability, for example with an internal `metaLine(nowMillis)` overload.

  `UgcItemFieldsTest` (network, like the other bili-api tests):
  ```kotlin
  val item = UgcItem.fromVideoInfo(BiliHttpApi.getPopularVideoData().data!!.list.first())
  assertTrue(item.authorFace.startsWith("http"))
  assertTrue(item.pubDate > 0)
  ```
- [ ] **Step 2:** Run
  `./gradlew :app:shared:testDebugUnitTest --tests '*RelativeTimeTest' --tests '*VideoCardDataTest'`
  and `./gradlew :bili-api:test --tests '*UgcItemFieldsTest'`.
  Expected: FAIL (unresolved references).
- [ ] **Step 3:** Add `authorFace` and `pubDate` to `UgcItem`, as trailing parameters with defaults. Fill them in the mappers whose source has them:
  - `fromRcmdItem(RcmdTopData.RcmdItem)`: `owner?.face`, `pubdate`
  - `fromVideoInfo`: `owner.face`, `pubdate`
  - `fromRegionDynamicListItem`: `face`, `pubdate`
  - `fromRegionRcmdArchive`: `pubdate` only (no face in its `Author`)

  Leave the others at their defaults. Then implement `formatRelativeTime`, add `upFace`, switch `pubTimeString` to `formatRelativeTime`, and add `metaLine()`. It joins upName, `"${playString}播放"` and pubTimeString with `" · "`, skipping blanks.
- [ ] **Step 4:** Run the same commands. Expected: PASS.
- [ ] **Step 5:** Run the compile check, then commit with the message `卡片数据：UP 头像、发布时间与相对时间`.

### Task 3: Video card and focus styling

**Files:**
- Create: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/component/BvCardDefaults.kt`
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/component/videocard/SmallVideoCard.kt`
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/component/videocard/LargeVideoCard.kt`
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/component/videocard/SeasonCard.kt`
- Modify: every `VideoCardData(` construction in `app/tv` (find them with `grep -rn "VideoCardData(" app/tv/src`)

**Interfaces:**
- Consumes: `BvColors` (Task 1); `VideoCardData.upFace`, `metaLine()` and `pubTime` (Task 2).
- Produces: `object BvCardDefaults { @Composable fun scale(): CardScale; @Composable fun border(): CardBorder; @Composable fun glow(): CardGlow }`.
  - Focused scale is 1.06.
  - The focused border is a 1.5dp white ring, following the card shape.
  - The focused glow is a soft dark shadow.

  Task 7 and Task 11 reuse it.
- `SmallVideoCard`'s public signature is unchanged.

- [ ] **Step 1:** Create `BvCardDefaults` with the TV `CardDefaults.scale/border/glow` builders and the values above.
- [ ] **Step 2:** Lay out `SmallVideoCard` like this:
  - 16:9 thumbnail with rounded corners.
  - Duration bottom-right on a dark pill, and the existing 4K tag top-right.
  - Remove the play and danmaku counts from the cover.
  - Below it, a row: a 22dp circular `AsyncImage` of `upFace` (the existing `UpIcon` when blank), then a column with the title (max 2 lines, ellipsis) and `metaLine()` (1 line, `TextSecondary`).
  - Apply `BvCardDefaults`.
- [ ] **Step 3:** Give `LargeVideoCard` the same thumbnail rules and focus. `SeasonCard` gets `BvCardDefaults` focus and rounded corners, and keeps its score badge.
- [ ] **Step 4:** At each `VideoCardData(` call site, pass `upFace` and `pubTime` from the source item where they exist:
  - `UgcItem`: `authorFace` and `pubDate`
  - `DynamicVideo`: `avatar` and `time`
  - History and favourites entities: their face/time fields, if they have them

  Otherwise leave them unset.
- [ ] **Step 5:** Run the compile check, then the on-device check on 推荐 and 分区. Expected: the mockup card layout, a scaled focused card with a white ring, and no broken cards in the App API mode (settings → 接口偏好).
- [ ] **Step 6:** Commit with the message `卡片：YouTube 风格视频卡片与焦点效果`.

### Task 4: Side menu labels, accent and re-select

**Files:**
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/screens/main/DrawerContent.kt` (the `DrawerItem` enum at about line 193; the item `onClick` at about line 156)
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/screens/MainScreen.kt` (about lines 71, 121 and 154)

**Interfaces:**
- Produces:
  - `DrawerContent(..., onReselect: (DrawerItem) -> Unit = {})`. It's called when the already-selected item is clicked.
  - `HomeContent(..., reloadKey: Int = 0)`. Task 5 reloads 为你推荐 and scrolls to the top whenever `reloadKey` increases.

- [ ] **Step 1:** Change `displayName` to `"分区"` for `UGC` and `"番剧影视"` for `PGC`. Keep the enum constant names.
- [ ] **Step 2:** In the item `onClick`, call `onReselect(item)` when `item == selectedItem`. Style the selected item with a `BvColors.Accent` icon and text and a subtle `ChipFill` background, replacing the large light pill.
- [ ] **Step 3:** In `MainScreen`, keep `var homeReloadKey by remember { mutableIntStateOf(0) }` and increment it in `onReselect` when the item is `DrawerItem.Home`. Pass it to `HomeContent(reloadKey = homeReloadKey)`, and add the parameter to `HomeContent` (unused until Task 5).
- [ ] **Step 4:** Run the compile check and the on-device check. Expected: the side menu reads 分区 and 番剧影视, and the selected item is pink.
- [ ] **Step 5:** Commit with the message `侧边栏：分区/番剧影视 命名与选中样式`.

### Task 5: Home: featured header and rows

**Files:**
- Create: `app/shared/src/main/kotlin/dev/aaa1115910/bv/entity/carddata/HomeFeaturedItem.kt`
- Create: `app/shared/src/main/kotlin/dev/aaa1115910/bv/viewmodel/home/HomeRows.kt`
- Create: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/screens/main/home/HomeFeaturedHeader.kt`
- Create: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/screens/main/home/HomeVideoRow.kt`
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/screens/main/HomeContent.kt` (rewrite the body; keep the ViewModels and the startup loading)
- Delete: `screens/main/home/RecommendScreen.kt`, `PopularScreen.kt` and `DynamicsScreen.kt`, plus `HomeTopNavItem` if `grep` shows no other users. Move their click handling (video vs. season jump) into `HomeContent`.
- Test: `app/shared/src/test/kotlin/dev/aaa1115910/bv/entity/carddata/HomeFeaturedItemTest.kt`
- Test: `app/shared/src/test/kotlin/dev/aaa1115910/bv/viewmodel/home/HomeRowsTest.kt`

**Interfaces:**
- Consumes: `UgcItem.authorFace/pubDate`, `formatRelativeTime` and `metaLine` (Task 2); `SmallVideoCard` and `BvCardDefaults` (Task 3); `HomeContent.reloadKey` (Task 4).
- Produces:
  - `data class HomeFeaturedItem(val aid: Long, val title: String, val cover: String, val upName: String, val upFace: String, val play: Int?, val pubTime: Long, val durationSec: Int, val is4K: Boolean, val description: String = "")`
  - `fun HomeFeaturedItem.statsLine(nowMillis: Long = System.currentTimeMillis()): String` (excludes 4K; the header draws 4K in the accent)
  - `HomeFeaturedItem.Companion.fromUgcItem(UgcItem)` and `fromDynamicVideo(DynamicVideo)`
  - `enum class HomeRow { Recommend, Following, Popular }` and `fun homeRows(isLogin: Boolean, followingCount: Int): List<HomeRow>`

- [ ] **Step 1: Write the failing tests.**
  ```kotlin
  assertEquals("103万播放 · 7小时前 · 24:52", item(play = 1035185, pubTime = now - 7 * 3600, durationSec = 1492).statsLine(now))
  assertEquals("24:52", item(play = null, pubTime = 0, durationSec = 1492).statsLine(now))
  assertEquals("1:28:44", item(play = null, pubTime = 0, durationSec = 5324).statsLine(now))
  assertEquals(listOf(HomeRow.Recommend, HomeRow.Popular), homeRows(isLogin = false, followingCount = 10))
  assertEquals(listOf(HomeRow.Recommend, HomeRow.Popular), homeRows(isLogin = true, followingCount = 0))
  assertEquals(listOf(HomeRow.Recommend, HomeRow.Following, HomeRow.Popular), homeRows(isLogin = true, followingCount = 3))
  ```
- [ ] **Step 2:** Run `./gradlew :app:shared:testDebugUnitTest --tests '*HomeFeaturedItemTest' --tests '*HomeRowsTest'`. Expected: FAIL.
- [ ] **Step 3:** Implement `HomeFeaturedItem`, `statsLine` and `homeRows`. The duration format is `m:ss` / `mm:ss` / `h:mm:ss`. `is4K` comes from `UgcItem.dimension?.is4K`.
- [ ] **Step 4:** Run the tests again. Expected: PASS.
- [ ] **Step 5:** Build `HomeFeaturedHeader(modifier, item: HomeFeaturedItem?)` (about the top 46% of the screen):
  - The cover is anchored right at about 68% width, with a left and bottom fade to `BvColors.Background`.
  - On the left: avatar and name, the title (2 lines), `statsLine()` with a trailing accent `4K` when `is4K`, and the description (2 lines) when it isn't blank.
  - `Crossfade` keyed on `aid`.
  - Request the cover through `resizedImageUrl` at a header-sized `ImageSize`. Add an enum entry if none fits.
- [ ] **Step 6:** Build `HomeVideoRow(title: String, items: List<VideoCardData>, onItemFocused: (Int) -> Unit, onItemClick: (Int) -> Unit, onNearEnd: () -> Unit)`:
  - A `LazyRow` of `SmallVideoCard`s.
  - Call `onNearEnd` when the focused index is `>= size - 5`.
- [ ] **Step 7:** Rewrite `HomeContent`:
  - No `TopNav`.
  - The header above a `LazyColumn` of the rows from `homeRows(...)`, titled 为你推荐, 关注的 UP 主 and 热门, sourced from `recommendViewModel`, `dynamicViewModel.dynamicVideoList` and `popularViewModel`.
  - Focusing a card sets a pending `HomeFeaturedItem`. A `LaunchedEffect(pending) { delay(300); shown = pending }` drives the header. Before anything is focused, the header shows the first 推荐 item.
  - On load, 推荐 loads first; popular and dynamic load after it completes.
  - Back from a row below the top scrolls to and focuses the first card of the top row. Back on the top row focuses the side menu (`navFocusRequester`).
  - When `reloadKey` increases: `recommendViewModel.clearData()`, `loadMore()`, scroll to the top.
- [ ] **Step 8:** Run the compile check and the on-device check. Expected:
  - The mockup C layout.
  - Moving quickly across 为你推荐 changes the header only after you stop.
  - Load-more works in each row.
  - With the App API mode, the header and rows still render.
- [ ] **Step 9:** Commit with the message `首页：推荐大图头部与分行布局`.

### Task 6: Show data and show-page logic

**Files:**
- Modify: `bili-api/src/main/kotlin/dev/aaa1115910/biliapi/http/entity/season/WebSeasonData.kt` (add `val areas: List<Area> = emptyList()`, reusing or adding `Area(id: Int, name: String)`)
- Modify: `bili-api/src/main/kotlin/dev/aaa1115910/biliapi/entity/video/season/SeasonDetail.kt`
- Create: `app/shared/src/main/kotlin/dev/aaa1115910/bv/viewmodel/season/SeasonPage.kt`
- Test: `app/shared/src/test/kotlin/dev/aaa1115910/bv/viewmodel/season/SeasonPageTest.kt`

**Interfaces:**
- Produces:
  - Fields on `SeasonDetail`, all trailing with defaults: `rating: Float? = null`, `areas: List<String> = emptyList()` and `wideCover: String = ""`.
    - Web: `rating?.score`, `areas.map { it.name }`, `bkgCover`.
    - App: `areas` and the rating if `AppSeasonData` has one; otherwise defaults.
  - `sealed interface SeasonBackdrop { data class Image(val url: String); data class BlurredPoster(val url: String) }` and `fun seasonBackdrop(detail: SeasonDetail): SeasonBackdrop`. The order is: `wideCover`, then the first episode cover (episodes, then sections), then `BlurredPoster(cover)`.
  - `data class PlayButtonState(val text: String, val enabled: Boolean, val epid: Int?)` and `fun playButtonState(detail: SeasonDetail, progress: SeasonDetail.UserStatus.Progress?, hiddenPaidEpisodeCount: Int): PlayButtonState`.
  - `fun episodeRanges(count: Int, size: Int = 20): List<IntRange>`. It returns `emptyList()` when `count <= size`; otherwise 1-based ranges.
  - `data class SeasonFacts(val rating: String?, val items: List<String>, val tags: List<String>)` and `fun seasonFacts(detail: SeasonDetail): SeasonFacts`. The rating is `"★ 9.8"`; `items` are year (from `publish.publishDate`), regions and `newEpDesc`, skipping blanks; `tags` = `styles`.

- [ ] **Step 1: Write the failing tests.** `Episode.duration` is in **milliseconds** and `Progress.lastTime` in **seconds**. Check this against `Episode.fromEpisode` and the season API, and correct the test if it's wrong.
  ```kotlin
  assertEquals(PlayButtonState("继续播放 第3话 · 剩 12 分钟", true, ep3.id), playButtonState(detail(eps = ep1..ep5 /* 23:05 each */), progress(lastEpId = ep3.id, lastTime = 660), 0))
  assertEquals(PlayButtonState("播放 第1话", true, ep1.id), playButtonState(detail(eps = ep1..ep5), null, 0))
  assertEquals(PlayButtonState("播放 第1话", true, ep1.id), playButtonState(detail(eps = ep1..ep5), progress(lastEpId = 999, lastTime = 10), 3))
  assertEquals(PlayButtonState("正片均需大会员或付费观看", false, null), playButtonState(detail(eps = emptyList()), null, 200))
  assertEquals(PlayButtonState("播放 PV1", true, pv.id), playButtonState(detail(eps = listOf(pv /* title "PV1" */)), null, 0))
  assertEquals(emptyList(), episodeRanges(20))
  assertEquals(listOf(1..20, 21..40, 41..60, 61..80, 81..92), episodeRanges(92))
  assertEquals(SeasonBackdrop.Image("wide"), seasonBackdrop(detail(wideCover = "wide")))
  assertEquals(SeasonBackdrop.Image("ep1cover"), seasonBackdrop(detail(wideCover = "", eps = listOf(ep1))))
  assertEquals(SeasonBackdrop.BlurredPoster("poster"), seasonBackdrop(detail(wideCover = "", cover = "poster", eps = emptyList())))
  assertEquals(SeasonFacts("★ 9.8", listOf("1999", "日本", "已完结, 全92话"), listOf("奇幻", "热血")), seasonFacts(全职猎人Detail))
  assertEquals(SeasonFacts(null, listOf("已完结, 全92话"), emptyList()), seasonFacts(detail(rating = null, areas = emptyList(), publishDate = "", styles = emptyList())))
  ```
  The `第N话` rule: when an episode's `title` parses as an integer, use `第N话`; otherwise use the title as-is. M is `ceil((durationMs/1000 - lastTime) / 60.0)`, at least 1.
- [ ] **Step 2:** Run `./gradlew :app:shared:testDebugUnitTest --tests '*SeasonPageTest'`. Expected: FAIL.
- [ ] **Step 3:** Add the `bili-api` fields and mappings. Implement `SeasonPage.kt`.
- [ ] **Step 4:** Run the tests again. Expected: PASS. Then run `./gradlew :bili-api:test --tests '*VideoDetailWebApiTest'`, still PASS.
- [ ] **Step 5:** Run the compile check, then commit with the message `剧集页：评分/地区/背景图数据与按钮文案逻辑`.

### Task 7: Show page UI (Netflix-style)

**Files:**
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/screens/SeasonInfoScreen.kt`. Replace `SeasonInfoPart`, `SeasonCover` and `SeasonBaseInfo` with a hero. Rework `SeasonEpisodeRow` and `SeasonEpisodeButton`. Remove the use of `SeasonEpisodesDialog` and delete it if it's unused. Restyle `SeasonSelector` into a posters row.

**Interfaces:**
- Consumes: Task 6 functions, `BvColors`, `BvCardDefaults`, and the existing `seasonViewModel.hiddenPaidEpisodeCount` / `lastPlayProgress`.

- [ ] **Step 1: Hero.**
  - Backdrop from `seasonBackdrop()`: an image anchored right, about 72% wide by 78% tall, with a left and bottom fade. `BlurredPoster` uses a blurred, darkened poster.
  - The title is large, then the `seasonFacts()` line (rating in accent, items separated by spacing, tags as outlined chips), then the description clamped to 3 lines.
  - Buttons: Play (light fill, text from `playButtonState()`, disabled look when `!enabled`, initial focus), 追番 (the existing follow toggle) and 选集 (scrolls to the 正片 row).
  - Play's click keeps the existing `onPlay` playlist-building logic, but uses `PlayButtonState.epid` to choose the episode.
- [ ] **Step 2: 正片 row.**
  - 16:9 episode thumbnails with `第N话` bottom-left and the duration bottom-right on the image, and the episode title below.
  - The last-watched episode gets an accent outline and a progress bar (`lastTime / duration`).
  - When `episodeRanges(size)` isn't empty, show range chips above the row; choosing one shows that slice.
- [ ] **Step 3: Other rows.** Each extra section becomes its own row in the same style. The other seasons (`seasons`) become a row of posters using `SeasonCard` styling.
- [ ] **Step 4:** Run the compile check and the on-device check on 全职猎人 (all free), 银魂 (all hidden: Play disabled with the exact text and no 正片 row), and a show with sections. Expected: matches `show-page.html`. Focus starts on Play.
- [ ] **Step 5:** Commit with the message `剧集页：Netflix 风格详情页`.

### Task 8: Player: UP info plumbing and the controls-layer key reducer

**Files:**
- Modify: `player/shared/src/main/kotlin/dev/aaa1115910/bv/player/entity/VideoPlayerData.kt` (`VideoPlayerVideoInfoData` at line 24)
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/util/PlayerActivityUtil.kt` (`launchPlayerActivity`)
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/activities/video/VideoPlayerV3Activity.kt` (`actionStart` and the extras reading)
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/screens/VideoPlayerV3Screen.kt` (line 52)
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/screens/VideoInfoScreen.kt` (its `launchPlayerActivity` calls)
- Modify: `player/tv/build.gradle.kts` (add `testImplementation(libs.kotlin.test)`)
- Create: `player/tv/src/main/kotlin/dev/aaa1115910/bv/player/tv/controller/ControlsLayerState.kt`
- Test: `player/tv/src/test/kotlin/dev/aaa1115910/bv/player/tv/controller/ControlsLayerStateTest.kt`

**Interfaces:**
- Produces:
  - `VideoPlayerVideoInfoData` gains `upName: String = ""`, `upFace: String = ""` and `upFollowed: Boolean = false`. Mobile's constructor call is unchanged.
  - `launchPlayerActivity(..., upName: String = "", upFace: String = "", upFollowed: Boolean = false)`, and the same on `actionStart`, as intent extras `"upName"`, `"upFace"` and `"upFollowed"`.
  - `data class PlayerHeader(val small: String, val smallFace: String, val title: String)` and `fun playerHeader(info: VideoPlayerVideoInfoData, fromSeason: Boolean): PlayerHeader`.
    - Video with an UP: `(upName, upFace, title)`.
    - Show: `(title, "", partTitle)`.
    - Neither: `("", "", title)`.
  - `enum class ControlsFocus { Hidden, ProgressBar, Buttons }`, `enum class RemoteKey { Ok, Left, Right, Up, Down, Back, Menu }`, `data class ControlsState(val focus: ControlsFocus = ControlsFocus.Hidden, val buttonIndex: Int = 0)`.
  - `sealed interface ControlsAction { PassThrough, None, TogglePlay, SeekBack, SeekForward, OpenPlaylist, OpenMenu, data class Activate(val index: Int) }` (the objects are `data object`).
  - `fun ControlsState.onKey(key: RemoteKey, buttonCount: Int): Pair<ControlsState, ControlsAction>` and `fun ControlsState.onPaused(): ControlsState` (Hidden → ProgressBar).

- [ ] **Step 1: Write the failing tests** (`ControlsLayerStateTest`). `H`, `P` and `B` are shorthand for `ControlsState(Hidden)`, `ControlsState(ProgressBar)` and `ControlsState(Buttons, i)`.
  ```kotlin
  // Hidden: everything except Down keeps today's behaviour.
  for (k in listOf(Ok, Left, Right, Up, Back, Menu)) assertEquals(H to PassThrough, H.onKey(k, 9))
  assertEquals(P to None, H.onKey(Down, 9))
  // Progress bar.
  assertEquals(P to SeekBack, P.onKey(Left, 9)); assertEquals(P to SeekForward, P.onKey(Right, 9))
  assertEquals(P to TogglePlay, P.onKey(Ok, 9)); assertEquals(H to OpenPlaylist, P.onKey(Up, 9))
  assertEquals(B(0) to None, P.onKey(Down, 9)); assertEquals(H to None, P.onKey(Back, 9))
  assertEquals(H to OpenMenu, P.onKey(Menu, 9))
  // Buttons.
  assertEquals(B(1) to None, B(0).onKey(Right, 9)); assertEquals(B(0) to None, B(0).onKey(Left, 9))
  assertEquals(B(8) to None, B(8).onKey(Right, 9)); assertEquals(B(4) to Activate(4), B(4).onKey(Ok, 9))
  assertEquals(P to None, B(3).onKey(Up, 9)); assertEquals(H to None, B(3).onKey(Back, 9))
  assertEquals(B(3) to None, B(3).onKey(Down, 9))
  assertEquals(P, H.onPaused()); assertEquals(B(2), B(2).onPaused())
  // Header.
  assertEquals(PlayerHeader("影视飓风", "f", "标题"), playerHeader(info(upName = "影视飓风", upFace = "f", title = "标题"), false))
  assertEquals(PlayerHeader("全职猎人", "", "第3话 自尊心×荒海×决斗"), playerHeader(info(title = "全职猎人", partTitle = "第3话 自尊心×荒海×决斗"), true))
  assertEquals(PlayerHeader("", "", "标题"), playerHeader(info(title = "标题"), false))
  ```
- [ ] **Step 2:** Add the test dependency, then run `./gradlew :player:tv:testDebugUnitTest --tests '*ControlsLayerStateTest'`. Expected: FAIL (unresolved).
- [ ] **Step 3:** Implement `ControlsLayerState.kt` and `playerHeader` (put `playerHeader` in the same file).
- [ ] **Step 4:** Run the tests again. Expected: PASS.
- [ ] **Step 5:** Wire the UP fields through `launchPlayerActivity` → `actionStart` → extras → `VideoPlayerV3Screen`'s `VideoPlayerVideoInfoData(...)`. In `VideoInfoScreen`, pass `videoDetail.author.name`, the author's face field (check the `Author` entity), and `isFollowing`.
- [ ] **Step 6:** Run the compile check, then commit with the message `播放器：UP 信息传递与控制层按键状态机`.

### Task 9: Player controls layer

**Files:**
- Create: `player/tv/src/main/kotlin/dev/aaa1115910/bv/player/tv/PlayerColors.kt`. `player/tv` does not depend on `app/shared`, so copy the four Global Constraints colors into `object PlayerColors { Background, TextPrimary, TextSecondary, Accent }` plus `ScrimStrong = Color(0xE6000000)`.
- Create: `player/tv/src/main/kotlin/dev/aaa1115910/bv/player/tv/controller/ControlsLayer.kt`
- Modify: `player/tv/src/main/kotlin/dev/aaa1115910/bv/player/tv/controller/VideoPlayerController.kt` (key handling at about lines 150–262; the `showInfo` usage)
- Modify: `player/tv/src/main/kotlin/dev/aaa1115910/bv/player/tv/controller/PlayStateTips.kt` (the pause indicator is no longer shown while the layer is visible)

**Interfaces:**
- Consumes: everything Task 8 produces; the existing `VideoPlayerController` callbacks (`onPlay`, `onPause`, `onGoTime`, `onPlayNewVideo`, `onDanmakuSwitchChange`, …).
- Produces:
  - `enum class ControlButtonId { PlayPause, Previous, Next, Resolution, Speed, Danmaku, Subtitle, Episodes, Settings }` (this order is the button order, so `buttonCount = 9`).
  - `@Composable fun ControlsLayer(state: ControlsState, header: PlayerHeader, positionMs: Long, durationMs: Long, bufferedMs: Long, isPlaying: Boolean, buttonValues: Map<ControlButtonId, String>, clock: String)`.
  - `MenuController` gains `initialItem` (see Task 10).

- [ ] **Step 1:** Build `ControlsLayer` to match `player.html` section 1:
  - Top gradient: `header.smallFace` avatar when present, `header.small`, `header.title`, and the clock.
  - Bottom gradient: an accent progress bar with buffered and knob, elapsed/total, and the button row with values (`清晰度 4K`, `倍速 1.0×`, `弹幕 开/关`, `字幕 开/关`, `选集 P1`).
  - The focused element is lit according to `state.focus` and `state.buttonIndex`.
  - The hint line text is exact, shown while `focus == ProgressBar`.
- [ ] **Step 2:** In `VideoPlayerController`, hold `var controls by remember { mutableStateOf(ControlsState()) }`.
  - Map Compose keys to `RemoteKey` and call `controls.onKey(...)` first.
  - On `PassThrough`, run today's existing branch for that key unchanged.
  - Execute the actions:
    - `TogglePlay`: `onPlay` / `onPause`
    - `SeekBack` / `SeekForward`: today's seek code
    - `OpenPlaylist`: `showListController = true`
    - `OpenMenu`: `showMenuController = true`
    - `Activate(i)`:
      - PlayPause: toggle
      - Previous / Next: the neighbour in `availableVideoList`, via `onPlayNewVideo`, doing nothing at the ends
      - Resolution / Speed / Subtitle / Settings: open the menu at that item
      - Danmaku: toggle through `onDanmakuSwitchChange` between an empty list and the most recent non-empty list of enabled types, held in the controller (`player/tv` can't read the app's `Prefs`)
      - Episodes: the playlist
  - Call `onPaused()` whenever playback pauses.
  - Auto-hide (set `focus = Hidden`) after 5s without key input while playing.
  - Retire the ↓ info bar (`showInfo`). `ControllerVideoInfo` stays only if something else uses it.
- [ ] **Step 3:** Run the compile check and the on-device check while playing a video:
  - ↓ opens the layer on the progress bar, and ←/→ seek.
  - ↓ moves to the buttons, ←/→ move between them, and OK on 清晰度 opens quality.
  - ↑ returns to the progress bar.
  - Back hides the layer; a second Back behaves as today.
  - Pause shows the layer, and it stays visible.
  - It hides after 5s while playing.
  - Opening a 番剧 episode shows the show title over `第N话`.
- [ ] **Step 4:** Commit with the message `播放器：YouTube 风格控制层`.

### Task 10: Player settings panel and other overlays

**Files:**
- Modify: `player/tv/src/main/kotlin/dev/aaa1115910/bv/player/tv/controller/MenuController.kt` and `controller/playermenu/*`
- Modify: `player/tv/src/main/kotlin/dev/aaa1115910/bv/player/tv/controller/VideoListController.kt`, `SeekController.kt`, `VideoShot.kt`, `SkipTip.kt` and `PlayStateTips.kt`

**Interfaces:**
- Consumes: `PlayerColors` (Task 9).
- Produces: `MenuController(..., initialItem: VideoPlayerMenuItem? = null)` (match the existing menu item type), used by Task 9.

- [ ] **Step 1:** Restyle `MenuController` into a right-side panel to match `player.html` section 2:
  - About 30% width, `ScrimStrong` background, header `播放设置`.
  - Rows: 清晰度, 视频编码, 画面比例, 播放速度 and 音质 with their current values in secondary text, a divider, then 弹幕设置, 字幕设置 and 其它设置 with `›`.
  - The focused row has a light fill, with its value in the accent.
  - OK opens that item's existing options in the same panel; Back returns to the list, and Back again closes it.
  - Honour `initialItem`.
- [ ] **Step 2:**
  - The playlist panel, skip tip and pause indicator get the same dark panel and accent.
  - The seek preview (`VideoShot`) is drawn at about twice today's size above the knob.
  - The `再按一次退出` toast is left as a system toast.
- [ ] **Step 3:** Run the compile check and the on-device check. Expected: Menu shows the side panel with values, choosing 清晰度 → 1080P switches quality, and the seek preview is larger.
- [ ] **Step 4:** Commit with the message `播放器：设置侧边面板与浮层样式`.

### Task 11: Remaining screens: video page, category pages, search, settings, lists, empty states

**Files:**
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/screens/VideoInfoScreen.kt`
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/screens/main/UgcContent.kt`, `PgcContent.kt` and their `ugc/` and `pgc/` sub-screens
- Modify: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/component/TopNav.kt` (chip styling only)
- Create: `app/tv/src/main/kotlin/dev/aaa1115910/bv/tv/component/EmptyState.kt`
- Modify: every screen that renders `已加载 … 条数据` (find them with `grep -rn "已加载" app/tv/src app/shared/src/main/res`)

**Interfaces:**
- Consumes: `formatRelativeTime` (Task 2), `SmallVideoCard` and `BvCardDefaults` (Task 3), and `BvColors`.
- Produces: `@Composable fun EmptyState(text: String, modifier: Modifier = Modifier)`, an icon above a short line in `TextSecondary`.

- [ ] **Step 1: Video page.**
  - Keep the layout and the blurred background.
  - The UP block shows the avatar.
  - Replace `formatPubTimeString()` there with `formatRelativeTime(publishDate.time / 1000)`.
  - Buttons and tag chips use light fill on focus and `ChipFill` otherwise.
  - 视频推荐 cards pass `upFace` and `pubTime` where the data has them.
- [ ] **Step 2: Category pages.** Chips use the same light-fill-on-focus / `ChipFill` style. Grids use the new cards. Banners get rounded corners and the `BvCardDefaults` focus.
- [ ] **Step 3: Empty states.** Where a list is empty after loading, show `EmptyState`: `还没有追番` for 正在追, `还没有历史记录` for history, `这里还没有内容` elsewhere. Keep the `长按确认键更改筛选` hint where it exists.
- [ ] **Step 4:** Search and Settings get no layout changes. Fix any hard-coded colors that clash with the new background. Recolour the loading indicator (`LoadingTip.kt` and `LoadingIndicator` uses) to `BvColors.Accent`.
- [ ] **Step 5:** Run the compile check and the on-device check across every screen listed. Expected: no stock purple remains in the TV app, and the empty 正在追 shows `还没有追番`.
- [ ] **Step 6:** Commit with the message `界面：视频页、分区、空状态等统一样式`.

### Task 12: On-device verification, both API modes, performance

**Files:** none, unless a problem is found. Fix problems in the task that owns them and re-commit there.

- [ ] **Step 1:** Run all unit tests:
  `./gradlew :app:shared:testDebugUnitTest :player:tv:testDebugUnitTest`
  and `./gradlew :bili-api:test --tests '*DimensionTest' --tests '*VideoDetailWebApiTest' --tests '*UgcItemFieldsTest'`.
  Expected: all PASS.
- [ ] **Step 2:** Run `./gradlew assembleDefaultDebug assembleDefaultR8Test` (this also builds mobile). Expected: `BUILD SUCCESSFUL`.
- [ ] **Step 3:** Do the on-device check, walking through every screen in the spec with settings → 接口偏好 set to **Web**, then again with **App**. Every screen loads, and missing avatar, time or 4K data renders cleanly. Then switch settings → 界面设置 to the light theme and check that home, the show page and the player stay readable; switch back to dark.
- [ ] **Step 4: Smoothness.**
  1. Run `adb -s 192.168.68.70:5555 shell dumpsys gfxinfo dev.aaa1115910.bv.r8test reset`.
  2. Send 20 `KEYCODE_DPAD_RIGHT` presses 150ms apart along 为你推荐.
  3. Run `dumpsys gfxinfo dev.aaa1115910.bv.r8test` and read "Janky frames".
  4. Repeat on the show page's 正片 row.

  Target: janky frames ≤ 10%. If it's higher, reduce in this order:
  1. The header crossfade duration.
  2. Gradient overdraw: replace stacked gradients with one.
  3. The card focus glow.
  4. The header image size.

  Re-measure after each change.
- [ ] **Step 5:** Commit any fixes. Report the jank numbers and both API-mode results to the owner.
