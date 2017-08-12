package io.github.mosadie.MixBukkit.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.mixer.interactive.event.InteractiveEvent;
import com.mixer.interactive.event.control.input.ControlInputEvent;
import com.mixer.interactive.event.control.input.ControlMouseDownInputEvent;

import io.github.mosadie.MixBukkit.MixBukkit;

public class ControlInputEventBase extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled = false;

	public HandlerList getHandlers() {
	    return handlers;
	}

	public static HandlerList getHandlerList() {
	    return handlers;
	}
	
	private final MixBukkit mb;
	private final ControlInputEvent event;
	
	public ControlInputEventBase(MixBukkit mb, ControlInputEvent event) {
		this.mb = mb;
		this.event = event;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean canceled) {
		isCancelled = canceled;
	}
	
}
