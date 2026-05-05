# Scribble Book — Integration Guide

This guide explains how to add Scribble Book support to your mod: creating entries, hooking into events, reading book data, and registering custom unlock condition types.

---

## 1. Dependency Setup

Add Scribble Book as a compile-time dependency in your `build.gradle`:

```groovy
dependencies {
    compileOnly "org.skvipers:scribble_book:VERSION:api"
}
```

Declare the dependency in `neoforge.mods.toml`:

```toml
[[dependencies.your_modid]]
    modId = "scribble_book"
    type = "required"
    versionRange = "[VERSION,)"
    ordering = "BEFORE"
    side = "BOTH"
```

> If you only use JSON entries (no Java code), the dependency is **optional** — your entries simply won't appear if Scribble Book is absent.

---

## 2. Entry Data Paths

| Type | Path |
|------|------|
| Blocks | `data/<modid>/scribble_book/blocks/<id>.json` |
| Entities | `data/<modid>/scribble_book/entities/<id>.json` |
| Items | `data/<modid>/scribble_book/items/<id>.json` |
| Custom pages | `data/<modid>/scribble_book/custom/<id>.json` |
| Categories | `data/<modid>/scribble_book/categories/<id>.json` |
| Aliases | `data/<modid>/scribble_book/aliases/<any>.json` |

---

## 3. Entry Format

Entries support two formats. The legacy format is still fully supported.

### Legacy format (flat strings)

```json
{
  "title": "yourmod.entry.my_block.title",
  "basic": "yourmod.entry.my_block.basic",
  "deep":  "yourmod.entry.my_block.deep",
  "sneak_only": false
}
```

### New format (sections + blocks)

```json
{
  "title": "yourmod.entry.my_block.title",
  "sneak_only": false,
  "sections": [
    {
      "level": "basic",
      "blocks": [
        { "type": "text", "text": "yourmod.entry.my_block.basic" }
      ]
    },
    {
      "level": "deep",
      "blocks": [
        { "type": "text", "text": "yourmod.entry.my_block.deep" }
      ]
    }
  ]
}
```

### All entry fields

| Field | Default | Description |
|-------|---------|-------------|
| `title` | required | Display name or lang key |
| `sections` | — | New format: list of `{ level, blocks }` |
| `basic` / `deep` | — | Legacy format strings |
| `sneak_only` | `true` | Require Shift+RMB to trigger study |
| `countable` | `true` | Include in progress bar and `/scribblebook missing` |
| `always_visible` | `false` | Show without studying (for custom pages) |
| `unlock` | none | Condition that must be met to show this entry |

Any `title` / `text` value is treated as a lang key if a translation exists, otherwise rendered as-is.

---

## 4. Aliases

Redirect multiple IDs to a single entry:

```
data/<modid>/scribble_book/aliases/<any>.json
```

```json
{
  "yourmod:red_widget":  "yourmod:widget",
  "yourmod:blue_widget": "yourmod:widget"
}
```

Both blocks and entities share the alias map.

---

## 5. Study Events

Three events are fired on `NeoForge.EVENT_BUS` before a study completes. All are cancellable.

| Event | Triggered by |
|-------|-------------|
| `ScribbleBookStudyEvent` | Block study |
| `ScribbleBookEntityStudyEvent` | Entity study |
| `ScribbleBookItemStudyEvent` | Item study |

All events expose:

```java
event.getPlayer()       // Player performing the study
event.getEntryKey()     // Identifier of the entry being studied
event.setEntryKey(id)   // Override the entry key (custom sub-keys)
event.getInkCost()      // Ink units to consume
event.setInkCost(n)
event.getPaperCost()
event.setPaperCost(n)
event.cancel()
```

### Example — free study for your mod's blocks

```java
@SubscribeEvent
public static void onStudy(ScribbleBookStudyEvent event) {
    if (event.getEntryKey().getNamespace().equals("yourmod")) {
        event.setInkCost(0);
        event.setPaperCost(0);
    }
}
```

---

## 6. Reading Book Data

`BookData` is a data component on the `ItemStack`. Read it anywhere you have the item:

```java
BookData data = book.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);

KnowledgeLevel level = data.getLevel(Identifier.fromNamespaceAndPath("yourmod", "my_block"));
// null = not studied, BASIC = first study, DEEP = second study

boolean unlocked = data.isUnlocked(Identifier.fromNamespaceAndPath("yourmod", "my_page"));
```

`isUnlocked` returns `true` for entries whose unlock condition has been satisfied. Entries without an unlock condition are always shown but not tracked in `unlockedEntries`.

---

## 7. ScribbleBookAPI

`ScribbleBookAPI` is the public facade. Call it during your mod's constructor (before datapacks load).

### Register a custom unlock condition type

```java
ScribbleBookAPI.registerConditionType("yourmod:kills", MyKillsCondition.MAP_CODEC);
```

Datapacks can then use `"type": "yourmod:kills"` in any `unlock` field.

Your condition class must implement `UnlockCondition`:

```java
public record MyKillsCondition(int required) implements UnlockCondition {

    public static final MapCodec<MyKillsCondition> MAP_CODEC = RecordCodecBuilder.mapCodec(i ->
            i.group(Codec.INT.fieldOf("required").forGetter(MyKillsCondition::required))
            .apply(i, MyKillsCondition::new));

    @Override public String type() { return "yourmod:kills"; }

    @Override
    public boolean isMet(BookData bookData, ServerPlayer player) {
        return MyKillTracker.getKills(player) >= required;
    }
}
```

### Trigger re-evaluation manually

Call this after an external event that may satisfy a condition (e.g. your kill tracker updates):

```java
// Re-evaluate a specific book stack
ScribbleBookAPI.reEvaluateBookUnlocks(serverPlayer, bookStack);

// Re-evaluate all books in player's inventory
ScribbleBookAPI.reEvaluateAllBooks(serverPlayer);
```

---

## 8. Soft Dependency Pattern

```java
public static final boolean SCRIBBLE_BOOK = ModList.get().isLoaded("scribble_book");

// At call sites:
if (SCRIBBLE_BOOK) ScribbleBookCompat.register();
```

Keep all Scribble Book imports inside a separate `ScribbleBookCompat` class so the JVM only loads it when the mod is present.

---

## 9. Default Study Costs

Configured in `config/scribble_book-common.toml`:

| Key | Default | Description |
|-----|---------|-------------|
| `basicInkCost` | 10 | Ink units for first study |
| `basicPaperCost` | 1 | Paper sheets for first study |
| `deepInkCost` | 30 | Ink units for second study |
| `deepPaperCost` | 1 | Paper sheets for second study |

One ink bottle holds **100 units**. When depleted it becomes an empty bottle.
