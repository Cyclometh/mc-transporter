package com.cyclometh.bukkit.plugins.transporters;

import java.util.ListIterator;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
//
public class BlockEventListener implements Listener {
	private Transporters plugin;
	private LinkManager linkManager;
	
	
	public BlockEventListener(Transporters plugin, LinkManager linkManager){
		this.plugin=plugin;
		this.linkManager=linkManager;
	}
	
	@EventHandler
	public void onBlockPlace(BlockPlaceEvent event) {
		Block placedBlock=event.getBlock();

		if(placedBlock.getType()==Material.QUARTZ_BLOCK) {
			Block placedBelow=placedBlock.getRelative(BlockFace.DOWN);
			if(placedBelow.getType()==Material.CHEST)
			{
				int inventoryKey=0;
				Chest chest=(Chest)placedBelow.getState();
				inventoryKey=computeInventoryKey(chest);
				//create a transporter record.
				this.createTransporter(placedBlock.getLocation(), event.getPlayer(), inventoryKey);
			}
		}
	}
	
	@EventHandler
	public void onBlockDestroy(BlockBreakEvent event) {
		Block brokenBlock=event.getBlock();
		Block secondBlock;
		if(brokenBlock.getType()==Material.QUARTZ_BLOCK) {
			secondBlock=brokenBlock.getRelative(BlockFace.DOWN);
			if(secondBlock.getType()==Material.CHEST) {
				destroyTransporter(brokenBlock.getLocation());
			}
		}
		else if (brokenBlock.getType()==Material.CHEST) {
			secondBlock=brokenBlock.getRelative(BlockFace.UP);
			if(secondBlock.getType()==Material.QUARTZ_BLOCK) {
				destroyTransporter(secondBlock.getLocation());
			}
		}
		
	}
	@SuppressWarnings("deprecation")
	public int computeInventoryKey(Chest chest) {
		int inventoryKey=0;
		Inventory inventory=chest.getInventory();
		ListIterator<ItemStack> stackIterator=inventory.iterator();
		while(stackIterator.hasNext()){
			//yes, it's deprecated, but it's the best way to get a unique key.
			//maybe try some kind of hashing if this becomes a problem.
			ItemStack stack=stackIterator.next();
			if(stack!=null) { //we don't care if it's an empty stack.
				inventoryKey+=(int)stack.getType().getId();
			}
			
		}
		return inventoryKey;
	}
	public void createTransporter(Location loc, Player player, int inventoryKey)
	{
		if(plugin.isDebug()) {
			plugin.getLogger().info(String.format("Creating transporter at %s, %s, %s", loc.getX(), loc.getY(), loc.getZ()));
		}
		linkManager.createTransporterRecord(
				(int)loc.getX(), (int)loc.getY(), (int)loc.getZ(), player.getUniqueId().toString(), inventoryKey);
	}
	
	public void destroyTransporter(Location loc)
	{
		linkManager.deleteTransporterRecord((int)loc.getX(), (int)loc.getY(), (int)loc.getZ());
	}
}
