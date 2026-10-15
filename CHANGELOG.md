# Codex: Armor Stands changelog

## 1.0.1

### New
- **Elements: Claims support:** stands on land claimed with Elements: Claims can only be edited by players who can build there (the owner, or a rank with Build). Switch: `elements_claims` under `[protection]` in features.toml.

### Changed
- **Chunk limit hands over to Elements:** when Elements: Claims or Elements: Nexus 1.0.2+ is installed, their entity limits are used and `chunk_limit` is turned off, even if theirs are switched off. Add `"chunk_limit"` to `overlap_guard.force_on` to keep this mod's limit on.

## 1.0.0
- First release.
