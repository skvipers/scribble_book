# Scribble Book — Integration Guide

This guide explains how to add Scribble Book support to your mod: creating block entries, localising text, customising the tab icon, and hooking into the study event.

---

## 1. Dependency Setup

Add Scribble Book as a compile-time dependency in your `build.gradle`:

```groovy
dependencies {
    // Replace VERSION with the actual release version
    compileOnly "org.skvipers:scribble_book:VERSION:api"
}
```

Declare the dependency in your `neoforge.mods.toml` so NeoForge loads it before your mod:

```toml
[[dependencies.your_modid]]
    modId = "scribble_book"
    type = "required"
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
data/<your_modid>/scribble_book/entries/<block_id>.json
```

The `<block_id>` must match the block's registry name exactly (e.g. `my_furnace` for `yourmod:my_furnace`).

### JSON format

```json
{
  "title": "My Furnace",
  "basic": "A furnace that runs on magic instead of coal.",
  "deep":  "Efficiency doubles when placed near a mana crystal."
}
```

| Field   | Required | Description |
|---------|----------|-------------|
| `title` | Yes | Display name shown in the book |
| `basic` | Yes | Text shown after the first study (Shift+RMB) |
| `deep`  | No  | Text shown after the second study. Omit if there is no extra information |

If `deep` is absent or blank, the block can only be studied once.

### Using translation keys

Any field can hold a lang key instead of raw text. The book resolves it at render time using the client's active language:

```json
{
  "title": "yourmod.entry.my_furnace.title",
  "basic": "yourmod.entry.my_furnace.basic",
  "deep":  "yourmod.entry.my_furnace.deep"
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

## 3. Tab Icon

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

## 4. Study Event

`ScribbleBookStudyEvent` is fired on `NeoForge.EVENT_BUS` **before** a player studies a block. You can:

- Cancel studying entirely
- Change the ink or paper cost for specific blocks

```java
@SubscribeEvent
public static void onStudy(ScribbleBookStudyEvent event) {
    Identifier blockId = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock());

    // Make your blocks free to study
    if (blockId.getNamespace().equals("yourmod")) {
        event.setInkCost(0);
        event.setPaperCost(0);
    }

    // Prevent studying a specific block
    if (blockId.equals(Identifier.fromNamespaceAndPath("yourmod", "secret_block"))) {
        event.cancel();
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

## 5. Custom Entry Keys

By default the book uses the block's registry ID as the entry key (`minecraft:furnace`, etc.). You can override this in `ScribbleBookStudyEvent` to store progress under any identifier you choose — including dynamic sub-keys computed at study time.

### Why use a custom key?

- Your block has multiple knowledge "topics" depending on its state or context
- You want to model a multi-step discovery chain that isn't tied to a single block
- You need a sub-entry hierarchy (e.g. `yourmod:spawner/ores`, `yourmod:spawner/mobs`)

### How it works

1. Place your entry JSONs at the appropriate paths:

```
data/yourmod/scribble_book/entries/block_spawner.json          → yourmod:block_spawner
data/yourmod/scribble_book/entries/block_spawner/ores.json     → yourmod:block_spawner/ores
data/yourmod/scribble_book/entries/block_spawner/mobs.json     → yourmod:block_spawner/mobs
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
        // Dynamic sub-key based on block state
        String category = spawner.getCategory(); // e.g. "ores", "mobs"
        event.setEntryKey(Identifier.fromNamespaceAndPath("yourmod",
                "block_spawner/" + category));
    } else {
        // Base entry — no frame
        event.setEntryKey(Identifier.fromNamespaceAndPath("yourmod", "block_spawner"));
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

## 6. Reading Book Data  

`BookData` is stored as a data component on the Scribble Book item stack. You can read it anywhere you have access to the item:

```java
import org.skvipers.scribble_book.book.BookData;
import org.skvipers.scribble_book.book.KnowledgeLevel;
import org.skvipers.scribble_book.registry.ModDataComponents;

ItemStack book = player.getMainHandItem(); // or wherever you get it
BookData data = book.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);

Identifier blockId = Identifier.fromNamespaceAndPath("minecraft", "furnace");

if (data.hasEntry(blockId)) {
    KnowledgeLevel level = data.getLevel(blockId); // BASIC or DEEP
}
```

### KnowledgeLevel values

| Value | Meaning |
|-------|---------|
| `BASIC` | Player has studied the block once |
| `DEEP` | Player has studied the block twice (full knowledge) |
| `null` | Block has not been studied |

---

## 7. Default Study Costs

The server operator configures costs in `config/scribble_book-common.toml`:

| Config key | Default | Description |
|------------|---------|-------------|
| `basicInkCost` | 10 | Ink units for first study |
| `basicPaperCost` | 1 | Paper sheets for first study |
| `deepInkCost` | 30 | Ink units for second study |
| `deepPaperCost` | 1 | Paper sheets for second study |

One ink bottle holds **100 units**. When depleted it becomes an empty bottle.

---

## 8. Soft Dependency Pattern

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

## 9. Minimal Example

```
data/
  yourmod/
    scribble_book/
      entries/
        magic_furnace.json
        crystal_table.json

assets/
  yourmod/
    lang/
      en_us.json
      ru_ru.json
```

`magic_furnace.json`:
```json
{
  "title": "yourmod.entry.magic_furnace.title",
  "basic": "yourmod.entry.magic_furnace.basic",
  "deep":  "yourmod.entry.magic_furnace.deep"
}
```

`en_us.json` (excerpt):
```json
{
  "yourmod.entry.magic_furnace.title": "Magic Furnace",
  "yourmod.entry.magic_furnace.basic": "Smelts items using ambient mana. Requires no fuel.",
  "yourmod.entry.magic_furnace.deep":  "Output speed scales with local mana density. Place near a mana pool for best results."
}
```

That's all — no Java required for basic integration.
