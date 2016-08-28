package io.github.mosadie.BeamBukkit;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import pro.beam.interactive.net.packet.Protocol.Report;

public class ReportEvent extends Event{
	private static final HandlerList handlers = new HandlerList();

	public HandlerList getHandlers() {
	    return handlers;
	}

	public static HandlerList getHandlerList() {
	    return handlers;
	}
	
	private Report report;
	
	ReportEvent(Report report) {
		this.report = report;
	}
	
	public Report getReport() {
		return report;
	}
}
