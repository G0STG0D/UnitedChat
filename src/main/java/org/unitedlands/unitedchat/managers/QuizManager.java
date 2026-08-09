package org.unitedlands.unitedchat.managers;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import org.unitedlands.unitedchat.UnitedChat;
import org.unitedlands.unitedchat.quiz.*;
import org.unitedlands.unitedchat.utils.Config;
import org.unitedlands.unitedchat.utils.Messages;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import java.util.concurrent.ThreadLocalRandom;

public class QuizManager {

    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private final UnitedChat     plugin;
    private List<Question> questions;
    private List<String>   words;
    private final QuizStorage    storage;

    private BukkitTask quizTask;
    private Question   activeQuestion;
    private BossBar    activeBossBar;
    private BukkitTask countdownTask;
    private boolean    quizEnabled;

    private long nextQuizTime  = 0;
    private long quizStartTime = 0;

    private final Map<UUID, Pair<Long, BukkitTask>> mcCooldowns = new HashMap<>();

    public QuizManager(UnitedChat plugin) {
        this.plugin     = plugin;
        this.questions  = QuizUtils.loadQuestions(plugin);
        this.words      = QuizUtils.loadWords(plugin);
        this.storage     = new QuizStorage(plugin);
        this.quizEnabled = plugin.getConfig().getBoolean("quiz.enabled", true);

        plugin.getLogger().info("Loaded " + questions.size() + " quiz questions, " + words.size() + " scramble words.");
    }

    public void start() {
        if (quizTask != null)
            return;

        var intervalMs = (long) Config.get().quizInterval() * 60 * 1000;
        var interval   = intervalMs / 50;
        nextQuizTime   = System.currentTimeMillis() + intervalMs;
        quizTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            broadcastRandom(false);
            nextQuizTime = System.currentTimeMillis() + intervalMs;
        }, interval, interval);
    }

    public long getNextQuizTime() {
        return nextQuizTime;
    }

    public boolean isScheduled() {
        return quizTask != null;
    }

    public void stop() {
        stopScheduler();
        endQuiz(null);
    }

    public void reload() {
        stop();

        quizEnabled = plugin.getConfig().getBoolean("quiz.enabled", true);
        questions   = QuizUtils.loadQuestions(plugin);
        words       = QuizUtils.loadWords(plugin);

        plugin.getLogger().info("Reloaded " + questions.size() + " quiz questions, " + words.size() + " scramble words.");
        start();
    }

    public void forceNext() {
        forceNext(null);
    }

    public void forceNext(QuestionType type) {
        stop();
        broadcastRandom(true, type);
        start();
    }

    public boolean isActive() {
        return activeQuestion != null;
    }

    public void toggleQuizFor(Player player, boolean show) {
        if (activeQuestion == null) return;

        if (show) {
            player.showBossBar(activeBossBar);
            sendQuizQuestionTo(player);
            return;
        }

        player.hideBossBar(activeBossBar);
    }

    public void checkAnswer(Player player, String input) {
        if (activeQuestion == null) return;

        if (activeQuestion.type() == QuestionType.MULTIPLE_CHOICE)
            if (!QuizUtils.isPlausibleMcAnswer(input.trim(), activeQuestion) || !canAnswer(player))
                return;

        if (QuizUtils.isCorrect(input.trim(), activeQuestion))
            endQuiz(player);
    }

    private boolean canAnswer(Player player) {
        var cooldown = Config.get().quizMcAttemptCooldown() * 1000L;
        var last     = mcCooldowns.getOrDefault(player.getUniqueId(), Pair.of(0L, null));

        if (System.currentTimeMillis() - last.getLeft() < cooldown)
            return false;

        mcCooldowns.put(player.getUniqueId(), Pair.of(System.currentTimeMillis(), null));
        startCooldownDisplay(player, cooldown);
        return true;
    }

    public void setEnabled(boolean enabled) {
        quizEnabled = enabled;
        var configFile = new File(plugin.getDataFolder(), "config.yml");
        try {
            var content = Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            Files.writeString(configFile.toPath(),
                    content.replaceFirst("(?m)^(\\s+enabled:).*$", "$1 " + enabled),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to persist quiz.enabled: " + e.getMessage());
        }
    }

    public boolean isEnabled() {
        return quizEnabled;
    }

    private void broadcastRandom(boolean forced) {
        broadcastRandom(forced, null);
    }

    private void broadcastRandom(boolean forced, QuestionType type) {
        if (!forced && !isEnabled()) return;
        if (!forced && QuizUtils.getQuizEnabledPlayers(plugin).size() < Config.get().quizMinPlayers()) return;

        activeQuestion = type != null ? questionOfType(type) : nextQuestion();
        activeBossBar  = QuizUtils.createQuizBossBar();

        quizStartTime = System.currentTimeMillis();
        plugin.getLogger().info("[Quiz] " + QuizUtils.buildConsoleLog(activeQuestion));

        QuizUtils.getQuizEnabledPlayers(plugin).forEach(this::sendQuizQuestionTo);
        startQuiz();
    }

    private Question questionOfType(QuestionType type) {
        return switch (type) {
            case MULTIPLE_CHOICE -> randomFromYml(QuestionType.MULTIPLE_CHOICE);
            case OPEN_TEXT       -> randomFromYml(QuestionType.OPEN_TEXT);
            case WORD_SCRAMBLE   -> {
                var available = words.stream().filter(w -> !storage.isOnCooldown(w)).toList();
                yield available.isEmpty() ? QuizGenerator.generateMath() : QuizGenerator.generateWordScramble(available);
            }
            case MATH_FORMULA    -> QuizGenerator.generateMath();
        };
    }

    private void startQuiz() {
        mcCooldowns.clear();

        var timeout = Config.get().quizAnswerTimeout();
        var endTime = System.currentTimeMillis() + timeout * 1000L;

        countdownTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (activeQuestion == null) {
                countdownTask.cancel();
                return;
            }

            var remaining = endTime - System.currentTimeMillis();
            if (remaining <= 0) {
                endQuiz(null);
                if (countdownTask != null) countdownTask.cancel();
                return;
            }

            var progress = Math.min(1f, remaining / (timeout * 1000f));
            activeBossBar.progress(progress);
            activeBossBar.color(progress > 0.5f ? BossBar.Color.GREEN : progress > 0.25f ? BossBar.Color.YELLOW : BossBar.Color.RED);
        }, 1L, 1L);
    }

    private void endQuiz(Player winner) {
        if (activeQuestion == null || activeBossBar == null) return;

        var bar      = activeBossBar;
        var task     = countdownTask;
        var question = activeQuestion;

        activeQuestion = null;
        activeBossBar  = null;
        countdownTask  = null;

        mcCooldowns.values().stream()
                .map(Pair::getRight)
                .filter(Objects::nonNull)
                .forEach(BukkitTask::cancel);
        mcCooldowns.clear();
        if (task != null)
            task.cancel();

        Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bar));

        var elapsed = (System.currentTimeMillis() - quizStartTime) / 1000.0;
        if (winner != null)
            plugin.getLogger().info(String.format("[Quiz] Answered by %s in %.1fs", winner.getName(), elapsed));
        else
            plugin.getLogger().info(String.format("[Quiz] Timed out after %.1fs (no answer)", elapsed));

        var cfg = Config.get();
        if (!cfg.quizEndSoundName().isBlank())
            QuizUtils.getQuizEnabledPlayers(plugin).forEach(p ->
                    p.playSound(p.getLocation(), cfg.quizEndSoundName(), cfg.quizEndSoundVolume(), cfg.quizEndSoundPitch()));

        var answer = (question.type() == QuestionType.MULTIPLE_CHOICE)
                ? QuizUtils.getCorrectLetter(question) + " " + question.correct().getFirst()
                : question.correct().getFirst();

        var ingameAnswer = question.correct().getFirst();
        var annMsg = winner != null
                ? cfg.quizCorrectAnnouncement().replace("%player%", winner.getName()).replace("%answer%", ingameAnswer)
                : cfg.quizTimeoutAnnouncement().replace("%answer%", ingameAnswer);

        QuizUtils.getQuizEnabledPlayers(plugin).forEach(p -> p.sendMessage(QuizUtils.MM.deserialize(annMsg)));
        QuizUtils.sendDiscordAnnouncement(winner, answer, question, plugin);
        storage.record(questionKey(question), question, winner);

        var payout = cfg.quizPayout();
        if (winner != null && payout > 0 && plugin.getEconomy() != null) {
            plugin.getEconomy().depositPlayer(winner, payout);
            winner.sendMessage(
                    plugin.getChatMessageManager().getMessage(Messages.PAYOUT_MESSAGE,
                    Placeholder.unparsed("amount", plugin.getEconomy().format(payout)))
            );
        }
    }

    private String questionKey(Question question) {
        return switch (question.type()) {
            case MULTIPLE_CHOICE, OPEN_TEXT -> String.valueOf(question.id());
            case WORD_SCRAMBLE              -> question.correct().getFirst();
            case MATH_FORMULA               -> null;
        };
    }

    public List<Component> buildHighscore(String period, int page) {
        return storage.buildHighscore(period, page);
    }

    public void sendQuizQuestionTo(Player player) {
        if (activeQuestion == null) return;

        QuizUtils.buildMessage(activeQuestion).forEach(player::sendMessage);
        player.showBossBar(activeBossBar);

        var cfg = Config.get();
        if (!cfg.quizSoundName().isBlank())
            player.playSound(player.getLocation(), cfg.quizSoundName(), cfg.quizSoundVolume(), cfg.quizSoundPitch());
    }

    private Question nextQuestion() {

        return switch (QuizGenerator.pickWeightedType()) {
            case MULTIPLE_CHOICE -> randomFromYml(QuestionType.MULTIPLE_CHOICE);
            case OPEN_TEXT       -> randomFromYml(QuestionType.OPEN_TEXT);
            case WORD_SCRAMBLE   -> {
                var available = words.stream().filter(w -> !storage.isOnCooldown(w)).toList();
                yield available.isEmpty() ? QuizGenerator.generateMath() : QuizGenerator.generateWordScramble(available);
            }
            case MATH_FORMULA    -> QuizGenerator.generateMath();
        };
    }

    private Question randomFromYml(QuestionType type) {
        var pool = questions.stream()
                .filter(q -> q.type() == type && !storage.isOnCooldown(String.valueOf(q.id())))
                .toList();

        if (pool.isEmpty())
            return QuizGenerator.generateMath();

        var ulPool     = pool.stream().filter(Question::ulSpecific).toList();
        var normalPool = pool.stream().filter(q -> !q.ulSpecific()).toList();
        var ulWeight   = type == QuestionType.MULTIPLE_CHOICE
                ? Config.get().quizUlWeightMc()
                : Config.get().quizUlWeightText();

        List<Question> bucket;
        if (ulPool.isEmpty())
            bucket = normalPool;
        else if (normalPool.isEmpty())
            bucket = ulPool;
        else
            bucket = RANDOM.nextInt(100) < ulWeight ? ulPool : normalPool;

        var question = bucket.get(RANDOM.nextInt(bucket.size()));
        if (type != QuestionType.MULTIPLE_CHOICE)
            return question;

        var shuffled = new ArrayList<>(question.choices());
        Collections.shuffle(shuffled, RANDOM);

        return new Question(question.id(), question.type(), question.question(), shuffled, question.correct(), question.ulSpecific());
    }

    private void startCooldownDisplay(Player player, long cooldownMs) {
        var prev = mcCooldowns.get(player.getUniqueId());
        if (prev != null && prev.getRight() != null) prev.getRight().cancel();

        var endTime = System.currentTimeMillis() + cooldownMs;
        var task = new BukkitRunnable() {

            @Override public void run() {
                if (!player.isOnline() || activeQuestion == null) { cancel(); return; }
                var remaining = endTime - System.currentTimeMillis();
                if (remaining <= 0) {
                    player.sendActionBar(QuizUtils.MM.deserialize(Config.get().quizCooldownReady()));
                    cancel(); return;
                }
                player.sendActionBar(QuizUtils.cooldownBar(remaining, cooldownMs));
            }

        }.runTaskTimer(plugin, 0L, 1L);

        mcCooldowns.put(player.getUniqueId(), Pair.of(System.currentTimeMillis(), task));
    }

    private void stopScheduler() {
        if (quizTask == null) return;

        quizTask.cancel();
        quizTask = null;
    }

}
