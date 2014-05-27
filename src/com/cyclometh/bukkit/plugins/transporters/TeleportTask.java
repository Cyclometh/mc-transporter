package com.cyclometh.bukkit.plugins.transporters;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TeleportTask extends BukkitRunnable {
	private Transporters plugin;
	private Player player;
	private Location target;
	
	public TeleportTask(Player player, Location target, Transporters plugin) {
		this.plugin=plugin;
		this.player=player;
		this.target=target;
	}

	
	@Override
	public void run() {
		if (plugin.isDebug()) {
			plugin.getLogger().info("Teleport task started.");
		}
		
		
		World startWorld=player.getWorld();
		World targetWorld=target.getWorld();
		
		startWorld.playEffect(player.getLocation(), Effect.SMOKE, 0);
		startWorld.playSound(player.getLocation(), Sound.ENDERMAN_TELEPORT, 1, 0);
		this.target.getBlock();
		if(this.player.isInsideVehicle())
		{
			player.eject();
			
			Minecart minecart=(Minecart)player.getVehicle();
			Vector vel=minecart.getVelocity();
			minecart.remove();
			target.setY(target.getBlockY() + 0.5);
			minecart=(Minecart)target.getWorld().spawn(target, Minecart.class);
			minecart.setVelocity(vel);
			player.teleport(target);
			minecart.setPassenger(player);
		}
		else
		{
			player.teleport(this.target, TeleportCause.PLUGIN);
		}
		targetWorld.playEffect(target, Effect.SMOKE, 0);
		targetWorld.playSound(target, Sound.ENDERMAN_TELEPORT, 1, 0);
	}

}
