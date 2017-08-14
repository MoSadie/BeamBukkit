package io.github.mosadie.MixBukkit.events;

import com.mixer.interactive.event.control.input.ControlMouseUpInputEvent;

import io.github.mosadie.MixBukkit.MixBukkit;

public class ControlMouseUpInput extends ControlInputEventBase{
	
	private final ControlMouseUpInputEvent event;
	
	public ControlMouseUpInput(MixBukkit mb, ControlMouseUpInputEvent event) {
		super(mb, event);
		this.event = event;
	}
	
	@Override
	public ControlMouseUpInputEvent getEvent() {
		return event;
	}
}
