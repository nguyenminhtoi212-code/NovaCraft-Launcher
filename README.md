 <div align="center">

<img width="120" height="120" alt="1000413428" src="https://github.com/user-attachments/assets/37565f99-0859-4be7-9bea-7df4fb9c948a" />

# NovaCraft Launcher

**Minecraft: Java Edition Launcher for Android**

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Architecture](https://img.shields.io/badge/Architecture-64--bit%20(arm64--v8a)-00E5FF.svg)](#download--requirements)
[![Language](https://img.shields.io/badge/Language-English%20%7C%20Ti%E1%BA%BFng%20Vi%E1%BB%87t-9C27B0.svg)](#)

[Overview](#overview) • [Download](#download--requirements) • [Quick Start](#quick-start) • [Features](#features) • [Troubleshooting](#troubleshooting) • [Privacy & Security](#privacy--security) • [Developers](#for-developers) • [Credits](#credits--license)

</div>

> **Disclaimer:** NovaCraft Launcher is an independent, community-driven project and is not affiliated with Mojang Studios, Microsoft, or any third-party projects listed below.

---

## Overview

| About NovaCraft Launcher | Core Highlights |
| :--- | :--- |
| **NovaCraft Launcher** is a streamlined, modern launcher designed to run **Minecraft: Java Edition** natively on Android devices. The workflow is simple: create an instance, install mods or worlds if needed, and start playing. | • **Isolated Instances:** Each instance maintains its own Minecraft version, mods, worlds, and configurations.<br>• **Automatic Initial Setup:** Game RAM allocation, resolution scale, and render distance auto-tune to your device's RAM.<br>• **Full Content Ecosystem:** Supports Vanilla, Fabric, Forge, NeoForge, and the Rust-based Horizon engine, with integrated Modrinth and CurseForge downloads. |

---

## Download & Requirements

| System Requirements | Release Package |
| :--- | :--- |
| • **OS:** Android 8.0 (API 26) or newer<br>• **CPU:** **64-bit (`arm64-v8a`)** only *(32-bit `armeabi-v7a` support has been removed)*<br>• **RAM:** 3 GB minimum (4 GB+ recommended)<br>• **Storage:** Several GB of free storage space | **`NovaCraft-<version>-64bit-arm64-v8a.apk`**<br>The single unified release build optimized for stability and peak performance on 64-bit Android devices. |

---

## Quick Start

| Step | Action | Instructions |
| :---: | :--- | :--- |
| **1** | **Launch & Storage** | Open **NovaCraft Launcher**. On Android 11+, the launcher complies with Scoped Storage rules using the app's private directory (`novacraft`) and the system SAF picker for importing files and worlds. |
| **2** | **Add Account** | Open the **Accounts** menu and sign in with **Microsoft** or **Ely.by** for online multiplayer, or use **Offline** mode with local session storage. |
| **3** | **Create Instance** | Tap **Create Instance**, select your Minecraft version, and choose **Vanilla**, **Fabric**, **Forge**, **NeoForge**, or the **Rust Engine (Horizon)**. |
| **4** | **Download & Play** | On the **Home** screen, tap **DOWNLOAD** and wait for the game files to finish downloading (internet required), then tap **PLAY**. |
| **5** | **Mods & Worlds** | Install mods, resource packs, shaders, and Java worlds (`.zip` archives or `saves/` folders) anytime via the **Mods & Worlds** menu. |

---

## Features

| Category | Capabilities |
| :--- | :--- |
| **Game & Runtimes** | • **Multiple Isolated Instances**, each with independent versions, mods, worlds, and settings.<br>• **Loaders:** **Vanilla**, **Fabric**, **Forge (1.13+)**, and **NeoForge (1.20.2+)**. Mod, modpack, and world searches automatically adapt to your instance's loader and version.<br>• **Built-in Java Runtimes:** **Java 8, 17, 21, and 25**, automatically selected based on your instance's Minecraft version.<br>• **Renderer Options:** **Auto** (recommended), **Krypton**, **MobileGlues**, **GL4ES**, **Kopper Zink**, and **Freedreno (Snapdragon Turnip)**. |
| **Mods & World Content** | • **Modrinth & CurseForge Integration:** Browse and install mods, modpacks, resource packs, shaders, datapacks, and worlds with smart recommendations directly in-app.<br>• **Unified World & File Importer:** Import `.zip` archives or full Minecraft Java world folders with automatic `level.dat` NBT and official Mojang structure validation. |
| **Controls & Utilities** | • **Touch Control Editor:** Customize on-screen button layouts, sizes, and modifier keys (`Shift`/`Ctrl`/`Alt`), plus a responsive **Bedrock-style touch hotbar**.<br>• **Peripherals Support:** Full **Gamepad (controller)** support (including Controlify compatibility) as well as physical **mouse and keyboard** input.<br>• **Built-in Screen Recorder & Crash Reports:** Hardware-accelerated screen recording and built-in crash log viewer with easy log sharing.<br>• **Multilingual Interface:** Full **English** and **Vietnamese** support, alongside Indonesian, Spanish, Brazilian Portuguese, and Thai. |

*(Note: Forge 1.12.2 and older, NeoForge 1.20.1 and older, and Quilt are not supported.)*

---

## Troubleshooting

| Issue | Recommended Solution |
| :--- | :--- |
| **Game crashes or closes unexpectedly** | Allocate no more than half of your device's total RAM to the game, close background apps, or lower the resolution scale in **Settings**. View detailed crash logs directly inside the app. |
| **Low FPS / Stuttering** | Reduce render distance, lower the resolution scale, or switch to a different graphics renderer in **Settings**. |
| **Mods fail to load** | Verify that the installed mod's required loader (Fabric/Forge/NeoForge) and Minecraft version exactly match your selected instance. |

---

## Privacy & Security

| Safe Authentication | Verified Downloads | Encrypted Connections |
| :--- | :--- | :--- |
| Microsoft passwords are never stored; authentication runs exclusively through Microsoft's official OAuth window. | Minecraft assets and libraries download directly from official Mojang servers and are verified against official SHA-1 checksums. | All network traffic strictly uses encrypted HTTPS connections. |

---

## For Developers

| Build Requirements | Build Command |
| :--- | :--- |
| • **JDK 17**<br>• **Android SDK** (Compile SDK 36, Min SDK 26, Target ABI `arm64-v8a`) | `gradle assembleRelease` |

---

## Credits & License

**NovaCraft Launcher** is created and maintained by **Nguyễn Minh Tới**, developed on Android using **Android Studio** with development assistance from **Google AI Studio**, featuring full **English** and **Vietnamese** localization support.

### Special Thanks to the Community & Original Authors
Heartfelt thanks to **[ItsVynzzz](https://github.com/ItsVynzzzz)**, the original creator of **[AmberLauncher](https://github.com/ItsVynzzzz/AmberLauncher)**, as well as the open-source community, testers, and upstream developers whose foundational work makes this project possible.

| Project | Author(s) | License |
| :--- | :--- | :--- |
| **AmberLauncher** *(Base Source)* | [ItsVynzzz](https://github.com/ItsVynzzzz) | GPL-3.0 |
| **NUX Launcher** | IsraaDeveloper | GPL-3.0 |
| **Zalith Launcher 2** | MovTery & contributors | GPL-3.0 |
| **PojavLauncher** | artdeell, khanhtran & team | LGPL-3.0 |
| **Fold Craft Launcher** | FCL Team | GPL-3.0 |

### Third-Party Components & Ecosystem Services

| Domain | Projects & Licenses |
| :--- | :--- |
| **Rendering & Engines** | Horizon & Starling Android Port (MIT), MobileGlues (LGPL-2.1), GL4ES (MIT), Mesa 3D / Zink / Turnip (MIT), ANGLE (BSD-3-Clause), LWJGL (BSD-3-Clause), SDL3 (Zlib) |
| **Core Libraries** | Jetpack Compose & Material 3 (Apache-2.0), OkHttp & Okio (Apache-2.0), ByteHook (MIT), authlib-injector (AGPL-3.0), TouchController (LGPL-3.0), sora-editor (LGPL-2.1), skinview3d (MIT) |
| **Community & APIs** | Modrinth API, CurseForge API, Ely.by, mclo.gs, Fabric, Forge, NeoForge, Quilt, Controlify |

---

*Released under the **GNU General Public License v3.0** (see [LICENSE](LICENSE)). Copyright © 2026 **Nguyễn Minh Tới** for original modifications, localization, and newly written code. Upstream components remain the property of their respective authors.*  
*In accordance with the additional terms of Zalith Launcher 2, the names "ZalithLauncher" and "ZL" are not used, and the Unofficial Modified Version statement and copyright notices remain displayed in the application.*  
*Minecraft is a trademark of Mojang AB / Microsoft Corporation. NovaCraft Launcher does not include or distribute copies of the game; a legitimate Minecraft: Java Edition account is required for online play.*
