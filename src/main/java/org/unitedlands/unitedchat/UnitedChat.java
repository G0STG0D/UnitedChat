package org.unitedlands.unitedchat;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.unitedchat.commands.ChatToggleCommand;
import org.unitedlands.unitedchat.hooks.Placeholders;
import org.unitedlands.unitedchat.managers.BroadcastManager;
import org.unitedlands.unitedchat.managers.ChatMessageManager;
import org.unitedlands.unitedchat.managers.ChatSettingsManager;
import org.unitedlands.unitedchat.managers.QuizManager;
import org.unitedlands.unitedchat.listeners.PlayerListener;
import org.unitedlands.unitedchat.commands.tabcompleters.ChatToggleCommandCompleter;
import org.unitedlands.unitedchat.utils.Config;


public class UnitedChat extends JavaPlugin {

    private ChatMessageManager chatMessageManager;
    public ChatMessageManager getChatMessageManager() {
        return chatMessageManager;
    }

    private ChatSettingsManager chatSettingsManager;
    public ChatSettingsManager getChatSettingsManager() {
        return chatSettingsManager;
    }

    private QuizManager quizManager;
    public QuizManager getQuizManager() {
        return quizManager;
    }

    private Economy economy;
    public Economy getEconomy() {
        return economy;
    }

    @Override
    public void onEnable() {

        chatMessageManager  = new ChatMessageManager();
        chatSettingsManager = new ChatSettingsManager(this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().warning("[Exception] PlaceholderAPI is required!");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        new Placeholders(this, chatSettingsManager).register();

        var vaultRsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (vaultRsp != null)
            economy = vaultRsp.getProvider();
        else
            getLogger().warning("Vault economy not found! Quiz payouts disabled.");

        saveDefaultConfig();

        if (!getDataFolder().toPath().resolve("quiz/questions.yml").toFile().exists())
            saveResource("quiz/questions.yml", false);
        if (!getDataFolder().toPath().resolve("quiz/words.txt").toFile().exists())
            saveResource("quiz/words.txt", false);
        Config.load(getConfig());

        quizManager = new QuizManager(this);
        quizManager.start();

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        var chatToggle = new ChatToggleCommand(this);
        getCommand("unitedchat").setExecutor(chatToggle);
        getCommand("unitedchat").setTabCompleter(new ChatToggleCommandCompleter(this));

        var intervalTicks = Config.get().broadcastInterval() * 60 * 20;
        new BroadcastManager(this).runTaskTimer(this, intervalTicks, intervalTicks);

        getLogger().info("UnitedChat initialized.");
    }

}