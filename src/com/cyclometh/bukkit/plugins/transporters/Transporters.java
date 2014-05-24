/*
 * Transporters- a Bukkit plugin.
 * Class: Transporters
 * Description: Transporters lets you build point-to-point teleporters in Minecraft,
 * similar to the way the "Star Trek" transporter works. 
 * This class is the main plugin class and handles setting up and tearing down
 * the plugin.
 * Author: Cyclometh (cyclometh@gmail.com)
 * Version: 1.0
 * 
 */

package com.cyclometh.bukkit.plugins.transporters;
import java.sql.SQLException;
import lib.PatPeter.SQLibrary.Database;
import lib.PatPeter.SQLibrary.SQLite;
import org.bukkit.plugin.java.JavaPlugin;

public class Transporters extends JavaPlugin {
	private TransporterTriggerListener triggerListener;
	private BlockEventListener blockListener;
	private Database sql=null;
	private LinkManager linkManager;
	
	private String prefix;
	private String directory;
	private String fileName;
	private String extension;
	private boolean debug;
	
	@Override
	public void onEnable(){
		
		this.saveDefaultConfig();
		
		loadConfiguration();
		if(debug)
		{
			getLogger().info("Configuration loaded.");
		}
		
		initDatabase();
		if(debug)
		{
			getLogger().info("Dataabse connection initialized.");
		}
		
		linkManager=new LinkManager(this, this.prefix, this.directory, this.fileName, this.extension);
		
		this.triggerListener=new TransporterTriggerListener(this, sql, linkManager);
		this.blockListener=new BlockEventListener(this, linkManager);
		
		getServer().getPluginManager().registerEvents(this.triggerListener, this);
		getServer().getPluginManager().registerEvents(this.blockListener,  this);
		
		getLogger().info("Transporters enabled.");
	}

	public boolean isDebug() {
		return debug;
	}

	@Override
	public void onDisable() {
		getLogger().info("Transporters disabled.");
		sql.close();
	}

	@Override
	public void onLoad() {
		getLogger().info("Transporters loaded.");
	}
	
	private void loadConfiguration() {
		
		this.debug=this.getConfig().getBoolean("debug", false);
		this.prefix=this.getConfig().getString("DatabasePrefix", "[TransportersDB]");
		this.directory=this.getConfig().getString("DatabasePath", this.getDataFolder().getAbsolutePath());
		this.fileName=this.getConfig().getString("DatabaseName", "TransportersDB");
		this.extension=this.getConfig().getString("DatabaseExtension", ".db");
	}
	
	
	private void initDatabase() {
		getLogger().info(String.format("prefix: %s, directory: %s, filename: %s, extension: %s", this.prefix, this.directory, this.fileName, this.extension));
		sql=new SQLite(getLogger(), this.prefix, this.directory, this.fileName, this.extension);
		sql.open();
		if(!sql.isTable("TransporterLinks"))
		{
			//database objects need to be created.
			setupDatabase();
		}
		sql.close();
	}
	
	private void setupDatabase()
	{
		try {
			getLogger().info("Database not initialized, creating database tables.");
			sql.query("CREATE TABLE TransporterLinks(ID INTEGER PRIMARY KEY ASC, X INT, Y INT, Z INT, "
					+ "KeyValue INT, PlayerUUID TEXT, ParentLinkID INT NULL);");
			sql.query("CREATE UNIQUE INDEX locIdx ON TransporterLinks(X, Y, Z);");
			sql.query("CREATE INDEX keyIdx ON TransporterLinks(KeyValue);");
			sql.query("CREATE TABLE LastTransported(ID INTEGER PRIMARY KEY ASC, PlayerUUID TEXT, TransporterID INT;");
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
