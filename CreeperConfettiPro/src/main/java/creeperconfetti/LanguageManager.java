package creeperconfetti;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class LanguageManager {
    private static LanguageManager instance;
    private String currentLanguage = "en";
    private final Map<String, String> messages = new HashMap<>();
    private boolean initialized = false;
    private Runnable onInitializedCallback;
    private CreeperConfettiPro plugin;

    private static final String IP_API_URL = "http://ip-api.com/json/?fields=countryCode";
    private static final String LANGUAGE_CONFIG_PATH = "language";
    private static final String LANGUAGES_FOLDER = "languages";
    private static final String DEFAULT_LANGUAGE = "en";

    private static final Set<String> SUPPORTED_LANGUAGES = Set.of(
            "zh", "zht", "ja", "fr", "ru", "ko", "en", "es", "de", "it", "pt", "ar", "hi", "tr", "nl", "pl", "sv", "th"
    );

    public static LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    public void setPlugin(CreeperConfettiPro plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        initialize(null);
    }

    public void initialize(Runnable callback) {
        this.onInitializedCallback = callback;
        copyDefaultLanguageFiles();
        detectLanguage();
    }

    private void copyDefaultLanguageFiles() {
        File languagesDir = new File(plugin.getDataFolder(), LANGUAGES_FOLDER);
        if (!languagesDir.exists()) {
            languagesDir.mkdirs();
        }

        for (String langCode : SUPPORTED_LANGUAGES) {
            File langFile = new File(languagesDir, langCode + ".yml");
            if (!langFile.exists()) {
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(LANGUAGES_FOLDER + "/" + langCode + ".yml")) {
                    if (is != null) {
                        plugin.saveResource(LANGUAGES_FOLDER + "/" + langCode + ".yml", false);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("无法复制默认语言文件: " + langCode + ".yml - " + e.getMessage());
                }
            }
        }
    }

    private void detectLanguage() {
        String configLanguage = plugin.getConfig().getString(LANGUAGE_CONFIG_PATH, "auto");

        if (configLanguage != null && !configLanguage.equalsIgnoreCase("auto") && !configLanguage.trim().isEmpty()) {
            currentLanguage = validateLanguageCode(configLanguage.toLowerCase());
            String languageName = getLanguageDisplayName(currentLanguage);
            plugin.getLogger().info(CreeperConfettiPro.colorizeConsole("§7" + plugin.getConfig().getString("language") + " → " + languageName + " (" + currentLanguage + ")"));
            loadMessages();
            initialized = true;

            if (onInitializedCallback != null) {
                onInitializedCallback.run();
            }
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(IP_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                InputStream inputStream = connection.getInputStream();
                YamlConfiguration response = YamlConfiguration.loadConfiguration(new InputStreamReader(inputStream));

                return response.getString("countryCode", "US");
            } catch (Exception e) {
                return "US";
            }
        }).thenAccept(countryCode -> {
            String detectedLang = mapCountryToLanguage(countryCode);

            currentLanguage = detectedLang;
            String languageName = getLanguageDisplayName(currentLanguage);
            plugin.getLogger().info(CreeperConfettiPro.colorizeConsole("§7" + countryCode + " → " + languageName + " (" + currentLanguage + ")"));

            loadMessages();
            initialized = true;

            if (onInitializedCallback != null) {
                onInitializedCallback.run();
            }
        });
    }

    private String mapCountryToLanguage(String countryCode) {
        switch (countryCode.toUpperCase()) {
            case "CN":
            case "MO":
                return "zh";
            case "HK":
            case "TW":
                return "zht";
            case "JP":
                return "ja";
            case "FR":
            case "BE":
            case "CH":
            case "LU":
            case "MC":
                return "fr";
            case "RU":
            case "BY":
            case "KZ":
            case "KG":
                return "ru";
            case "KR":
                return "ko";
            default:
                return "en";
        }
    }

    private String validateLanguageCode(String languageCode) {
        if (SUPPORTED_LANGUAGES.contains(languageCode)) {
            return languageCode;
        }
        plugin.getLogger().warning("无效的语言代码: " + languageCode + "，使用默认语言: English");
        return DEFAULT_LANGUAGE;
    }

    private String getLanguageDisplayName(String langCode) {
        switch (langCode) {
            case "zh": return "简体中文";
            case "zht": return "繁體中文";
            case "ja": return "日本語";
            case "fr": return "Français";
            case "ru": return "Русский";
            case "ko": return "한국어";
            case "en": return "English";
            case "es": return "Español";
            case "de": return "Deutsch";
            case "it": return "Italiano";
            case "pt": return "Português";
            case "ar": return "العربية";
            case "hi": return "हिन्दी";
            case "tr": return "Türkçe";
            case "nl": return "Nederlands";
            case "pl": return "Polski";
            case "sv": return "Svenska";
            case "th": return "ไทย";
            default: return "Unknown";
        }
    }

    private void loadMessages() {
        messages.clear();

        File langFile = new File(new File(plugin.getDataFolder(), LANGUAGES_FOLDER), currentLanguage + ".yml");
        YamlConfiguration config;

        if (langFile.exists()) {
            try (FileInputStream fis = new FileInputStream(langFile);
                 InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                config = YamlConfiguration.loadConfiguration(reader);
            } catch (Exception e) {
                plugin.getLogger().warning("无法加载语言文件 " + currentLanguage + ".yml: " + e.getMessage());
                loadDefaultMessages();
                loadConsoleMessages();
                return;
            }
        } else {
            loadDefaultMessages();
            loadConsoleMessages();
            return;
        }

        loadSection(config, "command");
        loadSection(config, "help");
        loadConsoleMessages();
    }

    private void loadSection(YamlConfiguration config, String section) {
        if (!config.contains(section)) {
            return;
        }
        for (String key : config.getConfigurationSection(section).getKeys(false)) {
            String fullKey = section + "." + key;
            String value = config.getString(fullKey);
            if (value != null) {
                messages.put(fullKey, translateAlternateColorCodes(value));
            }
        }
    }

    private String translateAlternateColorCodes(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private void loadDefaultMessages() {
        loadDefaultCommandMessages();
        loadDefaultHelpMessages();
    }

    private void loadDefaultCommandMessages() {
        messages.put("command.no_permission", translateAlternateColorCodes("&cYou don't have permission to use this command!"));
        messages.put("command.usage", translateAlternateColorCodes("&cUsage: /creeperconfetti <reload | reseteffect | seteffect | reloadlanguage | setlanguage | language>"));
        messages.put("command.reload_success", translateAlternateColorCodes("&aCreeperConfettiPro configuration reloaded!"));
        messages.put("command.reset_success", translateAlternateColorCodes("&aDefault confetti effect restored!"));
        messages.put("command.player_only", translateAlternateColorCodes("&cOnly players can use this command!"));
        messages.put("command.hold_firework", translateAlternateColorCodes("&cPlease hold a firework rocket with desired effects in your main hand."));
        messages.put("command.effect_set", translateAlternateColorCodes("&aConfetti effect is now set to the firework in your main hand!"));
        messages.put("command.language_set", translateAlternateColorCodes("&aPlugin language set to: "));
        messages.put("command.invalid_language", translateAlternateColorCodes("&cInvalid language code! Available: zh, zht, ja, fr, ru, ko, en, es, de, it, pt, ar, hi, tr, nl, pl, sv, th"));
        messages.put("command.current_language", translateAlternateColorCodes("&eCurrent plugin language: "));
        messages.put("command.language_reloaded", translateAlternateColorCodes("&aLanguage settings reloaded! Current language: "));
        messages.put("command.reloading", translateAlternateColorCodes("&eRe-detecting language settings..."));
        messages.put("command.set_language_usage", translateAlternateColorCodes("&cUsage: /creeperconfetti setlanguage <language_code>"));
        messages.put("command.available_languages", translateAlternateColorCodes("&7Available: zh, zht, ja, fr, ru, ko, en, es, de, it, pt, ar, hi, tr, nl, pl, sv, th"));
    }

    private void loadDefaultHelpMessages() {
        messages.put("help.header", translateAlternateColorCodes("&6=== CreeperConfettiPro Help ==="));
        messages.put("help.reload", translateAlternateColorCodes("&e/reload &7- Reload plugin configuration"));
        messages.put("help.reseteffect", translateAlternateColorCodes("&e/reseteffect &7- Restore default confetti effect"));
        messages.put("help.seteffect", translateAlternateColorCodes("&e/seteffect &7- Set firework in main hand as confetti effect"));
        messages.put("help.reloadlanguage", translateAlternateColorCodes("&e/reloadlanguage &7- Re-detect language settings"));
        messages.put("help.setlanguage", translateAlternateColorCodes("&e/setlanguage <code> &7- Set plugin language"));
        messages.put("help.language", translateAlternateColorCodes("&e/language &7- View current plugin language"));
    }

    private void loadConsoleMessages() {
        switch (currentLanguage) {
            case "zh":
                loadZhConsoleMessages();
                break;
            case "zht":
                loadZhtConsoleMessages();
                break;
            case "ja":
                loadJaConsoleMessages();
                break;
            case "fr":
                loadFrConsoleMessages();
                break;
            case "ru":
                loadRuConsoleMessages();
                break;
            case "ko":
                loadKoConsoleMessages();
                break;
            case "es":
                loadEsConsoleMessages();
                break;
            case "de":
                loadDeConsoleMessages();
                break;
            case "it":
                loadItConsoleMessages();
                break;
            case "pt":
                loadPtConsoleMessages();
                break;
            case "ar":
                loadArConsoleMessages();
                break;
            case "hi":
                loadHiConsoleMessages();
                break;
            case "tr":
                loadTrConsoleMessages();
                break;
            case "nl":
                loadNlConsoleMessages();
                break;
            case "pl":
                loadPlConsoleMessages();
                break;
            case "sv":
                loadSvConsoleMessages();
                break;
            case "th":
                loadThConsoleMessages();
                break;
            default:
                loadEnConsoleMessages();
                break;
        }
    }

    private void loadEnConsoleMessages() {
        messages.put("console.loading", "§6    CreeperConfettiPro plugin is loading...");
        messages.put("console.enabled", "§a    CreeperConfettiPro plugin enabled successfully!");
        messages.put("console.disabled", "§c    CreeperConfettiPro plugin is unloading...");
        messages.put("console.version", "§7    Version: §f");
        messages.put("console.author", "§7    Branch Author: §dNice_Cam_");
        messages.put("console.java_version", "§7    Server Java Version: §f");
        messages.put("console.thanks", "§7    Thank you for using this plugin!");
        messages.put("console.bstats_enabled", "§b    ☁️ Cloud statistics feature enabled!");
        messages.put("console.bstats_collecting", "§7    Collecting plugin usage data to optimize experience...");
        messages.put("console.java_version_low", "§c❌ Detected server Java version below 14, plugin will be disabled automatically!");
        messages.put("console.java_current_version", "§7Server current Java version: §f");
        messages.put("console.language_info", "§rCurrent plugin language: ");
        messages.put("console.language_config_detected", "§7Detected config language setting: ");
        messages.put("console.language_region_detected", "§7Detected server region code: ");
        messages.put("console.java_parse_error", "§eUnable to parse Java version: ");
    }

    private void loadZhConsoleMessages() {
        messages.put("console.loading", "§6    CreeperConfettiPro 插件正在加载中...");
        messages.put("console.enabled", "§a    CreeperConfettiPro 插件已成功启用！");
        messages.put("console.disabled", "§c    CreeperConfettiPro 插件正在卸载...");
        messages.put("console.version", "§7    版本: §f");
        messages.put("console.author", "§7    分支作者: §dNice_Cam_");
        messages.put("console.java_version", "§7    服务器Java版本: §f");
        messages.put("console.thanks", "§7    感谢使用本插件！");
        messages.put("console.bstats_enabled", "§b    ☁️ 云数据统计功能已启用！");
        messages.put("console.bstats_collecting", "§7    正在收集插件使用数据以优化体验...");
        messages.put("console.java_version_low", "§c❌ 检测到服务器Java版本低于14，插件将自动禁用！");
        messages.put("console.java_current_version", "§7服务器当前Java版本: §f");
        messages.put("console.language_info", "§r当前插件语言: ");
        messages.put("console.language_config_detected", "§7检测到配置文件语言设置: ");
        messages.put("console.language_region_detected", "§7检测到服务器地区代码: ");
        messages.put("console.java_parse_error", "§e无法解析Java版本号: ");
    }

    private void loadZhtConsoleMessages() {
        messages.put("console.loading", "§6    CreeperConfettiPro 插件正在加載中...");
        messages.put("console.enabled", "§a    CreeperConfettiPro 插件已成功啟用！");
        messages.put("console.disabled", "§c    CreeperConfettiPro 插件正在卸載...");
        messages.put("console.version", "§7    版本: §f");
        messages.put("console.author", "§7    分支作者: §dNice_Cam_");
        messages.put("console.java_version", "§7    伺服器Java版本: §f");
        messages.put("console.thanks", "§7    感謝使用本插件！");
        messages.put("console.bstats_enabled", "§b    ☁️ 雲數據統計功能已啟用！");
        messages.put("console.bstats_collecting", "§7    正在收集插件使用數據以優化體驗...");
        messages.put("console.java_version_low", "§c❌ 檢測到伺服器Java版本低於14，插件將自動禁用！");
        messages.put("console.java_current_version", "§7伺服器當前Java版本: §f");
        messages.put("console.language_info", "§r當前插件語言: ");
        messages.put("console.language_config_detected", "§7檢測到配置文件語言設置: ");
        messages.put("console.language_region_detected", "§7檢測到伺服器地區代碼: ");
        messages.put("console.java_parse_error", "§e無法解析Java版本號: ");
    }

    private void loadJaConsoleMessages() {
        messages.put("console.loading", "§6    CreeperConfettiPro プラグインを読み込んでいます...");
        messages.put("console.enabled", "§a    CreeperConfettiPro プラグインが正常に有効化されました！");
        messages.put("console.disabled", "§c    CreeperConfettiPro プラグインを無効化しています...");
        messages.put("console.version", "§7    バージョン: §f");
        messages.put("console.author", "§7    作者: §dNice_Cam_");
        messages.put("console.java_version", "§7    サーバーJavaバージョン: §f");
        messages.put("console.thanks", "§7    このプラグインをご利用いただきありがとうございます！");
        messages.put("console.bstats_enabled", "§b    ☁️ クラウド統計機能が有効になりました！");
        messages.put("console.bstats_collecting", "§7    プラグイン使用データを収集してエクスペリエンスを最適化しています...");
        messages.put("console.java_version_low", "§c❌ サーバーJavaバージョンが14未満を検出しました。プラグインは自動的に無効になります！");
        messages.put("console.java_current_version", "§7サーバーの現在のJavaバージョン: §f");
        messages.put("console.language_info", "§r現在のプラグイン言語: ");
        messages.put("console.language_config_detected", "§7設定ファイルの言語設定を検出しました: ");
        messages.put("console.language_region_detected", "§7サーバーの地域コードを検出しました: ");
        messages.put("console.java_parse_error", "§eJavaバージョン番号の解析に失敗しました: ");
    }

    private void loadFrConsoleMessages() {
        messages.put("console.loading", "§6    Le plugin CreeperConfettiPro est en cours de chargement...");
        messages.put("console.enabled", "§a    Le plugin CreeperConfettiPro a été activé avec succès !");
        messages.put("console.disabled", "§c    Le plugin CreeperConfettiPro est en cours de désactivation...");
        messages.put("console.version", "§7    Version: §f");
        messages.put("console.author", "§7    Auteur: §dNice_Cam_");
        messages.put("console.java_version", "§7    Version Java du serveur: §f");
        messages.put("console.thanks", "§7    Merci d'utiliser ce plugin !");
        messages.put("console.bstats_enabled", "§b    ☁️ La fonction de statistiques cloud est activée !");
        messages.put("console.bstats_collecting", "§7    Collecte des données d'utilisation du plugin pour optimiser l'expérience...");
        messages.put("console.java_version_low", "§c❌ Version Java du serveur inférieure à 14 détectée, le plugin sera automatiquement désactivé !");
        messages.put("console.java_current_version", "§7Version Java actuelle du serveur: §f");
        messages.put("console.language_info", "§rLangue actuelle du plugin: ");
        messages.put("console.language_config_detected", "§7Paramètre de langue détecté dans la configuration: ");
        messages.put("console.language_region_detected", "§7Code de région du serveur détecté: ");
        messages.put("console.java_parse_error", "§eImpossible de parser la version Java: ");
    }

    private void loadRuConsoleMessages() {
        messages.put("console.loading", "§6    Плагин CreeperConfettiPro загружается...");
        messages.put("console.enabled", "§a    Плагин CreeperConfettiPro успешно активирован!");
        messages.put("console.disabled", "§c    Плагин CreeperConfettiPro деактивируется...");
        messages.put("console.version", "§7    Версия: §f");
        messages.put("console.author", "§7    Автор: §dNice_Cam_");
        messages.put("console.java_version", "§7    Версия Java сервера: §f");
        messages.put("console.thanks", "§7    Спасибо за использование этого плагина!");
        messages.put("console.bstats_enabled", "§b    ☁️ Функция облачной статистики включена!");
        messages.put("console.bstats_collecting", "§7    Сбор данных об использовании плагина для оптимизации опыта...");
        messages.put("console.java_version_low", "§c❌ Обнаружена версия Java сервера ниже 14, плагин будет автоматически отключен!");
        messages.put("console.java_current_version", "§7Текущая версия Java сервера: §f");
        messages.put("console.language_info", "§rТекущий язык плагина: ");
        messages.put("console.language_config_detected", "§7Обнаружена настройка языка в конфигурации: ");
        messages.put("console.language_region_detected", "§7Обнаружен код региона сервера: ");
        messages.put("console.java_parse_error", "§eНе удалось разобрать версию Java: ");
    }

    private void loadKoConsoleMessages() {
        messages.put("console.loading", "§6    CreeperConfettiPro 플러그인이 로드 중입니다...");
        messages.put("console.enabled", "§a    CreeperConfettiPro 플러그인이 성공적으로 활성화되었습니다!");
        messages.put("console.disabled", "§c    CreeperConfettiPro 플러그인을 비활성화하는 중입니다...");
        messages.put("console.version", "§7    버전: §f");
        messages.put("console.author", "§7    작성자: §dNice_Cam_");
        messages.put("console.java_version", "§7    서버 Java 버전: §f");
        messages.put("console.thanks", "§7    이 플러그인을 사용해 주셔서 감사합니다!");
        messages.put("console.bstats_enabled", "§b    ☁️ 클라우드 통계 기능이 활성화되었습니다!");
        messages.put("console.bstats_collecting", "§7    경험을 최적화하기 위해 플러그인 사용 데이터를 수집하는 중입니다...");
        messages.put("console.java_version_low", "§c❌ 서버 Java 버전이 14 미만으로 감지되어 플러그인이 자동으로 비활성화됩니다!");
        messages.put("console.java_current_version", "§7서버의 현재 Java 버전: §f");
        messages.put("console.language_info", "§r현재 플러그인 언어: ");
        messages.put("console.language_config_detected", "§7설정 파일에서 언어 설정을 감지했습니다: ");
        messages.put("console.language_region_detected", "§7서버 지역 코드 감지: ");
        messages.put("console.java_parse_error", "§eJava 버전을 구문 분석할 수 없습니다: ");
    }

    private void loadEsConsoleMessages() {
        messages.put("console.loading", "§6    El plugin CreeperConfettiPro se está cargando...");
        messages.put("console.enabled", "§a    ¡El plugin CreeperConfettiPro se ha habilitado correctamente!");
        messages.put("console.disabled", "§c    El plugin CreeperConfettiPro se está deshabilitando...");
        messages.put("console.version", "§7    Versión: §f");
        messages.put("console.author", "§7    Autor: §dNice_Cam_");
        messages.put("console.java_version", "§7    Versión Java del servidor: §f");
        messages.put("console.thanks", "§7    ¡Gracias por usar este plugin!");
        messages.put("console.bstats_enabled", "§b    ☁️ ¡Función de estadísticas en la nube habilitada!");
        messages.put("console.bstats_collecting", "§7    Recopilando datos de uso del plugin para optimizar la experiencia...");
        messages.put("console.java_version_low", "§c❌ ¡Se detectó una versión de Java del servidor inferior a 14, el plugin se deshabilitará automáticamente!");
        messages.put("console.java_current_version", "§7Versión actual de Java del servidor: §f");
        messages.put("console.language_info", "§rIdioma actual del plugin: ");
        messages.put("console.language_config_detected", "§7Configuración de idioma detectada: ");
        messages.put("console.language_region_detected", "§7Código de región del servidor detectado: ");
        messages.put("console.java_parse_error", "§eNo se pudo analizar la versión de Java: ");
    }

    private void loadDeConsoleMessages() {
        messages.put("console.loading", "§6    Das CreeperConfettiPro-Plugin wird geladen...");
        messages.put("console.enabled", "§a    CreeperConfettiPro-Plugin erfolgreich aktiviert!");
        messages.put("console.disabled", "§c    CreeperConfettiPro-Plugin wird deaktiviert...");
        messages.put("console.version", "§7    Version: §f");
        messages.put("console.author", "§7    Autor: §dNice_Cam_");
        messages.put("console.java_version", "§7    Server-Java-Version: §f");
        messages.put("console.thanks", "§7    Danke, dass Sie dieses Plugin verwenden!");
        messages.put("console.bstats_enabled", "§b    ☁️ Cloud-Statistikfunktion aktiviert!");
        messages.put("console.bstats_collecting", "§7    Sammeln von Plugin-Nutzungsdaten zur Optimierung der Erfahrung...");
        messages.put("console.java_version_low", "§c❌ Server-Java-Version unter 14 erkannt, Plugin wird automatisch deaktiviert!");
        messages.put("console.java_current_version", "§7Aktuelle Server-Java-Version: §f");
        messages.put("console.language_info", "§rAktuelle Plugin-Sprache: ");
        messages.put("console.language_config_detected", "§7Spracheinstellung aus Konfiguration erkannt: ");
        messages.put("console.language_region_detected", "§7Server-Regionscode erkannt: ");
        messages.put("console.java_parse_error", "§eJava-Version kann nicht analysiert werden: ");
    }

    private void loadItConsoleMessages() {
        messages.put("console.loading", "§6    Il plugin CreeperConfettiPro si sta caricando...");
        messages.put("console.enabled", "§a    Plugin CreeperConfettiPro abilitato con successo!");
        messages.put("console.disabled", "§c    Il plugin CreeperConfettiPro si sta disabilitando...");
        messages.put("console.version", "§7    Versione: §f");
        messages.put("console.author", "§7    Autore: §dNice_Cam_");
        messages.put("console.java_version", "§7    Versione Java del server: §f");
        messages.put("console.thanks", "§7    Grazie per aver utilizzato questo plugin!");
        messages.put("console.bstats_enabled", "§b    ☁️ Funzione di statistiche cloud abilitata!");
        messages.put("console.bstats_collecting", "§7    Raccolta dei dati di utilizzo del plugin per ottimizzare l'esperienza...");
        messages.put("console.java_version_low", "§c❌ Rilevata versione Java del server inferiore a 14, il plugin verrà disabilitato automaticamente!");
        messages.put("console.java_current_version", "§7Versione Java corrente del server: §f");
        messages.put("console.language_info", "§rLingua attuale del plugin: ");
        messages.put("console.language_config_detected", "§7Impostazione lingua rilevata dalla configurazione: ");
        messages.put("console.language_region_detected", "§7Codice regione server rilevato: ");
        messages.put("console.java_parse_error", "§eImpossibile analizzare la versione Java: ");
    }

    private void loadPtConsoleMessages() {
        messages.put("console.loading", "§6    O plugin CreeperConfettiPro está carregando...");
        messages.put("console.enabled", "§a    Plugin CreeperConfettiPro ativado com sucesso!");
        messages.put("console.disabled", "§c    O plugin CreeperConfettiPro está sendo desativado...");
        messages.put("console.version", "§7    Versão: §f");
        messages.put("console.author", "§7    Autor: §dNice_Cam_");
        messages.put("console.java_version", "§7    Versão Java do servidor: §f");
        messages.put("console.thanks", "§7    Obrigado por usar este plugin!");
        messages.put("console.bstats_enabled", "§b    ☁️ Função de estatísticas na nuvem ativada!");
        messages.put("console.bstats_collecting", "§7    Coletando dados de uso do plugin para otimizar a experiência...");
        messages.put("console.java_version_low", "§c❌ Versão Java do servidor abaixo de 14 detectada, o plugin será desativado automaticamente!");
        messages.put("console.java_current_version", "§7Versão Java atual do servidor: §f");
        messages.put("console.language_info", "§rIdioma atual do plugin: ");
        messages.put("console.language_config_detected", "§7Configuração de idioma detectada: ");
        messages.put("console.language_region_detected", "§7Código de região do servidor detectado: ");
        messages.put("console.java_parse_error", "§eNão foi possível analisar a versão Java: ");
    }

    private void loadArConsoleMessages() {
        messages.put("console.loading", "§6    جاري تحميل إضافة CreeperConfettiPro...");
        messages.put("console.enabled", "§a    تم تفعيل إضافة CreeperConfettiPro بنجاح!");
        messages.put("console.disabled", "§c    جاري تعطيل إضافة CreeperConfettiPro...");
        messages.put("console.version", "§7    الإصدار: §f");
        messages.put("console.author", "§7    المؤلف: §dNice_Cam_");
        messages.put("console.java_version", "§7    إصدار جافا للخادم: §f");
        messages.put("console.thanks", "§7    شكراً لاستخدامك هذه الإضافة!");
        messages.put("console.bstats_enabled", "§b    ☁️ تم تفعيل ميزة إحصائيات السحابة!");
        messages.put("console.bstats_collecting", "§7    جاري جمع بيانات استخدام الإضافة لتحسين التجربة...");
        messages.put("console.java_version_low", "§c❌ تم اكتشاف إصدار جافا للخادم أقل من 14، سيتم تعطيل الإضافة تلقائياً!");
        messages.put("console.java_current_version", "§7إصدار جافا الحالي للخادم: §f");
        messages.put("console.language_info", "§rلغة الإضافة الحالية: ");
        messages.put("console.language_config_detected", "§7تم اكتشاف إعداد اللغة من الملف: ");
        messages.put("console.language_region_detected", "§7تم اكتشاف رمز منطقة الخادم: ");
        messages.put("console.java_parse_error", "§eتعذر تحليل إصدار جافا: ");
    }

    private void loadHiConsoleMessages() {
        messages.put("console.loading", "§6    CreeperConfettiPro प्लगइन लोड हो रहा है...");
        messages.put("console.enabled", "§a    CreeperConfettiPro प्लगइन सफलतापूर्वक सक्षम किया गया!");
        messages.put("console.disabled", "§c    CreeperConfettiPro प्लगइन अक्षम हो रहा है...");
        messages.put("console.version", "§7    संस्करण: §f");
        messages.put("console.author", "§7    लेखक: §dNice_Cam_");
        messages.put("console.java_version", "§7    सर्वर जावा संस्करण: §f");
        messages.put("console.thanks", "§7    इस प्लगइन का उपयोग करने के लिए धन्यवाद!");
        messages.put("console.bstats_enabled", "§b    ☁️ क्लाउड सांख्यिकी सुविधा सक्षम की गई!");
        messages.put("console.bstats_collecting", "§7    अनुभव को अनुकूलित करने के लिए प्लगइन उपयोग डेटा एकत्र किया जा रहा है...");
        messages.put("console.java_version_low", "§c❌ सर्वर जावा संस्करण 14 से कम पाया गया, प्लगइन स्वचालित रूप से अक्षम कर दिया जाएगा!");
        messages.put("console.java_current_version", "§7वर्तमान सर्वर जावा संस्करण: §f");
        messages.put("console.language_info", "§rवर्तमान प्लगइन भाषा: ");
        messages.put("console.language_config_detected", "§7कॉन्फ़िग से भाषा सेटिंग का पता चला: ");
        messages.put("console.language_region_detected", "§7सर्वर क्षेत्र कोड का पता चला: ");
        messages.put("console.java_parse_error", "§eजावा संस्करण को पार्स नहीं किया जा सका: ");
    }

    private void loadTrConsoleMessages() {
        messages.put("console.loading", "§6    CreeperConfettiPro eklentisi yükleniyor...");
        messages.put("console.enabled", "§a    CreeperConfettiPro eklentisi başarıyla etkinleştirildi!");
        messages.put("console.disabled", "§c    CreeperConfettiPro eklentisi devre dışı bırakılıyor...");
        messages.put("console.version", "§7    Sürüm: §f");
        messages.put("console.author", "§7    Yazar: §dNice_Cam_");
        messages.put("console.java_version", "§7    Sunucu Java Sürümü: §f");
        messages.put("console.thanks", "§7    Bu eklentiyi kullandığınız için teşekkürler!");
        messages.put("console.bstats_enabled", "§b    ☁️ Bulut istatistikleri özelliği etkinleştirildi!");
        messages.put("console.bstats_collecting", "§7    Deneyimi optimize etmek için eklenti kullanım verileri toplanıyor...");
        messages.put("console.java_version_low", "§c❌ Sunucu Java sürümü 14'ün altında algılandı, eklenti otomatik olarak devre dışı bırakılacak!");
        messages.put("console.java_current_version", "§7Geçerli sunucu Java sürümü: §f");
        messages.put("console.language_info", "§rMevcut eklenti dili: ");
        messages.put("console.language_config_detected", "§7Yapılandırmadan dil ayarı algılandı: ");
        messages.put("console.language_region_detected", "§7Sunucu bölge kodu algılandı: ");
        messages.put("console.java_parse_error", "§eJava sürümü çözülemedi: ");
    }

    private void loadNlConsoleMessages() {
        messages.put("console.loading", "§6    CreeperConfettiPro plugin wordt geladen...");
        messages.put("console.enabled", "§a    CreeperConfettiPro plugin succesvol geactiveerd!");
        messages.put("console.disabled", "§c    CreeperConfettiPro plugin wordt uitgeschakeld...");
        messages.put("console.version", "§7    Versie: §f");
        messages.put("console.author", "§7    Auteur: §dNice_Cam_");
        messages.put("console.java_version", "§7    Server Java-versie: §f");
        messages.put("console.thanks", "§7    Bedankt voor het gebruik van deze plugin!");
        messages.put("console.bstats_enabled", "§b    ☁️ Cloud-statistiekenfunctie ingeschakeld!");
        messages.put("console.bstats_collecting", "§7    Plugin-gebruiksgegevens verzamelen om de ervaring te optimaliseren...");
        messages.put("console.java_version_low", "§c❌ Server Java-versie onder 14 gedetecteerd, plugin wordt automatisch uitgeschakeld!");
        messages.put("console.java_current_version", "§7Huidige server Java-versie: §f");
        messages.put("console.language_info", "§rHuidige plugin-taal: ");
        messages.put("console.language_config_detected", "§7Taalaanwerking gedetecteerd uit config: ");
        messages.put("console.language_region_detected", "§7Serverregiocode gedetecteerd: ");
        messages.put("console.java_parse_error", "§eKan Java-versie niet analyseren: ");
    }

    private void loadPlConsoleMessages() {
        messages.put("console.loading", "§6    Wtyczka CreeperConfettiPro się ładuje...");
        messages.put("console.enabled", "§a    Wtyczka CreeperConfettiPro została pomyślnie aktywowana!");
        messages.put("console.disabled", "§c    Wtyczka CreeperConfettiPro jest wyłączana...");
        messages.put("console.version", "§7    Wersja: §f");
        messages.put("console.author", "§7    Autor: §dNice_Cam_");
        messages.put("console.java_version", "§7    Wersja Javy serwera: §f");
        messages.put("console.thanks", "§7    Dziękujemy za korzystanie z tej wtyczki!");
        messages.put("console.bstats_enabled", "§b    ☁️ Funkcja statystyk chmurowych została włączona!");
        messages.put("console.bstats_collecting", "§7    Zbieranie danych użytkowania wtyczki w celu optymalizacji doświadczenia...");
        messages.put("console.java_version_low", "§c❌ Wykryto wersję Javy serwera poniżej 14, wtyczka zostanie automatycznie wyłączona!");
        messages.put("console.java_current_version", "§7Aktualna wersja Javy serwera: §f");
        messages.put("console.language_info", "§rAktualny język wtyczki: ");
        messages.put("console.language_config_detected", "§7Wykryto ustawienie języka z konfiguracji: ");
        messages.put("console.language_region_detected", "§7Wykryto kod regionu serwera: ");
        messages.put("console.java_parse_error", "§eNie można przeanalizować wersji Javy: ");
    }

    private void loadSvConsoleMessages() {
        messages.put("console.loading", "§6    CreeperConfettiPro-pluginen laddas...");
        messages.put("console.enabled", "§a    CreeperConfettiPro-pluginen aktiverades framgångsrikt!");
        messages.put("console.disabled", "§c    CreeperConfettiPro-pluginen inaktiveras...");
        messages.put("console.version", "§7    Version: §f");
        messages.put("console.author", "§7    Författare: §dNice_Cam_");
        messages.put("console.java_version", "§7    Serverns Java-version: §f");
        messages.put("console.thanks", "§7    Tack för att du använder detta plugin!");
        messages.put("console.bstats_enabled", "§b    ☁️ Molnstatistikfunktionen är aktiverad!");
        messages.put("console.bstats_collecting", "§7    Samlar in pluginanvändningsdata för att optimera upplevelsen...");
        messages.put("console.java_version_low", "§c❌ Serverns Java-version under 14 upptäckt, pluginen kommer att inaktiveras automatiskt!");
        messages.put("console.java_current_version", "§7Nuvarande serverns Java-version: §f");
        messages.put("console.language_info", "§rNuvarande plugin-språk: ");
        messages.put("console.language_config_detected", "§7Språkinställning upptäckt från konfiguration: ");
        messages.put("console.language_region_detected", "§7Serverregionkod upptäckt: ");
        messages.put("console.java_parse_error", "§eKan inte analysera Java-version: ");
    }

    private void loadThConsoleMessages() {
        messages.put("console.loading", "§6    กำลังโหลดปลั๊กอิน CreeperConfettiPro...");
        messages.put("console.enabled", "§a    เปิดใช้งานปลั๊กอิน CreeperConfettiPro สำเร็จแล้ว!");
        messages.put("console.disabled", "§c    กำลังปิดใช้งานปลั๊กอิน CreeperConfettiPro...");
        messages.put("console.version", "§7    เวอร์ชัน: §f");
        messages.put("console.author", "§7    ผู้เขียน: §dNice_Cam_");
        messages.put("console.java_version", "§7    เวอร์ชัน Java ของเซิร์ฟเวอร์: §f");
        messages.put("console.thanks", "§7    ขอบคุณที่ใช้ปลั๊กอินนี้!");
        messages.put("console.bstats_enabled", "§b    ☁️ เปิดใช้งานคุณสมบัติสถิติบนคลาวด์แล้ว!");
        messages.put("console.bstats_collecting", "§7    กำลังรวบรวมข้อมูลการใช้งานปลั๊กอินเพื่อปรับปรุงประสบการณ์...");
        messages.put("console.java_version_low", "§c❌ ตรวจพบเวอร์ชัน Java ของเซิร์ฟเวอร์ต่ำกว่า 14 ปลั๊กอินจะถูกปิดใช้งานโดยอัตโนมัติ!");
        messages.put("console.java_current_version", "§7เวอร์ชัน Java ปัจจุบันของเซิร์ฟเวอร์: §f");
        messages.put("console.language_info", "§rภาษาปลั๊กอินปัจจุบัน: ");
        messages.put("console.language_config_detected", "§7ตรวจพบการตั้งค่าภาษาจากคอนฟิก: ");
        messages.put("console.language_region_detected", "§7ตรวจพบรหัสพื้นที่เซิร์ฟเวอร์: ");
        messages.put("console.java_parse_error", "§eไม่สามารถแยกวิเคราะห์เวอร์ชัน Java ได้: ");
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, key);
    }

    public String getConsoleMessage(String key) {
        return messages.getOrDefault(key, key);
    }

    public String getCurrentLanguage() {
        return currentLanguage;
    }

    public String getCurrentLanguageDisplayName() {
        if (currentLanguage == null || currentLanguage.isEmpty()) {
            return "Unknown";
        }
        String displayName = getLanguageDisplayName(currentLanguage);
        return displayName != null ? displayName : "Unknown";
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void reloadLanguage(Runnable callback) {
        this.onInitializedCallback = callback;
        initialized = false;
        copyDefaultLanguageFiles();
        detectLanguage();
    }

    public void setLanguage(String languageCode, Runnable callback) {
        String validatedLanguage = validateLanguageCode(languageCode.toLowerCase());
        if (!validatedLanguage.equals(currentLanguage)) {
            currentLanguage = validatedLanguage;
            loadMessages();
            plugin.getConfig().set(LANGUAGE_CONFIG_PATH, currentLanguage);
            plugin.saveConfig();
        }

        if (callback != null) {
            callback.run();
        }
    }

    public Set<String> getAvailableLanguages() {
        return Collections.unmodifiableSet(SUPPORTED_LANGUAGES);
    }

    public String getLanguageDisplayNameFor(String langCode) {
        return getLanguageDisplayName(langCode);
    }
}