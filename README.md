<p align="center">
  <img src="src/main/resources/assets/acquiredutils/icon.png" alt="AcquiredUtils" width="128" height="128">
</p>

<h1 align="center">AcquiredUtils</h1>

<p align="center">
  Fabric utilities mod for FakePixel SMP.
</p>

<p align="center">
  <b>Minecraft 1.21.11</b> · <b>Fabric (Only!)</b>
</p>

---

## Overview

AcquiredUtils is a utility mod designed for the **FakePixel SMP** experience.

It provides lightweight quality-of-life features, rarity recognition, HUD customization, and other everyday gameplay tasks.

## Features

### QoL

- Slot Lock
- Inventory Search
- Inventory Full Warning
- Gear Comparison
- Pickup Alerts
- Held Item Position
- Recipe Unlock Highlights

### Overlay

- Rarity Highlight

### General

- Menu Scale

### Keybinds

- Slot Lock Key

## Compatibility

AcquiredUtils is developed specifically for:

- **FakePixel SMP**
- **Minecraft 1.21.11**
- **Fabric Loader**
- **Fabric API**

The mod is intended for client-side gameplay features and keeps its client-only code in `src/client/java`.

## Installation

1. Install Minecraft **1.21.11** with Fabric Loader.
2. Install the required Fabric dependencies.
3. Download the latest AcquiredUtils release.
4. Place the AcquiredUtils `.jar` file in your Minecraft `mods` directory.
5. Launch Minecraft using your Fabric installation.

## Development

### Requirements

- Java 21
- Gradle Wrapper
- Minecraft 1.21.11
- Fabric Loom
- Fabric API

### Build

Linux / GitHub Codespaces:

```bash
chmod +x gradlew
./gradlew build
```

Windows:

```bat
gradlew.bat build
```

## Project Structure

```text
AcquiredUtils/
├── build.gradle
├── gradle.properties
├── settings.gradle
├── gradlew
├── gradlew.bat
├── LICENSE
│
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
│
└── src/
    ├── main/
    │   ├── java/
    │   │   └── dev/ii8we/acquiredutils/
    │   │       ├── AcquiredUtils.java
    │   │       └── config/
    │   │           └── AcquiredUtilsConfig.java
    │   │
    │   └── resources/
    │       ├── fabric.mod.json
    │       ├── acquiredutils.accesswidener
    │       └── assets/acquiredutils/
    │           ├── icon.png
    │           ├── lang/
    │           │   └── en_us.json
    │           └── textures/gui/
    │               ├── button_save.png
    │               ├── checkbox_purple_checked.png
    │               ├── checkbox_purple_unchecked.png
    │               ├── close_button.png
    │               ├── dropdown_closed.png
    │               ├── dropdown_open_bg.png
    │               ├── dropdown_selection_highlight.png
    │               ├── header_footer_bar.png
    │               ├── icon_gear.png
    │               ├── icon_header.png
    │               ├── icon_keyboard.png
    │               ├── icon_mod.png
    │               ├── lock.png
    │               ├── menu_backdrop.png
    │               ├── panel_background.png
    │               ├── panel_frame.png
    │               ├── slider_handle.png
    │               ├── slider_track.png
    │               └── tab_active_frame.png
    │
    └── client/
        └── java/
            └── dev/ii8we/acquiredutils/client/
                ├── AcquiredUtilsClient.java
                ├── features/
                │   ├── ClientFeature.java
                │   ├── FeatureRegistry.java
                │   ├── ContainerOverlayHandler.java
                │   ├── InventorySearchHandler.java
                │   ├── InventoryFullWarningHandler.java
                │   ├── ItemComparisonHandler.java
                │   ├── SlotLockHandler.java
                │   ├── ItemPickupNotifier.java
                │   ├── ItemRarity.java
                │   ├── ItemRarityDetector.java
                │   ├── PickupNotification.java
                │   ├── RarityHighlightHandler.java
                │   ├── RecipeUnlockHighlightHandler.java
                │   └── PositionedItemInHandRenderer.java
                │
                ├── gui/
                │   ├── AcquiredUtilsConfigScreen.java
                │   ├── PickupHudEditorScreen.java
                │   │
                │   ├── section/
                │   │   ├── GeneralSection.java
                │   │   ├── GuiRow.java
                │   │   ├── ItemPickupSection.java
                │   │   ├── KeybindsSection.java
                │   │   ├── ModSection.java
                │   │   └── HighlightsSection.java
                │   │
                │   ├── theme/
                │   │   └── Theme.java
                │   │
                │   └── widget/
                │       ├── DropdownWidget.java
                │       ├── KeyListenerSlot.java
                │       ├── LockedKeybindWidget.java
                │       ├── ThemedButtonWidget.java
                │       └── ValueSliderWidget.java
                │
```

### Source Set Overview

- **`src/main/java`** — shared mod entrypoint and configuration.
- **`src/client/java`** — client-only gameplay utilities, GUI, HUD, inventory features, and visual highlights.
- **`src/main/resources`** — Fabric metadata, access configuration, language files, the mod icon, and GUI textures.
- **`src/client/java/.../gui`** — configuration screen, HUD editor, sections, theme, and custom widgets.
- **`src/client/java/.../pickup`** — pickup alerts and rarity detection/highlighting.
- **`src/client/java/.../recipe`** — recipe unlock highlighting.

## License

See the repository license file for licensing terms.

## QoL
- Inventory Full Warning: shows a small warning above the hotbar when all 36 normal storage slots are occupied.

