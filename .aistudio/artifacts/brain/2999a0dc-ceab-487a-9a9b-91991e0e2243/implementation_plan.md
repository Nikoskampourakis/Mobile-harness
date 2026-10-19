# Core Terminal UX, Multi-Session Tabs, Planning Mode & Provider Revamp

Comprehensive architecture and implementation plan delivering multi-session terminal tabs with split panes, broadcast command bar, sandboxed terminal profiles, customizable skins/themes, an interactive top Planning Mode goal progress bar, Antigravity CLI install fixes, and updated Gemini/xAI Grok provider engines.

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions incorporate the choices confirmed in Phase 1:
> - **Primary Priority**: Focus on Phase 2 & Core Terminal UX (multi-session tabs, session lifecycle, command bar) first, establishing the tabbed data structures that Profiles and Skins attach to.
> - **Planning Mode Top Bar**: Mounted as a persistent interactive banner directly above the workspace tabs, keeping goal completion and in-progress steps visible without crowding chat bubbles.
> - **Tab & Split Layout**: Dual-mode terminal supporting both horizontal scrollable tabs and an optional side-by-side or stacked split view (max 2 concurrent visible sessions) on larger screens and foldables.
> - **Antigravity CLI Fix**: Removed the hardcoded `forceEmbedded = true` flag causing missing-asset exceptions during APK installation; added online fallback and verification checks.
> - **Modern Gemini & xAI**: Aligned Gemini model defaults with `gemini-3.5-flash` and `gemini-3.1-pro-preview` over OpenAI-compatible REST endpoints, deprecating older 2.0 endpoints.

---

### 1. Overview & Core Concept

Mobile Harness currently supports a single project terminal session tied to the active project root. This update transforms the terminal into a full developer workstation featuring:

1. **Multi-Session Terminal Architecture**: Multiple concurrent terminal sessions (`TerminalSession`), each maintaining independent command history, working directory, output buffer, and running process handle.
2. **Tab Management & Split View**: Reorderable, renameable, and closeable tabs with quick "New Tab" action and an optional side-by-side / stacked split pane.
3. **Interactive Command Bar**: Persistent slide-up control dock enabling broadcast execution (send to all terminals, send to all except current), user-defined quick commands, stop-all, and clear-all actions.
4. **Terminal Profiles & Sandboxing (Experimental)**: Isolated environment profiles with customized home directories, package isolation flags, and per-profile startup scripts.
5. **Visual Frame & Skin System**: Retro, CRT, Matrix, Cyber, Glass, and Minimal outline frame skins with custom font size, line-height, and ASCII startup banners.
6. **Activity & Process Monitor**: Live overlay tracking active background processes, running terminals, CPU wake locks, and system metrics.
7. **Agent in Terminal**: Inline terminal assistant integration allowing developers to trigger AI agent guidance or prompt execution directly from the command bar.
8. **Top Planning Mode Progress Bar**: An interactive persistent bar positioned above workspace tabs displaying real-time task objectives, completion badges, and an expandable modal detail view.
9. **Provider & Runtime Fixes**: Fix Antigravity CLI installation error and update Gemini (`gemini-3.5-flash`) and xAI Grok (`grok-3`) provider routing.

---

### 2. User Experience & Visual Design

#### Key User Flows

1. **Multi-Tab Terminal Flow**:
   - The user opens the Terminal screen and sees a clean M3 tab row with active badges (e.g., `bash: root`, `npm: dev server`).
   - Tapping `+` opens a new tab in the current project directory or from a chosen profile.
   - Long-pressing a tab opens a bottom sheet to rename, duplicate, change profile, or close.
   - Tapping the "Split" toggle displays the active tab alongside a secondary tab either side-by-side (landscape/tablet) or stacked (portrait).

2. **Command Bar & Broadcast Flow**:
   - Sliding up the Command Bar reveals configurable quick chips (`git status`, `npm run build`, `ls -la`, `cargo check`).
   - The user types a command and selects:
     - **Run** (active tab)
     - **Broadcast All** (sends to all active terminal sessions simultaneously)
     - **Broadcast Others** (sends to all except current)
     - **Run in New Tab** (spawns new tab automatically)

3. **Planning Mode Progress Bar Flow**:
   - When an AI task starts or an execution plan is active, a persistent compact bar sits at the top of the workspace:
     `[● Implementing Section 2: Terminal Tabs (3/7 Goals Completed) ▾]`
   - Tapping the bar slides down an interactive checklist modal displaying:
     - Completed goals with green checkmarks
     - Currently running sub-tasks with pulsing progress indicators
     - Pending goals with step estimates
     - Option to pause, skip, or modify sub-goals

4. **Profiles & Custom Skins**:
   - In Terminal Settings, users choose from pre-styled aesthetic skins:
     - **Matrix Green**: High-contrast phosphor glow, vintage font, green prompt.
     - **Cyberpunk**: Violet/Pocket Orange borders, monospace neon accents.
     - **Glassmorphism**: Translucent backdrop with subtle blur and outline tint.
     - **Minimalist**: Clean flat borderless design with M3 surface colors.
   - Custom ASCII startup banner with project stats or system info.

---

### 3. Key Product Decisions & Trade-Offs

- **Multi-Session Architecture vs. Single PTY Multiplexer**:
  - *Chosen Approach*: Manage individual PRoot process runners and buffers mapped by session ID in `MainViewModel`, with independent buffer streams.
  - *Why*: True isolation avoids terminal multiplexer (tmux/screen) escape code parsing complexity on mobile touchscreens while giving users granular control over individual tabs.

- **Split View Constraint**:
  - *Chosen Approach*: Maximum 2 visible panes (50/50 or adjustable divider) on mobile/foldables.
  - *Why*: Exceeding 2 panes on smartphone viewports makes terminal text unreadable and keyboard input unwieldy.

- **Antigravity Package Installation Strategy**:
  - *Chosen Approach*: Remove `forceEmbedded = true`, allowing the installer to use online distribution bundle and provide npm fallback (`npm install -g @google/antigravity-cli`) if binary download is unavailable.
  - *Why*: Eliminates APK bloat while resolving the fatal missing asset crash.

---

### 4. Technical Architecture & Data Strategy

#### Component Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Workspace Top Header                            │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  PlanningProgressBar: [Goal 3/7: Terminal Tabs ● In Progress ▾]  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Workspace Tabs: [ Chat | Terminal (3) | Files | Diagnostics ]   │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
                                    │
       ┌────────────────────────────┴───────────────────────────┐
       ▼                                                        ▼
┌───────────────────────────────────┐       ┌───────────────────────────────────┐
│          TerminalScreen           │       │           Agent / Chat            │
│  ┌─────────────────────────────┐  │       │  ┌─────────────────────────────┐  │
│  │ TabRow: [Main] [Server] [+] │  │       │  │ Ask Clarification Card      │  │
│  ├─────────────────────────────┤  │       │  ├─────────────────────────────┤  │
│  │ Split View Container:       │  │       │  │ Tool Approvals & Diff View  │  │
│  │  ┌───────────┐ ┌──────────┐ │  │       │  └─────────────────────────────┘  │
│  │  │ Session A │ │ Session B│ │  │       └───────────────────────────────────┘
│  │  └───────────┘ └──────────┘ │  │
│  ├─────────────────────────────┤  │
│  │ Quick Toolbar (Ctrl, Alt...)│  │
│  ├─────────────────────────────┤  │
│  │ CommandBar: Broadcast dock  │  │
│  └─────────────────────────────┘  │
└───────────────────────────────────┘
                  │
                  ▼
┌───────────────────────────────────────────────────────────────────────┐
│               MainViewModel & TerminalManager Layer                   │
│  - terminalSessions: Map<String, TerminalSessionState>                │
│  - activeTerminalId: StateFlow<String>                                │
│  - splitTerminalId: StateFlow<String?>                                │
│  - terminalProfiles: List<TerminalProfile>                            │
│  - terminalSkin: StateFlow<TerminalSkinConfig>                        │
│  - activeExecutionPlan: StateFlow<ExecutionPlan?>                     │
│  - broadcastCommand(command: String, target: BroadcastScope)          │
└───────────────────────────────────────────────────────────────────────┘
                  │
                  ▼
┌───────────────────────────────────────────────────────────────────────┐
│                 PRoot Runtime / Process Execution                     │
│  - Isolated Guest Workspaces (/workspace/profile_<id>)                │
│  - RuntimeInstaller (Fixed Antigravity & Stack Overlays)              │
│  - Provider Bridges (Claude, DeepSeek, Antigravity, Gemini, Grok)    │
└───────────────────────────────────────────────────────────────────────┘
```

#### New Data Models

1. **`TerminalSessionState`**:
   - `id: String`, `name: String`, `cwd: String`
   - `lines: List<TerminalOutputLine>`, `liveOutput: String`
   - `isRunning: Boolean`, `profileId: String?`
   - `history: List<String>`, `historyIndex: Int`
   - `commandDraft: String`, `createdAtMillis: Long`

2. **`TerminalProfile`**:
   - `id: String`, `name: String`, `icon: String`
   - `customHomeDir: String?`, `startupScript: String?`
   - `environmentVariables: Map<String, String>`
   - `isIsolated: Boolean`

3. **`TerminalSkinConfig`**:
   - `style: TerminalSkinStyle` (MINIMAL, MATRIX, CYBERPUNK, GLASS, CRT)
   - `fontFamily: TerminalFontFamily` (MONOSPACE, FIRA_CODE, JETBRAINS_MONO)
   - `fontSizeSp: Float`, `lineHeightSp: Float`
   - `customBackgroundColor: Long?`, `customTextColor: Long?`
   - `showAsciiBanner: Boolean`, `customBannerText: String?`

4. **`PlanningGoalProgress`**:
   - `goalId: String`, `title: String`, `description: String`
   - `status: GoalStatus` (PENDING, IN_PROGRESS, COMPLETED, FAILED)
   - `subtasks: List<PlanTaskItem>`

---

### Implementation Phasing & Milestones (COMPLETED)

1. **Phase 1: Multi-Session State & Terminal Tabs** [COMPLETED]:
   - Refactored `MainViewModel` to support `Map<String, TerminalSession>` and `terminalSessions` StateFlow.
   - Built `TerminalTabBar` in `TerminalComponents.kt` with dynamic tabs, reordering, rename, duplicate, and close actions.
   - Implemented dual-pane split view with responsive layouts.

2. **Phase 2: Command Bar & Broadcast Engine** [COMPLETED]:
   - Slide-up `TerminalCommandBar` with Broadcast All / Broadcast Others.
   - Quick configurable action chips and stop-all / clear-all hooks.
   - Autocorrect & alias expansion engine (`TerminalAutocorrect`) for mobile typing.

3. **Phase 3: Planning Mode Top Progress Bar** [COMPLETED]:
   - Created `PlanningModeTopBar` positioned persistently above workspace content.
   - Expandable goal detail modal sheet displaying live progress percentage, checkmarks for accomplished goals, and active subtasks.

4. **Phase 4: Profiles, Skins & Visual Customization** [COMPLETED]:
   - Terminal profile management with sandboxed paths, startup scripts, and environment variables.
   - Frame skin styles (Matrix, Cyberpunk, Glass, CRT, Minimal) and ASCII banner generator.
   - Terminal settings screen (`TerminalSettingsModalSheet`) with live preview, font family/size, and JSON import/export.

5. **Phase 5: Provider Fixes, Antigravity Harness & Agent-in-Terminal** [COMPLETED]:
   - Fixed Antigravity CLI bundle extraction (removed `forceEmbedded = true`, added fallback shim and permissions).
   - Upgraded Gemini provider to modern `gemini-3.5-flash` and fixed query parameter URL generation in `ProviderApiClient`.
   - Re-implemented xAI Grok (`grok-3`) provider across `LocalFormatGateway` and `DshRuntimeBridge`.
   - Added Process & Activity Manager (`ProcessActivityManagerDialog`) and inline Terminal Agent talk feature.
