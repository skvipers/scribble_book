# Changelog

## [0.4.0]

### Architecture
- Entries now use a structured **sections + content blocks** model instead of flat strings. The legacy format (`basic`/`deep` string fields) remains fully supported and is auto-converted on load.
- Introduced `ContentBlock` sealed interface (`text`, `item`, `image`) as the building block for entry content. `item` and `image` blocks are parsed and stored but not yet rendered — planned for a future release.
- `BookData` extended with an `unlockedEntries` set. Unlock state is stored directly on the `ItemStack` alongside study progress, preserving the physical book design — handing a book to another player transfers all data including unlocked pages.
- Added `ClientboundOpenBookPacket`: the server now re-evaluates unlock conditions and syncs the result to the client before the screen opens, eliminating a race condition where stale state was displayed.
- Unlock conditions use a dynamic dispatch registry (`ConcurrentHashMap`) instead of a sealed hierarchy, allowing other mods to register custom condition types at init time.
- All codecs use `Codec.withAlternative` for forward and backward compatibility — existing book items load without errors after upgrading.

### New features
- **Unlock conditions** — any entry can declare an `unlock` condition; the entry is hidden until the condition is met. Nine built-in condition types:
  - `studied` — entry studied at a given level
  - `studied_count` — total studied count, optionally filtered by namespace
  - `advancement` — vanilla advancement completed
  - `killed_entity` — entity kill count via vanilla statistics
  - `picked_up_item` — item pickup count via vanilla statistics
  - `scoreboard` — scoreboard objective value
  - `all_of` — all sub-conditions met
  - `any_of` — at least one sub-condition met
  - `not` — logical negation
- **Custom pages** — data-driven pages under `data/<modid>/scribble_book/custom/`. Support `always_visible` (visible without studying) and `countable: false` (excluded from progress bar and `/scribblebook missing`).
- **Categories** — data-driven category tabs under `data/<modid>/scribble_book/categories/`. Each category defines a title, icon item, sort order, and entry list.
- **ScribbleBookAPI** — public facade for integration:
  - `registerConditionType(type, codec)` — register a custom unlock condition
  - `reEvaluateBookUnlocks(player, stack)` — re-evaluate a specific book
  - `reEvaluateAllBooks(player)` — re-evaluate all books in inventory
- Unlock conditions are re-evaluated on book open, entity kill, and player login.
- Guide page migrated from hardcode to `data/scribble_book/scribble_book/custom/guide.json`.

### Documentation
- `INTEGRATION.md` fully rewritten — covers dependency setup, entry format (both legacy and sections), all data paths, study events, `BookData` API, `ScribbleBookAPI`, and custom condition types.
- `DATAPACK.md` added — datapack developer reference covering all entry fields, content block types, categories, custom pages, and all 9 unlock condition types with full JSON examples.

---

## [0.3.0]

- Item study via spyglass (right-click held item with spyglass)
- 11 new item entries: elytra, trident, totem of undying, ender pearl, golden apple, enchanted golden apple, crossbow, shield, firework rocket, bundle, nether star, recovery compass
- Item study API — `ScribbleBookItemStudyEvent`

---

## [0.2.0]

- Entity entries and entity study
- Aliases — redirect multiple IDs to a single entry
- Advancement triggers for study events
- Admin command (`/scribblebook`)

---

## [0.1.0]

- Initial release
- Block study journal with basic and deep knowledge levels
- Ink and paper cost system with configurable values
- Soulbound enchantment — book survives death
- Progressive UI with per-entry knowledge display
