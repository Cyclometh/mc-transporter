package com.cyclometh.bukkit.plugins.transporters;

import java.sql.ResultSet;
import java.sql.SQLException;

import lib.PatPeter.SQLibrary.Database;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;

/*
 * Class: TransporterTriggerListener
 * Description: Listens for events that would cause a transporter
 * to be triggered, specifically player or vehicle movements.
 * When an event is fired the database is checked for existing
 * transporters.
 */
public class TransporterTriggerListener implements Listener {

	private Transporters plugin=null;
	private ResultSet results=null;
	private LinkManager linkManager=null;
	private Database sql;
	
	public TransporterTriggerListener(Transporters plugin, Database sql, LinkManager linkManager) {
		this.plugin=plugin;
		this.sql=sql;
		this.linkManager=linkManager;
	}
	

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Location target=null;
		Location to=event.getTo();
		Location from=event.getFrom();
		
		if(!didMove(from, to))
		{
			return;
		}
		
		//well, looks like they're set to go, so let's teleport them if they're standing on a teleporter.
		target=getTargetLocation(to, event.getPlayer().getUniqueId().toString());
		
		if (target!=null) {
			if(plugin.isDebug()) {
				plugin.getLogger().info(String.format("Preparing to teleport player %s at %s, %s, %s.", event.getPlayer().getName(), 
						from.getBlockX(), from.getBlockY(), from.getBlockZ()));
			}
			Bukkit.getScheduler().runTask(this.plugin, new TeleportTask(event.getPlayer(), target, this.plugin));
			linkManager.recordTransport(from.getBlockX(), from.getBlockY(), from.getBlockZ(), event.getPlayer().getUniqueId().toString());
		}
			
	}
	
	@EventHandler
	public void onVehicleMove(VehicleMoveEvent event) {
		Location target=null;
		Location to=event.getTo();
		Location from=event.getFrom();
		
		if(event.getVehicle().getType() != EntityType.MINECART) {
			return;
		}
		
		if (event.getVehicle().getPassenger()==null 
				|| event.getVehicle().getPassenger().getType() != EntityType.PLAYER) {
			//no one in it, no teleport.
			//maybe add ability to teleport minecarts by themselves, but that means changing
			//the link lookup code for hubs and meh.
			return;
		}
		
		//plugin.getLogger().info(String.format("From: %s To: %s", from.toString(), to.toString()));
		
		if(!didMove(from, to))
		{
			return;
		}
		
		
		Player player=(Player)event.getVehicle().getPassenger();
		target=getTargetLocation(to, event.getVehicle().getPassenger().getUniqueId().toString());
		if (target!=null) {
			if(plugin.isDebug()) {
				plugin.getLogger().info(String.format("Preparing to teleport player %s at %s, %s, %s.", player.getName(), 
						from.getBlockX(), from.getBlockY(), from.getBlockZ()));
			}
			Bukkit.getScheduler().runTask(this.plugin, new TeleportTask((Player)event.getVehicle().getPassenger(), target, this.plugin));
			linkManager.recordTransport(from.getBlockX(), from.getBlockY(), from.getBlockZ(), player.getUniqueId().toString());
		}
	}
	
	private boolean didMove(Location from, Location to) {
		int fromX, fromY, fromZ;
		fromX=from.getBlockX();
		fromY=from.getBlockY();
		fromZ=from.getBlockZ();
		
		int toX, toY, toZ;
		toX=to.getBlockX();
		toY=to.getBlockY();
		toZ=to.getBlockZ();
		
		if(Math.abs(fromX - toX) < 1 && Math.abs(fromY - toY) < 1 && Math.abs(fromZ - toZ) < 1) {
			return false;	//player didn't move far enough.
		}
		return true;
	}
	
	
	private Location getTargetLocation(Location currentLocation, String playerUUID) {
		int x=currentLocation.getBlockX();
		int y=currentLocation.getBlockY();
		int z=currentLocation.getBlockZ();
		try {
			String query=String.format("SELECT t1.X, t1.Y, t1.Z, lt.PlayerUUID FROM TransporterLinks t1 "
					+ "JOIN TransporterLinks t2 ON t1.KeyValue = t2.KeyValue "
					+ "LEFT JOIN LastTransported lt ON t1.ID=lt.TransporterID "
					+ "WHERE t2.X=%d AND t2.Y=%d AND t2.Z=%d "
					+ "AND (lt.PlayerUUID='%s' OR lt.PlayerUUID IS NULL) "
					+ "AND CASE WHEN t1.ParentLinkID IS NULL "
					+ "THEN t2.ParentLinkID IS NOT NULL ELSE t2.ParentLinkID IS NULL END "
					+ "ORDER BY lt.PlayerUUID DESC;", x, y-1, z, playerUUID);
			
			sql.open();
			results=sql.query(query);
			if(results.next())
			{
				return new Location(currentLocation.getWorld(),
						results.getInt(1)+0.5,		//center of the target block.
						results.getInt(2)+1,		//don't want to put them IN the target block.
						results.getInt(3)+0.5,		//center of the target block.
						currentLocation.getYaw(),	//keep orientation.
						currentLocation.getPitch()	//keep tilt.
						);
			}
			return null;
		} catch (SQLException e) {
			plugin.getLogger().warning(String.format("ERROR: SQLException thrown while attempting to execute query: %s.", e.getMessage()));
			return null;
		} finally {
			sql.close();
		}
	}
}
