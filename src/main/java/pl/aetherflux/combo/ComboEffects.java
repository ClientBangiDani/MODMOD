package pl.aetherflux.combo;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/** Efekty czasteczkowe (vanilla, po stronie klienta - tanie i bez wlasnego renderera). */
public final class ComboEffects {
    private static final Random RNG = Random.create();

    private ComboEffects() {}

    /** Prog efektownego wybuchu: 21, 27, 33, 39, a potem co 6 (45, 51, ...). */
    public static boolean isBurstThreshold(int hits) {
        return hits >= 21 && (hits - 21) % 6 == 0;
    }

    /** Wywolywane raz na kazde potwierdzone trafienie => wybuch na progu odpala sie jednokrotnie. */
    public static void onHit(Entity target, int hits) {
        ComboConfig cfg = ComboConfig.INSTANCE;
        if (!cfg.particles || !isBurstThreshold(hits)) return;
        ClientWorld w = (ClientWorld) target.getWorld();
        Vec3d c = ComboTracker.labelPos(target, 1f);
        int n = cfg.maxBurstParticles;
        for (int i = 0; i < n; i++) {
            double th = RNG.nextDouble() * Math.PI * 2;
            double ph = Math.acos(2 * RNG.nextDouble() - 1);
            double sp = 0.08 + RNG.nextDouble() * 0.12;
            double vx = Math.sin(ph) * Math.cos(th) * sp;
            double vy = Math.cos(ph) * sp;
            double vz = Math.sin(ph) * Math.sin(th) * sp;
            w.addParticleClient(i % 3 == 0 ? ParticleTypes.END_ROD : ParticleTypes.FIREWORK, c.x, c.y, c.z, vx, vy, vz);
        }
    }

    /** Subtelne czasteczki wokol napisu dla 14+ trafien. Malo i rzadko. */
    public static void tick(ClientWorld w) {
        ComboConfig cfg = ComboConfig.INSTANCE;
        if (!cfg.particles) return;
        ComboTracker.State s = ComboTracker.currentState();
        Entity e = ComboTracker.currentEntity();
        if (s == null || e == null || s.hits < 14) return;
        if (ComboTracker.tick() % 3 != 0) return;
        Vec3d c = ComboTracker.labelPos(e, 1f);
        for (int i = 0; i < 2; i++) {
            double ox = (RNG.nextDouble() - 0.5) * 1.4;
            double oy = (RNG.nextDouble() - 0.5) * 0.4;
            double oz = (RNG.nextDouble() - 0.5) * 1.4;
            w.addParticleClient(ParticleTypes.ELECTRIC_SPARK, c.x + ox, c.y + oy, c.z + oz, 0, 0.01, 0);
        }
    }
}
