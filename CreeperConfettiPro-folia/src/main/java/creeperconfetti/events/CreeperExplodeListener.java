package creeperconfetti.events;

import java.util.concurrent.ThreadLocalRandom;

import creeperconfetti.CreeperConfettiPro;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.util.Vector;

public class CreeperExplodeListener implements Listener {

    private static final String CONFETTI_CHANCE_CONFIG = "confetti_chance";

    @EventHandler
    public void onCreeperExplode(EntityExplodeEvent event) {
        if (!event.getEntityType().equals(EntityType.CREEPER)) {
            return;
        }

        double random = ThreadLocalRandom.current().nextDouble() * 100.0D;
        double chance = CreeperConfettiPro.getInstance().getConfig().getDouble(CONFETTI_CHANCE_CONFIG);

        if (random >= chance) {
            return;
        }

        event.setCancelled(true);

        Creeper creeper = (Creeper) event.getEntity();
        Location location = creeper.getLocation().add(new Vector(0, 1, 0));

        Firework firework = creeper.getWorld().spawn(location, Firework.class);

        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        fireworkMeta.addEffects(CreeperConfettiPro.loadConfettiEffects());
        fireworkMeta.setPower(0);
        firework.setFireworkMeta(fireworkMeta);

        location.getWorld().playSound(location, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 2.0F, 1.0F);

        creeper.remove();

        firework.getScheduler().runDelayed(CreeperConfettiPro.getInstance(), (ScheduledTask task) -> firework.detonate(), null, 1);
    }
}
