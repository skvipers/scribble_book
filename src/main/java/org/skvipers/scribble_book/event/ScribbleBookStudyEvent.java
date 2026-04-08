package org.skvipers.scribble_book.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired on NeoForge.EVENT_BUS before a player studies a block with the Scribble Book.
 *
 * <p>Listeners may:
 * <ul>
 *   <li>Cancel the event to prevent studying entirely.</li>
 *   <li>Override {@code entryKey} to store knowledge under a custom identifier
 *       instead of the default block registry ID. The book will look up the
 *       entry JSON and persist progress using this key.</li>
 *   <li>Override {@code inkCost} / {@code paperCost} to change resource consumption.</li>
 * </ul>
 *
 * <p>The default {@code entryKey} is the block's registry {@link Identifier}
 * (e.g. {@code minecraft:furnace}). Set a different key to support dynamic
 * sub-entries such as {@code yourmod:block_spawner/ores}.
 */
public class ScribbleBookStudyEvent extends Event implements ICancellableEvent {

    private final Player player;
    private final BlockPos pos;
    private final BlockState state;

    /** The key used to look up the entry JSON and store progress. Mutable. */
    private Identifier entryKey;

    private int inkCost;
    private int paperCost;

    public ScribbleBookStudyEvent(Player player, BlockPos pos, BlockState state,
                                  Identifier entryKey, int inkCost, int paperCost) {
        this.player   = player;
        this.pos      = pos;
        this.state    = state;
        this.entryKey = entryKey;
        this.inkCost  = inkCost;
        this.paperCost = paperCost;
    }

    public Player getPlayer()     { return player; }
    public BlockPos getPos()      { return pos; }
    public BlockState getState()  { return state; }

    /** The identifier used to look up and persist this study. Override to use a custom key. */
    public Identifier getEntryKey()               { return entryKey; }
    public void       setEntryKey(Identifier key) { this.entryKey = key; }

    public int  getInkCost()             { return inkCost; }
    public void setInkCost(int inkCost)  { this.inkCost = Math.max(0, inkCost); }

    public int  getPaperCost()                { return paperCost; }
    public void setPaperCost(int paperCost)   { this.paperCost = Math.max(0, paperCost); }
}
