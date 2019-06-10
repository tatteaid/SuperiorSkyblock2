package com.bgsoftware.superiorskyblock.handlers;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.gui.MenuTemplate;
import com.bgsoftware.superiorskyblock.gui.menus.Menu;
import com.bgsoftware.superiorskyblock.gui.menus.types.statistics.TopIslandsMenu;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public final class MenuHandler {

    private Map<UUID, Menu> menus;

    public MenuHandler(SuperiorSkyblockPlugin plugin) {
        menus = new HashMap<>();
    }

    public void load() {
        MenuTemplate.loadAll();
        //TopIslandsMenu.staticLoad();
    }

    public void save() {
        // TODO close menus

        for (Menu menu : new HashSet<>(menus.values())) {
            for (HumanEntity viewer : new HashSet<>(menu.getInventory().getViewers())) {
                viewer.closeInventory();
            }
        }
    }

    public Map<UUID, Menu> getMenus() {
        return menus;
    }
}
