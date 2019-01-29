package io.github.mosadie.mixbukkit.events;

import com.mixer.interactive.event.control.input.ControlMouseUpInputEvent;

import io.github.mosadie.mixbukkit.MixBukkit;

/**
 * The event for when a user finishes left-clicking on a button in MixPlay.
 * @author MoSadie
 */
public class ControlMouseUpInput extends ControlInputEventBase{
	
	/** The actual ControlMouseUpInputEvent provided by the Mixer Interactive Java API. */
	private final ControlMouseUpInputEvent event;
	
	/**
	 * Create a new ControlMouseUpInput event to send to all registered listeners.
	 * @param mb A reference to the MixBukkit object, so a listener can respond to the event.
	 * @param event The actual full ControlMouseUpInput from the Mixer Interactive Java API.
	 */
	public ControlMouseUpInput(MixBukkit mb, ControlMouseUpInputEvent event) {
		super(mb, event);
		this.event = event;
	}
	
	/**
	 * Get the ControlMouseUpInputEvent object from the Mixer Interactive Java API.
	 * @return The ControlMouseUpInputEvent for the event.
	 * @see ControlMouseUpInputEvent
	 */
	@Override
	public ControlMouseUpInputEvent getEvent() {
		return event;
	}
}
