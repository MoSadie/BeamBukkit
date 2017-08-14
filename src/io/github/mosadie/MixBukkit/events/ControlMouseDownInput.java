package io.github.mosadie.MixBukkit.events;

import com.mixer.interactive.event.control.input.ControlMouseDownInputEvent;
import io.github.mosadie.MixBukkit.MixBukkit;

public class ControlMouseDownInput extends ControlInputEventBase{
	
	private final ControlMouseDownInputEvent event;
	
	public ControlMouseDownInput(MixBukkit mb, ControlMouseDownInputEvent event) {
		super(mb, event);
		this.event = event;
	}
	
	@Override
	public ControlMouseDownInputEvent getEvent() {
		return event;
	}
}
