package org.skvipers.scribble_book;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@EventBusSubscriber(modid = ScribbleBook.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue BASIC_INK_COST = BUILDER
            .comment("Ink units cost for a basic study (Shift+RMB). Ink bottle holds 100 units.")
            .defineInRange("basicInkCost", 10, 0, 100);

    private static final ForgeConfigSpec.IntValue BASIC_PAPER_COST = BUILDER
            .comment("Paper cost for a basic study (Shift+RMB)")
            .defineInRange("basicPaperCost", 1, 0, 64);

    private static final ForgeConfigSpec.IntValue DEEP_INK_COST = BUILDER
            .comment("Ink units cost for a deep study (Shift+RMB on already studied block). Ink bottle holds 100 units.")
            .defineInRange("deepInkCost", 30, 0, 100);

    private static final ForgeConfigSpec.IntValue DEEP_PAPER_COST = BUILDER
            .comment("Paper cost for a deep study (Shift+RMB on already studied block)")
            .defineInRange("deepPaperCost", 1, 0, 64);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int basicInkCost = 10;
    public static int basicPaperCost = 1;
    public static int deepInkCost = 30;
    public static int deepPaperCost = 1;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        basicInkCost = BASIC_INK_COST.get();
        basicPaperCost = BASIC_PAPER_COST.get();
        deepInkCost = DEEP_INK_COST.get();
        deepPaperCost = DEEP_PAPER_COST.get();
    }
}
