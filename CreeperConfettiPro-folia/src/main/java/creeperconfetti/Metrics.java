package creeperconfetti;

import org.bstats.charts.SimplePie;
import org.bukkit.plugin.java.JavaPlugin;

class MetricsHelper {

    private final JavaPlugin plugin;

    public MetricsHelper(JavaPlugin plugin) {
        this.plugin = plugin;
        initMetrics();
    }

    private void initMetrics() {
        int pluginId = 29666;
        org.bstats.bukkit.Metrics metrics = new org.bstats.bukkit.Metrics(plugin, pluginId);


        metrics.addCustomChart(
                new SimplePie("chart_id", () -> "My value")
        );
    }
}
