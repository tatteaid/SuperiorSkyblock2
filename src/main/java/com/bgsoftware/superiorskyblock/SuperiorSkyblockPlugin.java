package com.bgsoftware.superiorskyblock;

import com.bgsoftware.superiorskyblock.api.SuperiorSkyblock;
import com.bgsoftware.superiorskyblock.api.SuperiorSkyblockAPI;
import com.bgsoftware.superiorskyblock.api.handlers.MenusManager;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandFlag;
import com.bgsoftware.superiorskyblock.api.island.IslandPermission;
import com.bgsoftware.superiorskyblock.api.island.SortingType;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.handlers.CommandsHandler;
import com.bgsoftware.superiorskyblock.grid.WorldGenerator;
import com.bgsoftware.superiorskyblock.handlers.BlockValuesHandler;
import com.bgsoftware.superiorskyblock.handlers.DataHandler;
import com.bgsoftware.superiorskyblock.handlers.GridHandler;
import com.bgsoftware.superiorskyblock.handlers.KeysHandler;
import com.bgsoftware.superiorskyblock.handlers.MenusHandler;
import com.bgsoftware.superiorskyblock.handlers.MissionsHandler;
import com.bgsoftware.superiorskyblock.handlers.PlayersHandler;
import com.bgsoftware.superiorskyblock.handlers.ProvidersHandler;
import com.bgsoftware.superiorskyblock.handlers.SchematicsHandler;
import com.bgsoftware.superiorskyblock.handlers.SettingsHandler;
import com.bgsoftware.superiorskyblock.handlers.UpgradesHandler;
import com.bgsoftware.superiorskyblock.hooks.ProtocolLibHook;
import com.bgsoftware.superiorskyblock.island.SIsland;
import com.bgsoftware.superiorskyblock.listeners.BlocksListener;
import com.bgsoftware.superiorskyblock.listeners.ChunksListener;
import com.bgsoftware.superiorskyblock.listeners.CustomEventsListener;
import com.bgsoftware.superiorskyblock.listeners.GeneratorsListener;
import com.bgsoftware.superiorskyblock.listeners.MenusListener;
import com.bgsoftware.superiorskyblock.listeners.PlayersListener;
import com.bgsoftware.superiorskyblock.listeners.ProtectionListener;
import com.bgsoftware.superiorskyblock.listeners.SettingsListener;
import com.bgsoftware.superiorskyblock.listeners.UpgradesListener;
import com.bgsoftware.superiorskyblock.metrics.Metrics;
import com.bgsoftware.superiorskyblock.nms.NMSAdapter;
import com.bgsoftware.superiorskyblock.nms.NMSBlocks;
import com.bgsoftware.superiorskyblock.nms.NMSTags;
import com.bgsoftware.superiorskyblock.tasks.CalcTask;
import com.bgsoftware.superiorskyblock.utils.chunks.ChunksProvider;
import com.bgsoftware.superiorskyblock.tasks.CropsTask;
import com.bgsoftware.superiorskyblock.utils.exceptions.HandlerLoadException;
import com.bgsoftware.superiorskyblock.utils.islands.SortingComparators;
import com.bgsoftware.superiorskyblock.utils.items.EnchantsUtils;
import com.bgsoftware.superiorskyblock.utils.threads.Executor;
import com.bgsoftware.superiorskyblock.wrappers.SSuperiorPlayer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public final class SuperiorSkyblockPlugin extends JavaPlugin implements SuperiorSkyblock {

    private static SuperiorSkyblockPlugin plugin;

    private GridHandler gridHandler = null;
    private BlockValuesHandler blockValuesHandler = null;
    private SchematicsHandler schematicsHandler = null;
    private PlayersHandler playersHandler = null;
    private MissionsHandler missionsHandler = null;
    private MenusHandler menusHandler = null;
    private KeysHandler keysHandler = null;
    private ProvidersHandler providersHandler = null;
    private UpgradesHandler upgradesHandler = null;
    private CommandsHandler commandsHandler = null;

    private SettingsHandler settingsHandler = null;
    private DataHandler dataHandler = null;

    private NMSAdapter nmsAdapter;
    private NMSTags nmsTags;
    private NMSBlocks nmsBlocks;

    @Override
    public void onEnable() {
        plugin = this;
        new Metrics(this);

        getServer().getPluginManager().registerEvents(new BlocksListener(this), this);
        getServer().getPluginManager().registerEvents(new ChunksListener(this), this);
        getServer().getPluginManager().registerEvents(new CustomEventsListener(this), this);
        getServer().getPluginManager().registerEvents(new GeneratorsListener(this), this);
        getServer().getPluginManager().registerEvents(new MenusListener(), this);
        getServer().getPluginManager().registerEvents(new PlayersListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new SettingsListener(this), this);
        getServer().getPluginManager().registerEvents(new UpgradesListener(this), this);

        loadNMSAdapter();
        loadAPI();
        Executor.init(this);

        loadSortingTypes();
        loadIslandFlags();

        EnchantsUtils.registerGlowEnchantment();

        loadWorld();

        reloadPlugin(true);

        if (Updater.isOutdated()) {
            log("");
            log("A new version is available (v" + Updater.getLatestVersion() + ")!");
            log("Version's description: \"" + Updater.getVersionDescription() + "\"");
            log("");
        }

        ChunksProvider.init();

        Executor.sync(() -> {
            for(Player player : Bukkit.getOnlinePlayers()){
                SuperiorPlayer superiorPlayer = SSuperiorPlayer.of(player);
                superiorPlayer.updateLastTimeStatus();
                Island island = gridHandler.getIslandAt(superiorPlayer.getLocation());
                Island playerIsland = superiorPlayer.getIsland();

                if(superiorPlayer.hasIslandFlyEnabled()){
                    if(island != null && island.hasPermission(superiorPlayer, IslandPermission.FLY)){
                        player.setAllowFlight(true);
                        player.setFlying(true);
                    }else{
                        superiorPlayer.toggleIslandFly();
                    }
                }

                if(playerIsland != null){
                    ((SIsland) playerIsland).setLastTimeUpdate(-1);
                }

                if(island != null)
                    island.setPlayerInside(superiorPlayer, true);
            }

            CropsTask.startTask();
        }, 1L);
    }

    @Override
    public void onDisable() {
        if(Bukkit.getPluginManager().isPluginEnabled("ProtocolLib"))
            ProtocolLibHook.disable(this);

        ChunksProvider.stop();
        CropsTask.cancelTask();
        try {
            dataHandler.saveDatabase(false);
            missionsHandler.saveMissionsData();

            for(Island island : gridHandler.getIslandsToPurge())
                island.disbandIsland();

            for (Player player : Bukkit.getOnlinePlayers()) {
                player.closeInventory();
                SuperiorPlayer superiorPlayer = SSuperiorPlayer.of(player);
                superiorPlayer.updateLastTimeStatus();
                Island playerIsland = superiorPlayer.getIsland();
                if(playerIsland != null){
                    playerIsland.updateLastTime();
                }
                nmsAdapter.setWorldBorder(superiorPlayer, null);
                if (superiorPlayer.hasIslandFlyEnabled()) {
                    player.setAllowFlight(false);
                    player.setFlying(false);
                }
            }

            CalcTask.cancelTask();
            Executor.close();
            dataHandler.closeConnection();
        }catch(Exception ignored){}
    }

    private void loadNMSAdapter(){
        String version = getServer().getClass().getPackage().getName().split("\\.")[3];
        try {
            nmsAdapter = (NMSAdapter) Class.forName("com.bgsoftware.superiorskyblock.nms.NMSAdapter_" + version).newInstance();
            nmsTags = (NMSTags) Class.forName("com.bgsoftware.superiorskyblock.nms.NMSTags_" + version).newInstance();
            nmsBlocks = (NMSBlocks) Class.forName("com.bgsoftware.superiorskyblock.nms.NMSBlocks_" + version).newInstance();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    private void loadWorld(){
        String worldName = (settingsHandler = new SettingsHandler(this)).islandWorldName;
        loadWorld(worldName, World.Environment.NORMAL);
        if(settingsHandler.netherWorldEnabled)
            loadWorld(worldName + "_nether", World.Environment.NETHER);
        if(settingsHandler.endWorldEnabled)
            loadWorld(worldName + "_the_end", World.Environment.THE_END);
    }

    private void loadWorld(String worldName, World.Environment environment){
        WorldCreator.name(worldName).type(WorldType.FLAT).environment(environment).generator(new WorldGenerator()).createWorld();

        if(getServer().getPluginManager().isPluginEnabled("Multiverse-Core")){
            getServer().dispatchCommand(getServer().getConsoleSender(), "mv import " + worldName + " normal -g " + getName());
            getServer().dispatchCommand(getServer().getConsoleSender(), "mv modify set generator " + getName() + " " + worldName);
        }
    }

    private void loadAPI(){
        try{
            Field plugin = SuperiorSkyblockAPI.class.getDeclaredField("plugin");
            plugin.setAccessible(true);
            plugin.set(null, this);
            plugin.setAccessible(false);
        }catch(Exception ignored){}
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) {
        return new WorldGenerator();
    }

    public void reloadPlugin(boolean loadGrid){
        Locale.reload();
        CalcTask.startTask();

        blockValuesHandler = new BlockValuesHandler(this);
        settingsHandler = new SettingsHandler(this);
        upgradesHandler = new UpgradesHandler(this);
        missionsHandler = new MissionsHandler(this);

        commandsHandler = new CommandsHandler(this, settingsHandler.islandCommand);
        nmsAdapter.registerCommand(commandsHandler);

        if(loadGrid) {
            playersHandler = new PlayersHandler();
            gridHandler = new GridHandler(this);
        }
        else{
            gridHandler.updateSpawn();
        }

        schematicsHandler = new SchematicsHandler(this);
        providersHandler = new ProvidersHandler(this);
        menusHandler = new MenusHandler(this);
        keysHandler = new KeysHandler(this);

        Executor.sync(() -> {
            if(gridHandler.getSpawnIsland().getCenter(World.Environment.NORMAL).getWorld() == null){
                new HandlerLoadException("The spawn location is in invalid world.", HandlerLoadException.ErrorLevel.PLUGIN_SHUTDOWN).printStackTrace();
                Bukkit.getPluginManager().disablePlugin(this);
                return;
            }

            if (loadGrid) {
                try {
                    dataHandler = new DataHandler(this);
                }catch(HandlerLoadException ex){
                    if(!HandlerLoadException.handle(ex))
                        return;
                }
            }

            for(Player player : Bukkit.getOnlinePlayers())
                nmsAdapter.setWorldBorder(SSuperiorPlayer.of(player), gridHandler.getIslandAt(player.getLocation()));
        });
    }

    private void loadSortingTypes(){
        SortingType.register("WORTH", SortingComparators.WORTH_COMPARATOR);
        SortingType.register("LEVEL", SortingComparators.LEVEL_COMPARATOR);
        SortingType.register("RATING", SortingComparators.RATING_COMPARATOR);
        SortingType.register("PLAYERS", SortingComparators.PLAYERS_COMPARATOR);
    }

    private void loadIslandFlags(){
        IslandFlag.register("ALWAYS_DAY");
        IslandFlag.register("ALWAYS_MIDDLE_DAY");
        IslandFlag.register("ALWAYS_NIGHT");
        IslandFlag.register("ALWAYS_MIDDLE_NIGHT");
        IslandFlag.register("ALWAYS_RAIN");
        IslandFlag.register("ALWAYS_SHINY");
        IslandFlag.register("CREEPER_EXPLOSION");
        IslandFlag.register("CROPS_GROWTH");
        IslandFlag.register("EGG_LAY");
        IslandFlag.register("ENDERMAN_GRIEF");
        IslandFlag.register("FIRE_SPREAD");
        IslandFlag.register("GHAST_FIREBALL");
        IslandFlag.register("LAVA_FLOW");
        IslandFlag.register("NATURAL_ANIMALS_SPAWN");
        IslandFlag.register("NATURAL_MONSTER_SPAWN");
        IslandFlag.register("PVP");
        IslandFlag.register("SPAWNER_ANIMALS_SPAWN");
        IslandFlag.register("SPAWNER_MONSTER_SPAWN");
        IslandFlag.register("TNT_EXPLOSION");
        IslandFlag.register("TREE_GROWTH");
        IslandFlag.register("WATER_FLOW");
        IslandFlag.register("WITHER_EXPLOSION");
    }

    @Override
    public GridHandler getGrid(){
        return gridHandler;
    }

    @Override
    public BlockValuesHandler getBlockValues() {
        return blockValuesHandler;
    }

    @Override
    public SchematicsHandler getSchematics() {
        return schematicsHandler;
    }

    @Override
    public PlayersHandler getPlayers() {
        return playersHandler;
    }

    @Override
    public MissionsHandler getMissions() {
        return missionsHandler;
    }

    @Override
    public MenusManager getMenus() {
        return menusHandler;
    }

    @Override
    public KeysHandler getKeys() {
        return keysHandler;
    }

    @Override
    public ProvidersHandler getProviders() {
        return providersHandler;
    }

    @Override
    public UpgradesHandler getUpgrades() {
        return upgradesHandler;
    }

    @Override
    public CommandsHandler getCommands() {
        return commandsHandler;
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public SettingsHandler getSettings() {
        return settingsHandler;
    }

    public NMSAdapter getNMSAdapter() {
        return nmsAdapter;
    }

    public NMSTags getNMSTags(){
        return nmsTags;
    }

    public NMSBlocks getNMSBlocks() {
        return nmsBlocks;
    }

    public static void log(String message){
        message = ChatColor.translateAlternateColorCodes('&', message);
        if(message.contains(ChatColor.COLOR_CHAR + ""))
            Bukkit.getConsoleSender().sendMessage(ChatColor.getLastColors(message.substring(0, 2)) + "[" + plugin.getDescription().getName() + "] " + message);
        else
            plugin.getLogger().info(message);
    }

    public static SuperiorSkyblockPlugin getPlugin(){
        return plugin;
    }

}
