package creeperconfetti;

import creeperconfetti.commands.CreeperConfettiCommand;
import creeperconfetti.events.CreeperExplodeListener;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class CreeperConfettiPro extends JavaPlugin {
    private LanguageManager languageManager;
    private boolean javaVersionChecked = false;

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

        checkJavaVersion();
    }

    private void checkJavaVersion() {
        String javaVersion = System.getProperty("java.version");
        if (!isJava14OrAbove(javaVersion)) {
            getLogger().severe(colorizeConsole(languageManager.getConsoleMessage("console.java_version_low")));
            getLogger().severe(colorizeConsole(languageManager.getConsoleMessage("console.java_current_version") + javaVersion));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        javaVersionChecked = true;
    }

    private void onLanguageInitialized() {
        if (!javaVersionChecked) {
            checkJavaVersion();
            if (!javaVersionChecked) return;
        }

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

        new MetricsHelper(this);
        getLogger().info(colorizeConsole(languageManager.getConsoleMessage("console.bstats_enabled")));
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

    private boolean isJava14OrAbove(String version) {
        try {
            String[] parts = version.split("\\.");
            int majorVersion = Integer.parseInt(parts[0]);
            if (majorVersion > 1) {
                return majorVersion >= 14;
            } else {
                int minorVersion = Integer.parseInt(parts[1]);
                return minorVersion >= 14;
            }
        } catch (Exception e) {
            getLogger().warning(colorizeConsole(languageManager.getConsoleMessage("console.java_parse_error") + version));
            return false;
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