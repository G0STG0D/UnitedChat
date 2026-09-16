package org.unitedlands.unitedchat.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.unitedlands.unitedchat.UnitedChat;
import org.unitedlands.unitedchat.managers.BroadcastManager;
import org.unitedlands.unitedchat.quiz.QuestionType;
import org.unitedlands.unitedchat.utils.Config;
import org.unitedlands.unitedchat.utils.Messages;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.component;
import static net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.unparsed;

import java.util.List;

public class ChatToggleCommand implements CommandExecutor {

    private final UnitedChat plugin;
    private final List<String> chatFeatures;

    public ChatToggleCommand(UnitedChat plugin) {
        this.plugin = plugin;
        chatFeatures = Config.get().features();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String @NotNull [] args) {

        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("united.chat.admin")) {
                sender.sendMessage(plugin.getChatMessageManager().getMessage(Messages.NO_PERM));
                return false;
            }

            plugin.reloadConfig();
            Config.reload(plugin.getConfig());

            // Reapply runtime configuration
            Bukkit.getScheduler().cancelTasks(plugin);
            var intervalTicks = Config.get().broadcastInterval() * 60 * 20;
            new BroadcastManager(plugin).runTaskTimer(plugin, intervalTicks, intervalTicks);
            plugin.getQuizManager().reload();

            sender.sendMessage(plugin.getChatMessageManager().getMessage(Messages.RELOADED));
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("quiz")) {
            if (args[1].equalsIgnoreCase("force")) {
                if (!sender.hasPermission("united.chat.admin")) {
                    sender.sendMessage(plugin.getChatMessageManager().getMessage(Messages.NO_PERM));
                    return true;
                }

                QuestionType type = null;
                if (args.length >= 3) {
                    try {
                        type = QuestionType.valueOf(args[2].toUpperCase());
                    }
                    catch (IllegalArgumentException ignored) {}
                }

                sender.sendMessage(plugin.getChatMessageManager().getMessage(Messages.QUIZ_FORCED));
                plugin.getQuizManager().forceNext(type);
                return true;
            }
            if (args[1].equalsIgnoreCase("toggle") && args.length == 3) {
                if (!sender.hasPermission("united.chat.admin")) {
                    sender.sendMessage(plugin.getChatMessageManager().getMessage(Messages.NO_PERM));
                    return true;
                }
                var on = args[2].equalsIgnoreCase("on");
                plugin.getQuizManager().setEnabled(on);
                sender.sendMessage(plugin.getChatMessageManager().getMessage(Messages.QUIZ_TOGGLED,
                        component("toggle", text(on ? "enabled" : "disabled"))));
                return true;
            }
            if (args[1].equalsIgnoreCase("highscore")) {
                var period = "alltime";
                var page = 1;

                if (args.length >= 3) {
                    if (args[2].equalsIgnoreCase("weekly") || args[2].equalsIgnoreCase("monthly") || args[2].equalsIgnoreCase("alltime")) {
                        period = args[2].toLowerCase();
                        if (args.length >= 4) page = parsePageArg(args[3]);
                    } else {
                        page = parsePageArg(args[2]);
                    }
                }

                plugin.getQuizManager().buildHighscore(period, page).forEach(sender::sendMessage);
                return true;
            }
            if (args[1].equalsIgnoreCase("next")) {
                var qm = plugin.getQuizManager();
                if (!qm.isEnabled()) {
                    sender.sendMessage(plugin.getChatMessageManager().getMessage(Messages.QUIZ_NEXT_DISABLED));
                } else if (qm.isActive()) {
                    sender.sendMessage(plugin.getChatMessageManager().getMessage(Messages.QUIZ_NEXT_ACTIVE));
                } else if (!qm.isScheduled()) {
                    sender.sendMessage(plugin.getChatMessageManager().getMessage(Messages.QUIZ_NEXT_NOT_RUNNING));
                } else {
                    var remaining = qm.getNextQuizTime() - System.currentTimeMillis();
                    sender.sendMessage(plugin.getChatMessageManager().getMessage(Messages.QUIZ_NEXT,
                            unparsed("time", formatCountdown(remaining))));
                }
                return true;
            }
        }

        if (!(sender instanceof Player player))
            return true;

        if (args.length == 0) {
            player.sendMessage(plugin.getChatMessageManager().getMessage(Messages.CHAT_TOGGLE_COMMAND));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("reset")) {
            handleReset(player);
            return true;
        }

        if (args.length == 3) {
            var feature = args[1];
            if (!chatFeatures.contains(feature)) {
                player.sendMessage(plugin.getChatMessageManager().getMessage(Messages.INVALID_FEATURE));
                return false;
            }

            boolean toggleOn = args[2].equalsIgnoreCase("on");

            if (feature.equalsIgnoreCase("quiz")) {
                player.sendMessage(plugin.getChatMessageManager().getMessage(Messages.TOGGLED_FEATURE,
                        component("feature", text("Quizzes")),
                        component("toggle", text(toggleOn ? "on" : "off")))
                );
                toggleQuiz(player, toggleOn);
                return true;
            }

            plugin.getChatSettingsManager().setKeyValue(player, feature, toggleOn ? "on" : "off");

            player.sendMessage(plugin.getChatMessageManager().getMessage(Messages.TOGGLED_FEATURE,
                    component("feature", text(feature)),
                    component("toggle", text(toggleOn ? "on" : "off"))));
            return true;
        }

        player.sendMessage(plugin.getChatMessageManager().getMessage(Messages.CHAT_TOGGLE_COMMAND));
        return true;
    }


    private String formatCountdown(long ms) {
        if (ms <= 0) return "soon";
        var totalSec = ms / 1000;
        var min      = totalSec / 60;
        var sec      = totalSec % 60;

        if (min > 0 && sec > 0)
            return min + "m " + sec + "s";

        if (min > 0)
            return min + "m";

        return sec + "s";
    }

    private int parsePageArg(String arg) {
        try { return Math.max(1, Integer.parseInt(arg)); } catch (NumberFormatException e) { return 1; }
    }

    private void toggleQuiz(Player player, Boolean toggleOn) {
        plugin.getChatSettingsManager().setQuizEnabled(player, toggleOn);
        plugin.getQuizManager().toggleQuizFor(player, toggleOn);
    }

    private void handleReset(Player player) {
        plugin.getChatSettingsManager().removeKey(player, "chatrank");
        plugin.getChatSettingsManager().removeKey(player, "chatprefix");
        plugin.getChatSettingsManager().removeKey(player, "tabprefix");
        plugin.getChatSettingsManager().removeKey(player, "chatnation");
        plugin.getChatSettingsManager().removeKey(player, "tabnation");
        plugin.getChatSettingsManager().removeKey(player, "chattown");
        plugin.getChatSettingsManager().removeKey(player, "tabtown");
        plugin.getChatSettingsManager().removeKey(player, "quiz");

        plugin.getChatSettingsManager().setKeyValue(player, "chatrank", "on");
        plugin.getChatSettingsManager().setKeyValue(player, "chatprefix", "on");
        plugin.getChatSettingsManager().setKeyValue(player, "quiz", "on");

        // Legacy settings, remove those to keep everything clean
        plugin.getChatSettingsManager().removeKey(player, "prefixes");
        plugin.getChatSettingsManager().removeKey(player, "ranks");
        plugin.getChatSettingsManager().removeKey(player, "broadcasts");
        plugin.getChatSettingsManager().removeKey(player, "gradients");
        plugin.getChatSettingsManager().removeKey(player, "games");

        player.sendMessage(plugin.getChatMessageManager().getMessage(Messages.RESET));
    }

}
