package io.github.mosadie.MixBukkit.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ControlMouseDownInput extends Event{
	private static final HandlerList handlers = new HandlerList();

	public HandlerList getHandlers() {
	    return handlers;
	}

	public static HandlerList getHandlerList() {
	    return handlers;
	}
	
	private final MixBukkit mb;
	
}
