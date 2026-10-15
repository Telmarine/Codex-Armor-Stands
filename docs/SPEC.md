# Codex: Armor Stands — Spec (draft)

Server-side armor stand editor for NeoForge 1.21.1. Vanilla clients only: an edit tool plus vanilla chest menus, no resource pack, no client mod. Modeled on ArmorStandEditor-Reborn (Paper).

Everything ships in 1.0.0. Revisions come after CurseForge feedback.

## Identity
- Name: Codex: Armor Stands · mod ID `codex_armor_stands` · package `com.tac5studios.codexarmorstands`
- Server-side only (`DEDICATED_SERVER`), NeoForge 21.1.x, Java 21, version 1.0.0
- Ranges: `minecraft_version_range=[1.21.1]`, `neo_version_range=[21.1.0,21.2)`
- Runs on its own or next to Nexus and Economy; no dependencies between them
- Custom license (free, credit Telmarine, no resale)

## Edit tool [1.0.0]
- Plain **flint** by default. Config can change the item and can require a custom name (e.g. "Stand Editor") so normal flint does nothing.
- **Right-click air/block** with the tool → opens the Mode menu.
- **Left-click a stand** → current mode one step **+**. **Right-click a stand** → one step **−** (or toggles, for on/off modes).
- **Sneak + scroll hotbar** while holding the tool → switch axis X → Y → Z (action bar shows mode, axis, step).
- **Swap-hands key (F)** with the tool → current mode on the nearest stand within 5 blocks (+). Sneak + F = −. Reaches small, invisible and marker stands.
- Action bar after every edit: `Right Arm · X · 15° (fine)`.
- `/armorstand tool` gives the configured tool (staff) [1.0.0].

## Modes
| Mode | What it does | Step (coarse / fine) | Tag |
|---|---|---|---|
| Head, Body, Left Arm, Right Arm, Left Leg, Right Leg | Pose per axis | 30° / 3° | 1.0.0 |
| Rotate | Turn the whole stand | 45° / 3° | 1.0.0 |
| Move | Nudge along the axis | 1 block / 1/32 block | 1.0.0 |
| Show Arms | On/off | — | 1.0.0 |
| Small | On/off | — | 1.0.0 |
| Base Plate | On/off | — | 1.0.0 |
| Invisible | On/off | — | 1.0.0 |
| Gravity | On/off | — | 1.0.0 |
| Glowing | On/off | — | 1.0.0 |
| Invulnerable | On/off (owner/staff can still remove) | — | 1.0.0 |
| Name Visible | On/off (name set with a name tag) | — | 1.0.0 |
| Scale | `generic.scale` attribute | 0.1 / 0.01, limits 0.5–2.0 | 1.0.0 |
| Lock Slots | Lock all slots (no take/put) | — | 1.0.0 |
| Equipment | Chest menu of the stand's 6 slots | — | 1.0.0 |
| Presets | Pick a preset pose | — | 1.0.0 |
| Copy / Paste | 9 saved copies per player | — | 1.0.0 |
| Mirror | Swap left/right pose | — | 1.0.0 |
| Reset | Pose back to default | — | 1.0.0 |
| Coarse / Fine | Step size switch | — | 1.0.0 |

## Menus (vanilla chest, 6 rows)
- **Mode menu**: one item per mode (e.g. player head = Head, leather chestplate = Body, sticks = arms/legs, compass = Rotate, piston = Move, glass = Invisible, slab = Base Plate, feather = Gravity, glowstone = Glowing, obsidian = Invulnerable, iron bars = Lock Slots, chest = Equipment, book = Presets, barrier = Reset, clock = Coarse/Fine). Bottom row: X / Y / Z axis buttons, close. Modes the player can't use are hidden.
- **Equipment menu**: the stand's head, chest, legs, feet, main hand, off hand. Drag items in and out like a normal chest. Disabled while the stand is locked (owner/staff bypass).
- **Presets menu**: one named item per preset, click to apply.

## Presets [1.0.0]
- **Classic set** (VanillaTweaks Armor Statues set, e.g. Attention, Walking, Running, Pointing, Sitting, Salute, Facepalm…). Exact angles taken from the Armor Statues source at build time.
- **Bedrock set**: Default, Solemn, Athena, Brandish, Honor, Entertain, Salute, Riposte, Zombie, Cancan, Hero.
- **Originals** (11): Bow, Respect, Thinker, Shrug, Pointing Up, At Ease, Crossed Arms, Sword Raised, Shield Guard, Lounging, Offering. Checked to differ from protected sets.
- One file per pose: `config/codex_armor_stands/presets/<id>.json` (name, group, icon, order, enabled, six parts). Drop in a file + `/armorstand reload` to add one. Built-ins copied once (`.installed`), deleted ones stay deleted. Old single `presets.json` is split once and renamed `presets.json.old`.

## Ownership & protection [1.0.0]
- A stand records who placed it (persistent entity data). Owner + staff (`bypass` node) can edit; others can't.
- Unowned stands (placed before the mod, or by commands) are editable by whoever can build there.
- Claims: Elements: Claims, OPAC and FTB Chunks checked before every edit (reflection, all optional). Editing only where the player could break blocks. Elements: Claims [1.0.1]: `ClaimsAPI.isClaimed` + `canBuild`; Build in the claim counts as land owner.
- Locked stands show a short particle puff when someone tries to take an item.
- Marker stands are skipped (can't be clicked).

## Permissions (NeoForge permission API — works with Nexus ranks)
Full node = `armorstands.` + path, e.g. `armorstands.edit.pose`.
- `use` (open the tool menu) — default everyone
- `edit.pose`, `edit.rotate`, `edit.move`, `edit.toggles`, `edit.lock`, `edit.equipment`, `edit.presets`, `edit.reset` — default everyone
- `edit.scale`, `edit.copy`, `edit.mirror` — default everyone
- `edit.invulnerable`, `edit.glowing` — default OP 2
- `bypass` (edit any stand, ignore claims and locks) — default OP 2
- `edit.itemframe`, `rename.color` — default everyone
- `tool`, `head`, `limit.bypass` — default OP 2
- `admin` (`/armorstand reload`, `/armorstand tool <player>`) — default OP 3

## Config `config/codex_armor_stands/`
- `features.toml`: master `enabled`, one switch per mode, `[overlap_guard]` with `force_on`
- `tool.toml`: item id, required name (empty = any), target range
- `steps.toml`: coarse/fine angles, move distances, scale limits
- `presets/`: one file per preset pose (+ README.txt)
- `messages.toml`: every player-facing line, `&` colors

## Overlap guard [1.0.0]
- If **Armor Statues** (`armorstatues`) or **Armor Poser** (`armorposer`) is installed, the editor turns itself off unless `force_on` is set. Logged once at boot.

## Commands
- `/armorstand tool [player]`, `/armorstand reload`, `/armorstand info` (owner, lock state of the stand you look at) [1.0.0]

## Extras [1.0.0]
- **Item Frames** mode: right-click a frame = invisible on/off, left-click = glowing on/off (claims checked).
- `/armorstand head <name>` gives a player head (staff).
- Per-chunk limit on placing armor stands (off by default, `steps.toml` per_chunk = 32), `limit.bypass` node. [1.0.1] Handed over to Elements: Claims or Elements: Nexus 1.0.2+ when installed (their settings win, even when off); `overlap_guard.force_on = ["chunk_limit"]` keeps it.
- Name tags with `&` colors give stands colored names (`rename.color`).
- `/as` alias for `/armorstand`.
