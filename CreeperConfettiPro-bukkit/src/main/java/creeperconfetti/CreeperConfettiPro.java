package creeperconfetti;

import creeperconfetti.commands.CreeperConfettiCommand;
import creeperconfetti.events.CreeperExplodeListener;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CreeperConfettiPro extends JavaPlugin {
    private LanguageManager languageManager;

    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_BLACK = "\033[30m";
    private static final String ANSI_DARK_BLUE = "\033[34m";
    private static final String ANSI_DARK_GREEN = "\033[32m";
    private static final String ANSI_DARK_AQUA = "\033[36m";
    private static final String ANSI_DARK_RED = "\033[31m";
    private static final String ANSI_DARK_PURPLE = "\033[35m";
    private static final String ANSI_GOLD = "\033[33m";
    private static final String ANSI_GRAY = "\033[37m";
    private static final String ANSI_DARK_GRAY = "\033[90m";
    private static final String ANSI_BLUE = "\033[94m";
    private static final String ANSI_GREEN = "\033[92m";
    private static final String ANSI_AQUA = "\033[96m";
    private static final String ANSI_RED = "\033[91m";
    private static final String ANSI_LIGHT_PURPLE = "\033[95m";
    private static final String ANSI_YELLOW = "\033[93m";
    private static final String ANSI_WHITE = "\033[97m";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        instance = this;

        languageManager = LanguageManager.getInstance();
        languageManager.setPlugin(this);
        languageManager.initialize(this::onLanguageInitialized);
    }

    private void onLanguageInitialized() {
        String version = getDescription().getVersion();
        String javaVersion = System.getProperty("java.version");
        String langName = languageManager.getCurrentLanguageDisplayName();
        String langCode = languageManager.getCurrentLanguage();

        String border = colorizeConsole(ChatColor.GOLD + "----------------------------------------");

        getLogger().info(border);
        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.loading")));
        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.version") + ChatColor.WHITE + version));
        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.author")));
        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.java_version") + ChatColor.WHITE + javaVersion));
        getLogger().info(border);

        getServer().getPluginManager().registerEvents(new CreeperExplodeListener(), this);
        Objects.requireNonNull(getCommand("creeperconfetti")).setExecutor(new CreeperConfettiCommand());

        try {
            new MetricsHelper(this);
            getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.bstats_enabled")));
        } catch (Exception e) {
            getLogger().warning("Failed to enable bStats metrics: " + e.getMessage());
        }
        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.bstats_collecting")));

        getLogger().info(border);
        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.enabled")));
        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.thanks")));
        getLogger().info(border);

        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.language_info") + langName + " (" + langCode + ")"));
    }

    @Override
    public void onDisable() {
        String version = getDescription().getVersion();

        String border = colorizeConsole(ChatColor.GOLD + "----------------------------------------");

        getLogger().info(border);
        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.disabled")));
        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.version") + ChatColor.WHITE + version));
        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.thanks")));
        getLogger().info(border);
    }

    private static CreeperConfettiPro instance;
    public static CreeperConfettiPro getInstance() {
        return instance;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    private static final FireworkEffect DEFAULT_CONFETTI_EFFECT = FireworkEffect.builder()
            .with(FireworkEffect.Type.BALL)
            .withColor(Color.RED, Color.YELLOW, Color.WHITE)
            .withFade(Color.ORANGE)
            .flicker(true)
            .build();

    public static List<FireworkEffect> loadConfettiEffects() {
        List<FireworkEffect> effects = new ArrayList<>();
        Object raw = getInstance().getConfig().get("confetti_effect");
        if (raw instanceof List<?>) {
            List<?> list = (List<?>) raw;
            for (Object entry : list) {
                if (entry instanceof FireworkEffect) {
                    effects.add((FireworkEffect) entry);
                } else if (entry instanceof ConfigurationSection) {
                    FireworkEffect effect = parseFireworkEffect((ConfigurationSection) entry);
                    if (effect != null) {
                        effects.add(effect);
                    }
                } else if (entry instanceof Map<?, ?>) {
                    ConfigurationSection section = new MemoryConfiguration().createSection("effect", (Map<?, ?>) entry);
                    FireworkEffect effect = parseFireworkEffect(section);
                    if (effect != null) {
                        effects.add(effect);
                    }
                }
            }
        }

        if (effects.isEmpty()) {
            effects.add(DEFAULT_CONFETTI_EFFECT);
        }
        return effects;
    }

    private static FireworkEffect parseFireworkEffect(ConfigurationSection section) {
        try {
            FireworkEffect.Type type = FireworkEffect.Type.valueOf(section.getString("type", "BALL").toUpperCase());
            boolean flicker = section.getBoolean("flicker", false);
            boolean trail = section.getBoolean("trail", false);
            List<Color> colors = parseColors(section.get("colors"));
            if (colors.isEmpty()) {
                return null;
            }
            List<Color> fadeColors = parseColors(section.get("fade-colors"));
            return FireworkEffect.builder()
                    .with(type)
                    .flicker(flicker)
                    .trail(trail)
                    .withColor(colors)
                    .withFade(fadeColors)
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Color> parseColors(Object raw) {
        List<Color> colors = new ArrayList<>();
        if (raw instanceof List<?>) {
            List<?> list = (List<?>) raw;
            for (Object entry : list) {
                if (entry instanceof Color) {
                    colors.add((Color) entry);
                } else if (entry instanceof ConfigurationSection) {
                    Color color = parseColor((ConfigurationSection) entry);
                    if (color != null) {
                        colors.add(color);
                    }
                } else if (entry instanceof Map<?, ?>) {
                    Color color = parseColor(new MemoryConfiguration().createSection("color", (Map<?, ?>) entry));
                    if (color != null) {
                        colors.add(color);
                    }
                }
            }
        }
        return colors;
    }

    private static Color parseColor(ConfigurationSection section) {
        try {
            int red = section.getInt("RED", -1);
            int green = section.getInt("GREEN", -1);
            int blue = section.getInt("BLUE", -1);
            if (red < 0 || red > 255 || green < 0 || green > 255 || blue < 0 || blue > 255) {
                return null;
            }
            return Color.fromRGB(red, green, blue);
        } catch (Exception e) {
            return null;
        }
    }

    public static String colorizeConsole(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        boolean hasColor = false;

        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                char code = text.charAt(i + 1);
                String ansi = getAnsiForCode(code);
                if (ansi != null) {
                    result.append(ansi);
                    hasColor = true;
                    i += 2;
                    continue;
                }
            }
            result.append(c);
            i++;
        }

        if (hasColor) {
            result.append(ANSI_RESET);
        }

        return result.toString();
    }

    private static String getAnsiForCode(char code) {
        switch (code) {
            case '0': return ANSI_BLACK;
            case '1': return ANSI_DARK_BLUE;
            case '2': return ANSI_DARK_GREEN;
            case '3': return ANSI_DARK_AQUA;
            case '4': return ANSI_DARK_RED;
            case '5': return ANSI_DARK_PURPLE;
            case '6': return ANSI_GOLD;
            case '7': return ANSI_GRAY;
            case '8': return ANSI_DARK_GRAY;
            case '9': return ANSI_BLUE;
            case 'a': return ANSI_GREEN;
            case 'b': return ANSI_AQUA;
            case 'c': return ANSI_RED;
            case 'd': return ANSI_LIGHT_PURPLE;
            case 'e': return ANSI_YELLOW;
            case 'f': return ANSI_WHITE;
            case 'r': return ANSI_RESET;
            default: return null;
        }
    }
}
