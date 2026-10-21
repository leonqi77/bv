# BV TV UI refresh: design

Date: 2026-10-21 · Fork: `leonqi77/bv` · Scope: the Android TV app (`app/tv`, plus the shared pieces it uses in `app/shared`, `player/tv`, `bili-api`)

## Why

BV works, but on a TV it looks like a developer build: one flat grid, a thin white focus border, the library's stock purple-grey palette, internal jargon (UGC / PGC) in the menu, and bare screens such as "已加载 0 条数据". The owner watches on a Google TV (onn 4K Pro) and wants BV to feel as polished as YouTube for TV.

YouTube is the reference because Bilibili, like YouTube, is built around creators (UP主). Every video belongs to a channel, unlike Netflix-style catalogs where the title is what matters. So creator identity (avatar and name) is shown wherever a video is. Shows (番剧影视) are the exception and borrow Netflix's title page.

## Goals

1. A home screen in the style of YouTube's newer TV home: a large featured header for the focused video, with rows of cards below.
2. YouTube-style video cards everywhere: a clean thumbnail, the UP's avatar, a two-line title, and "UP · 播放量 · 时间".
3. Focus that is obvious from the couch.
4. A Netflix-style show page.
5. Player controls that show what you're watching and put common switches one press away.
6. One consistent look: near-black background, Bilibili pink accent, consistent text sizes.

## Non-goals

- No new features beyond what these screens need. No new data sources except the small field additions listed below.
- No change to how the remote keys work, except ↓ in the player (see Player).
- The phone app (`app/mobile`) is not touched and must still build.
- Danmaku rendering, subtitles, the playback engine, login, and the settings content stay as they are. Settings only picks up the new styling.

## Anti-goals (failure even if it "works")

- **Lag on the onn.** It is a 32-bit ARM box (armeabi-v7a, Google TV 14) with a modest GPU. If an effect (blur, large-image crossfades, scaling animations) makes D-pad movement stutter, tone it down or drop it. Smooth focus movement matters more than any single effect.
- **Losing a video's identity.** A card without its UP or title is a regression.
- **Making it a different app.** Sections, navigation structure, and key mappings stay recognisable. This is a refresh, not a rebuild.
- **Breaking either API mode.** BV can fetch through the Web API or the App API (settings → 接口偏好). Both must keep working, even where one provides less data.

## Constraints and facts the builder needs

- TV UI uses `androidx.tv.material3` (`app/shared/.../ui/theme/Theme.kt`, `Typography.kt`). The current dark scheme is close to library defaults. A light theme option (`ThemeType` Auto/Dark/Light) exists.
- Device output is 1920×1080. Keep the usual TV safe margins (roughly 48dp horizontal, 27dp vertical).
- Images: covers already go through a resizing helper (`resizedImageUrl(ImageSize…)`). Request sizes that match how large the image is drawn. The featured header needs a larger size than cards.
- **Card data gaps:** `UgcItem` (`bili-api/.../entity/ugc/UgcItem.kt`) has the UP name but no avatar and no publish time.
  - The web feeds used by 推荐, 热门 and the category pages provide `owner.face` / `pubdate` (or equivalents), so add them.
  - Some sources (notably the App API feed, `fromSmallCoverV5`) may not provide them. Cards must look right without an avatar (fall back to the existing UP icon) and without a time (omit that part).
- **Resolution:** a 4K tag is only shown where `dimension` exists (热门 today, plus the video page). Do not add per-card network requests to get it, because extra request bursts trigger Bilibili's risk control.
- **Show data gaps:** `SeasonDetail` has `styles`, `description`, `newEpDesc` and `publish`, but no rating or region. The web season API provides `rating.score` and `areas[].name`, so add them as optional fields. When they're missing, leave those parts out of the facts line.
- **Watch progress:** Bilibili reports only the last-watched episode and position (`userStatus.progress`: `lastEpId`, `lastTime`). Only that episode can show a progress bar.
- Members-only episodes are already hidden on show pages (setting 隐藏付费剧集). Keep that behaviour.

## Look and feel

- **Background:** near-black (about `#0F0F10`). Primary text about `#F1F1F1`, secondary about `#AAAAAA`.
- **Accent:** Bilibili pink `#FB7299`. Use it for the selected side-menu item, progress bars, ratings, the 4K highlight in text, and focus rings on primary buttons. Don't use it for large areas.
- **Focus (all focusable cards and buttons):**
  - Focused card: scales to about 1.05–1.08×, gets a 2–3dp white ring around the thumbnail, and a soft drop shadow.
  - Focused button or chip: switches to a light fill with dark text.
  - Movement must stay smooth on the onn (see Anti-goals).
- **Text:** one consistent scale across screens (page title / section title / card title / metadata). Card titles get two lines, then an ellipsis.
- **Times:**
  - Relative within a week: 刚刚, N分钟前, N小时前, N天前.
  - After that, a date: 2026-10-20, dropping the year when it's the current year.
  - Never a raw timestamp like `2026-10-20 04:00:00`.
- **Counts:** keep today's 万 / 亿 formatting.

## Screens

### Side menu (`DrawerContent.kt`)

Same items and order. Labels change: UGC → **分区**, PGC → **影视**, logged-out user item `点击登录` → **登录** (logged in still shows the username, ellipsized on one line). The expanded side menu is narrowed (about 160dp instead of the library default of about a third of the screen) to fit the two-character labels.

It stays collapsed to icons until focused, as it does now. When expanded, the selected item uses the pink accent instead of the large light pill.

### Home (`HomeContent.kt`, `screens/main/home/*`): featured header + rows

The top tab bar (推荐 / 热门 / 动态) is replaced by one vertically scrolling page:

1. **Featured header**, the top ~half of the screen, showing the currently focused video:
   - The cover image is anchored right and fades into the background on the left and bottom.
   - On the left: UP avatar + name, large title (up to 2 lines), then a stats line (播放量 · 时间 · 时长, plus a pink **4K** when known).
   - A short description (up to 2 lines) only when the feed provides one.
   - The header changes only after focus has rested for about 300ms, with a short crossfade, so fast scrolling doesn't redraw it on every key press.
2. **Rows** below, each with a title, scrolling sideways:
   - **为你推荐** (today's 推荐 feed)
   - **关注的 UP 主** (today's 动态 feed; shown only when logged in)
   - **热门** (today's 热门 feed)

   Each row keeps its existing load-more behaviour when you scroll to its end.

Pressing ↓ moves to the next row. The header stays at the top and the rows move up underneath it. The new home page should open as fast as today's 推荐 tab: load 推荐 first, and the other rows after it.

**Refresh and Back (replacing what the tab bar did):**
- Choosing 首页 in the side menu while already on home reloads 为你推荐 and scrolls to the top, like tapping Home on YouTube. Today this is done by pressing OK on a tab.
- Back from any row jumps to the first card of the top row.
- Back on the top row moves focus to the side menu.

### Video cards (`SmallVideoCard.kt` and the screens that use it; `LargeVideoCard.kt` follows the same style)

- **Thumbnail (16:9, rounded corners):**
  - Duration bottom-right on a dark pill.
  - **4K** tag top-right when known.
  - Play and danmaku counts move off the image.
- **Below the thumbnail:**
  - UP avatar (circle) next to a two-line title.
  - Under the title: `UP名 · N万播放 · 时间`.
- Focus as described in Look and feel.

### Category pages (分区, 影视: `UgcContent.kt`, `PgcContent.kt` and sub-screens)

Same structure (category chips, banner carousel, sub-category chips, grids). They get the new cards, chip styling, colors and focus treatment. Show posters (`SeasonCard.kt`) get the same focus treatment and rounded corners, and keep their score badge.

### Video page (`VideoInfoScreen.kt`)

Layout stays the same, including the blurred-artwork background. It gets the new type scale, buttons and chips:
- UP block with avatar.
- Dates use the new time format.
- The resolution label added earlier stays.
- 视频推荐 uses the new cards.

### Show page (`SeasonInfoScreen.kt`): Netflix-style

- **Backdrop:**
  - The show's wide artwork fills the screen behind a dark left-and-bottom fade.
  - If the season has no wide artwork, use the first visible episode's still.
  - If there are no episodes either, use a blurred poster.
- **Top-left block:**
  - Large title.
  - One facts line: pink ★ rating · year · region · status/episode count (`newEpDesc`), then genre tags (`styles`) as small outlined chips. Missing facts are left out.
  - Description cut to 3 lines.
- **Buttons:**
  - Primary Play button, large, with light fill. Focus starts here when the page opens. Its label:
    - "继续播放 第N话 · 剩 M 分钟" when there's progress.
    - "播放 第1话" otherwise.
    - "正片均需大会员或付费观看" (disabled look) when everything is hidden.
  - Then 追番 (follow toggle) and 选集.
- **正片 row:**
  - Episode thumbnails (16:9) with 第N话 and duration on the image, and the episode title below.
  - The last-watched episode gets a pink outline and a progress bar.
  - Shows with more than 20 episodes get range chips (1–20, 21–40, …) above the row, which replace today's paging dialog.
- **Extra sections** (PV, 花絮, 经典回顾…): their own rows below, same thumbnails.
- **Other seasons** (系列共 N 部): a row of season posters at the bottom.

### Player (`player/tv/.../controller/*`)

Key mapping stays the same (OK play/pause, ←/→ seek, ↑ playlist, Menu settings, Back exit with confirm), with one change: **↓ now opens the controls layer** instead of the 3-second info bar.

**Controls layer** (shown on pause, or on ↓):
- Top: a dark gradient with the UP avatar + name (and 已关注 when known), the title, and a clock on the right.
  - The UP data is passed in from the video page when it starts the player. It already has it in `VideoDetail`, so no new request is needed.
  - Add these as optional fields with defaults to the player's video-info data. That data is shared with `app/mobile`, which ignores them.
  - Shows have no UP. For a show, that spot shows the show's title, with 第N话 + episode title as the main title.
  - When the data is missing (for example, playing from another entry point), show the title only.
- Bottom: a dark gradient with:
  - A pink progress bar with a buffered portion and a knob, and elapsed/total time.
  - A row of buttons: play/pause, previous, next, then 清晰度 (current value), 倍速, 弹幕 on/off, 字幕, 选集, and ⚙ (opens the settings panel).
  - A small hint line explaining the keys.
- **Keys while the layer is showing** (YouTube for TV model, decided by the owner):
  - When the layer opens, focus is on the **progress bar**. ←/→ seek exactly as today, and OK plays/pauses.
  - ↓ moves focus to the button row, where ←/→ move between buttons and OK activates the focused button.
  - ↑ from the button row returns to the progress bar. ↑ on the progress bar opens the playlist, as today.
  - Back first hides the layer. Back with the layer hidden behaves exactly as today (exit when paused, confirm when playing).
  - The hint line reads "←/→ 快退/快进 · ↓ 更多操作 · ↑ 播放列表 · Menu 设置" while the progress bar has focus.
- **With the layer hidden,** every key behaves as today, except that ↓ opens the layer.
- The layer auto-hides after about 5 seconds without input while playing. It stays visible while paused.
- Each button opens the same choices the Menu offers today, in the panel below.

**Seeking (←/→):** same bar, with a larger preview thumbnail (`VideoShot`) above the knob.

**Settings panel (Menu):**
- A solid dark side panel on the right (about 30% of the width) instead of floating text.
- Header 播放设置, with rows showing their current values: 清晰度, 视频编码, 画面比例, 播放速度, 音质. Then 弹幕设置, 字幕设置, 其它设置.
- Choosing a row opens its options in the same panel; Back returns.

**Also restyled:** the playlist (↑), the pause indicator, the skip tips and the "再按一次退出" prompt all get the same dark panel and pink accent.

### Search, Settings, lists, empty and loading states

- **Search and Settings:** same layout, new colors, focus and type.
- **Lists such as 正在追, 历史记录 and 收藏:** new cards.
- **Empty lists:** an icon plus a short friendly line, such as "还没有追番" or "这里还没有内容", instead of "已加载 0 条数据". The 长按确认键更改筛选 hint stays where it applies.
- **Loading:** keep the existing indicator, recoloured.

## Decided by the owner

- Home direction: featured header + rows (option C of three mockups).
- Side menu renamed to 分区 / 影视, `点击登录` → `登录`, and the expanded side menu narrowed to fit the two-character labels.
- Show page: Netflix-style, as in the mockup.
- Player controls included, as in the mockup, including ↓ opening the controls layer. While it is open, keys follow the YouTube model (progress bar first, ↓ to buttons, Back hides the layer).
- YouTube is the main reference, because BV is creator-centric.

## Calls made during design (not explicitly confirmed)

- Only the last-watched episode shows a progress bar. The mockup also marked earlier episodes as watched, but Bilibili doesn't provide that data.
- On home, 推荐/热门/动态 become rows, and the tab bar goes away.
- Featured header: changes after a ~300ms focus pause, with a crossfade.
- Accent color is Bilibili pink `#FB7299`.
- The show backdrop falls back from wide art to the first episode still to a blurred poster.
- The light theme setting remains, but this refresh is designed and tuned for dark. Light only needs to stay readable.
- Mockups for reference (local only): `.superpowers/brainstorm/*/content/home-directions-v2.html`, `show-page.html`, `player.html`.

## Left to the builder

- Exact sizes, spacing, corner radii, animation durations and shadow values, within the guidance above.
- How to structure the code: shared design tokens, a reusable card, focus modifiers and so on. Prefer a small set of shared building blocks over per-screen styling.
- Whether the header and rows on home are one `LazyColumn` or another structure, as long as focus movement and load-more work.
- The order of work, provided each step leaves the app working. The work splits naturally into: theme and tokens → cards and focus → home → show page → player → remaining screens.
- Performance tuning on the onn: measure, then simplify effects that stutter.

## How we'll know it's done

- Checked on the owner's onn 4K Pro over adb (`192.168.68.70:5555`), using the optimized `r8Test` build (R8, not debuggable, compiled ahead of time with `cmd package compile -m speed`). The debug build is several times slower and doesn't count.
- On the onn, every screen above matches its description. Moving focus quickly through home rows and the episode row doesn't stutter or skip.
- Both API modes (Web and App) load home, category pages, video pages and show pages without errors, including when the avatar, time or 4K data is missing.
- `app/mobile` still builds. Existing tests pass.
