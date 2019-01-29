package io.github.mosadie.mixbukkit.events;

import com.mixer.interactive.event.control.input.ControlInputEvent;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.mosadie.mixbukkit.MixBukkit;

/**
 * The base class for all Input Events.
 * An Input Event is a single input from a user using MixPlay.
 * Examples include a user pressing/releasing a button or moving a joystick.
 * 
 * @author MoSadie
 */
public class ControlInputEventBase extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private boolean isCancelled = false;

	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
	    return handlers;
	}
	
	/** A reference to the MixBukkit plugin. */
	private final MixBukkit mb;

	/** The actual ControlInputEvent provided by the Mixer Interactive Java API. */
	private final ControlInputEvent event;

	/** The ID of the control used in the event. */
	private final String controlID;
	
	/**
	 * Create a new ControlInputEventBase event to send to all registered listeners.
	 * @param mb A refrence to the MixBukkit object, so a listener can respond to the event.
	 * @param event The actual full ControlInputEvent from the Mixer Interactive Java API.
	 */
	public ControlInputEventBase(MixBukkit mb, ControlInputEvent event) {
		this.mb = mb;
		this.event = event;
		this.controlID = event.getControlInput().getControlID();
	}

	/**
	 * Returns if this event has been cancelled.
	 * @return True if this event has been cancelled, false otherwise.
	 */
	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	/**
	 * Set the cancellation state of this event.
	 * @param canceled The new cancellation state of this event.
	 */
	@Override
	public void setCancelled(boolean canceled) {
		isCancelled = canceled;
	}
	
	/**
	 * Gets the control ID of the control that was used in this event.
	 * Control IDs are usually set by the developer in the Interactive Lab.
	 * @return The control ID of the control used.
	 */
	public String getControlID() {
		return controlID;
	}
	
	/**
	 * Get the ControlInputEvent object from the Mixer Interactive Java API.
	 * @return The ControlInputEvent for the event.
	 * @see ControlInputEvent
	 */
	public ControlInputEvent getEvent() {
		return event;
	}
}
