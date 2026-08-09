package org.unitedlands.unitedchat.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.unitedlands.unitedchat.UnitedChat;
import org.unitedlands.unitedchat.quiz.Question;
import org.unitedlands.unitedchat.quiz.QuizUtils;
import org.unitedlands.unitedchat.utils.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class QuizStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final UnitedChat plugin;
    private final File       file;
    private       QuizData   data;

    public QuizStorage(UnitedChat plugin) {
        this.plugin = plugin;
        this.data   = new QuizData();
        this.file   = new File(plugin.getDataFolder(), "quiz/quiz_history.json");

        file.getParentFile().mkdirs();
        load();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isOnCooldown(String key) {
        if (key == null)
            return false;

        var cutoff = System.currentTimeMillis() - Config.get().quizCooldownDays() * 86_400_000L;
        for (var i = data.history.size() - 1; i >= 0; i--) {
            var e = data.history.get(i);
            if (key.equals(e.id))
                return e.timestamp > cutoff;
        }

        return false;
    }

    public void record(String key, Question question, Player winner) {
        var plainQuestion = PlainTextComponentSerializer.plainText()
                .serialize(QuizUtils.MM.deserialize(question.question()));

        data.history.add(new HistoryEntry(
                key,
                plainQuestion,
                question.correct().getFirst(),
                winner != null ? winner.getUniqueId().toString() : null,
                System.currentTimeMillis()
        ));

        if (winner != null) {
            var uuid = winner.getUniqueId().toString();
            var prev = data.scores.getOrDefault(uuid, new PlayerData(winner.getName(), 0));
            data.scores.put(uuid, new PlayerData(winner.getName(), prev.wins + 1));
        }

        save();
    }

    public List<Component> buildHighscore(String period, int page) {
        var cfg      = Config.get();
        var pageSize = cfg.quizHighscorePageSize();
        var scores   = period.equals("alltime") ? data.scores : scoresForPeriod(cutoffFor(period));
        var sorted   = scores.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, PlayerData> e) -> e.getValue().wins).reversed())
                .toList();

        var periodLabel = switch (period) {
            case "weekly"  -> "Weekly";
            case "monthly" -> "Monthly";
            default        -> "All Time";
        };

        var lines = new ArrayList<Component>();
        lines.add(QuizUtils.MM.deserialize(cfg.quizHighscoreHeader().replace("%period%", periodLabel)));

        if (sorted.isEmpty()) {
            lines.add(QuizUtils.MM.deserialize(cfg.quizHighscoreEmpty()));
        } else {
            var ranks = new int[sorted.size()];
            for (var i = 0; i < sorted.size(); i++) {
                if (i == 0 || sorted.get(i).getValue().wins != sorted.get(i - 1).getValue().wins)
                    ranks[i] = i + 1;
                else
                    ranks[i] = ranks[i - 1];
            }

            var totalPages  = (int) Math.ceil(sorted.size() / (double) pageSize);
            var clampedPage = Math.max(1, Math.min(page, totalPages));
            var from        = (clampedPage - 1) * pageSize;
            var to          = Math.min(from + pageSize, sorted.size());

            for (var i = from; i < to; i++) {
                var stats = sorted.get(i).getValue();
                var label = stats.wins == 1
                        ? cfg.quizHighscoreWinSingular()
                        : cfg.quizHighscoreWinPlural();

                lines.add(QuizUtils.MM.deserialize(cfg.quizHighscoreEntry()
                        .replace("%rank%",       String.valueOf(ranks[i]))
                        .replace("%name%",       stats.name)
                        .replace("%wins%",       String.valueOf(stats.wins))
                        .replace("%wins_label%", label)));
            }

            if (totalPages > 1)
                lines.add(buildPageNav(period, clampedPage, totalPages));
        }

        return lines;
    }

    private Component buildPageNav(String period, int page, int totalPages) {
        var prev = page > 1
                ? QuizUtils.MM.deserialize("<yellow><bold>[◀]</bold></yellow>")
                        .clickEvent(ClickEvent.runCommand("/uc quiz highscore " + period + " " + (page - 1)))
                        .hoverEvent(HoverEvent.showText(QuizUtils.MM.deserialize("<gray>Previous page</gray>")))
                : QuizUtils.MM.deserialize("<dark_gray><bold>[◀]</bold></dark_gray>");

        var next = page < totalPages
                ? QuizUtils.MM.deserialize("<yellow><bold>[▶]</bold></yellow>")
                        .clickEvent(ClickEvent.runCommand("/uc quiz highscore " + period + " " + (page + 1)))
                        .hoverEvent(HoverEvent.showText(QuizUtils.MM.deserialize("<gray>Next page</gray>")))
                : QuizUtils.MM.deserialize("<dark_gray><bold>[▶]</bold></dark_gray>");

        var format = Config.get().quizHighscorePageNav()
                .replace("%page%", String.valueOf(page))
                .replace("%total%", String.valueOf(totalPages));

        return QuizUtils.MM.deserialize(format,
                Placeholder.component("prev", prev),
                Placeholder.component("next", next));
    }

    private Map<String, PlayerData> scoresForPeriod(long cutoffMs) {
        var result = new LinkedHashMap<String, PlayerData>();
        for (var e : data.history) {
            if (e.timestamp < cutoffMs || e.winner == null) continue;
            var existing = data.scores.get(e.winner);
            var name = existing != null ? existing.name : e.winner;
            var prev = result.getOrDefault(e.winner, new PlayerData(name, 0));
            result.put(e.winner, new PlayerData(prev.name, prev.wins + 1));
        }
        return result;
    }

    private long cutoffFor(String period) {
        return switch (period) {
            case "weekly"  -> System.currentTimeMillis() - 7  * 86_400_000L;
            case "monthly" -> System.currentTimeMillis() - 30 * 86_400_000L;
            default        -> 0L;
        };
    }

    private void load() {
        if (!file.exists()) return;
        try (var reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            var loaded = GSON.fromJson(reader, QuizData.class);
            if (loaded != null) data = loaded;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load quiz_history.json: " + e.getMessage());
        }
    }

    private void save() {
        try (var writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save quiz_history.json: " + e.getMessage());
        }
    }

    static class HistoryEntry {
        String id;
        String question;
        String answer;
        String winner;
        long   timestamp;

        HistoryEntry(String id, String question, String answer, String winner, long timestamp) {
            this.id        = id;
            this.question  = question;
            this.answer    = answer;
            this.winner    = winner;
            this.timestamp = timestamp;
        }
    }

    static class PlayerData {
        String name;
        int    wins;

        PlayerData(String name, int wins) {
            this.name = name;
            this.wins = wins;
        }
    }

    static class QuizData {
        List<HistoryEntry>      history = new ArrayList<>();
        Map<String, PlayerData> scores  = new HashMap<>();
    }
}
