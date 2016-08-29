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
	private BeamBukkit bb;
	
	ReportEvent(Report report, BeamBukkit bb) {
		this.report = report;
		this.bb = bb;
	}
	
	public Report getReport() {
		return report;
	}
	
	public BeamBukkit getBeamBukkit() {
		return bb;
	}
}
