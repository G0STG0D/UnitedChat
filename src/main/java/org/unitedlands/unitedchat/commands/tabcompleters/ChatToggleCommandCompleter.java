package org.unitedlands.unitedchat.commands.tabcompleters;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.unitedlands.unitedchat.UnitedChat;
import org.unitedlands.unitedchat.utils.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ChatToggleCommandCompleter implements TabCompleter {

    private final List<String> baseCommands = new ArrayList<>();
    private final List<String> chatFeatures;
    private final List<String> toggleOptions = List.of("on", "off");

    public ChatToggleCommandCompleter(UnitedChat plugin) {
        this.chatFeatures = Config.get().features();

        baseCommands.add("reset");
        baseCommands.add("toggle");
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> options = null;
        String input = args[args.length - 1];

        switch (args.length) {
            case 1 -> {
                options = new ArrayList<>(baseCommands);
                options.add("quiz");
                if (sender.hasPermission("united.chat.admin"))
                    options.add("reload");
            }
            case 2 -> {
                if (args[0].equalsIgnoreCase("quiz")) {
                    options = new ArrayList<>(List.of("highscore", "next"));
                    if (sender.hasPermission("united.chat.admin")) {
                        options.add("force");
                        options.add("toggle");
                    }
                } else if (!args[0].equalsIgnoreCase("reset") && !args[0].equalsIgnoreCase("reload")) {
                    options = chatFeatures;
                }
            }
            case 3 -> {
                if (args[0].equalsIgnoreCase("quiz")) {
                    if (args[1].equalsIgnoreCase("toggle"))
                        options = toggleOptions;
                    else if (args[1].equalsIgnoreCase("highscore"))
                        options = List.of("weekly", "monthly", "alltime");
                    else if (args[1].equalsIgnoreCase("force") && sender.hasPermission("united.chat.admin"))
                        options = List.of("multiple_choice", "open_text", "math_formula", "word_scramble");
                } else {
                    options = toggleOptions;
                }
            }
        }

        if (options != null) {
            return options.stream()
                    .filter(opt -> opt.toLowerCase().startsWith(input.toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}