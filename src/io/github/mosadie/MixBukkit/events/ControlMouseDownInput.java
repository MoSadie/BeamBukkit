package io.github.mosadie.MixBukkit.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.mixer.interactive.event.control.input.ControlInputEvent;
import com.mixer.interactive.event.control.input.ControlMouseDownInputEvent;

import io.github.mosadie.MixBukkit.MixBukkit;

public class ControlMouseDownInput extends ControlInputEventBase{
	
	private final int button;
	
	public ControlMouseDownInput(MixBukkit mb, ControlMouseDownInputEvent event) {
		super(mb, event);
		button = event.getButton();
	}
	
	/**
	 * @see com.mixer.interactive.event.control.input.ControlMouseDownInputEvent#getButton()
	 */
	public int getButton() {
		return button;
	}
}
