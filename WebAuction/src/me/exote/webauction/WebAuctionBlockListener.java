package me.exote.webauction;

import java.net.MalformedURLException;
import java.sql.ResultSet;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WebAuctionBlockListener extends BlockListener{
	
	private final WebAuction plugin;
	
	public WebAuctionBlockListener(final WebAuction plugin) {
        this.plugin = plugin;
    }
	@Override
	public void onBlockBreak(BlockBreakEvent event){
		Block block = event.getBlock();
		Player player = event.getPlayer();
		if ((block.getTypeId() == 63)||(block.getTypeId() == 68)){
			Sign thisSign = (Sign)block.getState();
			if (thisSign.getLine(0).equals("[WebAuction]")){
				if (!plugin.permission.has(player, "wa.remove")){
					event.setCancelled(true);
					player.sendMessage(WebAuction.logPrefix + "You do not have permission to remove that");
				}else{
					player.sendMessage(WebAuction.logPrefix + "WebAuction sign removed.");
				}
			}
		}
	}
    public void onSignChange(SignChangeEvent event){
    	String[] lines = event.getLines();
    	Player player = event.getPlayer();
    	Block sign = event.getBlock();
    	World world = sign.getWorld();
    	Boolean allowEvent = false;
    	if (player != null){
    	if (lines[0].equals("[WebAuction]")){
    		if (lines[1].equals("Shout")){
    			if (plugin.permission.has(player, "wa.create.sign.shout")){
    				allowEvent = true;
    				player.sendMessage(WebAuction.logPrefix + "Shout sign created.");
    				int radius = Integer.parseInt(lines[2]);
    				try{
    					WebAuction.shoutSigns.put(sign.getLocation(), radius);
        				String queryInsertRec = "INSERT INTO WA_ShoutSigns (world, radius, x, y, z) VALUES ('" + world.getName() +"', "+radius+", "+sign.getX()+", "+sign.getY()+", "+sign.getZ()+");";
        				try {
        					plugin.manageMySQL.insertQuery(queryInsertRec);
        				} catch (Exception e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}
    				} catch (Exception e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    			
    			}
    		}
    		if (lines[1].equals("Recent")){
    			if (plugin.permission.has(player, "wa.create.sign.recent")){
    				allowEvent = true;
    				player.sendMessage(WebAuction.logPrefix + "Recent auction sign created.");
    				int amount = 1;
    				try{
    					amount = Integer.parseInt(lines[2]);
    				}catch(NumberFormatException nfe)
    				{
    					amount = 1;	
    				}
    				if (amount <= WebAuction.auctionCount){
    				String recentQuery = "SELECT * FROM WA_Auctions ORDER BY id DESC;";
    				ResultSet result = null;
    				try{
    					result = plugin.manageMySQL.sqlQuery(recentQuery);
    					int counter = 0;
    					while (counter < amount)
    					{
    					  if (result.next()) {
    						counter++;
    					  }
    					}
    					ItemStack stack = new ItemStack(result.getInt("name"), result.getInt("quantity"), result.getShort("damage"));
    					int quant = result.getInt("quantity");
    					Double price = result.getDouble("price");
    					String formattedPrice = WebAuction.economy.format(price);
    					WebAuction.recentSigns.put(sign.getLocation(), amount);
        				String queryInsertRec = "INSERT INTO WA_RecentSigns (world, offset, x, y, z) VALUES ('" + world.getName() +"', "+amount+", "+sign.getX()+", "+sign.getY()+", "+sign.getZ()+");";
        				try {
        					plugin.manageMySQL.insertQuery(queryInsertRec);
        				} catch (Exception e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}
    					event.setLine(1, stack.getType().toString());
    					event.setLine(2, quant+"");
    					event.setLine(3, ""+formattedPrice);
    				} catch (Exception e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
    				}else{
    					WebAuction.recentSigns.put(sign.getLocation(), amount);
	    				event.setLine(1, "Recent");
	    				event.setLine(2, amount+"");
	    				event.setLine(3, "Not Available");
        				String queryInsertRec = "INSERT INTO WA_RecentSigns (world, offset, x, y, z) VALUES ('" + world.getName() +"', "+amount+", "+sign.getX()+", "+sign.getY()+", "+sign.getZ()+");";
        				try {
        					plugin.manageMySQL.insertQuery(queryInsertRec);
        				} catch (Exception e) {
        					// TODO Auto-generated catch block
        					e.printStackTrace();
        				}
			    	}
    			
    			}
    		}
    		if (lines[1].equals("Deposit")){
    			if (plugin.permission.has(player, "wa.create.sign.deposit")){
    				allowEvent = true;
    				player.sendMessage(WebAuction.logPrefix + "Deposit point created");
    			}
    			
    		}
    		if (lines[1].equals("Withdraw")){
    			if (plugin.permission.has(player, "wa.create.sign.withdraw")){
    				allowEvent = true;
    				player.sendMessage(WebAuction.logPrefix + "Withdraw point created");
    			}
    		}
    		if ((lines[1].equals("MailBox"))||(lines[1].equals("Mailbox"))||(lines[1].equals("Mail Box"))){
    			if (lines[2].equals("Deposit")){
      			  if (plugin.permission.has(player, "wa.create.sign.mailbox.deposit")){	        
      				allowEvent = true;
      				player.sendMessage(WebAuction.logPrefix + "Deposit Mail Box created");
      			  }
      			}else{
      				if (plugin.permission.has(player, "wa.create.sign.mailbox.withdraw")){	        
        				allowEvent = true;
        				player.sendMessage(WebAuction.logPrefix + "Withdraw Mail Box created");
        			  }
      			}
    		}
    		if (allowEvent == false){
    			event.setCancelled(true);
    			sign.setTypeId(0);
    			ItemStack stack = new ItemStack(323, 1);
    			player.getInventory().addItem(stack);
    			player.sendMessage(WebAuction.logPrefix + "You do not have permission");
    		}
    	}
    	}
    }
	
}
