package org.unitedlands.unitedchat.player;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.unitedlands.unitedchat.Formatter;
import org.unitedlands.unitedchat.UnitedChat;

import com.palmergames.bukkit.TownyChat.events.AsyncChatHookEvent;

import java.util.List;

public class PlayerListener implements Listener {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Formatter formatter = new Formatter();
    private final UnitedChat plugin;

    public PlayerListener(UnitedChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        FileConfiguration config = plugin.getConfig();
        List<String> firstMotd = config.getStringList("messages.first-join-motd");
        List<String> motd = config.getStringList("messages.motd");
        Player p = event.getPlayer();

        if (p.hasPlayedBefore()) {
            for (String s : motd) {
                p.sendMessage(miniMessage.deserialize(PlaceholderAPI.setPlaceholders(p, s)));
            }
        } else {
            for (String s : firstMotd) {
                p.sendMessage(miniMessage.deserialize(PlaceholderAPI.setPlaceholders(p, s)));
            }
        }
    }

    @EventHandler
    public void onChat(AsyncChatHookEvent event) {
        
        Player player = event.getPlayer();

        String message = event.getMessage();
        message = message.replace("§f", ""); // remove weird color

        String finalizedMessage = formatter.finalizeMessage(player, message);

        if (plugin.getChatSettingsManager().isGradientEnabled(player)) {
            event.setMessage(formatter.gradientMessage(finalizedMessage, plugin.getChatSettingsManager().getGradient(player)));
            return;
        }
        event.setMessage(formatter.colorMessage(finalizedMessage));
        event.setMessage(message);
    }

}
