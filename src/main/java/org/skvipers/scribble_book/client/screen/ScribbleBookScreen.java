package org.skvipers.scribble_book.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.skvipers.scribble_book.ScribbleBook;
import org.skvipers.scribble_book.book.BookCategory;
import org.skvipers.scribble_book.book.BookCategoryLoader;
import org.skvipers.scribble_book.book.BookData;
import org.skvipers.scribble_book.book.BookEntry;
import org.skvipers.scribble_book.book.BookEntryLoader;
import org.skvipers.scribble_book.book.ContentBlock;
import org.skvipers.scribble_book.book.CustomEntryLoader;
import org.skvipers.scribble_book.book.EntityEntryLoader;
import org.skvipers.scribble_book.book.ImageBlock;
import org.skvipers.scribble_book.book.ItemBlock;
import org.skvipers.scribble_book.book.ItemEntryLoader;
import org.skvipers.scribble_book.book.ItemsBlock;
import org.skvipers.scribble_book.book.RecipeBlock;
import org.skvipers.scribble_book.book.KnowledgeLevel;
import org.skvipers.scribble_book.book.TextBlock;
import org.skvipers.scribble_book.registry.ModDataComponents;

import java.util.*;

public class ScribbleBookScreen extends Screen {

    // --- Textures ---
    private static final Identifier TEXTURE          = Identifier.fromNamespaceAndPath(ScribbleBook.MODID, "textures/gui/scribble_book_ui.png");
    private static final Identifier SPR_PAGE_PREV    = Identifier.fromNamespaceAndPath("minecraft", "widget/page_backward");
    private static final Identifier SPR_PAGE_PREV_HOV= Identifier.fromNamespaceAndPath("minecraft", "widget/page_backward_highlighted");
    private static final Identifier SPR_PAGE_NEXT    = Identifier.fromNamespaceAndPath("minecraft", "widget/page_forward");
    private static final Identifier SPR_PAGE_NEXT_HOV= Identifier.fromNamespaceAndPath("minecraft", "widget/page_forward_highlighted");
    private static final Identifier SPR_SCROLL_UP    = Identifier.fromNamespaceAndPath("minecraft", "statistics/sort_up");
    private static final Identifier SPR_SCROLL_DOWN  = Identifier.fromNamespaceAndPath("minecraft", "statistics/sort_down");
    private static final Identifier CELL_TEXTURE     = Identifier.fromNamespaceAndPath(ScribbleBook.MODID, "textures/gui/cell.png");

    // --- Book layout ---
    private static final int TEX_W      = 295;
    private static final int TEX_H      = 256;
    private static final int BOOK_H     = 182;

    private static final int L_TEXT_X   = 15;
    private static final int L_TEXT_Y   = 14;
    private static final int L_TEXT_W   = 123;
    private static final int L_TEXT_H   = 148;

    private static final int R_TEXT_X   = 157;
    private static final int R_TEXT_Y   = 14;
    private static final int R_TEXT_W   = 121;
    private static final int R_TEXT_H   = 148;

    private static final int PAGE_BTN_W   = 23;
    private static final int PAGE_BTN_H   = 13;
    private static final int SCROLL_BTN_W = 18;
    private static final int SCROLL_BTN_H = 18;

    // --- Tab sprites (in scribble_book_ui.png, below the book at y=230) ---
    // Adjust TAB_W / TAB_H / TAB_V_* to match actual sprite dimensions
    private static final int TAB_W    = 19;
    private static final int TAB_H    = 17;
    private static final int TAB_GAP  = 1;   // vertical gap between tabs
    private static final int TAB_TOP    = 18;  // Y offset from bookY for first tab row
    private static final int TAB_L_INSET = 11; // how many px the left tab overlaps the book edge
    private static final int TAB_R_INSET = 11; // how many px the right tab overlaps the book edge
    // UV coords in scribble_book_ui.png (below the book)
    private static final int TAB_U_L  = 2;
    private static final int TAB_V_L  = 204;  // left-side tab variant
    private static final int TAB_U_R  = 2;
    private static final int TAB_V_R  = 187;  // right-side tab variant

    // --- Colours ---
    private static final int COL_TEXT   = 0xFF3B2A1A;
    private static final int COL_TITLE  = 0xFF2A1A0A;
    private static final int COL_HINT   = 0xFF888060;
    private static final int COL_SEL_BG = 0x33000000;
    private static final int COL_SEP    = 0xFF8B6914;

    // --- Render elements ---
    private sealed interface RenderElement
            permits RenderElement.TextLine, RenderElement.ItemLine, RenderElement.ImageLine,
                    RenderElement.ItemsRowLine, RenderElement.RecipeLine {
        record TextLine(FormattedCharSequence seq) implements RenderElement {}
        record ItemLine(ItemStack stack) implements RenderElement {}
        record ImageLine(Identifier texture, int drawW, int drawH, int xOffset) implements RenderElement {}
        record ItemsRowLine(List<ItemStack> stacks, boolean bg, int xOffset, int gap) implements RenderElement {}
        record RecipeLine(ItemStack[] grid, ItemStack output) implements RenderElement {}
    }

    // --- Tab group ---
    private record TabGroup(String namespace, ItemStack icon, List<Map.Entry<Identifier, BookEntry>> entries) {}

    // --- State ---
    private final ItemStack bookStack;
    private final List<TabGroup> tabs = new ArrayList<>();

    private int selectedTab     = 0;
    private int tabScroll       = 0;   // how many tab slots scrolled down
    private int selectedIndex   = 0;
    private int entryPage       = 0;
    private int contentScrollY  = 0;   // pixel offset for content scroll

    // Current tab's entry list (reference into tabs.get(selectedTab))
    private List<Map.Entry<Identifier, BookEntry>> entries = List.of();

    // Computed in init()
    private int bookX, bookY;
    private int entriesPerPage;
    private int maxContentH;   // pixel height of the content area on the right page
    private int tabsPerSide;   // max tab rows on one side

    // Absolute button rects
    private int prevBtnX, prevBtnY, nextBtnX, nextBtnY;
    private int upBtnX, upBtnY, downBtnX, downBtnY;

    private List<RenderElement> renderElements = List.of();
    private int totalContentH = 0;
    private int scribbleEntryListY;

    // -------------------------------------------------------------------------

    public ScribbleBookScreen(ItemStack bookStack) {
        super(Component.translatable("scribble_book.screen.title"));
        this.bookStack = bookStack;
        buildTabs();
    }

    private void buildTabs() {
        BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);

        // Collect visible entries: studied entries + always_visible entries that pass unlock check
        Map<Identifier, BookEntry> visibleEntries = new LinkedHashMap<>();
        java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(
                        java.util.stream.Stream.concat(
                                BookEntryLoader.INSTANCE.getAllEntries().entrySet().stream(),
                                EntityEntryLoader.INSTANCE.getAllEntries().entrySet().stream()
                        ),
                        ItemEntryLoader.INSTANCE.getAllEntries().entrySet().stream()
                ),
                CustomEntryLoader.INSTANCE.getAllEntries().entrySet().stream()
        )
                .filter(e -> {
                    BookEntry entry = e.getValue();
                    if (entry.unlock().isPresent()) return data.isUnlocked(e.getKey());
                    return entry.alwaysVisible() || data.hasEntry(e.getKey());
                })
                .sorted(Map.Entry.comparingByValue((a, b) -> a.title().compareToIgnoreCase(b.title())))
                .forEach(e -> visibleEntries.put(e.getKey(), e.getValue()));

        // Assign entries to category tabs (in sortOrder)
        Set<Identifier> claimed = new HashSet<>();
        List<TabGroup> categoryTabs = new ArrayList<>();
        for (var catEntry : BookCategoryLoader.INSTANCE.getSorted()) {
            BookCategory cat = catEntry.getValue();
            List<Map.Entry<Identifier, BookEntry>> catEntries = cat.entries().stream()
                    .filter(visibleEntries::containsKey)
                    .map(id -> Map.entry(id, visibleEntries.get(id)))
                    .toList();
            if (catEntries.isEmpty()) continue;
            claimed.addAll(cat.entries());
            categoryTabs.add(new TabGroup(
                    "category:" + catEntry.getKey(),
                    resolveCategoryIcon(cat.icon()),
                    new ArrayList<>(catEntries)));
        }

        // Unclaimed entries grouped by namespace (fallback)
        Map<String, List<Map.Entry<Identifier, BookEntry>>> byNs = new LinkedHashMap<>();
        visibleEntries.entrySet().stream()
                .filter(e -> !claimed.contains(e.getKey()))
                .forEach(e -> byNs.computeIfAbsent(e.getKey().getNamespace(), k -> new ArrayList<>()).add(e));

        // scribble_book first, then category tabs, then namespace fallback tabs
        tabs.add(new TabGroup(ScribbleBook.MODID, bookStack.copyWithCount(1), byNs.getOrDefault(ScribbleBook.MODID, List.of())));
        tabs.addAll(categoryTabs);

        List<String> nsOrder = new ArrayList<>();
        if (byNs.containsKey("minecraft")) nsOrder.add("minecraft");
        byNs.keySet().stream()
                .filter(k -> !k.equals(ScribbleBook.MODID) && !k.equals("minecraft"))
                .sorted().forEach(nsOrder::add);
        for (String ns : nsOrder) {
            tabs.add(new TabGroup(ns, resolveTabIcon(ns), byNs.get(ns)));
        }

        entries = tabs.isEmpty() ? List.of() : tabs.get(0).entries();
    }

    private static ItemStack resolveCategoryIcon(Identifier iconId) {
        return BuiltInRegistries.ITEM.getOptional(iconId)
                .filter(item -> item != Items.AIR)
                .map(ItemStack::new)
                .orElse(new ItemStack(Items.BOOK));
    }

    private static ItemStack resolveTabIcon(String namespace) {
        if (namespace.equals("minecraft")) {
            return new ItemStack(Items.GRASS_BLOCK);
        }
        // Try creative tab icon: find a tab whose icon item belongs to this namespace
        for (var tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            ItemStack icon = tab.getIconItem();
            if (!icon.isEmpty()) {
                Identifier iconId = BuiltInRegistries.ITEM.getKey(icon.getItem());
                if (iconId != null && iconId.getNamespace().equals(namespace)) {
                    return icon.copyWithCount(1);
                }
            }
        }
        // Fallback: first item registered under this namespace
        for (var item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (id != null && id.getNamespace().equals(namespace)) {
                return new ItemStack(item);
            }
        }
        return new ItemStack(Items.BOOK);
    }

    // -------------------------------------------------------------------------

    @Override
    protected void init() {
        bookX = (width  - TEX_W)  / 2;
        bookY = (height - BOOK_H) / 2;

        Font font = Minecraft.getInstance().font;
        entriesPerPage = L_TEXT_H / (font.lineHeight + 2);
        // title (lineHeight+3) + separator (1+4) = lineHeight+8 consumed before content
        maxContentH    = R_TEXT_H - font.lineHeight - 8;
        // progress section: label(lH+2) + bar(5+2) + pct(lH+3) + separator(1+4)
        scribbleEntryListY = bookY + L_TEXT_Y + font.lineHeight + 2 + 5 + 2 + font.lineHeight + 3 + 1 + 4;
        tabsPerSide    = Math.min(8, (BOOK_H - TAB_TOP) / (TAB_H + TAB_GAP));

        int navY = bookY + L_TEXT_Y + L_TEXT_H + 4;
        prevBtnX = bookX + L_TEXT_X;
        prevBtnY = navY;
        nextBtnX = bookX + L_TEXT_X + L_TEXT_W - PAGE_BTN_W;
        nextBtnY = navY;

        downBtnX = bookX + R_TEXT_X + R_TEXT_W - SCROLL_BTN_W + 6;
        downBtnY = bookY + R_TEXT_Y + R_TEXT_H + 2 - 6;
        upBtnX   = downBtnX;
        upBtnY   = downBtnY - SCROLL_BTN_H - 2 + 11;

        rebuildRenderElements(font);
    }

    // -------------------------------------------------------------------------

    @Override
    public void extractRenderState(GuiGraphicsExtractor gui, int mx, int my, float pt) {
        gui.fill(0, 0, width, height, 0x80000000);

        // Tabs are rendered BEHIND the book so the selected tab overlaps the edge
        gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                bookX, bookY, 0, 0, TEX_W, BOOK_H, TEX_W, TEX_H);

        if (!tabs.isEmpty()) renderTabs(gui);

        Font font = Minecraft.getInstance().font;
        renderLeftPage(gui, font, mx, my);
        renderRightPage(gui, font);

        super.extractRenderState(gui, mx, my, pt);
    }

    // --- Tabs ----------------------------------------------------------------

    /** Returns true if tab scroll-up arrow is active. */
    private boolean tabHasUp() {
        return tabScroll > 0;
    }

    /**
     * Returns true if tab scroll-down arrow is needed.
     * Down arrow replaces the last slot, so its threshold is
     * "remaining tabs don't fit in (totalSlots - upSlots) slots".
     */
    private boolean tabHasDown() {
        int totalSlots = tabsPerSide * 2;
        int upSlots    = tabHasUp() ? 1 : 0;
        // remaining tabs > available slots (i.e. they overflow without a down arrow)
        return tabs.size() - tabScroll > totalSlots - upSlots;
    }

    /** Absolute screen rect [x, y, w, h] for tab slot, or null if it's an arrow slot. */
    private int[] tabSlotRect(int slot) {
        int side = slot < tabsPerSide ? 0 : 1;
        int row  = slot % tabsPerSide;
        int sy   = bookY + TAB_TOP + row * (TAB_H + TAB_GAP);
        int sx   = (side == 0) ? bookX - TAB_W + TAB_L_INSET : bookX + TEX_W - TAB_R_INSET;
        return new int[]{sx, sy, TAB_W, TAB_H};
    }

    private void renderTabs(GuiGraphicsExtractor gui) {
        if (tabs.isEmpty()) return;
        int totalSlots = tabsPerSide * 2;
        boolean hasUp   = tabHasUp();
        boolean hasDown = tabHasDown();
        int tabIdx = tabScroll;

        for (int slot = 0; slot < totalSlots; slot++) {
            int[] r    = tabSlotRect(slot);
            int sx     = r[0], sy = r[1];
            int side   = slot < tabsPerSide ? 0 : 1;

            // First slot → up-scroll arrow
            if (slot == 0 && hasUp) {
                gui.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_SCROLL_UP, sx, sy, TAB_W, TAB_H);
                continue;
            }
            // Last slot → down-scroll arrow
            if (slot == totalSlots - 1 && hasDown) {
                gui.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_SCROLL_DOWN, sx, sy, TAB_W, TAB_H);
                continue;
            }
            if (tabIdx >= tabs.size()) continue;

            boolean isSel = (tabIdx == selectedTab);
            int drawX = sx + (isSel ? (side == 0 ? 1 : -1) : 0);
            int uv_u  = (side == 0) ? TAB_U_L : TAB_U_R;
            int uv_v  = (side == 0) ? TAB_V_L : TAB_V_R;

            gui.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    drawX, sy, uv_u, uv_v, TAB_W, TAB_H, TEX_W, TEX_H);

            // Render item icon scaled to ~12×12 centred in the tab
            float iconScale = 0.75f;
            int iconX = drawX + TAB_W / 2 + (side == 0 ? 1 : -1);
            int iconY = sy    + TAB_H / 2;
            gui.pose().pushMatrix();
            gui.pose().translate(iconX, iconY);
            gui.pose().scale(iconScale, iconScale);
            gui.pose().translate(-iconX, -iconY);
            gui.fakeItem(tabs.get(tabIdx).icon(), iconX - 8, iconY - 8);
            gui.pose().popMatrix();

            tabIdx++;
        }
    }

    // --- Left page -----------------------------------------------------------

    private void renderLeftPage(GuiGraphicsExtractor gui, Font font, int mx, int my) {
        if (isScribbleTab()) renderProgressSection(gui, font);

        int lx     = bookX + L_TEXT_X;
        int ly     = isScribbleTab() ? scribbleEntryListY : bookY + L_TEXT_Y;
        int entryH = font.lineHeight + 2;

        BookData data  = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);
        int start      = entryPage * entriesPerPage;

        for (int i = 0; i < entriesPerPage; i++) {
            int idx = start + i;
            if (idx >= entries.size()) break;

            var e      = entries.get(idx);
            KnowledgeLevel lvl = data.getLevel(e.getKey());
            boolean sel = (idx == selectedIndex);
            int ey = ly + i * entryH;

            if (sel) gui.fill(lx - 1, ey - 1, lx + L_TEXT_W + 1, ey + entryH - 1, COL_SEL_BG);

            String prefix = e.getValue().alwaysVisible()  ? "  "
                    : (lvl == KnowledgeLevel.DEEP)        ? "✦ " : "• ";
            String label  = prefix + resolve(net.minecraft.locale.Language.getInstance(), e.getValue().title());
            if (font.width(label) > L_TEXT_W)
                label = font.plainSubstrByWidth(label, L_TEXT_W - font.width("…")) + "…";

            gui.text(font, label, lx + 1, ey, sel ? 0xFF5C3A1A : COL_TEXT, false);
        }

        // Page indicator
        if (totalEntryPages() > 1) {
            String ind  = (entryPage + 1) + " / " + totalEntryPages();
            int    indX = bookX + L_TEXT_X + (L_TEXT_W - font.width(ind)) / 2;
            gui.text(font, ind, indX, prevBtnY - 2, COL_HINT, false);
        }

        if (entryPage > 0) {
            boolean hov = isHovered(mx, my, prevBtnX, prevBtnY, PAGE_BTN_W, PAGE_BTN_H);
            gui.blitSprite(RenderPipelines.GUI_TEXTURED,
                    hov ? SPR_PAGE_PREV_HOV : SPR_PAGE_PREV,
                    prevBtnX, prevBtnY, PAGE_BTN_W, PAGE_BTN_H);
        }
        if ((entryPage + 1) * entriesPerPage < entries.size()) {
            boolean hov = isHovered(mx, my, nextBtnX, nextBtnY, PAGE_BTN_W, PAGE_BTN_H);
            gui.blitSprite(RenderPipelines.GUI_TEXTURED,
                    hov ? SPR_PAGE_NEXT_HOV : SPR_PAGE_NEXT,
                    nextBtnX, nextBtnY, PAGE_BTN_W, PAGE_BTN_H);
        }
    }

    private boolean isScribbleTab() {
        return !tabs.isEmpty() && tabs.get(selectedTab).namespace().equals(ScribbleBook.MODID);
    }

    private void renderProgressSection(GuiGraphicsExtractor gui, Font font) {
        BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);

        int maxPoints = 0, points = 0;
        for (var e : java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(
                        BookEntryLoader.INSTANCE.getAllEntries().entrySet().stream(),
                        EntityEntryLoader.INSTANCE.getAllEntries().entrySet().stream()
                ),
                ItemEntryLoader.INSTANCE.getAllEntries().entrySet().stream()
        ).filter(e -> e.getValue().countable()).collect(java.util.stream.Collectors.toList())) {
            int entryMax = e.getValue().hasDeepSection() ? 2 : 1;
            maxPoints += entryMax;
            KnowledgeLevel lvl = data.getLevel(e.getKey());
            if (lvl == KnowledgeLevel.BASIC)     points += 1;
            else if (lvl == KnowledgeLevel.DEEP) points += entryMax;
        }

        int lx = bookX + L_TEXT_X;
        int ly = bookY + L_TEXT_Y;

        // Label
        String label = net.minecraft.locale.Language.getInstance().getOrDefault("scribble_book.progress.label");
        gui.text(font, label, lx, ly, COL_HINT, false);
        ly += font.lineHeight + 2;

        // Bar
        int barW = L_TEXT_W, barH = 5;
        int fillW = (maxPoints > 0 && points > 0)
                ? (points == maxPoints ? barW : Math.max(1, barW * points / maxPoints))
                : 0;
        gui.fill(lx, ly, lx + barW, ly + barH, 0xFF4A3520);
        if (fillW > 0) gui.fill(lx, ly, lx + fillW, ly + barH, COL_SEP);
        ly += barH + 2;

        // Percentage
        String pctStr = computeDisplayPct(points, maxPoints) + "%";
        gui.text(font, pctStr, lx + (barW - font.width(pctStr)) / 2, ly, COL_HINT, false);
        ly += font.lineHeight + 3;

        // Separator
        gui.fill(lx, ly, lx + L_TEXT_W, ly + 1, COL_SEP);
        // ly + 1 + 4 = scribbleEntryListY
    }

    private int computeDisplayPct(int points, int maxPoints) {
        if (maxPoints == 0 || points == 0) return 0;
        if (points == maxPoints)           return 100;
        int rounded = Math.round((float) points / maxPoints * 100f);
        if (rounded == 0)   return 1;
        if (rounded == 100) return 99;
        return rounded;
    }

    // --- Right page ----------------------------------------------------------

    private void renderRightPage(GuiGraphicsExtractor gui, Font font) {
        if (entries.isEmpty()) {
            gui.text(font, "Изучи блоки с помощью",
                    bookX + R_TEXT_X, bookY + R_TEXT_Y + 40, COL_HINT, false);
            gui.text(font, "Shift + ПКМ по блоку.",
                    bookX + R_TEXT_X, bookY + R_TEXT_Y + 54, COL_HINT, false);
            return;
        }

        BookEntry entry = entries.get(selectedIndex).getValue();
        int rx = bookX + R_TEXT_X;
        int ry = bookY + R_TEXT_Y;

        String title = resolve(net.minecraft.locale.Language.getInstance(), entry.title());
        if (font.width(title) > R_TEXT_W)
            title = font.plainSubstrByWidth(title, R_TEXT_W - font.width("…")) + "…";
        gui.text(font, title, rx, ry, COL_TITLE, false);
        ry += font.lineHeight + 3;

        gui.fill(rx, ry, rx + R_TEXT_W, ry + 1, COL_SEP);
        ry += 4;

        int contentStartY = ry;
        gui.enableScissor(rx, contentStartY, rx + R_TEXT_W, contentStartY + maxContentH);
        int curY = 0;
        for (RenderElement el : renderElements) {
            int elH = elHeight(el, font);
            if (curY + elH <= contentScrollY) { curY += elH; continue; }
            if (curY >= contentScrollY + maxContentH) break;

            int drawY = contentStartY + (curY - contentScrollY);
            switch (el) {
                case RenderElement.TextLine tl ->
                    gui.text(font, tl.seq(), rx, drawY, COL_TEXT, false);
                case RenderElement.ItemLine il -> {
                    if (!il.stack().isEmpty()) {
                        gui.fakeItem(il.stack(), rx, drawY + 1);
                        gui.text(font, il.stack().getHoverName().getVisualOrderText(),
                                rx + 18, drawY + (18 - font.lineHeight) / 2, COL_TEXT, false);
                    }
                }
                case RenderElement.ImageLine img ->
                    gui.blit(RenderPipelines.GUI_TEXTURED, img.texture(),
                            rx + img.xOffset(), drawY + 2, 0, 0, img.drawW(), img.drawH(), img.drawW(), img.drawH());
                case RenderElement.ItemsRowLine row -> {
                    int x = rx + row.xOffset();
                    for (ItemStack stack : row.stacks()) {
                        if (row.bg())
                            gui.blit(RenderPipelines.GUI_TEXTURED, CELL_TEXTURE, x, drawY, 0, 0, 26, 26, 26, 26);
                        if (!stack.isEmpty()) gui.fakeItem(stack, x + 5, drawY + 5);
                        x += 26 + row.gap();
                    }
                }
                case RenderElement.RecipeLine rl -> {
                    String arrow = "→";
                    int arrowW = font.width(arrow);
                    int totalW = 78 + 4 + arrowW + 4 + 26;
                    int gx = rx + Math.max(0, (R_TEXT_W - totalW) / 2);
                    int gy = drawY + 2;
                    for (int row = 0; row < 3; row++) {
                        for (int col = 0; col < 3; col++) {
                            int sx = gx + col * 26, sy = gy + row * 26;
                            gui.blit(RenderPipelines.GUI_TEXTURED, CELL_TEXTURE, sx, sy, 0, 0, 26, 26, 26, 26);
                            ItemStack item = rl.grid()[row * 3 + col];
                            if (!item.isEmpty()) gui.fakeItem(item, sx + 5, sy + 5);
                        }
                    }
                    int arrowX = gx + 78 + 4;
                    int arrowY = gy + 39 - font.lineHeight / 2;
                    gui.text(font, arrow, arrowX, arrowY, COL_TEXT, false);
                    int ox = arrowX + arrowW + 4;
                    int oy = gy + (78 - 26) / 2;
                    gui.blit(RenderPipelines.GUI_TEXTURED, CELL_TEXTURE, ox, oy, 0, 0, 26, 26, 26, 26);
                    if (!rl.output().isEmpty()) gui.fakeItem(rl.output(), ox + 5, oy + 5);
                }
            }
            curY += elH;
        }
        gui.disableScissor();

        if (contentScrollY > 0)
            gui.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_SCROLL_UP,   upBtnX,   upBtnY,   SCROLL_BTN_W, SCROLL_BTN_H);
        if (contentScrollY + maxContentH < totalContentH)
            gui.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_SCROLL_DOWN, downBtnX, downBtnY, SCROLL_BTN_W, SCROLL_BTN_H);
    }

    // --- Input ---------------------------------------------------------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        int mx = (int) event.x();
        int my = (int) event.y();

        // Close on right-click
        if (event.button() == 1) {
            onClose();
            return true;
        }

        // --- Tab clicks ---
        if (!tabs.isEmpty()) {
            int totalSlots = tabsPerSide * 2;
            boolean hasUp   = tabHasUp();
            boolean hasDown = tabHasDown();
            int tabIdx = tabScroll;

            for (int slot = 0; slot < totalSlots; slot++) {
                int[] r = tabSlotRect(slot);

                if (slot == 0 && hasUp) {
                    if (isHovered(mx, my, r[0], r[1], r[2], r[3])) { tabScroll--; return true; }
                    continue;
                }
                if (slot == totalSlots - 1 && hasDown) {
                    if (isHovered(mx, my, r[0], r[1], r[2], r[3])) { tabScroll++; return true; }
                    continue;
                }
                if (tabIdx >= tabs.size()) continue;

                if (isHovered(mx, my, r[0], r[1], r[2], r[3])) {
                    selectTab(tabIdx);
                    return true;
                }
                tabIdx++;
            }
        }

        // --- Page nav ---
        if (entryPage > 0 && isHovered(mx, my, prevBtnX, prevBtnY, PAGE_BTN_W, PAGE_BTN_H)) {
            entryPage--;
            selectEntry(entryPage * entriesPerPage);
            return true;
        }
        if ((entryPage + 1) * entriesPerPage < entries.size()
                && isHovered(mx, my, nextBtnX, nextBtnY, PAGE_BTN_W, PAGE_BTN_H)) {
            entryPage++;
            selectEntry(entryPage * entriesPerPage);
            return true;
        }

        // --- Content scroll ---
        Font font  = Minecraft.getInstance().font;
        int step   = font.lineHeight;
        if (contentScrollY > 0 && isHovered(mx, my, upBtnX, upBtnY, SCROLL_BTN_W, SCROLL_BTN_H)) {
            contentScrollY = Math.max(0, contentScrollY - step);
            return true;
        }
        if (contentScrollY + maxContentH < totalContentH
                && isHovered(mx, my, downBtnX, downBtnY, SCROLL_BTN_W, SCROLL_BTN_H)) {
            contentScrollY = Math.min(totalContentH - maxContentH, contentScrollY + step);
            return true;
        }

        // --- Entry click ---
        int entryH = font.lineHeight + 2;
        int lx     = bookX + L_TEXT_X;
        int ly     = isScribbleTab() ? scribbleEntryListY : bookY + L_TEXT_Y;
        int start  = entryPage * entriesPerPage;

        for (int i = 0; i < entriesPerPage; i++) {
            int idx = start + i;
            if (idx >= entries.size()) break;
            int ey = ly + i * entryH;
            if (isHovered(mx, my, lx, ey, L_TEXT_W, entryH)) {
                selectEntry(idx);
                return true;
            }
        }

        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int step = Minecraft.getInstance().font.lineHeight * 3;
        if (dy > 0 && contentScrollY > 0) {
            contentScrollY = Math.max(0, contentScrollY - step);
            return true;
        }
        if (dy < 0 && contentScrollY + maxContentH < totalContentH) {
            contentScrollY = Math.min(totalContentH - maxContentH, contentScrollY + step);
            return true;
        }
        return false;
    }

    // --- Helpers -------------------------------------------------------------

    private void selectTab(int idx) {
        if (idx < 0 || idx >= tabs.size()) return;
        selectedTab    = idx;
        entries        = tabs.get(idx).entries();
        selectedIndex  = 0;
        entryPage      = 0;
        contentScrollY = 0;
        rebuildRenderElements(Minecraft.getInstance().font);
    }

    private void selectEntry(int idx) {
        if (idx < 0 || idx >= entries.size()) return;
        selectedIndex  = idx;
        contentScrollY = 0;
        rebuildRenderElements(Minecraft.getInstance().font);
    }

    private void rebuildRenderElements(Font font) {
        if (entries.isEmpty()) { renderElements = List.of(); totalContentH = 0; return; }
        var selected = entries.get(selectedIndex);
        BookEntry entry = selected.getValue();
        net.minecraft.locale.Language lang = net.minecraft.locale.Language.getInstance();

        List<ContentBlock> blocks;
        if (entry.alwaysVisible()) {
            blocks = entry.getBlocks(KnowledgeLevel.BASIC);
        } else {
            BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);
            KnowledgeLevel lvl = data.getLevel(selected.getKey());
            if (lvl == KnowledgeLevel.DEEP && entry.hasDeepSection()) {
                List<ContentBlock> combined = new ArrayList<>();
                combined.addAll(entry.getBlocks(KnowledgeLevel.BASIC));
                combined.addAll(entry.getBlocks(KnowledgeLevel.DEEP));
                blocks = combined;
            } else {
                blocks = entry.getBlocks(KnowledgeLevel.BASIC);
            }
        }

        List<RenderElement> elements = new ArrayList<>();
        boolean firstBlock = true;
        for (ContentBlock block : blocks) {
            if (!firstBlock) elements.add(new RenderElement.TextLine(FormattedCharSequence.EMPTY));
            firstBlock = false;

            if (block instanceof TextBlock t) {
                String text = resolve(lang, t.text());
                for (String para : text.split("\n", -1)) {
                    if (para.isBlank()) elements.add(new RenderElement.TextLine(FormattedCharSequence.EMPTY));
                    else font.split(FormattedText.of(para), R_TEXT_W)
                             .forEach(seq -> elements.add(new RenderElement.TextLine(seq)));
                }
            } else if (block instanceof ItemBlock ib) {
                ItemStack stack = BuiltInRegistries.ITEM.getOptional(ib.item())
                        .filter(item -> item != Items.AIR)
                        .map(ItemStack::new)
                        .orElse(ItemStack.EMPTY);
                elements.add(new RenderElement.ItemLine(stack));
            } else if (block instanceof ImageBlock img) {
                int dw, dh;
                if (img.width() <= 0 && img.height() <= 0) {
                    dw = R_TEXT_W; dh = R_TEXT_W;
                } else if (img.width() > 0 && img.height() > 0) {
                    if (img.width() > R_TEXT_W) {
                        dw = R_TEXT_W;
                        dh = img.height() * R_TEXT_W / img.width();
                    } else {
                        dw = img.width(); dh = img.height();
                    }
                } else if (img.width() > 0) {
                    dw = Math.min(img.width(), R_TEXT_W); dh = dw;
                } else {
                    dw = R_TEXT_W; dh = img.height();
                }
                int xOffset = switch (img.align()) {
                    case "center" -> (R_TEXT_W - dw) / 2;
                    case "right"  -> R_TEXT_W - dw;
                    default       -> 0;
                };
                elements.add(new RenderElement.ImageLine(img.texture(), dw, dh, xOffset));
            } else if (block instanceof ItemsBlock ib) {
                int gap = ib.gap();
                int cellStep = 26 + gap;
                int perRow = Math.max(1, (R_TEXT_W + gap) / cellStep);
                List<ItemStack> stacks = ib.items().stream()
                        .map(id -> BuiltInRegistries.ITEM.getOptional(id)
                                .filter(item -> item != Items.AIR)
                                .map(ItemStack::new)
                                .orElse(ItemStack.EMPTY))
                        .toList();
                for (int i = 0; i < stacks.size(); i += perRow) {
                    List<ItemStack> row = stacks.subList(i, Math.min(i + perRow, stacks.size()));
                    int rowW = row.size() * 26 + (row.size() - 1) * gap;
                    int xOffset = switch (ib.align()) {
                        case "right"  -> R_TEXT_W - rowW;
                        case "center" -> (R_TEXT_W - rowW) / 2;
                        default       -> 0;
                    };
                    elements.add(new RenderElement.ItemsRowLine(row, ib.background(), Math.max(0, xOffset), gap));
                }
            } else if (block instanceof RecipeBlock rb) {
                ItemStack[] grid = new ItemStack[9];
                List<String> slots = rb.grid();
                for (int i = 0; i < 9; i++) {
                    String id = i < slots.size() ? slots.get(i) : "";
                    grid[i] = (id == null || id.isEmpty()) ? ItemStack.EMPTY
                            : BuiltInRegistries.ITEM.getOptional(Identifier.parse(id))
                                    .filter(item -> item != Items.AIR)
                                    .map(ItemStack::new)
                                    .orElse(ItemStack.EMPTY);
                }
                ItemStack output = BuiltInRegistries.ITEM.getOptional(rb.output())
                        .filter(item -> item != Items.AIR)
                        .map(ItemStack::new)
                        .orElse(ItemStack.EMPTY);
                elements.add(new RenderElement.RecipeLine(grid, output));
            }
        }

        renderElements = elements;
        totalContentH  = 0;
        for (RenderElement el : elements) totalContentH += elHeight(el, font);
    }

    private int elHeight(RenderElement el, Font font) {
        return switch (el) {
            case RenderElement.TextLine t    -> font.lineHeight;
            case RenderElement.ItemLine i    -> 18;
            case RenderElement.ImageLine il  -> il.drawH() + 4;
            case RenderElement.ItemsRowLine r -> 26;
            case RenderElement.RecipeLine r  -> 82;
        };
    }

    private int totalEntryPages() {
        return Math.max(1, (entries.size() + entriesPerPage - 1) / entriesPerPage);
    }

    /** Resolves a string as a lang key if it exists, otherwise returns it as-is. */
    private static String resolve(net.minecraft.locale.Language lang, String key) {
        return lang.has(key) ? lang.getOrDefault(key) : key;
    }

    private boolean isHovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
