package me.exote.webauction;

import java.net.MalformedURLException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WebAuctionCommands implements CommandExecutor{
	private final WebAuction plugin;
	
	public WebAuctionCommands(final WebAuction plugin) {
        this.plugin = plugin;
    }
	public static String MD5(String str) {
	    MessageDigest md = null;
	    try {
	      md = MessageDigest.getInstance("MD5");
	    } catch (NoSuchAlgorithmException e) {
	      e.printStackTrace();
	    }
	    md.update(str.getBytes());

	    byte[] byteData = md.digest();

	    StringBuffer hexString = new StringBuffer();
	    for (int i = 0; i < byteData.length; i++) {
	      String hex = Integer.toHexString(0xFF & byteData[i]);
	      if (hex.length() == 1) {
	        hexString.append('0');
	      }
	      hexString.append(hex);
	    }
	    return hexString.toString();
	  }
	@Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] split) {
		
		int params = split.length;
		Player player = (Player) sender;
		if (params == 0){
			return false;
		}
		else if (params == 1){
			return false;
		}
		else if (params == 2){
			if (split[0].equals("password")){
				if (split[1] != null){
					String newPass = MD5(split[1]);
					String queryUpdate = "UPDATE WA_Players SET pass='"+newPass+"' WHERE name='"+player.getName()+"';";
					try {
						this.plugin.manageMySQL.updateQuery(queryUpdate);
						player.sendMessage(WebAuction.logPrefix + "Password changed");
						return true;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return false;
				}
			}
		}
		return false;
	}
}