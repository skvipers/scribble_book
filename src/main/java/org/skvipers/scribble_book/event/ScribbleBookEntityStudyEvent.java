package org.skvipers.scribble_book.event;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

@Cancelable
public class ScribbleBookEntityStudyEvent extends Event {

    private final Player player;
    private final Entity entity;
    private ResourceLocation entryKey;
    private int inkCost;
    private int paperCost;

    public ScribbleBookEntityStudyEvent(Player player, Entity entity,
                                        ResourceLocation entryKey, int inkCost, int paperCost) {
        this.player    = player;
        this.entity    = entity;
        this.entryKey  = entryKey;
        this.inkCost   = inkCost;
        this.paperCost = paperCost;
    }

    public Player   getPlayer() { return player; }
    public Entity   getEntity() { return entity; }

    public ResourceLocation getEntryKey()                    { return entryKey; }
    public void             setEntryKey(ResourceLocation key){ this.entryKey = key; }

    public int  getInkCost()             { return inkCost; }
    public void setInkCost(int inkCost)  { this.inkCost = Math.max(0, inkCost); }

    public int  getPaperCost()               { return paperCost; }
    public void setPaperCost(int paperCost)  { this.paperCost = Math.max(0, paperCost); }
}
