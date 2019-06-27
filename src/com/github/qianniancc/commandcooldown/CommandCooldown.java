package com.github.qianniancc.commandcooldown;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandCooldown extends JavaPlugin implements Listener {
	private HashMap<String, Long> configuredCommands = new HashMap<String, Long>();
	private HashMap<String, HashMap<String, Long>> playerCommands = new HashMap<String, HashMap<String, Long>>();
	private HashSet<String> unaffectedPlayers = new HashSet<String>();

	public void onEnable() {
		if (!getDataFolder().exists()) {
			getDataFolder().mkdir();
		}
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			saveDefaultConfig();
		}
		getLogger().info("启动成功！");
		reloadConfig();
		loadConfig();
		this.getServer().getPluginManager().registerEvents(this, this);
	}

	private void loadConfig() {
		configuredCommands.clear();
		unaffectedPlayers.clear();

		File dataDir = this.getDataFolder();
		dataDir.mkdirs();
		File cmdConfig = new File(dataDir, "commands.txt");
		File playerOverrides = new File(dataDir, "players-na.txt");
		if (!cmdConfig.isFile())
			try {
				cmdConfig.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		if (!playerOverrides.isFile())
			try {
				playerOverrides.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}

		try (FileReader Frdr = new FileReader(cmdConfig); BufferedReader rdr = new BufferedReader(Frdr)) {
			String tempLine;
			while ((tempLine = rdr.readLine()) != null) {
				String[] line = tempLine.split(" ", 2);
				configuredCommands.put(line[0], Integer.parseInt(line[1]) * 1000L);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try (FileReader Frdr = new FileReader(playerOverrides); BufferedReader rdr = new BufferedReader(Frdr)) {
			String tempLine;
			while ((tempLine = rdr.readLine()) != null) {
				unaffectedPlayers.add(tempLine);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("ccreload")) {
			if ((!sender.isOp()) && (!sender.hasPermission("cc.reload")) && (!sender.hasPermission("cc.*"))) {
				String TextNotReloadPerm = getConfig().getString("NotReloadPerm");
				String NotReloadPerm = ChatColor.translateAlternateColorCodes('&', TextNotReloadPerm);
				sender.sendMessage(NotReloadPerm);
				return false;
			}
			try {
				reloadConfig();
				loadConfig();
				String TextReloadOK = getConfig().getString("ReloadOK");
				String ReloadOK = ChatColor.translateAlternateColorCodes('&', TextReloadOK);
				sender.sendMessage(ReloadOK);
			} catch (Exception e) {
				String TextReloadError = getConfig().getString("ReloadError");
				String ReloadError = ChatColor.translateAlternateColorCodes('&', TextReloadError);
				sender.sendMessage(ReloadError);
			}
			return true;
		}
		return false;
	}

	@EventHandler
	public void onCmd(PlayerCommandPreprocessEvent e) {
		String cmd = e.getMessage().trim();
		if (cmd.startsWith("/")) {
			cmd = cmd.substring(1).trim();
		}
		int firstSpace = cmd.indexOf(' ');
		if (firstSpace < 0) {
			firstSpace = cmd.length();
		}
		cmd = cmd.substring(0, firstSpace);
		cmd = cmd.toLowerCase();
		if ((getConfig().getString("CCAll") != "false") && (!this.configuredCommands.containsKey(cmd))) {
			cmd = "*";
		}
		if (!this.configuredCommands.containsKey(cmd)) {
			return;
		}
		if (e.getPlayer().isOp()) {
			return;
		}
		if ((e.getPlayer().hasPermission("cc.*")) || (e.getPlayer().hasPermission("cc.nocc"))) {
			return;
		}
		String playerName = e.getPlayer().getName();

		HashMap<String, Long> playerCmd = (HashMap<String, Long>) this.playerCommands.get(playerName);
		long lastIssuedTime = 0L;
		if ((playerCmd != null) && (playerCmd.containsKey(cmd))) {
			lastIssuedTime = ((Long) playerCmd.get(cmd)).longValue();
		}
		long interval = ((Long) this.configuredCommands.get(cmd)).longValue();
		long ts = System.currentTimeMillis();
		if (ts - lastIssuedTime < interval) {
			e.setCancelled(true);
			String TextCCPrefix = getConfig().getString("CCPrefix");
			String CCPrefix = ChatColor.translateAlternateColorCodes('&', TextCCPrefix);
			String TextCCSuffix = getConfig().getString("CCSuffix");
			String CCSuffix = ChatColor.translateAlternateColorCodes('&', TextCCSuffix);
			e.getPlayer().sendMessage(CCPrefix + (interval + lastIssuedTime - ts) / 1000L + CCSuffix);
			return;
		}
		if (playerCmd == null) {
			playerCmd = new HashMap<String, Long>();
			this.playerCommands.put(playerName, playerCmd);
		}
		playerCmd.put(cmd, Long.valueOf(ts));
	}
}