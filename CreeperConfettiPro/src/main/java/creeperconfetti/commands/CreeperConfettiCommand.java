package creeperconfetti.commands;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import creeperconfetti.CreeperConfettiPro;
import creeperconfetti.LanguageManager;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.jetbrains.annotations.NotNull;

public class CreeperConfettiCommand implements TabExecutor {

    private static final List<FireworkEffect> DEFAULT_CONFETTI_EFFECT = Collections.emptyList();

    @Override
    public boolean onCommand(CommandSender sender, @NotNull Command command, @NotNull String label, String @NotNull [] args) {
        LanguageManager languageManager = CreeperConfettiPro.getInstance().getLanguageManager();

        if (!sender.hasPermission("creeperconfetti.command")) {
            sender.sendMessage(languageManager.getMessage("command.no_permission"));
            return true;
        }

        if (args.length == 0) {
            showHelp(sender, languageManager);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender, languageManager);
                break;
            case "reseteffect":
                handleResetEffect(sender, languageManager);
                break;
            case "seteffect":
                handleSetEffect(sender, languageManager);
                break;
            case "reloadlanguage":
                handleReloadLanguage(sender, languageManager);
                break;
            case "setlanguage":
                handleSetLanguage(sender, args, languageManager);
                break;
            case "language":
                handleShowLanguage(sender, languageManager);
                break;
            case "help":
                showHelp(sender, languageManager);
                break;
            default:
                sender.sendMessage(languageManager.getMessage("command.usage"));
                sender.sendMessage(languageManager.getMessage("help.header"));
                break;
        }

        return true;
    }

    private void showHelp(CommandSender sender, LanguageManager languageManager) {
        sender.sendMessage(languageManager.getMessage("help.header"));
        sender.sendMessage(languageManager.getMessage("help.reload"));
        sender.sendMessage(languageManager.getMessage("help.reseteffect"));
        sender.sendMessage(languageManager.getMessage("help.seteffect"));
        sender.sendMessage(languageManager.getMessage("help.reloadlanguage"));
        sender.sendMessage(languageManager.getMessage("help.setlanguage"));
        sender.sendMessage(languageManager.getMessage("help.language"));
    }

    private void handleReload(CommandSender sender, LanguageManager languageManager) {
        CreeperConfettiPro.getInstance().reloadConfig();
        sender.sendMessage(languageManager.getMessage("command.reload_success"));
    }

    private void handleResetEffect(CommandSender sender, LanguageManager languageManager) {
        CreeperConfettiPro.getInstance().getConfig().set("confetti_effect", DEFAULT_CONFETTI_EFFECT);
        CreeperConfettiPro.getInstance().saveConfig();
        sender.sendMessage(languageManager.getMessage("command.reset_success"));
    }

    private void handleSetEffect(CommandSender sender, LanguageManager languageManager) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(languageManager.getMessage("command.player_only"));
            return;
        }

        ItemStack itemInMainHand = player.getInventory().getItemInMainHand();

        if (itemInMainHand.getType() != Material.FIREWORK_ROCKET) {
            sender.sendMessage(languageManager.getMessage("command.hold_firework"));
            return;
        }

        FireworkMeta fireworkMeta = (FireworkMeta) itemInMainHand.getItemMeta();
        if (fireworkMeta == null || !fireworkMeta.hasEffects()) {
            sender.sendMessage(languageManager.getMessage("command.hold_firework"));
            return;
        }

        CreeperConfettiPro.getInstance().getConfig().set("confetti_effect", fireworkMeta.getEffects());
        CreeperConfettiPro.getInstance().saveConfig();
        sender.sendMessage(languageManager.getMessage("command.effect_set"));

        showFireworkEffect(player);
    }

    private void showFireworkEffect(Player player) {
        Firework firework = player.getWorld().spawn(player.getLocation().add(0, 1, 0), Firework.class);

        FireworkMeta showcaseFireworkMeta = firework.getFireworkMeta();
        List<FireworkEffect> effects = (List<FireworkEffect>) CreeperConfettiPro.getInstance()
                .getConfig().get("confetti_effect");

        if (effects != null && !effects.isEmpty()) {
            showcaseFireworkMeta.addEffects(effects);
        } else {
            showcaseFireworkMeta.addEffects(DEFAULT_CONFETTI_EFFECT);
        }

        showcaseFireworkMeta.setPower(0);
        firework.setFireworkMeta(showcaseFireworkMeta);

        Objects.requireNonNull(firework);
        Bukkit.getScheduler().runTaskLater(
                CreeperConfettiPro.getInstance(),
                firework::detonate,
                1L
        );
    }

    private void handleReloadLanguage(CommandSender sender, LanguageManager languageManager) {
        sender.sendMessage(languageManager.getMessage("command.reloading"));

        languageManager.reloadLanguage(() -> {
            Bukkit.getScheduler().runTask(CreeperConfettiPro.getInstance(), () -> {
                sender.sendMessage(languageManager.getMessage("command.language_reloaded") +
                        languageManager.getCurrentLanguageDisplayName());
            });
        });
    }

    private void handleSetLanguage(CommandSender sender, String[] args, LanguageManager languageManager) {
        if (args.length < 2) {
            sender.sendMessage(languageManager.getMessage("command.set_language_usage"));
            sender.sendMessage(languageManager.getMessage("command.available_languages"));
            return;
        }

        String languageCode = args[1];
        languageManager.setLanguage(languageCode, () -> {
            Bukkit.getScheduler().runTask(CreeperConfettiPro.getInstance(), () -> {
                sender.sendMessage(languageManager.getMessage("command.language_set") +
                        languageManager.getCurrentLanguageDisplayName());
            });
        });
    }

    private void handleShowLanguage(CommandSender sender, LanguageManager languageManager) {
        sender.sendMessage(languageManager.getMessage("command.current_language") +
                languageManager.getCurrentLanguageDisplayName() + " (" + languageManager.getCurrentLanguage() + ")");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "reseteffect", "seteffect", "reloadlanguage", "setlanguage", "language", "help");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("setlanguage")) {
            return List.of("zh", "zht", "ja", "fr", "ru", "ko", "en", "es", "de", "it", "pt", "ar", "hi", "tr", "nl", "pl", "sv", "th");
        }

        return Collections.emptyList();
    }
}