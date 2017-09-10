package io.github.mosadie.MixBukkit.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mixer.api.MixerAPI;
import com.mixer.api.resource.MixerUser;
import com.mixer.api.services.impl.UsersService;
import com.mixer.interactive.GameClient;

import io.github.mosadie.MixBukkit.MixBukkit;
//import pro.beam.interactive.net.packet.Protocol.Report;

public class mixbukkit implements CommandExecutor{
	MixBukkit plugin;

	String[] help = {"How to use the MixBukkit command:","/mixbukkit help -> displays this message.","/mixbukkit debug <on or off> -> Enables/Disables debug mode","/mixbukkit setup -> Starts the setup wizard for the Mixer connection","/mixbukkit chat <message> -> Sends a message to Mixer chat as the connected user","/mixbukkit whisper <Mixer User> <message> -> Sends a whisper message to a user on Mixer from the connected Mixer channel"};

	public mixbukkit(MixBukkit plugin) {
		this.plugin = plugin;
	}
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.isOp() && sender instanceof Player) {
			sender.sendMessage("You need to be OP to use this command!");
			return true;
		}
		
		if (args.length==0) {
			sender.sendMessage(help);
			return true;
		}
		switch(args[0]) {
		case "help":
			sender.sendMessage(help);
			return true;
		case "debug":
			if (args.length == 1) {
				sender.sendMessage(help);
				return true;
			}
			switch (args[1]) {
			case "on":
				plugin.setDebugCS(sender);
				return true;
			case "off":
				plugin.setDebugCS(null);
				return true;
			default:
				sender.sendMessage("/mixbukkit debug <on or off>");
				return true;
			}
		case "setup":
			if (plugin.mixer != null) {
				plugin.mixer = null;
			}
			if (plugin.gameClient != null) {
				plugin.gameClient.disconnect();
				plugin.gameClient = null;
			}
			plugin.setup(sender);
			return true;
		case "chat":
			if (plugin.mixer == null) {
				sender.sendMessage("Please run /mixbukkit setup before running this command!");
				return true;
			}
			if (args.length == 1) {
				sender.sendMessage(help);
				return true;
			}
			plugin.setupChatIfNotDoneAlready();
			String message = "";
			for (int i = 1; i < args.length; i++) {
				if (i == 1) message = args[i];
				else message = message+" "+args[i];
			}
			plugin.chat(message);
			sender.sendMessage("<"+plugin.user.username+" via Mixer> "+message);
			return true;
		case "whisper":
			if (plugin.mixer == null) {
				sender.sendMessage("Please run /mixbukkit setup before running this command!");
				return true;
			}
			if (args.length <= 2 ) {
				sender.sendMessage(help);
				return true;
			}
			plugin.setupChatIfNotDoneAlready();
			String whisper = "";
			for (int i = 2; i < args.length; i++) {
				if (i == 2) whisper = args[i];
				else whisper = whisper+" "+args[i];
			}
			MixerUser recipient;
			try {
				recipient = plugin.mixer.use(UsersService.class).search(args[1]).get().get(0);
				plugin.whisper(recipient, whisper);
				sender.sendMessage("<"+sender.getName()+" -> "+recipient.username+" via Mixer> "+whisper);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return true;
		case "status":
			sender.sendMessage("Status of MixBukkit plugin:");
			sender.sendMessage("Mixer API setup: " + (plugin.mixer == null ? "No" : "Yes"));
			sender.sendMessage("Game Client setup: " + (plugin.gameClient == null ? "No" : "Yes"));
			sender.sendMessage("Mixer User found: " + (plugin.user == null ? "No" : "Yes"));
			sender.sendMessage("Mixer Chat found: " + (plugin.chat == null ? "No" : "Yes"));
			sender.sendMessage("Mixer Chat Connection setup: " + (plugin.chatConnectable == null ? "No" : "Yes"));
			return true;
		default: 
			return false;
		}
	}

}
