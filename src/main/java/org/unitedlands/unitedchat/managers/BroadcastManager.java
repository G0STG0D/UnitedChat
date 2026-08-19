package org.unitedlands.unitedchat.managers;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.scheduler.BukkitRunnable;
import org.unitedlands.unitedchat.UnitedChat;
import org.unitedlands.unitedchat.utils.Config;

import java.util.List;
import java.util.Random;

@SuppressWarnings("unchecked")
public class BroadcastManager extends BukkitRunnable {

    private static final long QUIZ_GRACE_MS = 30_000L;

    private final UnitedChat plugin;
    private final List<List<String>> broadcasts;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Random random = new Random();

    private final String soundName;
    private final float soundVolume;
    private final float soundPitch;
    private final boolean playSound;

    public BroadcastManager(UnitedChat plugin) {
        this.plugin = plugin;
        this.broadcasts = loadBroadcasts();

        this.soundVolume = Config.get().broadcastSoundVolume();
        this.soundPitch = Config.get().broadcastSoundPitch();

        var configuredSound = Config.get().broadcastSoundName();

        if (configuredSound.isEmpty()) {
            this.soundName = null;
            this.playSound = false;
        } else {
            // Validate the sound name using the registry.
            NamespacedKey key = NamespacedKey.fromString(configuredSound);
            Sound foundSound = (key != null) ? Registry.SOUNDS.get(key) : null;
            if (foundSound != null) {
                this.soundName = configuredSound;
                this.playSound = true;
            } else {
                plugin.getLogger().warning("[BroadcastManager] Invalid sound name in config: '" + configuredSound + "'");
                this.soundName = null;
                this.playSound = false;
            }
        }
    }

    private List<List<String>> loadBroadcasts() {
        return Config.get().broadcasts().values().stream()
                .filter(value -> value instanceof List<?>)
                .map(value -> (List<String>) value)
                .toList();
    }

    @Override
    public void run() {
        if (broadcasts.isEmpty()) return;

        var quizManager = plugin.getQuizManager();
        if (quizManager.isActive()) return;
        if (quizManager.isScheduled() && quizManager.getNextQuizTime() - System.currentTimeMillis() <= QUIZ_GRACE_MS) return;

        List<String> broadcast = broadcasts.get(random.nextInt(broadcasts.size()));
        for (String line : broadcast) {
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.sendMessage(miniMessage.deserialize(line));
                if (playSound) {
                    player.playSound(player.getLocation(), soundName, SoundCategory.MASTER, soundVolume, soundPitch);
                }
            });
        }
    }
}
