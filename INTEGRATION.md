# Scribble Book — Integration Guide

This guide explains how to add Scribble Book support to your mod: creating block and entity entries, localising text, customising the tab icon, and hooking into the study event.

---

## 1. Dependency Setup

Add Scribble Book as a compile-time dependency in your `build.gradle`:

```groovy
dependencies {
    // Replace VERSION with the actual release version
    compileOnly "org.skvipers:scribble_book:VERSION:api"
}
```

Declare the dependency in your `mods.toml` so Forge loads it before your mod:

```toml
[[dependencies.your_modid]]
    modId = "scribble_book"
    mandatory = true
    versionRange = "[VERSION,)"
    ordering = "BEFORE"
    side = "BOTH"
```

> If you only use the data-pack integration (JSON entries, no Java code), the dependency is **optional** — your entries simply won't appear if Scribble Book is absent.

---

## 2. Adding Block Entries

Entries are loaded as a server-side data pack. No Java code required.

### File location

```
data/<your_modid>/scribble_book/blocks/<block_id>.json
```

The `<block_id>` must match the block's registry name exactly (e.g. `my_furnace` for `yourmod:my_furnace`).

### JSON format

```json
{
  "title": "My Furnace",
  "basic": "A furnace that runs on magic instead of coal.",
  "deep":  "Efficiency doubles when placed near a mana crystal.",
  "sneak_only": false
}
```

| Field        | Required | Description |
|--------------|----------|-------------|
| `title`      | Yes | Display name shown in the book |
| `basic`      | Yes | Text shown after the first study |
| `deep`       | No  | Text shown after the second study. Omit if there is no extra information |
| `sneak_only` | No  | If `true`, the entry is only triggered by Shift+RMB (default: `false`) |

If `deep` is absent or blank, the block can only be studied once.

### Using translation keys

Any field can hold a lang key instead of raw text. The book resolves it at render time using the client's active language:

```json
{
  "title": "yourmod.entry.my_furnace.title",
  "basic": "yourmod.entry.my_furnace.basic",
  "deep":  "yourmod.entry.my_furnace.deep",
  "sneak_only": false
}
```

```json
// assets/yourmod/lang/en_us.json
{
  "yourmod.entry.my_furnace.title": "My Furnace",
  "yourmod.entry.my_furnace.basic": "A furnace that runs on magic instead of coal.",
  "yourmod.entry.my_furnace.deep":  "Efficiency doubles when placed near a mana crystal."
}
```

This is the **recommended approach** — it keeps all translatable strings in one place and supports any language your mod provides.

---

## 3. Adding Entity Entries

Entity entries work identically to block entries but are triggered by interacting with a living entity.

### File location

```
data/<your_modid>/scribble_book/entities/<entity_id>.json
```

### JSON format

```json
{
  "title": "yourmod.entity.my_creature.title",
  "basic": "yourmod.entity.my_creature.basic",
  "deep":  "yourmod.entity.my_creature.deep",
  "sneak_only": true
}
```

Setting `sneak_only: true` is recommended for hostile or dangerous mobs — it prevents accidentally triggering the book while trying to attack.

---

## 4. Aliases

Aliases let you redirect multiple block or entity IDs to a single entry. This is useful when several variants share the same knowledge (e.g. all coloured beds pointing to one `bed` entry).

### File location

```
data/<your_modid>/scribble_book/aliases/<any_name>.json
```

Multiple alias files are merged at load time, so you can split them however you like.

### JSON format

```json
{
  "yourmod:red_widget":  "yourmod:widget",
  "yourmod:blue_widget": "yourmod:widget",
  "yourmod:green_widget": "yourmod:widget"
}
```

Keys are the IDs to redirect; values are the canonical entry key to look up. Both blocks and entities share the same alias map.

---

## 5. Tab Icon

Each mod's entries are grouped into a separate tab in the book UI. The icon is resolved automatically in this priority order:

1. The first creative mode tab whose icon item belongs to your mod's namespace
2. The first item registered under your namespace in the item registry
3. Fallback: a plain book

In most cases this works without any configuration — your mod's main creative tab icon will be used.

### Explicit icon override *(planned)*

A future release will support declaring a custom icon via:

```
data/<your_modid>/scribble_book/tab_icon.json
```

```json
{
  "icon": "yourmod:my_special_block"
}
```

---

## 6. Study Event

`ScribbleBookStudyEvent` is fired on `MinecraftForge.EVENT_BUS` **before** a player studies a block. You can:

- Cancel studying entirely
- Change the ink or paper cost for specific blocks

```java
@SubscribeEvent
public static void onStudy(ScribbleBookStudyEvent event) {
    ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(event.getState().getBlock());

    // Make your blocks free to study
    if (blockId != null && blockId.getNamespace().equals("yourmod")) {
        event.setInkCost(0);
        event.setPaperCost(0);
    }

    // Prevent studying a specific block
    if (new ResourceLocation("yourmod", "secret_block").equals(blockId)) {
        event.setCanceled(true);
    }
}
```

### Event fields

| Method | Type | Description |
|--------|------|-------------|
| `getPlayer()` | `Player` | The player who is studying |
| `getPos()` | `BlockPos` | Position of the block being studied |
| `getState()` | `BlockState` | Block state at that position |
| `getTargetLevel()` | `KnowledgeLevel` | `BASIC` (first study) or `DEEP` (second study) |
| `getInkCost()` / `setInkCost(int)` | `int` | Ink units consumed (0–100 per bottle) |
| `getPaperCost()` / `setPaperCost(int)` | `int` | Paper sheets consumed |

Costs are clamped to `≥ 0`. Cancelling the event prevents both resource consumption and data storage.

---

## 7. Custom Entry Keys

By default the book uses the block's registry ID as the entry key (`minecraft:furnace`, etc.). You can override this in `ScribbleBookStudyEvent` to store progress under any identifier you choose — including dynamic sub-keys computed at study time.

### Why use a custom key?

- Your block has multiple knowledge "topics" depending on its state or context
- You want to model a multi-step discovery chain that isn't tied to a single block
- You need a sub-entry hierarchy (e.g. `yourmod:spawner/ores`, `yourmod:spawner/mobs`)

### How it works

1. Place your entry JSONs at the appropriate paths:

```
data/yourmod/scribble_book/blocks/block_spawner.json          → yourmod:block_spawner
data/yourmod/scribble_book/blocks/block_spawner/ores.json     → yourmod:block_spawner/ores
data/yourmod/scribble_book/blocks/block_spawner/mobs.json     → yourmod:block_spawner/mobs
```

2. Override `entryKey` in the event:

```java
@SubscribeEvent
public static void onStudy(ScribbleBookStudyEvent event) {
    BlockState state = event.getState();
    if (!(state.getBlock() instanceof BlockSpawnerBlock)) return;

    Level level = event.getPlayer().level();
    BlockEntity be = level.getBlockEntity(event.getPos());

    if (be instanceof BlockSpawnerEntity spawner && spawner.hasFrame()) {
        String category = spawner.getCategory(); // e.g. "ores", "mobs"
        event.setEntryKey(new ResourceLocation("yourmod", "block_spawner/" + category));
    } else {
        event.setEntryKey(new ResourceLocation("yourmod", "block_spawner"));
    }
}
```

3. The book will:
   - Look up the entry JSON by `event.getEntryKey()`
   - Persist BASIC / DEEP progress separately per key
   - Display the entry under the correct tab (grouped by namespace)

### Notes

- If you change the key, you are responsible for setting appropriate `inkCost` / `paperCost` too, since the default costs are computed from the block's default key before the event fires.
- Sub-keys (`block_spawner/ores`) appear as separate entries in the book list, sorted alphabetically alongside other entries of the same namespace.
- A `null` entry for the overridden key causes the book to show the "nothing worth noting" message, which you can use to silently block studying in certain states.

---

## 8. Reading Book Data

`BookData` is stored as NBT on the Scribble Book item stack. You can read it anywhere you have access to the item:

```java
import org.skvipers.scribble_book.book.BookData;
import org.skvipers.scribble_book.book.KnowledgeLevel;

ItemStack book = player.getMainHandItem(); // or wherever you get it
BookData data = BookData.fromStack(book);

ResourceLocation blockId = new ResourceLocation("minecraft", "furnace");

if (data.hasEntry(blockId)) {
    KnowledgeLevel level = data.getLevel(blockId); // BASIC or DEEP
}
```

### KnowledgeLevel values

| Value | Meaning |
|-------|---------|
| `BASIC` | Player has studied the entry once |
| `DEEP` | Player has studied the entry twice (full knowledge) |
| `null` | Entry has not been studied |

---

## 9. Default Study Costs

The server operator configures costs in `config/scribble_book-common.toml`:

| Config key | Default | Description |
|------------|---------|-------------|
| `basicInkCost` | 10 | Ink units for first study |
| `basicPaperCost` | 1 | Paper sheets for first study |
| `deepInkCost` | 30 | Ink units for second study |
| `deepPaperCost` | 1 | Paper sheets for second study |

One ink bottle holds **100 units**. When depleted it becomes an empty bottle.

---

## 10. Soft Dependency Pattern

If Scribble Book is optional for your mod, guard all API calls:

```java
public static final boolean SCRIBBLE_BOOK_LOADED =
    ModList.get().isLoaded("scribble_book");

// Then at call sites:
if (SCRIBBLE_BOOK_LOADED) {
    ScribbleBookCompat.register();
}
```

Keep all Scribble Book imports inside a separate `ScribbleBookCompat` class so the JVM only loads it when the mod is present.

---

## 11. Minimal Example

```
data/
  yourmod/
    scribble_book/
      blocks/
        magic_furnace.json
        crystal_table.json
      entities/
        fire_sprite.json
      aliases/
        blocks.json

assets/
  yourmod/
    lang/
      en_us.json
```

`magic_furnace.json`:
```json
{
  "title": "yourmod.entry.magic_furnace.title",
  "basic": "yourmod.entry.magic_furnace.basic",
  "deep":  "yourmod.entry.magic_furnace.deep",
  "sneak_only": false
}
```

`fire_sprite.json`:
```json
{
  "title": "yourmod.entity.fire_sprite.title",
  "basic": "yourmod.entity.fire_sprite.basic",
  "sneak_only": true
}
```

`aliases/blocks.json` (redirect all coloured variants to one entry):
```json
{
  "yourmod:red_crystal_lamp":   "yourmod:crystal_lamp",
  "yourmod:blue_crystal_lamp":  "yourmod:crystal_lamp",
  "yourmod:green_crystal_lamp": "yourmod:crystal_lamp"
}
```

`en_us.json` (excerpt):
```json
{
  "yourmod.entry.magic_furnace.title": "Magic Furnace",
  "yourmod.entry.magic_furnace.basic": "Smelts items using ambient mana. Requires no fuel.",
  "yourmod.entry.magic_furnace.deep":  "Output speed scales with local mana density. Place near a mana pool for best results.",
  "yourmod.entity.fire_sprite.title": "Fire Sprite",
  "yourmod.entity.fire_sprite.basic": "A small elemental that ignites nearby blocks. Immune to fire damage."
}
```

That's all — no Java required for basic integration.
