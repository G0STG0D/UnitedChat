package org.unitedlands.unitedchat.listeners;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.unitedlands.unitedchat.utils.Config;
import org.unitedlands.unitedchat.utils.Formatter;
import org.unitedlands.unitedchat.UnitedChat;

import com.palmergames.bukkit.TownyChat.events.AsyncChatHookEvent;

import java.time.Duration;
import java.util.Objects;

public class PlayerListener implements Listener {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Formatter formatter = new Formatter();
    private final UnitedChat plugin;

    public PlayerListener(UnitedChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var cfg    = Config.get();

        var titleText    = player.hasPlayedBefore() ? cfg.joinTitle()    : cfg.firstJoinTitle();
        var subtitleText = player.hasPlayedBefore() ? cfg.joinSubtitle() : cfg.firstJoinSubtitle();

        var title = Title.title(
                miniMessage.deserialize(PlaceholderAPI.setPlaceholders(player, titleText)),
                miniMessage.deserialize(PlaceholderAPI.setPlaceholders(player, subtitleText)),
                Title.Times.times(
                        Duration.ofMillis(cfg.titleFadeIn() * 50L),
                        Duration.ofMillis(cfg.titleStay() * 50L),
                        Duration.ofMillis(cfg.titleFadeOut() * 50L)
                )
        );
        player.showTitle(title);

        Bukkit.getScheduler().runTask(plugin, () -> plugin.getQuizManager().sendQuizQuestionTo(event.getPlayer()));
    }

    @EventHandler
    public void onChat(AsyncChatHookEvent event) {

        Player player  = event.getPlayer();
        String message = event.getMessage().replace("§f", ""); // remove weird color

        if (plugin.getQuizManager().isActive() && Config.get().quizChannels().contains(event.getChannel().getName().toLowerCase()))
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> plugin.getQuizManager().checkAnswer(player, message), 2L);

        String finalizedMessage = formatter.finalizeMessage(player, message);
        String channelName = event.getChannel().getName().toLowerCase();

        String chatcolour = Objects.requireNonNull(LuckPermsProvider.get().getUserManager()
                .getUser(player.getUniqueId())).getCachedData().getMetaData().getMetaValue("chatcolour");

        // If player has an equipped chat colour from UnitedCosmetics (defined by LuckPerms meta), apply it.
        // Only send to global chat.
        if (channelName.equals("general") && chatcolour != null && !chatcolour.isEmpty()) {
            event.setMessage(formatter.colorMessage(chatcolour + finalizedMessage));
            return;
        }

        // Fallback to normal chat.
        event.setMessage(formatter.colorMessage(finalizedMessage));
    }

}
