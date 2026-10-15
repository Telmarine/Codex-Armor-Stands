# Codex: Armor Stands

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62B47A?style=for-the-badge) ![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-E68A2E?style=for-the-badge) ![Side](https://img.shields.io/badge/Side-Server-5865F2?style=for-the-badge) ![Type](https://img.shields.io/badge/Type-Quality%20of%20Life-8A2BE2?style=for-the-badge) [![Source](https://img.shields.io/badge/Source-GitHub-181717?style=for-the-badge&logo=github)](https://github.com/Telmarine/Codex-Armor-Stands)

A server-side armor stand editor for NeoForge 1.21.1. Players pose, move and style armor stands with a tool and chest menus.

## How to use it

Hold the edit tool (plain **flint** by default).

| Action | What it does |
| --- | --- |
| Right-click air or a block | Open the editor menu and pick a mode |
| Left-click a stand | Current mode, one step **+** (or toggle) |
| Right-click a stand | Current mode, one step **−** (or toggle) |
| Sneak + scroll the hotbar | Switch axis X → Y → Z |
| Swap-hands key (F) | Edit the nearest stand (+). Sneak + F steps back. Handy for small, invisible or marker stands |

The action bar shows the mode, axis and value after every edit. Coarse/fine steps are switched in the menu.

## Modes

- **Pose**: head, body, left/right arm, left/right leg, on any axis (30° or 3° steps)
- **Rotate**, **Move** (1 block or 1/32 block), **Scale** (0.5x to 2x by default)
- **On/off**: show arms, small, base plate, invisible, gravity, glowing, invulnerable, name visible, lock slots
- **Equipment**: the stand's armor and hand slots in a chest menu
- **Presets**: 40 poses (the Bedrock set, the classic VanillaTweaks set and 11 originals made for this mod). Each pose is its own file, so you can add, edit or turn off poses without touching the jar (see below)
- **Copy / Paste**: 9 saved slots per player, kept across restarts
- **Mirror**, **Reset Pose**
- **Item Frames**: right-click a frame to hide it, left-click to make it glow

Name tags with `&` color codes give stands a colored name.

## Protection

- Stands remember who placed them. The owner, the owner of the land, and staff can edit them.
- Nobody can edit stands on land claimed by someone else (Elements: Claims, Open Parties and Claims, FTB Chunks). In Elements: Claims, anyone with Build in that claim counts as the owner of the land.
- Locked stands can only be changed or broken by the owner and staff.
- Optional per-chunk armor stand limit. When Elements: Claims or Elements: Nexus 1.0.2+ is installed, their entity limits are used instead.

## Commands

- `/armorstand tool [player]`: give the edit tool (staff)
- `/armorstand head <name>`: give a player head (staff)
- `/armorstand info`: owner, lock and scale of the stand you're looking at
- `/armorstand reload`: reload messages and presets (admin)

`/as` works as a short alias.

## Permissions

Nodes are `armorstands.<name>` and work with Elements: Nexus ranks, other permission mods, or OP levels.

| Node | Default |
| --- | --- |
| `use`, `edit.pose`, `edit.rotate`, `edit.move`, `edit.toggles`, `edit.scale`, `edit.lock`, `edit.equipment`, `edit.presets`, `edit.copy`, `edit.mirror`, `edit.reset`, `edit.itemframe`, `rename.color` | everyone |
| `edit.glowing`, `edit.invulnerable`, `tool`, `head`, `bypass`, `limit.bypass` | OP 2 |
| `admin` | OP 3 |

## Config

`config/codex_armor_stands/`
- `features.toml`: an on/off switch for every mode and feature, plus the overlap guard
- `tool.toml`: tool item, required name, F-key range
- `steps.toml`: step sizes, scale limits, chunk limit, copy slots
- `presets/`: one `.json` file per preset pose
- `messages.toml`: every message, `&` colors

If Armor Statues or Armor Poser is installed, the editor turns itself off. Add `"editor"` to `overlap_guard.force_on` to keep it on. Add `"chunk_limit"` to keep this mod's chunk limit on next to Elements: Claims or Elements: Nexus.

## Adding your own poses

Every preset is its own file in `config/codex_armor_stands/presets/`. No rebuild needed:

1. Pose a stand with the tool and note the X / Y / Z values on the action bar.
2. Add a file, e.g. `presets/waving.json`:
   ```json
   {
     "name": "Waving",
     "group": "My Server",
     "icon": "minecraft:armor_stand",
     "order": 500,
     "right_arm": [-140, 20, 0],
     "left_arm": [-10, 0, -10]
   }
   ```
3. Run `/armorstand reload`.

The file name is the pose id. Parts you leave out (`head`, `body`, `left_arm`, `right_arm`, `left_leg`, `right_leg`) stay straight. Set `"enabled": false` to hide a pose. Built-in poses you delete stay deleted, and new built-ins from later versions are added once. A `README.txt` in the folder explains the format.

## Credits

Classic preset poses come from the VanillaTweaks Armor Statues set. Bedrock poses are from Minecraft Bedrock Edition.

## Building

`gradlew build`. The jar is `build/libs/codex_armor_stands-<version>.jar`.
