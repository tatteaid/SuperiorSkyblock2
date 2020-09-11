package com.bgsoftware.superiorskyblock.commands;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.bgsoftware.superiorskyblock.api.upgrades.Upgrade;
import com.bgsoftware.superiorskyblock.api.upgrades.UpgradeLevel;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.hooks.PlaceholderHook;
import com.bgsoftware.superiorskyblock.upgrades.SUpgradeLevel;
import com.bgsoftware.superiorskyblock.utils.StringUtils;
import com.bgsoftware.superiorskyblock.utils.events.EventResult;
import com.bgsoftware.superiorskyblock.utils.events.EventsCaller;
import com.bgsoftware.superiorskyblock.utils.islands.IslandPrivileges;
import com.bgsoftware.superiorskyblock.wrappers.player.SSuperiorPlayer;
import com.bgsoftware.superiorskyblock.wrappers.SoundWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CmdRankup implements ISuperiorCommand {

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("rankup");
    }

    @Override
    public String getPermission() {
        return "superior.island.rankup";
    }

    @Override
    public String getUsage(java.util.Locale locale) {
        return "rankup <" + Locale.COMMAND_ARGUMENT_UPGRADE_NAME.getMessage(locale) + ">";
    }

    @Override
    public String getDescription(java.util.Locale locale) {
        return Locale.COMMAND_DESCRIPTION_RANKUP.getMessage(locale);
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public boolean canBeExecutedByConsole() {
        return false;
    }

    @Override
    public void execute(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        SuperiorPlayer superiorPlayer = SSuperiorPlayer.of(sender);
        Island island = superiorPlayer.getIsland();

        if(island == null){
            Locale.INVALID_ISLAND.send(superiorPlayer);
            return;
        }

        if(!superiorPlayer.hasPermission(IslandPrivileges.RANKUP)){
            Locale.NO_RANKUP_PERMISSION.send(superiorPlayer, island.getRequiredPlayerRole(IslandPrivileges.RANKUP));
            return;
        }

        String upgradeName = args[1];

        if(!plugin.getUpgrades().isUpgrade(upgradeName)){
            Locale.INVALID_UPGRADE.send(superiorPlayer, upgradeName, StringUtils.getUpgradesString(plugin));
            return;
        }

        Upgrade upgrade = plugin.getUpgrades().getUpgrade(upgradeName);
        UpgradeLevel upgradeLevel = island.getUpgradeLevel(upgrade), nextUpgradeLevel = upgrade.getUpgradeLevel(upgradeLevel.getLevel() + 1);

        String permission = nextUpgradeLevel == null ? "" : nextUpgradeLevel.getPermission();

        if(!permission.isEmpty() && !superiorPlayer.hasPermission(permission)){
            Locale.NO_UPGRADE_PERMISSION.send(superiorPlayer);
            return;
        }

        String requiredCheckFailure = nextUpgradeLevel == null ? "" : nextUpgradeLevel.checkRequirements(superiorPlayer);

        boolean hasNextLevel;

        if(!requiredCheckFailure.isEmpty()){
            Locale.sendMessage(superiorPlayer, requiredCheckFailure, false);
            hasNextLevel = false;
        }

        else {
            EventResult<Pair<List<String>, Double>> event = EventsCaller.callIslandUpgradeEvent(superiorPlayer, island, upgradeName, upgradeLevel.getCommands(), upgradeLevel.getPrice());

            double nextUpgradePrice = event.getResult().getValue();

            if (event.isCancelled()) {
                hasNextLevel = false;
            } else if(plugin.getProviders().getMoneyInBank(superiorPlayer) < nextUpgradePrice){
                Locale.NOT_ENOUGH_MONEY_TO_UPGRADE.send(superiorPlayer);
                hasNextLevel = false;
            } else {
                if (nextUpgradePrice > 0)
                    plugin.getProviders().withdrawMoney(superiorPlayer, nextUpgradePrice);

                for (String command : event.getResult().getKey()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderHook.parse(superiorPlayer, command
                            .replace("%player%", superiorPlayer.getName())
                            .replace("%leader%", island.getOwner().getName()))
                    );
                }

                hasNextLevel = true;
            }
        }

        SUpgradeLevel.ItemData itemData = ((SUpgradeLevel) upgradeLevel).getItemData();
        SoundWrapper sound = hasNextLevel ? itemData.hasNextLevelSound : itemData.noNextLevelSound;

        if(sound != null)
            sound.playSound(superiorPlayer.asPlayer());
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        SuperiorPlayer superiorPlayer = SSuperiorPlayer.of(sender);
        Island island = superiorPlayer.getIsland();

        if(args.length == 2 && island != null && superiorPlayer.hasPermission(IslandPrivileges.RANKUP)){
            List<String> list = new ArrayList<>();

            for(Upgrade upgrade : plugin.getUpgrades().getUpgrades()){
                if(upgrade.getName().toLowerCase().contains(args[1].toLowerCase()))
                    list.add(upgrade.getName().toLowerCase());
            }

            return list;
        }

        return new ArrayList<>();
    }

}
