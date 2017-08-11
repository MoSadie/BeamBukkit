package io.github.mosadie.MixBukkit;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

//import pro.beam.interactive.net.packet.Protocol.Report;

public class ReportEvent extends Event{
	private static final HandlerList handlers = new HandlerList();

	public HandlerList getHandlers() {
	    return handlers;
	}

	public static HandlerList getHandlerList() {
	    return handlers;
	}
	
	//private Report report;
	private MixBukkit mb;
	
	//ReportEvent(Report report, MixBukkit mb) {
	//	this.report = report;
	//	this.mb = mb;
	//}
	
	//public Report getReport() {
	//	return report;
	//}
	
	public MixBukkit getMixBukkit() {
		return mb;
	}
}
