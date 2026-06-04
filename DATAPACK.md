# Scribble Book — Datapack Guide

This guide covers everything a modpack developer needs to add entries, categories, custom pages, and unlock conditions through datapacks — no Java required.

---

## 1. Data Paths

| Content type | Path |
|---|---|
| Block entries | `data/<modid>/scribble_book/blocks/<id>.json` |
| Entity entries | `data/<modid>/scribble_book/entities/<id>.json` |
| Item entries | `data/<modid>/scribble_book/items/<id>.json` |
| Custom pages | `data/<modid>/scribble_book/custom/<id>.json` |
| Categories | `data/<modid>/scribble_book/categories/<id>.json` |
| Aliases | `data/<modid>/scribble_book/aliases/<any>.json` |

The filename (without `.json`) becomes the entry ID within the namespace. For `data/mypack/scribble_book/blocks/reactor_core.json` the full entry ID is `mypack:reactor_core`.

---

## 2. Entry Formats

All entry types (blocks, entities, items, custom) use the same JSON format.

### Legacy format

```json
{
  "title": "mypack.entry.reactor_core.title",
  "basic": "mypack.entry.reactor_core.basic",
  "deep":  "mypack.entry.reactor_core.deep",
  "sneak_only": false
}
```

`basic` is shown after the first study, `deep` after the second. Omit `deep` for a single-level entry.

### Sections format

```json
{
  "title": "mypack.entry.reactor_core.title",
  "sneak_only": false,
  "countable": true,
  "always_visible": false,
  "sections": [
    {
      "level": "basic",
      "blocks": [
        { "type": "text", "text": "mypack.entry.reactor_core.basic" }
      ]
    },
    {
      "level": "deep",
      "blocks": [
        { "type": "text",   "text": "mypack.entry.reactor_core.deep" },
        { "type": "item",   "item": "mypack:reactor_core" },
        { "type": "image",  "texture": "mypack:textures/gui/reactor_diagram.png", "width": 128, "height": 64, "align": "center" },
        { "type": "items",  "items": ["minecraft:iron_ingot", "mypack:coil"], "background": true },
        { "type": "recipe", "output": "mypack:reactor_core", "grid": ["mypack:coil","mypack:coil","mypack:coil","mypack:coil","minecraft:iron_block","mypack:coil","mypack:coil","mypack:coil","mypack:coil"] }
      ]
    }
  ]
}
```

Both formats are fully supported. Use the sections format for multi-block content or unlock conditions.

### Entry fields

| Field | Default | Description |
|---|---|---|
| `title` | required | Display name or lang key |
| `sections` | — | List of `{ "level", "blocks" }` objects |
| `basic` / `deep` | — | Legacy format text strings |
| `sneak_only` | `true` | Require Shift+RMB to trigger study |
| `countable` | `true` | Count in progress bar and `/scribblebook missing` |
| `always_visible` | `false` | Show in the book without studying (custom pages) |
| `unlock` | none | Condition that must be met to reveal this entry |

Any `title` or `text` value is looked up as a lang key first. If no translation exists it is rendered as literal text.

---

## 3. Content Blocks

Each section's `blocks` array can contain any mix of the five block types.

### `text`

```json
{ "type": "text", "text": "mypack.entry.my_entry.description" }
```

Supports lang keys or literal strings. Use `\n` for line breaks within a single block.

### `item`

```json
{ "type": "item", "item": "minecraft:diamond" }
```

Renders one item icon with its hover name on the same line.

### `image`

```json
{
  "type": "image",
  "texture": "mypack:textures/gui/my_image.png",
  "width": 128,
  "height": 64,
  "align": "center"
}
```

| Field | Default | Description |
|---|---|---|
| `texture` | required | Resource location of the texture file |
| `width` | `0` | Display width in pixels. `0` = fill page width |
| `height` | `0` | Display height in pixels. `0` = same as width |
| `align` | `"left"` | Horizontal alignment: `"left"`, `"center"`, `"right"` |

Place the texture at `assets/<namespace>/textures/<path>.png` inside your resource pack. Both width and height can be omitted if you want the image to fill the full page width.

### `items`

Displays a row of item icons with optional slot backgrounds. Items are split into rows automatically if they exceed the page width.

```json
{
  "type": "items",
  "items": ["minecraft:iron_ingot", "minecraft:gold_ingot", "minecraft:diamond"],
  "background": true,
  "align": "center",
  "gap": 1
}
```

| Field | Default | Description |
|---|---|---|
| `items` | required | List of item registry IDs |
| `background` | `true` | Render slot background behind each item |
| `align` | `"center"` | Row alignment: `"left"`, `"center"`, `"right"` |
| `gap` | `1` | Pixel gap between slots |

### `recipe`

Displays a 3×3 crafting grid with an arrow and output slot. The layout is static — you supply the items directly rather than referencing a recipe ID.

```json
{
  "type": "recipe",
  "output": "minecraft:golden_apple",
  "grid": [
    "minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:gold_ingot",
    "minecraft:gold_ingot", "minecraft:apple",      "minecraft:gold_ingot",
    "minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:gold_ingot"
  ]
}
```

| Field | Default | Description |
|---|---|---|
| `output` | required | Registry ID of the crafted result |
| `grid` | required | 9-element list of item IDs, row-major (top-left → bottom-right). Use `""` for empty slots |

For shapeless or shaped recipes that don't fill all 9 slots, put the ingredients in the occupied positions and leave the rest as `""`.

---

## 4. Categories

Categories group entries into tabs in the book UI.

**Path:** `data/<modid>/scribble_book/categories/<id>.json`

```json
{
  "title": "mypack.category.machines.title",
  "icon": "mypack:reactor_core",
  "sort_order": 10,
  "entries": [
    "mypack:reactor_core",
    "mypack:power_conduit",
    "mypack:energy_cell"
  ]
}
```

| Field | Default | Description |
|---|---|---|
| `title` | required | Display name or lang key |
| `icon` | required | Item registry ID used as the tab icon |
| `sort_order` | `0` | Lower values appear first |
| `entries` | `[]` | Ordered list of entry IDs shown in this tab |

An entry can appear in multiple categories. Entries not listed in any category still appear in the book under a default "all" view.

---

## 5. Custom Pages

Custom pages are entries that are not tied to studying a block, entity, or item. They are placed under `custom/` and are typically used for guides, lore, or content gated behind unlock conditions.

**Path:** `data/<modid>/scribble_book/custom/<id>.json`

A guide page visible from the start:

```json
{
  "title": "mypack.guide.title",
  "countable": false,
  "always_visible": true,
  "sections": [
    {
      "level": "basic",
      "blocks": [
        { "type": "text", "text": "mypack.guide.intro" }
      ]
    }
  ]
}
```

A lore page that unlocks only after the player studies a specific entry:

```json
{
  "title": "mypack.lore.ancient_secret.title",
  "countable": false,
  "sections": [
    {
      "level": "basic",
      "blocks": [
        { "type": "text", "text": "mypack.lore.ancient_secret.text" }
      ]
    }
  ],
  "unlock": {
    "type": "studied",
    "entry": "mypack:ancient_tome"
  }
}
```

---

## 6. Unlock Conditions

The `unlock` field accepts any condition object with a `type` key. If the condition is not met the entry is hidden entirely. Conditions are re-evaluated each time the book is opened, on entity kills, and on player login.

### `studied`

True when the player has studied the given entry at the specified level.

```json
{
  "type": "studied",
  "entry": "minecraft:beacon",
  "level": "basic"
}
```

| Field | Default | Description |
|---|---|---|
| `entry` | required | Entry ID to check |
| `level` | `"basic"` | `"basic"` (any study) or `"deep"` (second study) |

### `studied_count`

True when the player has studied at least `count` entries in total, optionally filtered by namespace.

```json
{
  "type": "studied_count",
  "count": 10,
  "namespace": "minecraft"
}
```

| Field | Default | Description |
|---|---|---|
| `count` | required | Minimum number of studied entries |
| `namespace` | — | If set, only count entries from this namespace |

### `advancement`

True when the player has completed the specified advancement.

```json
{
  "type": "advancement",
  "advancement": "minecraft:story/enter_the_end"
}
```

### `killed_entity`

True when the player has killed at least `count` of the specified entity type (tracked by vanilla statistics).

```json
{
  "type": "killed_entity",
  "entity": "minecraft:ender_dragon",
  "count": 1
}
```

| Field | Default | Description |
|---|---|---|
| `entity` | required | Entity type registry ID |
| `count` | `1` | Minimum kill count |

### `picked_up_item`

True when the player has picked up at least `count` of the specified item (tracked by vanilla statistics).

```json
{
  "type": "picked_up_item",
  "item": "minecraft:nether_star",
  "count": 1
}
```

| Field | Default | Description |
|---|---|---|
| `item` | required | Item registry ID |
| `count` | `1` | Minimum pickup count |

> Note: this condition re-evaluates on entity kill and book open events, not immediately on pickup.

### `scoreboard`

True when the player's score on the given objective is at least `min`.

```json
{
  "type": "scoreboard",
  "objective": "my_quest_points",
  "min": 100
}
```

| Field | Default | Description |
|---|---|---|
| `objective` | required | Scoreboard objective name |
| `min` | `1` | Minimum score value |

### `all_of`

True when every condition in the list is met.

```json
{
  "type": "all_of",
  "conditions": [
    { "type": "advancement", "advancement": "minecraft:story/enter_the_end" },
    { "type": "killed_entity", "entity": "minecraft:enderman", "count": 5 }
  ]
}
```

### `any_of`

True when at least one condition in the list is met.

```json
{
  "type": "any_of",
  "conditions": [
    { "type": "studied", "entry": "minecraft:beacon" },
    { "type": "studied", "entry": "minecraft:conduit" }
  ]
}
```

### `not`

Inverts a condition.

```json
{
  "type": "not",
  "condition": {
    "type": "studied",
    "entry": "mypack:spoiler_entry"
  }
}
```

Conditions can be nested arbitrarily:

```json
{
  "type": "all_of",
  "conditions": [
    { "type": "advancement", "advancement": "minecraft:story/enter_the_end" },
    {
      "type": "any_of",
      "conditions": [
        { "type": "killed_entity", "entity": "minecraft:ender_dragon" },
        { "type": "scoreboard", "objective": "dragon_kills", "min": 1 }
      ]
    }
  ]
}
```

---

## 7. Aliases

Aliases redirect one or more IDs to a single entry. Useful for block/entity variants that should share one page.

**Path:** `data/<modid>/scribble_book/aliases/<any>.json`

```json
{
  "mypack:red_widget":  "mypack:widget",
  "mypack:blue_widget": "mypack:widget",
  "mypack:green_widget": "mypack:widget"
}
```

Studying any of the keys will record progress under `mypack:widget`. Blocks and entities share the same alias map.

Multiple alias files can coexist; they are all merged at load time.

---

## 8. Translations

All `title` and `text` values are resolved as lang keys at render time. Place your translations in a resource pack:

```
assets/<modid>/lang/en_us.json
```

```json
{
  "mypack.entry.reactor_core.title": "Reactor Core",
  "mypack.entry.reactor_core.basic": "A basic reactor core generates power when supplied with coolant.",
  "mypack.entry.reactor_core.deep": "Advanced models support overclock modules, but risk meltdown above 80% load.",
  "mypack.category.machines.title": "Machines"
}
```

If a key has no translation the raw string is rendered as-is, so literal text works without a lang file.
