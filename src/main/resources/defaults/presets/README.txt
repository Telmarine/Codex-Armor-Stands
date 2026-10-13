Codex: Armor Stands - preset poses
=====================================

Every preset is its own file in this folder: <id>.json (the file name is the id).

Add a pose:     drop a new .json file in here, then run /armorstand reload.
Turn one off:   set "enabled": false (or delete the file).
Change one:     edit its file, then /armorstand reload.

File format:
{
  "name": "Waving",                  shown in the Presets menu
  "group": "My Server",              small grey line under the name (optional)
  "icon": "minecraft:armor_stand",   menu icon, any item id (optional)
  "order": 500,                      menu position, lowest first (optional)
  "enabled": true,                   false hides it (optional)
  "head":      [x, y, z],
  "body":      [x, y, z],
  "left_arm":  [x, y, z],
  "right_arm": [x, y, z],
  "left_leg":  [x, y, z],
  "right_leg": [x, y, z]
}

Angles are in degrees, the same X / Y / Z values the edit tool shows on the
action bar, so you can pose a stand by hand and copy the numbers. A part you
leave out is straight (0, 0, 0).

Tips:
- Body and head turn at the neck, not the waist. Keep body tilts small and let
  the head show the lean.
- To bring a hanging arm toward the middle, use Z (sideways tilt), not Y.

New presets that come with later versions are added once. If you delete one of
the built-in files it stays deleted (the list is kept in .installed).
