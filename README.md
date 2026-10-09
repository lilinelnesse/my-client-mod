# My Client Mod (Fabric, Minecraft 1.21.4)

A starter environment for building a custom Minecraft client with your own mods.
Fabric is the usual choice for client mods: it's lightweight, updates fast, and has great Mixin support.

## Requirements
- JDK 21 (Temurin/Adoptium is fine)
- Minecraft Java Edition account (to play in the dev client online; offline dev works without)
- An IDE: IntelliJ IDEA (Community is fine) recommended

## Quick start
0. (Only if you don't use IntelliJ) Generate the Gradle wrapper once: install Gradle 8.12+ and run `gradle wrapper --gradle-version 8.12`.
1. Open this folder in IntelliJ IDEA ("Open" -> select `build.gradle`), let Gradle sync.
2. Run the dev client:
   - IntelliJ: use the generated `Minecraft Client` run config, or
   - Terminal: `./gradlew runClient` (Windows: `gradlew.bat runClient`)
3. In-game, press **G** to see the sample keybind message. Check the log for "mixin hook works".
4. Build a jar to install in a normal launcher: `./gradlew build` -> `build/libs/my-client-mod-1.4.0.jar`
   (drop it in a Fabric profile's `mods/` folder along with Fabric API).

## In-game module menu
Press **Right Shift** in-game (rebindable under Controls -> My Client Mod) to open the menu.
Each module is a toggle button; settings are saved to `config/myclientmod.properties`.
Included modules: Coordinates HUD, FPS Counter, Armor & Item HUD, Block Highlight, Fullbright, Auto Sprint.
Modules with options have a **Settings** button next to them (sliders, toggles, a text box for block names).

### Block Highlight
Turn it on, open its Settings, and type block names separated by commas, e.g. `diamond_ore,chest,spawner`
(the `minecraft:` prefix is optional; modded blocks need the full `modid:block`). Pick a color, range (up to 256 blocks) and max count.
It rescans about once a second. "See through walls" draws the outlines over everything.
Many multiplayer servers treat see-through-walls block highlighting as cheating (x-ray) and ban for it. Check server rules.

To add your own module: add a `new Module(...)` to `ModuleManager.java` (and to its `ALL` list),
then check `YourModule.isEnabled()` wherever it should do something (a tick event, the HUD, or a mixin).

## Layout
- `src/main/java/com/example/clientmod/gui/ModMenuScreen.java`: the in-game menu screen
- `src/main/java/com/example/clientmod/module/`: `Module`, `ModuleManager` (all modules + saving) and `setting/` (option types)
- `src/main/java/com/example/clientmod/feature/`: the actual features (HUD, Fullbright, BlockHighlighter)
- `src/main/java/com/example/clientmod/gui/ModuleSettingsScreen.java`: per-module settings page
- `src/main/java/com/example/clientmod/MyClientMod.java`: client entrypoint (events, keybinds, HUD, etc.)
- `src/main/java/com/example/clientmod/mixin/`: Mixins that modify vanilla code
- `src/main/resources/fabric.mod.json`: mod metadata and entrypoints
- `src/main/resources/myclientmod.mixins.json`: register every new mixin class here
- `src/main/resources/assets/myclientmod/`: lang files, textures, etc.
- `gradle.properties`: Minecraft / Fabric versions

## Adding more mods
- Each new mod can be its own Gradle project like this one, or add more classes/mixins to this one to make a single "client" mod pack.
- To build on top of other mods (e.g. Sodium, Cloth Config), add their Maven repo in `build.gradle` `repositories {}` and a `modImplementation` line.
- To use other mods in the dev client only: `modRuntimeOnly "maven.modrinth:sodium:<version>"` with the Modrinth Maven added.

## Changing Minecraft version
Get matching values from https://fabricmc.net/develop and update `gradle.properties`.
Java version must match (1.20.5+ uses Java 21; 1.18-1.20.4 uses Java 17), and `build.gradle` has the Java level in two places.

## Note
Use mods responsibly: many multiplayer servers ban client mods that give unfair advantages (x-ray, killaura, etc.).
