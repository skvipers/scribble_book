package org.skvipers.scribble_book.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class ScribbleBookStudyEvent extends Event {

    private final Player player;
    private final BlockPos pos;
    private final BlockState state;
    private ResourceLocation entryKey;
    private int inkCost;
    private int paperCost;

    public ScribbleBookStudyEvent(Player player, BlockPos pos, BlockState state,
                                  ResourceLocation entryKey, int inkCost, int paperCost) {
        this.player    = player;
        this.pos       = pos;
        this.state     = state;
        this.entryKey  = entryKey;
        this.inkCost   = inkCost;
        this.paperCost = paperCost;
    }

    public Player getPlayer()    { return player; }
    public BlockPos getPos()     { return pos; }
    public BlockState getState() { return state; }

    public ResourceLocation getEntryKey()                    { return entryKey; }
    public void             setEntryKey(ResourceLocation key){ this.entryKey = key; }

    public int  getInkCost()            { return inkCost; }
    public void setInkCost(int inkCost) { this.inkCost = Math.max(0, inkCost); }

    public int  getPaperCost()              { return paperCost; }
    public void setPaperCost(int paperCost) { this.paperCost = Math.max(0, paperCost); }
}
