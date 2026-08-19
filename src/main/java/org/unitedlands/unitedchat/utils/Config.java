package org.unitedlands.unitedchat.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;
import java.util.Map;

@SuppressWarnings("DataFlowIssue")
public record Config(
        // Features
        List<String> features,

        // Broadcaster
        Map<String, Object> broadcasts,
        int                 broadcastInterval,
        String              broadcastSoundName,
        float               broadcastSoundVolume,
        float               broadcastSoundPitch,

        // Quiz
        int    quizInterval,
        int    quizMinPlayers,
        int    quizPayout,
        String quizHeader,
        String quizFooter,
        int    quizWordWrapLength,
        String quizAnswerPrompt,
        String quizChoiceFormat,
        String quizSoundName,
        float  quizSoundVolume,
        float  quizSoundPitch,
        String quizBossBarTitle,
        int    quizAnswerTimeout,
        int    quizMcAttemptCooldown,
        String quizCooldownBar,
        String quizCooldownReady,
        String quizCorrectAnnouncement,
        String quizTimeoutAnnouncement,
        String quizEndSoundName,
        float  quizEndSoundVolume,
        float  quizEndSoundPitch,
        String quizDiscordAnnouncement,
        String quizDiscordWebhookUrl,
        String quizNoQuestions,
        String quizWordScramblePrompt,
        String quizMathPrompt,
        String quizHighscoreHeader,
        String quizHighscoreEntry,
        String quizHighscorePageNav,
        String quizHighscoreEmpty,
        String quizHighscoreWinSingular,
        String quizHighscoreWinPlural,
        int    quizHighscorePageSize,
        int    quizWeightMc,
        int    quizWeightText,
        int    quizWeightMath,
        int    quizWeightWord,
        int          quizUlWeightMc,
        int          quizUlWeightText,
        int          quizCooldownDays,
        List<String> quizChannels,

        // Gradients
        Map<String, Object> presets,

        // Messages
        Map<String, Object> messages,
        String              messagePrefix,
        String              joinTitle,
        String              joinSubtitle,
        String              firstJoinTitle,
        String              firstJoinSubtitle,
        int                 titleFadeIn,
        int                 titleStay,
        int                 titleFadeOut
) {

    private static Config instance;

    public static Config get() {
        return instance;
    }

    public static void load(FileConfiguration cfg) {
        var features = cfg.getStringList("features");

        var broadcasts        = cfg.getConfigurationSection("broadcaster.messages").getValues(false);
        var broadcastInterval = cfg.getInt("broadcaster.interval", 15);
        var soundName         = cfg.getString("broadcaster.sound.name", "").trim();
        var soundVolume       = (float) cfg.getDouble("broadcaster.sound.volume", 0.3);
        var soundPitch        = (float) cfg.getDouble("broadcaster.sound.pitch", 1.2);

        var quizInterval              = cfg.getInt("quiz.interval", 15);
        var quizMinPlayers            = cfg.getInt("quiz.min-players", 1);
        var quizPayout                = cfg.getInt("quiz.payout", 500);
        var quizHeader                = cfg.getString("quiz.header", "");
        var quizFooter                = cfg.getString("quiz.footer", "");
        var quizWordWrapLength        = cfg.getInt("quiz.word-wrap-length", 49);
        var quizAnswerPrompt          = cfg.getString("quiz.answer-prompt", "");
        var quizChoiceFormat          = cfg.getString("quiz.choice-format", "<yellow>%letter%)</yellow> <white>%choice%</white>");
        var quizSoundName             = cfg.getString("quiz.sound.name", "").trim();
        var quizSoundVolume           = (float) cfg.getDouble("quiz.sound.volume", 0.5);
        var quizSoundPitch            = (float) cfg.getDouble("quiz.sound.pitch", 1.2);
        var quizBossBarTitle          = cfg.getString("quiz.bossbar-title", "<yellow>❓ Quiz</yellow>");
        var quizAnswerTimeout         = cfg.getInt("quiz.answer-timeout", 15);
        var quizMcAttemptCooldown     = cfg.getInt("quiz.mc-attempt-cooldown", 10);
        var quizCooldownBar           = cfg.getString("quiz.cooldown-bar", "<gray>Answer available in: </gray>%bar%");
        var quizCooldownReady         = cfg.getString("quiz.cooldown-ready", "<green>✔ Answer available!</green>");
        var quizCorrectAnnouncement   = cfg.getString("quiz.correct-announcement", "<green>%player% answered correctly!</green>");
        var quizTimeoutAnnouncement   = cfg.getString("quiz.timeout-announcement", "<red>Time's up! Nobody answered correctly.</red>");
        var quizEndSoundName          = cfg.getString("quiz.end-sound.name", "minecraft:ui.toast.challenge_complete").trim();
        var quizEndSoundVolume        = (float) cfg.getDouble("quiz.end-sound.volume", 1.0);
        var quizEndSoundPitch         = (float) cfg.getDouble("quiz.end-sound.pitch", 1.0);
        var quizDiscordAnnouncement   = cfg.getString("quiz.discord-announcement", "**%name%** has correctly answered **%answer%** to the question\\n> *%question%*");
        var quizDiscordWebhookUrl     = cfg.getString("quiz.discord-webhook-url", "<WEBHOOK_URL_HERE>");
        var quizNoQuestions           = cfg.getString("quiz.no-questions", "<red>No quiz questions are currently available. Please contact an administrator.</red>");
        var quizWordScramblePrompt    = cfg.getString("quiz.word-scramble-prompt", "Unscramble this word: <yellow><bold>%word%</bold></yellow>");
        var quizMathPrompt            = cfg.getString("quiz.math-prompt", "What is %formula%?");
        var quizHighscoreHeader       = cfg.getString("quiz.highscore.header", "\n<gold><bold>⭐ Quiz Highscore</bold></gold>");
        var quizHighscoreEntry        = cfg.getString("quiz.highscore.entry", " <gray>#%rank%</gray> <white><bold>%name%</bold></white> <dark_gray>—</dark_gray> <yellow>%wins% %wins_label%</yellow>");
        var quizHighscorePageNav      = cfg.getString("quiz.highscore.page-nav", "\n<prev><gray>  Page %page%/%total%  </gray><next>");
        var quizHighscoreEmpty        = cfg.getString("quiz.highscore.empty", "<gray> No entries yet.");
        var quizHighscoreWinSingular  = cfg.getString("quiz.highscore.wins-singular", "win");
        var quizHighscoreWinPlural    = cfg.getString("quiz.highscore.wins-plural", "wins");
        var quizHighscorePageSize     = cfg.getInt("quiz.highscore.page-size", 10);
        var quizWeightMc              = cfg.getInt("quiz.type-weights.multiple-choice", 25);
        var quizWeightText            = cfg.getInt("quiz.type-weights.open-text", 25);
        var quizWeightMath            = cfg.getInt("quiz.type-weights.math-formula", 25);
        var quizWeightWord            = cfg.getInt("quiz.type-weights.word-scramble", 25);
        var quizUlWeightMc            = cfg.getInt("quiz.ul-weights.multiple-choice", 75);
        var quizUlWeightText          = cfg.getInt("quiz.ul-weights.open-text", 75);
        var quizCooldownDays          = cfg.getInt("quiz.cooldown-days", 30);
        var quizChannels              = cfg.getStringList("quiz.channels").stream().map(String::toLowerCase).toList();

        var presets = cfg.getConfigurationSection("presets").getValues(false);

        var messages      = cfg.getConfigurationSection("messages").getValues(false);
        var prefix        = cfg.getString("messages.prefix", "");
        var joinTitle          = cfg.getString("messages.join-title.title", "");
        var joinSubtitle       = cfg.getString("messages.join-title.subtitle", "");
        var firstJoinTitle     = cfg.getString("messages.first-join-title.title", "");
        var firstJoinSubtitle  = cfg.getString("messages.first-join-title.subtitle", "");
        var titleFadeIn        = cfg.getInt("messages.title-fade-in", 10);
        var titleStay          = cfg.getInt("messages.title-stay", 60);
        var titleFadeOut       = cfg.getInt("messages.title-fade-out", 20);

        instance = new Config(
                features,
                broadcasts,
                broadcastInterval,
                soundName,
                soundVolume,
                soundPitch,
                quizInterval,
                quizMinPlayers,
                quizPayout,
                quizHeader,
                quizFooter,
                quizWordWrapLength,
                quizAnswerPrompt,
                quizChoiceFormat,
                quizSoundName,
                quizSoundVolume,
                quizSoundPitch,
                quizBossBarTitle,
                quizAnswerTimeout,
                quizMcAttemptCooldown,
                quizCooldownBar,
                quizCooldownReady,
                quizCorrectAnnouncement,
                quizTimeoutAnnouncement,
                quizEndSoundName,
                quizEndSoundVolume,
                quizEndSoundPitch,
                quizDiscordAnnouncement,
                quizDiscordWebhookUrl,
                quizNoQuestions,
                quizWordScramblePrompt,
                quizMathPrompt,
                quizHighscoreHeader,
                quizHighscoreEntry,
                quizHighscorePageNav,
                quizHighscoreEmpty,
                quizHighscoreWinSingular,
                quizHighscoreWinPlural,
                quizHighscorePageSize,
                quizWeightMc,
                quizWeightText,
                quizWeightMath,
                quizWeightWord,
                quizUlWeightMc,
                quizUlWeightText,
                quizCooldownDays,
                quizChannels,
                presets,
                messages,
                prefix,
                joinTitle,
                joinSubtitle,
                firstJoinTitle,
                firstJoinSubtitle,
                titleFadeIn,
                titleStay,
                titleFadeOut
        );
    }

    public static void reload(FileConfiguration cfg) {
        load(cfg);
    }

    public Component getMessage(String name, TagResolver.Single... resolvers) {
        var raw = instance.messages().get(name);
        if (raw == null)
            return MiniMessage.miniMessage()
                    .deserialize("<red>Message <yellow>" + name + "<red> could not be found in the config file!");

        return MiniMessage.miniMessage().deserialize(instance.messagePrefix() + raw, resolvers);
    }

    public Component getMessage(String name) {
        return getMessage(name, new TagResolver.Single[0]);
    }

}
