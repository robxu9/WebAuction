package me.exote.webauction;

import java.io.File;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.alta189.sqlLibrary.MySQL.mysqlCore;
import com.alta189.sqlLibrary.SQLite.sqlCore;

public class WebAuction extends JavaPlugin{
	
	private final WebAuctionPlayerListener playerListener = new WebAuctionPlayerListener(this);
    private final WebAuctionBlockListener blockListener = new WebAuctionBlockListener(this);
	public static String logPrefix = "[WebAuction] ";
	public static Logger log = Logger.getLogger("Minecraft");
	public static File pFolder = new File("plugins/WebAuction");
	public  Server server = this.getServer();
	public PluginDescriptionFile info;
    public static mysqlCore manageMySQL; // MySQL handler
	public sqlCore manageSQLite; // SQLite handler
    public static ArrayList<Location> mailLocations = new ArrayList<Location>();
    public static ArrayList<Location> recentRemove = new ArrayList<Location>();
    public static ArrayList<Location> shoutRemove = new ArrayList<Location>();
    public static HashMap<Player, Long> playerTimer = new HashMap<Player, Long>();
    public static HashMap<Location, Integer> recentSigns = new HashMap<Location, Integer>();
    public static HashMap<Location, Integer> shoutSigns = new HashMap<Location, Integer>();
	public Plugin plugin = this;
	public String dbHost = null;
	public String dbUser = null;
	public String dbPass = null;
	public String dbDatabase = null;
	public Boolean getMessages = false;
	public static int signDelay = 0;
	public FileConfiguration config;
	public static Permission permission = null;
	public static Economy economy = null;
	public static int lastAuction = 0;
	public static int auctionCount = 0;
	
	public static long getCurrentMilli(){
		Calendar cal = Calendar.getInstance();
		long seconds = cal.getTimeInMillis();
		return seconds;
	}
	public void onEnable() {
		
		this.log.info(this.logPrefix + "WebAuction is initializing");
		File f = new File("plugins/WebAuction");
		f.mkdir();
		info = this.getDescription();
		PluginManager pm = getServer().getPluginManager();
		config = getConfig();
		dbHost = config.getString("dbHost", "localhost");
		dbUser = config.getString("dbUser", "root");
		dbPass = config.getString("dbPass", "pass123");
		dbDatabase = config.getString("dbDatabase", "minecraft");
		getMessages = config.getBoolean("reportSalesInGame", false);
		signDelay = config.getInt("signDelayMilli", 1000);
		
		getCommand("wa").setExecutor(new WebAuctionCommands(this));
		
		if (this.dbHost.equals(null)) {this.log.severe(this.logPrefix + "MySQL host is not defined"); }
		if (this.dbUser.equals(null)) {this.log.severe(this.logPrefix + "MySQL username is not defined"); }
		if (this.dbPass.equals(null)) {this.log.severe(this.logPrefix + "MySQL  password is not defined"); }
		if (this.dbDatabase.equals(null)) {this.log.severe(this.logPrefix + "MySQL database is not defined"); }
		
		setupEconomy();
		
		// Declare MySQL Handler
		this.manageMySQL = new mysqlCore(this.log, this.logPrefix, this.dbHost, this.dbDatabase, this.dbUser, this.dbPass);
			
		this.log.info(this.logPrefix + "MySQL Initializing");
		// Initialize MySQL Handler
		this.manageMySQL.initialize();
		try {
			if (this.manageMySQL.checkConnection()) { // Check if the Connection was successful
				this.log.info(this.logPrefix + "MySQL connection successful");
				
				if (!this.manageMySQL.checkTable("WA_Players")) {
					this.log.info(this.logPrefix + "Creating table WA_Players");
					String query = "CREATE TABLE WA_Players (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name VARCHAR(255), pass VARCHAR(255), money DOUBLE, canBuy INT, canSell INT, isAdmin INT);";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_StorageCheck")) {
					this.log.info(this.logPrefix + "Creating table WA_StorageCheck");
					String query = "CREATE TABLE WA_StorageCheck (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), time INT);";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_WebAdmins")) {
					this.log.info(this.logPrefix + "Creating table WA_WebAdmins");
					String query = "CREATE TABLE WA_WebAdmins (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name VARCHAR(255));";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_Items")) {
					this.log.info(this.logPrefix + "Creating table WA_Items");
					String query = "CREATE TABLE WA_Items (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name INT, damage INT, player VARCHAR(255), quantity INT);";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_Enchantments")) {
					this.log.info(this.logPrefix + "Creating table WA_Enchantments");
					String query = "CREATE TABLE WA_Enchantments (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), enchName VARCHAR(255), enchId INT, level INT);";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_EnchantLinks")) {
					this.log.info(this.logPrefix + "Creating table WA_EnchantLinks");
					String query = "CREATE TABLE WA_EnchantLinks (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), enchId INT, itemTableId INT, itemId INT);";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_Auctions")) {
					this.log.info(this.logPrefix + "Creating table WA_Auctions");
					String query = "CREATE TABLE WA_Auctions (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name INT, damage INT, player VARCHAR(255), quantity INT, price DOUBLE, started INT);";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_SellPrice")) {
					this.log.info(this.logPrefix + "Creating table WA_SellPrice");
					String query = "CREATE TABLE WA_SellPrice (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name INT, damage INT, time INT, quantity INT, price DOUBLE, seller VARCHAR(255), buyer VARCHAR(255));";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_MarketPrices")) {
					this.log.info(this.logPrefix + "Creating table WA_MarketPrices");
					String query = "CREATE TABLE WA_MarketPrices (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name INT, damage INT, time INT, marketprice DOUBLE, ref INT);";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_Mail")) {
					this.log.info(this.logPrefix + "Creating table WA_Mail");
					String query = "CREATE TABLE WA_Mail (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), name INT, damage INT, player VARCHAR(255), quantity INT);";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_RecentSigns")) {
					this.log.info(this.logPrefix + "Creating table WA_RecentSigns");
					String query = "CREATE TABLE WA_RecentSigns (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), world VARCHAR(255), offset INT, x INT, y INT, z INT);";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_ShoutSigns")) {
					this.log.info(this.logPrefix + "Creating table WA_ShoutSigns");
					String query = "CREATE TABLE WA_ShoutSigns (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), world VARCHAR(255), radius INT, x INT, y INT, z INT);";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_ProtectedLocations")) {
					this.log.info(this.logPrefix + "Creating table WA_ProtectedLocations");
					String query = "CREATE TABLE WA_ProtectedLocations (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), world VARCHAR(255), x INT, y INT, z INT);";
					this.manageMySQL.createTable(query);
				}
				if (!this.manageMySQL.checkTable("WA_SaleAlerts")) {
					this.log.info(this.logPrefix + "Creating table WA_SaleAlerts");
					String query = "CREATE TABLE WA_SaleAlerts (id INT NOT NULL AUTO_INCREMENT, PRIMARY KEY(id), seller VARCHAR(255), quantity INT, price DOUBLE, buyer VARCHAR(255), item VARCHAR(255), alerted BOOLEAN Default '0');";
					this.manageMySQL.createTable(query);
				}
			} else {
				this.log.severe(this.logPrefix + "MySQL connection failed");
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String query2 = "SELECT id FROM WA_Auctions ORDER BY id DESC";
		ResultSet result2 = null;
		
		try {
			result2 = this.manageMySQL.sqlQuery(query2);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		try {
			if (result2 != null){
				result2.next();
				lastAuction = result2.getInt("id");
				this.log.info(this.logPrefix + "Current Auction id = "+lastAuction);
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		String query3 = "SELECT * FROM WA_ShoutSigns";
		ResultSet result3 = null;
		
		try {
			result3 = this.manageMySQL.sqlQuery(query3);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		try {
			if (result3 != null){
				while (result3.next()){					
					World world = this.getServer().getWorld(result3.getString(2));					
					Location signLoc = new Location(world, result3.getInt("x"), result3.getInt("y"), result3.getInt("z") );
					int radius = result3.getInt("radius");
					shoutSigns.put(signLoc, radius);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		String query1 = "SELECT * FROM WA_RecentSigns";
		ResultSet result1 = null;
		
		try {
			result1 = WebAuction.manageMySQL.sqlQuery(query1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		try {
			if (result1 != null){
				while (result1.next()){					
					World world = this.getServer().getWorld(result1.getString(2));					
					Location signLoc = new Location(world, result1.getInt("x"), result1.getInt("y"), result1.getInt("z") );
					int offset = result1.getInt("offset");
					recentSigns.put(signLoc, offset);
				}
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		Player[] playerList1 = getServer().getOnlinePlayers();
		for (int i = 0; i < playerList1.length; i++) {
			Player player = playerList1[i];
			WebAuction.playerTimer.put(player, WebAuction.getCurrentMilli());
		}
		if (getMessages == true){
		try{
			this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {

				public void run() {
					Player[] playerList = getServer().getOnlinePlayers();
					for (int i = 0; i < playerList.length; i++) {
						Player player = playerList[i];
						String queryAlerts = "SELECT * FROM WA_SaleAlerts WHERE seller= '"+player.getName()+"' AND alerted= '0';";
						ResultSet resultAlerts = null;
						try {
							resultAlerts = WebAuction.manageMySQL.sqlQuery(queryAlerts);
						} catch (Exception e) {
							e.printStackTrace();
						} 
						try {
							if (resultAlerts != null){
								while (resultAlerts.next()){	
									int id = resultAlerts.getInt("id");
									String buyer = resultAlerts.getString("buyer");
									String item = resultAlerts.getString("item");
									int quantity = resultAlerts.getInt("quantity");
									Double priceEach = resultAlerts.getDouble("price");
									Double priceTotal = quantity+priceEach;
									player.sendMessage(WebAuction.logPrefix + "You sold "+quantity+" "+item+" to "+buyer+" for "+priceEach+" each.");
									String updateAlerts = "UPDATE WA_SaleAlerts SET alerted = '1' WHERE id = '"+id+"';";
									WebAuction.manageMySQL.updateQuery(updateAlerts);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						} 
						
					}
				}
			}, 1 * 30L, 1 * 30L);
		}catch (Exception e){
			e.printStackTrace();
		}
		}
		try{
			this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {

				public void run() {
					String queryLatest = "SELECT * FROM WA_Auctions ORDER BY id DESC";
					ResultSet result2 = null;
					
					try {
						result2 = WebAuction.manageMySQL.sqlQuery(queryLatest);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					try {
						
						if (result2 != null){
							result2.next();
							if (lastAuction < result2.getInt("id"))
							{
								lastAuction = result2.getInt("id");
								Player[] playerList = getServer().getOnlinePlayers();
								Boolean[] inRange = new Boolean[playerList.length];
								Iterator iteratorShout = shoutSigns.keySet().iterator();
								ItemStack stack = new ItemStack(result2.getInt("name"), result2.getInt("quantity"), result2.getShort("damage"));
								Double price = result2.getDouble("price");
		    					String formattedPrice = economy.format(price);
		    					WebAuction.log.info(WebAuction.logPrefix + "New Auction: "+stack.getAmount()+" "+stack.getType().toString()+" selling for "+formattedPrice+" each.");
								for (int j = 0; j < playerList.length; j++) {
									inRange[j] = false;
								}
							    while (iteratorShout.hasNext()) {
							    	Location key = (Location) iteratorShout.next();
							    	int blockMatCheck = key.getBlock().getTypeId();
			    					if ((blockMatCheck == 63)||(blockMatCheck == 68))
			    					{
			    						Double xValue = key.getX();
			    						Double zValue = key.getZ();
			    						int radius = shoutSigns.get(key);
			    						for (int i = 0; i < playerList.length; i++) {
			    							Player player = playerList[i];
			    							Double playerX = player.getLocation().getX();
			    							Double playerZ = player.getLocation().getZ();
			    							if ((playerX < xValue + radius)&&(playerX > xValue - radius)){
			    								if ((playerZ < zValue + radius)&&(playerZ > zValue - radius)){
			    									inRange[i] = true;
			    								}
			    							}
			    						}
			    					}else{
			    						shoutRemove.add(key);
			    					}			    					
							    }
							    Iterator it = shoutRemove.iterator();
			    				while (it.hasNext()){
			    					Location signLoc = (Location) it.next();
			    					recentSigns.remove(signLoc);
			    					String queryRemove = "DELETE FROM WA_ShoutSigns WHERE world='"+signLoc.getWorld().getName()+"' AND x='"+signLoc.getX()+"' AND y='"+signLoc.getY()+"' AND z='"+signLoc.getZ()+"'";
									try {
										WebAuction.manageMySQL.insertQuery(queryRemove);
									} catch (MalformedURLException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (InstantiationException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (IllegalAccessException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
			    				}
			    				shoutRemove.clear();
							    for (int j = 0; j < playerList.length; j++) {
									if (inRange[j] == true)
									{
										playerList[j].sendMessage(WebAuction.logPrefix + "New Auction: "+stack.getAmount()+" "+stack.getType().toString()+" selling for "+formattedPrice+" each.");
									}
								}
							}
						}
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
				}
			}, 1 * 90L, 1 * 90L);
		}catch (Exception e){
			e.printStackTrace();
		}
		try{
			this.getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {

				public void run() {
					String query2 = "SELECT id FROM WA_Auctions ORDER BY id DESC";
					ResultSet result2 = null;
					ResultSet resultTest = null;
					int resultLength = 0;
					try {
						result2 = WebAuction.manageMySQL.sqlQuery(query2);
						resultTest = result2;
						
					} catch (Exception e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					try {
						if (resultTest != null){
							resultTest.beforeFirst();  
							resultTest.last();  
							resultLength = resultTest.getRow(); 
							auctionCount = resultLength;
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						//e.printStackTrace();
					}
					Iterator iterator2 = recentSigns.keySet().iterator();
				    while (iterator2.hasNext()) {
				    	Location key = (Location) iterator2.next();
				    	int theInt = recentSigns.get(key);
				    	if (theInt <= resultLength){
	    				String recentQuery = "SELECT * FROM WA_Auctions ORDER BY id DESC;";
	    				ResultSet result = null;
	    				try{
	    					result = WebAuction.manageMySQL.sqlQuery(recentQuery);
	    					int counter = 0;
	    					while (counter < theInt)
	    					{
	    					  if (result.next()) {
	    						counter++;
	    					  }
	    					}
	    					ItemStack stack = new ItemStack(result.getInt("name"), result.getInt("quantity"), result.getShort("damage"));
	    					int quant = result.getInt("quantity");
	    					Double price = result.getDouble("price");
	    					String formattedPrice = economy.format(price);
	    					int blockMatCheck = key.getBlock().getTypeId();
	    					if ((blockMatCheck == 63)||(blockMatCheck == 68))
	    					{
	    						Sign thisSign =  (Sign) key.getBlock().getState();
		    					thisSign.setLine(1, stack.getType().toString());
		    					thisSign.setLine(2, quant+"");
		    					thisSign.setLine(3, ""+formattedPrice);
	    						thisSign.update();
	    					}else{
	    						recentRemove.add(key);
	    					}
	    				} catch (Exception e) {
	    					// TODO Auto-generated catch block
	    					e.printStackTrace();
	    				}
				    	}else{
				    		int blockMatCheck = key.getBlock().getTypeId();
	    					if ((blockMatCheck == 63)||(blockMatCheck == 68))
	    					{
	    						Sign thisSign =  (Sign) key.getBlock().getState();
		    					thisSign.setLine(1, "Recent");
		    					thisSign.setLine(2, theInt+"");
		    					thisSign.setLine(3, "Not Available");
	    						thisSign.update();
	    					}else{
	    						recentRemove.add(key);
	    					}
				    	}
				    }
				    Iterator it = recentRemove.iterator();
    				while (it.hasNext()){
    					Location signLoc = (Location) it.next();
    					recentSigns.remove(signLoc);
    					String queryRemove = "DELETE FROM WA_RecentSigns WHERE world='"+signLoc.getWorld().getName()+"' AND x='"+signLoc.getX()+"' AND y='"+signLoc.getY()+"' AND z='"+signLoc.getZ()+"'";
						try {
							WebAuction.manageMySQL.insertQuery(queryRemove);
						} catch (MalformedURLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InstantiationException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IllegalAccessException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
    				}
    				recentRemove.clear();
    				
				}
			}, 1 * 160L, 1 * 160L);
		}catch (Exception e){
			//e.printStackTrace();
		}
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.SIGN_CHANGE, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLUGIN_ENABLE, new server(this), Priority.Monitor, this);
        pm.registerEvent(Event.Type.PLUGIN_DISABLE, new server(this), Priority.Monitor, this);
	}
	public void onDisable() {
		manageMySQL.close();
	}
	
	private Boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }
	
    private Boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }
}
