package io.github.mosadie.mixbukkit.events;

import com.mixer.interactive.event.control.input.ControlMouseDownInputEvent;
import io.github.mosadie.mixbukkit.MixBukkit;

/**
 * The event for when a user starts left-clicking on a button in MixPlay.
 * @author MoSadie
 */
public class ControlMouseDownInput extends ControlInputEventBase {
	
	/** The actual ControlMouseDownInputEvent provided by the Mixer Interactive Java API. */
	private final ControlMouseDownInputEvent event;
	
	/**
	 * Create a new ControlMouseDownInput event to send to all registered listeners.
	 * @param mb A reference to the MixBukkit object, so a listener can respond to the event.
	 * @param event The actual full ControlMouseDownInputEvent from the Mixer Interactive Java API.
	 */
	public ControlMouseDownInput(MixBukkit mb, ControlMouseDownInputEvent event) {
		super(mb, event);
		this.event = event;
	}
	
	/**
	 * Get the ControlMouseDownInputEvent object from the Mixer Interactive Java API.
	 * @return The ControlMouseDownInputEvent for the event.
	 * @see ControlMouseDownInputEvent
	 */
	@Override
	public ControlMouseDownInputEvent getEvent() {
		return event;
	}
}
