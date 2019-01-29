package io.github.mosadie.mixbukkit.events;

import com.mixer.interactive.event.control.input.ControlMoveInputEvent;

import io.github.mosadie.mixbukkit.MixBukkit;

/**
 * The event used when a user on Mixer moves a joystick control.
 * @author MoSadie
 */
public class ControlMoveInput extends ControlInputEventBase {
	
	private final float X;
	private final float Y;
	private final ControlMoveInputEvent event;
	
	/**
	 * Construct a ControlMoveInput event.
	 * @param mb A reference to the MixBukkit object.
	 * @param event The full ControlMoveInputEvent from the Mixer Interactve Java API.
	 */
	public ControlMoveInput(MixBukkit mb, ControlMoveInputEvent event) {
		super(mb, event);
		this.event = event;
		X = event.getX();
		Y = event.getY();
	}

	/**
	 * Get the ControlMoveInputEvent object from the Mixer Interactive Java API.
	 * @return The ControlMoveInputEvent for the event.
	 * @see ControlMoveInputEvent
	 */
	@Override
	public ControlMoveInputEvent getEvent() {
		return event;
	}
	
	/**
	 * Get the X position of the joystick.
	 * Ranges between -1 (Left) and 1 (Right) inclusive.
	 * @return The X position of the joystick.
	 */
	public float getX() {
		return X;
	}
	
	/**
	 * Get the Y position of the joystick.
	 * Ranges between -1 (Top of joystick) and 1 (Bottom of joystick) inclusive.
	 * @return The Y position of the joystick.
	 */
	public float getY() {
		return Y;
	}
}
