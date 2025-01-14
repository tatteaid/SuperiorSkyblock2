package com.bgsoftware.superiorskyblock.core.database.bridge;

import com.bgsoftware.superiorskyblock.api.data.DatabaseBridge;
import com.bgsoftware.superiorskyblock.api.data.DatabaseBridgeMode;
import com.bgsoftware.superiorskyblock.api.data.DatabaseFilter;
import com.bgsoftware.superiorskyblock.api.missions.Mission;
import com.bgsoftware.superiorskyblock.api.objects.Pair;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
public class PlayersDatabaseBridge {

    private static final Map<UUID, Map<FutureSave, Set<Object>>> SAVE_METHODS_TO_BE_EXECUTED = new ConcurrentHashMap<>();

    private PlayersDatabaseBridge() {
    }

    public static void saveTextureValue(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.updateObject("players",
                createFilter("uuid", superiorPlayer),
                new Pair<>("last_used_skin", superiorPlayer.getTextureValue())
        ));
    }

    public static void savePlayerName(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.updateObject("players",
                createFilter("uuid", superiorPlayer),
                new Pair<>("last_used_name", superiorPlayer.getName())
        ));
    }

    public static void saveUserLocale(SuperiorPlayer superiorPlayer) {
        Locale userLocale = superiorPlayer.getUserLocale();
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.updateObject("players_settings",
                createFilter("player", superiorPlayer),
                new Pair<>("language", userLocale.getLanguage() + "-" + userLocale.getCountry())
        ));
    }

    public static void saveToggledBorder(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.updateObject("players_settings",
                createFilter("player", superiorPlayer),
                new Pair<>("toggled_border", superiorPlayer.hasWorldBorderEnabled())
        ));
    }

    public static void saveDisbands(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.updateObject("players",
                createFilter("uuid", superiorPlayer),
                new Pair<>("disbands", superiorPlayer.getDisbands())
        ));
    }

    public static void saveToggledPanel(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.updateObject("players_settings",
                createFilter("player", superiorPlayer),
                new Pair<>("toggled_panel", superiorPlayer.hasToggledPanel())
        ));
    }

    public static void saveIslandFly(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.updateObject("players_settings",
                createFilter("player", superiorPlayer),
                new Pair<>("island_fly", superiorPlayer.hasIslandFlyEnabled())
        ));
    }

    public static void saveBorderColor(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.updateObject("players_settings",
                createFilter("player", superiorPlayer),
                new Pair<>("border_color", superiorPlayer.getBorderColor().name())
        ));
    }

    public static void saveLastTimeStatus(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.updateObject("players",
                createFilter("uuid", superiorPlayer),
                new Pair<>("last_time_updated", superiorPlayer.getLastTimeStatus())
        ));
    }

    public static void saveMission(SuperiorPlayer superiorPlayer, Mission<?> mission, int finishCount) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.insertObject("players_missions",
                new Pair<>("player", superiorPlayer.getUniqueId().toString()),
                new Pair<>("name", mission.getName().toLowerCase(Locale.ENGLISH)),
                new Pair<>("finish_count", finishCount)
        ));
    }

    public static void removeMission(SuperiorPlayer superiorPlayer, Mission<?> mission) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.deleteObject("players_missions",
                createFilter("player", superiorPlayer, new Pair<>("name", mission.getName().toLowerCase(Locale.ENGLISH)))
        ));
    }

    public static void savePersistentDataContainer(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> databaseBridge.insertObject("players_custom_data",
                new Pair<>("player", superiorPlayer.getUniqueId().toString()),
                new Pair<>("data", superiorPlayer.getPersistentDataContainer().serialize())
        ));
    }

    public static void removePersistentDataContainer(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge ->
                databaseBridge.deleteObject("players_custom_data", createFilter("player", superiorPlayer)));
    }

    public static void insertPlayer(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> {
            Locale userLocale = superiorPlayer.getUserLocale();

            databaseBridge.insertObject("players",
                    new Pair<>("uuid", superiorPlayer.getUniqueId().toString()),
                    new Pair<>("last_used_name", superiorPlayer.getName()),
                    new Pair<>("last_used_skin", superiorPlayer.getTextureValue()),
                    new Pair<>("disbands", superiorPlayer.getDisbands()),
                    new Pair<>("last_time_updated", superiorPlayer.getLastTimeStatus())
            );

            databaseBridge.insertObject("players_settings",
                    new Pair<>("player", superiorPlayer.getUniqueId().toString()),
                    new Pair<>("language", userLocale.getLanguage() + "-" + userLocale.getCountry()),
                    new Pair<>("toggled_panel", superiorPlayer.hasToggledPanel()),
                    new Pair<>("border_color", superiorPlayer.getBorderColor().name()),
                    new Pair<>("toggled_border", superiorPlayer.hasWorldBorderEnabled()),
                    new Pair<>("island_fly", superiorPlayer.hasIslandFlyEnabled())
            );
        });
    }

    public static void updatePlayer(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> {
            Locale userLocale = superiorPlayer.getUserLocale();
            databaseBridge.updateObject("players",
                    createFilter("uuid", superiorPlayer),
                    new Pair<>("last_used_name", superiorPlayer.getName()),
                    new Pair<>("last_used_skin", superiorPlayer.getTextureValue()),
                    new Pair<>("disbands", superiorPlayer.getDisbands()),
                    new Pair<>("last_time_updated", superiorPlayer.getLastTimeStatus())
            );

            databaseBridge.updateObject("players_custom_data",
                    createFilter("player", superiorPlayer),
                    new Pair<>("data", superiorPlayer.getPersistentDataContainer().serialize())
            );

            databaseBridge.updateObject("players_settings",
                    createFilter("player", superiorPlayer),
                    new Pair<>("language", userLocale.getLanguage() + "-" + userLocale.getCountry()),
                    new Pair<>("toggled_panel", superiorPlayer.hasToggledPanel()),
                    new Pair<>("border_color", superiorPlayer.getBorderColor().name()),
                    new Pair<>("toggled_border", superiorPlayer.hasWorldBorderEnabled()),
                    new Pair<>("island_fly", superiorPlayer.hasIslandFlyEnabled())
            );

            databaseBridge.deleteObject("players_missions", createFilter("player", superiorPlayer));

            for (Map.Entry<Mission<?>, Integer> missionEntry : superiorPlayer.getCompletedMissionsWithAmounts().entrySet()) {
                saveMission(superiorPlayer, missionEntry.getKey(), missionEntry.getValue());
            }
        });
    }

    public static void deletePlayer(SuperiorPlayer superiorPlayer) {
        runOperationIfRunning(superiorPlayer.getDatabaseBridge(), databaseBridge -> {
            DatabaseFilter playerFilter = createFilter("player", superiorPlayer);
            databaseBridge.deleteObject("players", createFilter("uuid", superiorPlayer));
            databaseBridge.deleteObject("players_custom_data", playerFilter);
            databaseBridge.deleteObject("players_settings", playerFilter);
            databaseBridge.deleteObject("players_missions", playerFilter);
        });
    }

    public static void markPersistentDataContainerToBeSaved(SuperiorPlayer superiorPlayer) {
        Set<Object> varsForPersistentData = SAVE_METHODS_TO_BE_EXECUTED.computeIfAbsent(superiorPlayer.getUniqueId(), u -> new EnumMap<>(FutureSave.class))
                .computeIfAbsent(FutureSave.PERSISTENT_DATA, e -> new HashSet<>());
        if (varsForPersistentData.isEmpty())
            varsForPersistentData.add(new Object());
    }

    public static boolean isModified(SuperiorPlayer superiorPlayer) {
        return SAVE_METHODS_TO_BE_EXECUTED.containsKey(superiorPlayer.getUniqueId());
    }

    public static void executeFutureSaves(SuperiorPlayer superiorPlayer) {
        Map<FutureSave, Set<Object>> futureSaves = SAVE_METHODS_TO_BE_EXECUTED.remove(superiorPlayer.getUniqueId());
        if (futureSaves != null) {
            for (Map.Entry<FutureSave, Set<Object>> futureSaveEntry : futureSaves.entrySet()) {
                switch (futureSaveEntry.getKey()) {
                    case PERSISTENT_DATA: {
                        if (superiorPlayer.isPersistentDataContainerEmpty())
                            removePersistentDataContainer(superiorPlayer);
                        else
                            savePersistentDataContainer(superiorPlayer);
                        break;
                    }
                }
            }
        }
    }

    public static void executeFutureSaves(SuperiorPlayer superiorPlayer, FutureSave futureSave) {
        Map<FutureSave, Set<Object>> futureSaves = SAVE_METHODS_TO_BE_EXECUTED.get(superiorPlayer.getUniqueId());

        if (futureSaves == null)
            return;

        Set<Object> values = futureSaves.remove(futureSave);

        if (values == null)
            return;

        if (futureSaves.isEmpty())
            SAVE_METHODS_TO_BE_EXECUTED.remove(superiorPlayer.getUniqueId());

        switch (futureSave) {
            case PERSISTENT_DATA: {
                if (superiorPlayer.isPersistentDataContainerEmpty())
                    removePersistentDataContainer(superiorPlayer);
                else
                    savePersistentDataContainer(superiorPlayer);
                break;
            }
        }
    }

    private static DatabaseFilter createFilter(String id, SuperiorPlayer superiorPlayer, Pair<String, Object>... others) {
        List<Pair<String, Object>> filters = new LinkedList<>();
        filters.add(new Pair<>(id, superiorPlayer.getUniqueId().toString()));
        if (others != null)
            filters.addAll(Arrays.asList(others));
        return DatabaseFilter.fromFilters(filters);
    }

    private static void runOperationIfRunning(DatabaseBridge databaseBridge, Consumer<DatabaseBridge> databaseBridgeConsumer) {
        if (databaseBridge.getDatabaseBridgeMode() == DatabaseBridgeMode.SAVE_DATA)
            databaseBridgeConsumer.accept(databaseBridge);
    }

    public enum FutureSave {

        PERSISTENT_DATA

    }

}
