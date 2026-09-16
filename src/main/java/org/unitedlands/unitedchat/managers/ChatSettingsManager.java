package org.unitedlands.unitedchat.managers;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.unitedlands.unitedchat.UnitedChat;
import org.unitedlands.unitedchat.player.ChatFeature;

public class ChatSettingsManager {

    private final UnitedChat plugin;

    public ChatSettingsManager(UnitedChat plugin) {
        this.plugin = plugin;
    }

    public String getGradient(Player player) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (!pdc.has(getKey("gradient"))) {
            return null;
        }
        return pdc.get(getKey("gradient"), PersistentDataType.STRING);
    }

    public void setQuizEnabled(Player player, boolean toggle) {
        player.getPersistentDataContainer().set(getKey("quiz-enabled"), PersistentDataType.BOOLEAN, toggle);
    }

    public boolean isQuizEnabled(Player player) {
        var pdc = player.getPersistentDataContainer();
        if (!pdc.has(getKey("quiz-enabled"))) {
            return true;
        }

        return pdc.get(getKey("quiz-enabled"), PersistentDataType.BOOLEAN);
    }

    public void toggleChatFeature(Player player, ChatFeature feature, boolean toggle) {
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, feature.toString());
        if (toggle) {
            pdc.set(key, PersistentDataType.INTEGER, 1);
        } else {
            pdc.set(key, PersistentDataType.INTEGER, 0);
        }
    }

    public void removeKey(Player player, String name) {
        NamespacedKey key = getKey(name);
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (pdc.has(key))
            pdc.remove(key);
    }

    public void setKeyValue(Player player, String name, String value) {
        NamespacedKey key = getKey(name);
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        pdc.set(key, PersistentDataType.STRING, value);
    }

    public String getKeyValue(Player player, String name) {
        NamespacedKey key = getKey(name);
        PersistentDataContainer pdc = player.getPersistentDataContainer();
        if (!pdc.has(key))
            return "";
        return pdc.get(key, PersistentDataType.STRING);
    }

    private NamespacedKey getKey(String name) {
        return new NamespacedKey(plugin, name);
    }

}
