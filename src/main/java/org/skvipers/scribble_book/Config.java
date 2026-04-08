package org.skvipers.scribble_book;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = ScribbleBook.MODID)
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue BASIC_INK_COST = BUILDER
            .comment("Ink units cost for a basic study (Shift+RMB). Ink bottle holds 100 units.")
            .defineInRange("basicInkCost", 10, 0, 100);

    private static final ModConfigSpec.IntValue BASIC_PAPER_COST = BUILDER
            .comment("Paper cost for a basic study (Shift+RMB)")
            .defineInRange("basicPaperCost", 1, 0, 64);

    private static final ModConfigSpec.IntValue DEEP_INK_COST = BUILDER
            .comment("Ink units cost for a deep study (Shift+RMB on already studied block). Ink bottle holds 100 units.")
            .defineInRange("deepInkCost", 30, 0, 100);

    private static final ModConfigSpec.IntValue DEEP_PAPER_COST = BUILDER
            .comment("Paper cost for a deep study (Shift+RMB on already studied block)")
            .defineInRange("deepPaperCost", 1, 0, 64);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int basicInkCost = 1;
    public static int basicPaperCost = 1;
    public static int deepInkCost = 3;
    public static int deepPaperCost = 1;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        basicInkCost = BASIC_INK_COST.get();
        basicPaperCost = BASIC_PAPER_COST.get();
        deepInkCost = DEEP_INK_COST.get();
        deepPaperCost = DEEP_PAPER_COST.get();
    }
}
