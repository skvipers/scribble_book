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
import org.skvipers.scribble_book.book.BookData;
import org.skvipers.scribble_book.book.BookEntry;
import org.skvipers.scribble_book.book.BookEntryLoader;
import org.skvipers.scribble_book.book.EntityEntryLoader;
import org.skvipers.scribble_book.book.ItemEntryLoader;
import org.skvipers.scribble_book.book.KnowledgeLevel;
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

    // Special synthetic identifier for the built-in guide entry (not a real block)
    private static final Identifier GUIDE_ID = Identifier.fromNamespaceAndPath(ScribbleBook.MODID, "guide");
    private static final BookEntry  GUIDE_ENTRY = new BookEntry(
            "scribble_book.guide.title",
            "scribble_book.guide.text",
            "",
            true
    );

    // --- Tab group ---
    private record TabGroup(String namespace, ItemStack icon, List<Map.Entry<Identifier, BookEntry>> entries) {}

    // --- State ---
    private final ItemStack bookStack;
    private final List<TabGroup> tabs = new ArrayList<>();

    private int selectedTab     = 0;
    private int tabScroll       = 0;   // how many tab slots scrolled down
    private int selectedIndex   = 0;
    private int entryPage       = 0;
    private int contentLine     = 0;

    // Current tab's entry list (reference into tabs.get(selectedTab))
    private List<Map.Entry<Identifier, BookEntry>> entries = List.of();

    // Computed in init()
    private int bookX, bookY;
    private int entriesPerPage;
    private int maxContentLines;
    private int tabsPerSide;  // max tab rows on one side

    // Absolute button rects
    private int prevBtnX, prevBtnY, nextBtnX, nextBtnY;
    private int upBtnX, upBtnY, downBtnX, downBtnY;

    private List<FormattedCharSequence> contentLines = List.of();
    private int scribbleEntryListY;

    // -------------------------------------------------------------------------

    public ScribbleBookScreen(ItemStack bookStack) {
        super(Component.translatable("scribble_book.screen.title"));
        this.bookStack = bookStack;
        buildTabs();
    }

    private void buildTabs() {
        BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);

        Map<String, List<Map.Entry<Identifier, BookEntry>>> byNs = new LinkedHashMap<>();
        // Combine block and entity entries, filter to what the player has studied
        java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(
                        BookEntryLoader.INSTANCE.getAllEntries().entrySet().stream(),
                        EntityEntryLoader.INSTANCE.getAllEntries().entrySet().stream()
                ),
                ItemEntryLoader.INSTANCE.getAllEntries().entrySet().stream()
        )
                .filter(e -> data.hasEntry(e.getKey()))
                .sorted(Map.Entry.comparingByValue((a, b) -> a.title().compareToIgnoreCase(b.title())))
                .forEach(e -> byNs.computeIfAbsent(e.getKey().getNamespace(), k -> new ArrayList<>()).add(e));

        // Guide entry is always first in the scribble_book tab
        byNs.computeIfAbsent(ScribbleBook.MODID, k -> new ArrayList<>())
                .add(0, Map.entry(GUIDE_ID, GUIDE_ENTRY));

        // scribble_book first, minecraft second, then other mods alphabetically
        List<String> order = new ArrayList<>();
        if (byNs.containsKey(ScribbleBook.MODID)) order.add(ScribbleBook.MODID);
        if (byNs.containsKey("minecraft")) order.add("minecraft");
        byNs.keySet().stream()
                .filter(k -> !k.equals(ScribbleBook.MODID) && !k.equals("minecraft"))
                .sorted().forEach(order::add);

        for (String ns : order) {
            ItemStack icon = ns.equals(ScribbleBook.MODID) ? bookStack.copyWithCount(1) : resolveTabIcon(ns);
            tabs.add(new TabGroup(ns, icon, byNs.get(ns)));
        }

        entries = tabs.isEmpty() ? List.of() : tabs.get(0).entries();
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
        entriesPerPage  = L_TEXT_H / (font.lineHeight + 2);
        // title (lineHeight+3) + separator (1+4) = lineHeight+8 consumed before content
        maxContentLines = (R_TEXT_H - font.lineHeight - 8) / font.lineHeight;
        // progress section: label(lH+2) + bar(5+2) + pct(lH+3) + separator(1+4)
        scribbleEntryListY = bookY + L_TEXT_Y + font.lineHeight + 2 + 5 + 2 + font.lineHeight + 3 + 1 + 4;
        tabsPerSide     = Math.min(8, (BOOK_H - TAB_TOP) / (TAB_H + TAB_GAP));

        int navY = bookY + L_TEXT_Y + L_TEXT_H + 4;
        prevBtnX = bookX + L_TEXT_X;
        prevBtnY = navY;
        nextBtnX = bookX + L_TEXT_X + L_TEXT_W - PAGE_BTN_W;
        nextBtnY = navY;

        downBtnX = bookX + R_TEXT_X + R_TEXT_W - SCROLL_BTN_W + 6;
        downBtnY = bookY + R_TEXT_Y + R_TEXT_H + 2 - 6;
        upBtnX   = downBtnX;
        upBtnY   = downBtnY - SCROLL_BTN_H - 2 + 11;

        rebuildContentLines(font);
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

            String prefix = e.getKey().equals(GUIDE_ID) ? "  "
                    : (lvl == KnowledgeLevel.DEEP)       ? "✦ " : "• ";
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
        var allBlock  = BookEntryLoader.INSTANCE.getAllEntries();
        var allEntity = EntityEntryLoader.INSTANCE.getAllEntries();
        BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);

        int maxPoints = 0, points = 0;
        var allItem = ItemEntryLoader.INSTANCE.getAllEntries();
        for (var e : java.util.stream.Stream.concat(
                java.util.stream.Stream.concat(allBlock.entrySet().stream(), allEntity.entrySet().stream()),
                allItem.entrySet().stream()
        ).collect(java.util.stream.Collectors.toList())) {
            int entryMax = e.getValue().hasDeepText() ? 2 : 1;
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

        int end = Math.min(contentLine + maxContentLines, contentLines.size());
        for (int i = contentLine; i < end; i++) {
            gui.text(font, contentLines.get(i), rx, ry, COL_TEXT, false);
            ry += font.lineHeight;
        }

        if (contentLine > 0)
            gui.blitSprite(RenderPipelines.GUI_TEXTURED, SPR_SCROLL_UP,   upBtnX,   upBtnY,   SCROLL_BTN_W, SCROLL_BTN_H);
        if (contentLine + maxContentLines < contentLines.size())
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
        if (contentLine > 0 && isHovered(mx, my, upBtnX, upBtnY, SCROLL_BTN_W, SCROLL_BTN_H)) {
            contentLine--;
            return true;
        }
        if (contentLine + maxContentLines < contentLines.size()
                && isHovered(mx, my, downBtnX, downBtnY, SCROLL_BTN_W, SCROLL_BTN_H)) {
            contentLine++;
            return true;
        }

        // --- Entry click ---
        Font font  = Minecraft.getInstance().font;
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
        if (dy > 0 && contentLine > 0) { contentLine--; return true; }
        if (dy < 0 && contentLine + maxContentLines < contentLines.size()) { contentLine++; return true; }
        return false;
    }

    // --- Helpers -------------------------------------------------------------

    private void selectTab(int idx) {
        if (idx < 0 || idx >= tabs.size()) return;
        selectedTab   = idx;
        entries       = tabs.get(idx).entries();
        selectedIndex = 0;
        entryPage     = 0;
        contentLine   = 0;
        rebuildContentLines(Minecraft.getInstance().font);
    }

    private void selectEntry(int idx) {
        if (idx < 0 || idx >= entries.size()) return;
        selectedIndex = idx;
        contentLine   = 0;
        rebuildContentLines(Minecraft.getInstance().font);
    }

    private void rebuildContentLines(Font font) {
        if (entries.isEmpty()) { contentLines = List.of(); return; }
        var selected = entries.get(selectedIndex);
        BookEntry entry = selected.getValue();

        net.minecraft.locale.Language lang = net.minecraft.locale.Language.getInstance();

        // Guide entry is always shown in full; block entries depend on knowledge level
        String text;
        if (selected.getKey().equals(GUIDE_ID)) {
            text = resolve(lang, entry.basicText());
        } else {
            BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);
            KnowledgeLevel lvl = data.getLevel(selected.getKey());
            text = (lvl == KnowledgeLevel.DEEP && entry.hasDeepText())
                    ? resolve(lang, entry.basicText()) + "\n\n" + resolve(lang, entry.deepText())
                    : resolve(lang, entry.basicText());
        }

        List<FormattedCharSequence> lines = new ArrayList<>();
        for (String paragraph : text.split("\n")) {
            if (paragraph.isBlank()) lines.add(FormattedCharSequence.EMPTY);
            else                     lines.addAll(font.split(FormattedText.of(paragraph), R_TEXT_W));
        }
        contentLines = lines;
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
