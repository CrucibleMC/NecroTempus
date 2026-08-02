# NecroTempus

[![Minecraft 1.7.10](https://img.shields.io/badge/Minecraft-1.7.10-orange?style=flat-square)](https://minecraft.net)
[![Forge 10.13.4.1614](https://img.shields.io/badge/Forge-10.13.4.1614-orange?style=flat-square)](https://files.minecraftforge.net)
[![Java 8](https://img.shields.io/badge/Java-8-red?style=flat-square)](https://adoptium.net)
![Discord](https://img.shields.io/discord/682358465175355393?color=blue&label=Discord&logo=Discord&style=flat-square)

![NecroTempus](https://github.com/CrucibleMC/NecroTempus/assets/26889025/74b9f118-ef77-4842-8b3a-e4ebf7af94a2)

NecroTempus brings modern Minecraft features to 1.7.10 and connects the
[Crucible](https://github.com/CrucibleMC/Crucible) server APIs to compatible clients.
It provides a shared implementation for server-driven interfaces and their client-side rendering.

## How it works

On the server, NecroTempus implements newer Bukkit API features, centralizes API calls
and sends the required packets. On the client, it registers the corresponding interfaces
and renderers, then applies the updates received from the server.

## Features

- **Server detection:** identifies standard Crucible servers and servers running NecroTempus.
- **Boss bars:** adds customizable boss bars with mod integration support.
- **Titles and action bars:** displays server-controlled messages in modern UI formats.
- **Player list:** supports custom headers, footers and player heads.
- **Custom glyphs:** renders images as characters, including support for custom inventory layouts.
- **Modern fonts:** provides newer font rendering with optional Angelica compatibility.

## Requirements

- Minecraft 1.7.10 with Forge 10.13.4.1614
- Java 8
- [Omniconfig](https://github.com/CrucibleMC/Omniconfig)
- A Mixin 0.8.5 or newer provider, such as [UniMixins](https://github.com/LegacyModdingMC/UniMixins)

## Installation

1. Download NecroTempus, Omniconfig and a compatible Mixin provider.
2. Place the JAR files in the instance's `mods` folder.
3. Start or restart the client or server.

## Customization

Features can be controlled through the Bukkit API or CraftTweaker. See the
[wiki](https://github.com/CrucibleMC/NecroTempus/wiki) for usage examples and the
available APIs.

## Font configuration

Omniconfig provides four options, enabled by default:

- `glyphs`: custom glyph loading and rendering
- `modernFonts`: modern font loading and rendering
- `angelicaGlyphsIntegration`: custom glyph support in Angelica
- `angelicaModernFontsIntegration`: modern font support in Angelica

The glyph and modern-font modules are independent. Each Angelica option only affects
its matching module in Angelica's batching renderer, while the main module option takes
precedence over its integration option. Restart the client after changing these settings.

## License

See [LICENSE](LICENSE).
