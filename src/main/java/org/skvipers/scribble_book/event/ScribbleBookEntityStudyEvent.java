package org.skvipers.scribble_book.event;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Fired on NeoForge.EVENT_BUS before a player studies an entity with the Scribble Book.
 *
 * <p>Listeners may:
 * <ul>
 *   <li>Cancel the event to prevent studying entirely.</li>
 *   <li>Override {@code entryKey} to store knowledge under a custom identifier.</li>
 *   <li>Override {@code inkCost} / {@code paperCost} to change resource consumption.</li>
 * </ul>
 */
public class ScribbleBookEntityStudyEvent extends Event implements ICancellableEvent {

    private final Player player;
    private final Entity entity;

    /** The key used to look up the entry JSON and store progress. Mutable. */
    private Identifier entryKey;

    private int inkCost;
    private int paperCost;

    public ScribbleBookEntityStudyEvent(Player player, Entity entity,
                                        Identifier entryKey, int inkCost, int paperCost) {
        this.player    = player;
        this.entity    = entity;
        this.entryKey  = entryKey;
        this.inkCost   = inkCost;
        this.paperCost = paperCost;
    }

    public Player   getPlayer()   { return player; }
    public Entity   getEntity()   { return entity; }

    public Identifier getEntryKey()               { return entryKey; }
    public void       setEntryKey(Identifier key) { this.entryKey = key; }

    public int  getInkCost()              { return inkCost; }
    public void setInkCost(int inkCost)   { this.inkCost = Math.max(0, inkCost); }

    public int  getPaperCost()                { return paperCost; }
    public void setPaperCost(int paperCost)   { this.paperCost = Math.max(0, paperCost); }
}
