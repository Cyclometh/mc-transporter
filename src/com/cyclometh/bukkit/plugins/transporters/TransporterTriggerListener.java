package com.cyclometh.bukkit.plugins.transporters;

import java.sql.ResultSet;
import java.sql.SQLException;

import lib.PatPeter.SQLibrary.Database;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

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
		Location to=null;
		World world=null;
		
		Location loc=event.getTo();
		Location from=event.getFrom();
		
		int x, y, z;
		x=loc.getBlockX();
		y=loc.getBlockY();
		z=loc.getBlockZ();
		
		if(Math.abs(from.getBlockX()-x) < 1 && Math.abs(from.getBlockY() - y) < 1 && Math.abs(from.getBlockZ()-z) < 1) {
			return; 	//player didn't move far enough.
		}
		
		//well, looks like they're set to go, so let's teleport them if they're standing on a teleporter.
		world=loc.getWorld();
		if ((to=getTargetLocation(x,y,z, loc, world, event.getPlayer().getUniqueId().toString()))!=null) {
			if(plugin.isDebug()) {
				plugin.getLogger().info(String.format("Preparing to teleport player %s at %s, %s, %s.", event.getPlayer().getName(), x, y, z));
			}
			world.playEffect(loc, Effect.SMOKE, 0);
			world.playSound(loc, Sound.ENDERMAN_TELEPORT, 1, 0);
			event.getPlayer().teleport(to);
			world.playEffect(to, Effect.SMOKE, 0);
			
			linkManager.recordTransport(x, y, z, event.getPlayer().getUniqueId().toString());
		}
			
	}
	
	private Location getTargetLocation(int x, int y, int z, Location currentLocation, World world, String playerUUID) {
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
				return new Location(world,
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
