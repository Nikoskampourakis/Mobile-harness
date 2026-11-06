# Mobile Harness — Live Agent Experience & Core UI Overhaul

Comprehensive architectural blueprint and staged implementation plan to upgrade Mobile Harness into a polished, mobile-first AI agent environment while preserving 100% of the underlying CLI agents' native intelligence, authentication, models, and capabilities.

---

### User Review & Critical Principles

> [!IMPORTANT]
> **Preservation of Native Agent Intelligence**:
> - Claude Code, Codex, DeepSeek Harness, Gemini, Antigravity, and other supported engines will continue running their exact underlying binary processes, configuration, OAuth/API authentication, tools, and skills inside the private PRoot Linux environment.
> - No fake or fabricated agent activities or simulated chains-of-thought: all visualized progress events originate from normalized CLI event streams.
> - Voice conversations operate directly through the active CLI session without adding a second LLM or paid speech API.
> - Steer Mode sends live guiding instructions without terminating running processes or restarting sessions.

---

## Architecture & System Design

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Mobile Harness App UI                           │
│  ┌───────────────────────┐  ┌──────────────────────────────────────┐   │
│  │    Live Voice Mode    │  │       Project Artifact Viewer        │   │
│  │ [Mic|Cam|Subtitles|TTS]│  │ [Files|Diffs|Images|Code|Previews]   │   │
│  └───────────┬───────────┘  └──────────────────┬───────────────────┘   │
│              │                                 │                       │
│  ┌───────────┴─────────────────────────────────┴───────────────────┐   │
│  │              Agent Chat & Activity Timeline Screen              │   │
│  │  - Compact Markdown, Tables & LaTeX Math Renderer               │   │
│  │  - Normalized Activity Items (Read, Edit, Create, Run, Think)   │   │
│  │  - Steer Bar (Live Instructions) & Persistent FIFO Message Queue│   │
│  └───────────────────────────────┬─────────────────────────────────┘   │
│                                  │                                     │
│  ┌───────────────────────────────┴─────────────────────────────────┐   │
│  │                    Modern Mobile Terminal                       │   │
│  │  - Streamlined Console View (No Cluttered Button Grids)         │   │
│  │  - Compact Contextual Command Bar & Fast History Navigation     │   │
│  └───────────────────────────────┬─────────────────────────────────┘   │
└──────────────────────────────────┼─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│              MainViewModel & Event Normalization Layer                 │
│  - Event Normalizer (Claude JSON, DSH RPC, Codex, Antigravity)        │
│  - MessageQueueManager (FIFO queue, edit, delete, pause/resume)        │
│  - SteerDispatcher (In-flight guidance without process termination)    │
│  - TtsSubsystem (Android Neural / System TTS with streaming audio)    │
│  - SpeechRecognitionCoordinator (Continuous speech + captions)         │
│  - SyntaxHighlightingEngine (18+ languages supported)                 │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│              Linux Runtime / PRoot Process Layer                       │
│  - Active CLI Processes (Claude Code, DSH, Codex, Antigravity)         │
│  - Installed DevStacks: Web, Python, Android, C/C++, PHP, Rust         │
│  - /pocket-bridge/steer.json & permission hooks                        │
└────────────────────────────────────────────────────────────────────────┘
```

---

## Detailed Staged Implementation Plan

### Stage 1: Agent Event Normalization, Activity Visualization & Markdown LaTeX Rendering
- **Event Normalization Model**:
  - Extend `ActionKind` with `FILE_CREATE`, `SKILL_USE`, `TOOL_CALL`, `ERROR`, `COMPLETION`, and `PERMISSION_REQUEST`.
  - Add `affectedFiles`, `durationMillis`, `skillName`, `toolName`, `isReading`, and `isEditing` metadata to `ActivityItem`.
  - Build `AgentActivityVisualizer` in chat:
    - Reading files: compact chip with file icon, path, and subtle reading pulse.
    - Editing files: shows modified files with expandable unified diff preview.
    - Creating files: shows new project files with one-tap open/preview.
    - Running commands: clean execution status pill, expandable stdout/stderr, exit code badge.
    - Skills & Tools: non-intrusive skill badges without conversational repetition.
    - Progress & Thinking: displays actual reasoning/progress events received from the CLI, cleanly separated from user responses.
- **Markdown & LaTeX Mathematics Engine (`MarkdownText.kt`)**:
  - Full GitHub-Flavored Markdown parsing (headings, blockquotes, lists, horizontal dividers).
  - Mathematical LaTeX notation:
    - Display equations `$$...$$` and `\[...\]` formatted cleanly with styled typography and background container.
    - Inline equations `$...$` and `\(...\)` formatted with superscripts, subscripts, fractions, square roots, integrals, and mathematical symbols.
  - Markdown Tables: parse `| Header |` syntax and render alternating row tables with borders and horizontal scrolling.
  - Compact rendering: minimal padding, zero wasted card margins, space-efficient mobile bubbles.

### Stage 2: Steer Mode & Persistent Message Queue
- **Steer Mode**:
  - Create `SteerState` (`WAITING`, `DELIVERED`, `INCORPORATED`) and `SteerDispatcher`.
  - When user sends a Steer message while the agent is running:
    - Active process is **NOT** terminated (no `stopActiveSession()` calls).
    - Writes steer payload to `/pocket-bridge/steer.json` which the CLI permission/tool hook inspects before the next tool execution, and injects into the session stream.
    - Visual badge in chat indicating the steering directive and its live status.
- **Persistent Message Queue**:
  - Implement `MessageQueueManager` with FIFO ordering.
  - Expandable queue bottom drawer/card displaying pending messages.
  - Actions: Edit message, Delete from queue, Send Now, and Pause/Resume automatic submission.
  - Persisted in `AppPreferences` so backgrounding or app restarts preserve queued tasks.
  - Dispatches next message automatically upon previous task completion.

### Stage 3: Modern Mobile Terminal & Universal Syntax Highlighting & Rust Toolchain
- **Terminal Overhaul**:
  - Remove redundant 10-button permanent toolbars cluttering mobile screen height.
  - Redesign `TerminalCommandBar` into a sleek, compact single-line bar with expandable multiline editing, history chip/popup, dismiss keyboard action, and contextual overflow menu (Ctrl+C, Ctrl+D, clear, etc.).
  - Improve touch selection, cursor placement, and terminal scrolling performance.
- **Universal Syntax Highlighting (`SyntaxHighlighter.kt`)**:
  - Lexer and token highlighter supporting 18+ languages:
    - JavaScript, TypeScript, Python, Rust, Kotlin, Java, C, C++, C#, Go, Swift, HTML, CSS, JSON, YAML, TOML, Bash, SQL, Markdown.
  - Applied across code blocks in `MarkdownText.kt` and lines in `AdvancedCodeEditor.kt`.
- **Rust Toolchain (`DevStack.RUST`)**:
  - Add `RUST` to `DevStack` enum with label, description, and summary.
  - In `RuntimeInstaller.kt`:
    - `rustc`, `cargo`, and build essentials installation via robust APT package management.
    - Cargo PATH configuration (`/root/.cargo/bin:/usr/bin`).
    - Toolchain verification (`rustc --version && cargo --version`).
    - Uninstall and status detection (`isStackInstalled(DevStack.RUST)`).
    - UI in `SettingsScreenModern.kt` to install/remove Rust.

### Stage 4: Live Voice Mode & Neural TTS Subsystem
- **Live Voice Session Screen (`LiveVoiceModeDialog.kt`)**:
  - Floating or full-screen immersive voice overlay.
  - Visual animated sound wave / pulse indicating agent listening vs speaking states.
  - Live subtitles / captions toggle with real-time text transcription.
  - Microphone mute/unmute control.
  - Immediate interruption button (instantly cuts off speech and agent output).
  - Built-in keyboard input bar allowing the user to type without leaving voice mode.
  - Configurable keyboard shortcuts for mute, interrupt, captions, mic, close.
- **Camera Frame Integration**:
  - Front / rear camera preview toggle with proper `Manifest.permission.CAMERA` runtime flow.
  - User-controlled: disabled by default, one-tap enable.
  - Streams/attaches snapshots to prompts **only** if the active agent supports image/multimodal input (e.g. Claude Code with vision or Gemini).
- **Independent TTS Subsystem**:
  - Upgraded `TtsManager` with sentence-based streaming playback and instant cancel/flush.
  - Android System Neural TTS with selectable system voices, speech rate (0.5x - 2.5x), pitch, and volume controls.
  - Zero token consumption, no extra LLMs or external paid API requirements.

### Stage 5: Project Artifact Viewer, Agent vs Subscription Cleanup & Polish
- **Project Artifact Viewer (`ProjectArtifactsView.kt`)**:
  - Dedicated viewer accessible from workspace tabs and header chip.
  - Categorized file cards: Created Files, Modified Files, Generated Images, Documents, Source Code, and Build Artifacts (APKs).
  - Preview modal with image viewer, code viewer with syntax highlighting, and unified diff inspector.
- **Agent and Subscription Cleanup**:
  - Remove `OPENCLAW` and `HERMES` from subscription provider selection in `providersForAgent` and UI filters.
  - Clear architectural separation between:
    - AI Model Providers (Anthropic, Gemini, DeepSeek, xAI Grok, OpenRouter)
    - Subscriptions (Claude Code Pro/Team subscription login, Codex subscription)
    - CLI Agents (Claude Code, DeepSeek Harness, Codex CLI, Antigravity)
- **Regression Testing & Verification**:
  - Run `compile_applet` and `gradle :app:testDebugUnitTest`.

---

### Verification Checklist
- [ ] Existing CLI agents retain full intelligence and authentication
- [ ] Voice mode speaks without extra LLMs or external fees
- [ ] Speech interruption stops playback within <100ms
- [ ] Steer mode does not terminate active processes
- [ ] Message queue executes in FIFO order and survives restarts
- [ ] Markdown renders tables and formatted LaTeX math correctly
- [ ] Project artifacts are easily browsed and inspected
- [ ] Terminal is compact and free of redundant button grids
- [ ] Rust toolchain installs and verifies successfully
- [ ] Hermes/OpenClaw removed from subscription provider picker
