package org.skvipers.scribble_book.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.skvipers.scribble_book.book.BookData;
import org.skvipers.scribble_book.book.BookEntry;
import org.skvipers.scribble_book.book.BookEntryLoader;
import org.skvipers.scribble_book.book.EntityEntryLoader;
import org.skvipers.scribble_book.book.ItemEntryLoader;
import org.skvipers.scribble_book.book.KnowledgeLevel;
import org.skvipers.scribble_book.item.ScribbleBookItem;
import org.skvipers.scribble_book.registry.ModDataComponents;

import java.util.Comparator;
import java.util.stream.Stream;

public class ScribbleBookCommand {

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("scribblebook")
                .requires(src -> {
                    Entity entity = src.getEntity();
                    if (!(entity instanceof ServerPlayer sp)) return true; // console always allowed
                    return src.getServer().getPlayerList().isOp(
                            new NameAndId(sp.getUUID(), sp.getGameProfile().name()));
                })
                .then(Commands.literal("missing")
                        .executes(ctx -> executeMissing(ctx.getSource()))));
    }

    private static int executeMissing(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        ItemStack bookStack = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof ScribbleBookItem) {
                bookStack = stack;
                break;
            }
        }

        if (bookStack.isEmpty()) {
            source.sendFailure(Component.translatable("scribble_book.command.no_book"));
            return 0;
        }

        BookData data = bookStack.getOrDefault(ModDataComponents.BOOK_DATA.get(), BookData.EMPTY);

        Stream.concat(
                Stream.concat(
                        BookEntryLoader.INSTANCE.getAllEntries().entrySet().stream(),
                        EntityEntryLoader.INSTANCE.getAllEntries().entrySet().stream()
                ),
                ItemEntryLoader.INSTANCE.getAllEntries().entrySet().stream()
        )
                .filter(e -> !isFullyStudied(e.getValue(), data.getLevel(e.getKey())))
                .min(Comparator.comparing(e -> Component.translatable(e.getValue().title()).getString()))
                .ifPresentOrElse(
                        e -> source.sendSuccess(() -> Component.translatable(
                                "scribble_book.command.missing",
                                Component.translatable(e.getValue().title())), false),
                        () -> source.sendSuccess(() -> Component.translatable(
                                "scribble_book.command.complete"), false)
                );

        return 1;
    }

    private static boolean isFullyStudied(BookEntry entry, KnowledgeLevel level) {
        if (entry.hasDeepText()) return level == KnowledgeLevel.DEEP;
        return level != null;
    }
}
