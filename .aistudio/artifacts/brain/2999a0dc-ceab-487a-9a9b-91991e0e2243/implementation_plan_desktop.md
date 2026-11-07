# Mobile Harness — Android 16 Desktop Mode Compatibility Plan

This plan outlines the steps to implement full Desktop Mode compatibility for Mobile Harness on Android 16, ensuring a professional developer experience on tablets, foldables, and external displays. It also addresses user suggestions for architectural upgrades.

---

## 1. Core Desktop Windowing & Manifest
- **Resizability & Multi-Instance**:
  - Update `AndroidManifest.xml` with `android:resizableActivity="true"` and `android.window.PROPERTY_SUPPORTS_MULTI_INSTANCE_SYSTEM_UI`.
  - Handle `configChanges` (`orientation`, `screenSize`, `smallestScreenSize`, `screenLayout`) to prevent activity restarts during window resizing or display transitions.
- **Picture-in-Picture (PiP)**:
  - Enable PiP support for the live terminal or agent chat while multitasking.

## 2. Adaptive & Responsive UI (Compose Material 3 Adaptive)
- **WindowSizeClass Integration**:
  - Calculate `WindowSizeClass` in `MainActivity` and propagate it throughout the UI tree.
  - Implement `NavigationSuiteScaffold` to automatically switch between:
    - **Bottom Navigation Bar**: Compact screens (phones).
    - **Navigation Rail**: Medium screens (tablets/unfolded).
    - **Permanent Navigation Drawer**: Expanded/Large screens (desktop monitors).
- **Flexible Layouts**:
  - Replace fixed-width containers with `Modifier.widthIn(max = ...)` and `Modifier.fillMaxWidth()` for optimal readability on wide screens.
  - Implement **List-Detail canonical layouts** for the File Browser and Project list on large screens.

## 3. Desktop Interactions (Mouse & Keyboard)
- **Mouse Support**:
  - Implement **Hover states** on all interactive elements (buttons, project cards, file entries) using `Modifier.onPointerEvent`.
  - Add **Right-click context menus** for projects (Rename, Delete, Clone) and files (Open, Delete, Share).
  - Ensure scrollbars are visible and usable with mouse wheels.
- **Keyboard Navigation & Shortcuts**:
  - Support `Tab` key navigation through UI focus.
  - Implement global keyboard shortcuts:
    - `Ctrl + ,`: Open Settings.
    - `Ctrl + N`: New Project / New Chat.
    - `Ctrl + T`: Switch to Terminal.
    - `Ctrl + F`: Switch to File Browser.
    - `Ctrl + Shift + P`: Open Command Palette / Quick Search.
    - `Ctrl + S`: Save file in editor.
- **Drag-and-Drop**:
  - Allow dragging files from the workspace to external apps (and vice versa) using `Modifier.dragAndDropSource` and `Modifier.dragAndDropTarget`.

## 4. Terminal Experience Enhancements
- **Resolution Scaling**: Optimize font sizes and line heights for high-resolution desktop displays.
- **Enhanced Selection**: Support mouse-based click-and-drag text selection and keyboard-based selection (`Shift + Arrows`).
- **Shortcuts**: Standard terminal shortcuts (`Ctrl+C`, `Ctrl+D`, `Ctrl+L`) mapped to physical keyboard events.

## 5. Architectural Future-Proofing (User Suggestions)
- **Ubuntu 24.04 (Noble Numbat) Prep**:
  - Audit existing PRoot bindings for `clone3` and `glibc` 2.39 compatibility.
  - Update `RuntimeInstaller` metadata to support versioned rootfs selection.
- **AVF (Android Virtualization Framework) Backend**:
  - Add `BackendType` enum (`PROOT`, `AVF`) to `AppUiState`.
  - Implement capability detection for `/dev/kvm` and `android.permission.MANAGE_VIRTUAL_MACHINE`.
- **Local/Offline AI Fallback**:
  - Add `LOCAL_MODEL` to `ProviderKind`.
  - Prepare `LocalAiManager` to handle model downloads (GGUF/quantized) and integration with `llama.cpp` or on-device NPUs.

---

## Staged Implementation Plan

### Stage 1: Manifest & SDK Setup
- [x] Update `AndroidManifest.xml` for resizing and multi-instance.
- [x] Update `build.gradle.kts` with adaptive dependencies.
- [x] Integrate `WindowSizeClass` into `MainActivity`.

### Stage 2: Adaptive Navigation & Layouts
- [ ] Implement `NavigationSuiteScaffold` in `RootScreenHost`.
- [ ] Update `WorkspaceScreen` to use adaptive navigation for sub-tabs (Chat, Files, Terminal).
- [ ] Implement side-by-side List-Detail for Project list on wide screens.

### Stage 3: Desktop Input & Interactions
- [ ] Add mouse hover and right-click support to project/file lists.
- [ ] Implement core keyboard shortcuts mapping in `MainActivity` or root Composables.
- [ ] Add drag-and-drop support for file attachments.

### Stage 4: Polish & Refinement
- [ ] Optimize terminal rendering for large screens.
- [ ] Ensure terminal session persistence across window transitions.
- [ ] Update settings to reflect Ubuntu 24.04 and AVF roadmap.

### Stage 5: Verification & Compatibility Report
- [ ] Test on Phone (Portrait/Landscape).
- [ ] Test on Tablet (Split-screen/Full).
- [ ] Test on Desktop Windowing (Floating/Maximized).
- [ ] Verify Mouse/Keyboard peripherals.
- [ ] Final regression check.
