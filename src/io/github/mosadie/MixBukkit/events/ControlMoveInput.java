package io.github.mosadie.MixBukkit.events;

import com.mixer.interactive.event.control.input.ControlMoveInputEvent;

import io.github.mosadie.MixBukkit.MixBukkit;

public class ControlMoveInput extends ControlInputEventBase {
	
	private final float X;
	private final float Y;
	private final ControlMoveInputEvent event;
	
	public ControlMoveInput(MixBukkit mb, ControlMoveInputEvent event) {
		super(mb, event);
		this.event = event;
		X = event.getX();
		Y = event.getY();
	}

	@Override
	public ControlMoveInputEvent getEvent() {
		return event;
	}
	
	public float getX() {
		return X;
	}
	
	public float getY() {
		return Y;
	}
}
