package com.cyclometh.bukkit.plugins.transporters;

import java.sql.ResultSet;
import java.sql.SQLException;

import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.SQLite;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.logging.Logger;

public class LinkManager {

	BukkitScheduler scheduler;
	final Transporters plugin;
	final Logger log;
	private final String prefix;
	private final String directory;
	private final String fileName;
	private final String extension;
	private final boolean debug;
	
	
	public LinkManager(Transporters plugin, String prefix, String directory, String fileName, String extension){
		this.plugin=plugin;
		this.prefix=prefix;
		this.directory=directory;
		this.fileName=fileName;
		this.extension=extension;
		this.log = Logger.getLogger("Minecraft");
		this.scheduler=Bukkit.getServer().getScheduler();
		this.debug=plugin.isDebug();
		
	}
	public void createTransporterRecord(final int x, final int y, final int z, final String playerId, final int inventoryKey) {
		if(debug) {
			log.info(String.format("Creating transporter at %d, %d, %d, with player ID %s and inventory key %d.", x,y,z,playerId,inventoryKey));
		}

		this.scheduler.runTaskAsynchronously(plugin, 
			new Runnable() {
			private Database sql;
			@Override
			public void run() {
				try {
					if(debug){
						log.info("Initializing database connection.");
					}
						
					sql=new SQLite(log, prefix, directory, fileName, extension);
					sql.open();
					//first, does another transporter with this key link exist?
					
					if(debug) {
						log.info("Checking for existing parent transporter.");
					}
					
					String keyQuery=String.format("SELECT ID FROM TransporterLinks WHERE KeyValue=%d AND ParentLinkID IS NULL", inventoryKey);
					ResultSet keyQueryResults;
					
					String query;
					int parentId=0;
					
						keyQueryResults=sql.query(keyQuery);
						//should only be one result.
						if (keyQueryResults.next()) {
							parentId=keyQueryResults.getInt(1);
						}
					
						
					//determine type of record to insert.
					if(parentId != 0) {
						if(debug) {
							log.info("Parent record found. Inserting new child transporter.");
						}
						query=String.format("INSERT INTO TransporterLinks(X, Y, Z, KeyValue, PlayerUUID, ParentLinkID) VALUES (%d, %d, %d, %d, '%s', %d)",
								x, y, z, inventoryKey, playerId, parentId);
					}  else {
						if(debug) {
							log.info("No parent record found. Transporter is either first with this key or is a replacement parent.");
						}
						query=String.format("INSERT INTO TransporterLinks(X, Y, Z, KeyValue, PlayerUUID) VALUES (%d, %d, %d, %d, '%s')",
								x, y, z, inventoryKey, playerId);
					}
					
					sql.query(query);
					
					//check to see if we need to rebase child records.
					if(parentId==0)
					{
						//re-run the previous ID query to get the newly inserted key.
						keyQueryResults=sql.query(keyQuery);
						if(keyQueryResults.next()) {
							parentId=keyQueryResults.getInt(1);
						} else {
							log.warning("Error: Expected ID not found after INSERT. Transporter links database may be corrupt.");
							return;
						}
						if(debug) {
							log.info("Rebasing existing transporters to new parent.");
						}
						
						query=String.format("UPDATE TransporterLinks SET ParentLinkID=%d WHERE KeyValue=%d AND ParentLinkID IS NOT NULL;", 
								parentId, inventoryKey);
						sql.query(query);
					}
					
				} catch (SQLException e) {
					log.warning(String.format("Error attempting to create transporter record: %s", e.getMessage()));
					return;
				} catch (NullPointerException npe) {
					log.warning(String.format("Null pointer exception encountered: %s", npe.getMessage()));
					return;
				} finally {
					sql.close();
				}
			} 
		});
		 
	}
	
	public void deleteTransporterRecord(final int x, final int y, final int z) {
		this.scheduler.runTaskAsynchronously(plugin, new Runnable() {
			private Database sql;
			@Override
			public void run() {
				if(debug) {
					log.info(String.format("Deleting transporter at %d, %d, %d.", x, y, z));
				}
				try {
					if(debug) {
						log.info("Initializing database connection.");
					}
					sql=new SQLite(log, prefix, directory, fileName, extension);
					sql.open();
					
					String query=String.format("DELETE FROM TransporterLinks WHERE X=%d AND Y=%d AND Z=%d;", x,y,z);
					sql.query(query);
				} catch(SQLException e) {
					log.warning(String.format("Error attempting to create transporter record: %s", e.getMessage()));
				} finally {
					sql.close();
				}
			}
		});
	}
	
	public void recordTransport(final int x, final int y, final int z, final String playerUUID) {
		this.scheduler.runTaskAsynchronously(plugin,  new Runnable() {
			private Database sql;
			@Override
			public void run() {
				if(debug) {
					log.info(String.format("Recording player transport from %d, %d, %d. Player UUID: %s", x, y, z, playerUUID));
				}
				try {
					if(debug) {
						log.info("Initializing database connection.");
					}
					sql=new SQLite(log, prefix, directory, fileName, extension);
					sql.open();
					
					String query=String.format("INSERT OR REPLACE INTO LastTransported(ID, PlayerUUID, TransporterID)"
							+ "VALUES ((SELECT ID FROM LastTransported WHERE PlayerUUID='%s'), "
							+ "'%s',"
							+ "(SELECT ID FROM TransporterLinks tl WHERE tl.X=%d AND tl.Y=%d AND tl.Z=%d));", playerUUID, playerUUID, x,y-1,z);
					
					sql.query(query);
				} catch(SQLException e) {
					log.warning(String.format("Error attempting to create transporter record: %s", e.getMessage()));
				} finally {
					sql.close();
				}
			}
			
		});
	}

		
		
	
	
	
}
