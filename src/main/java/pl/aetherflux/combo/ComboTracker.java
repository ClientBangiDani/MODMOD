package pl.aetherflux.combo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Licznik kombinacji.
 *
 * ROZROZNIENIE:
 *  - PROBA ataku  = zdarzenie AttackEntityCallback (gracz kliknal atak na encje). To NIE jest trafienie.
 *  - POTWIERDZONE trafienie = po probie ataku, w oknie confirmWindowTicks, klient zobaczyl u TEGO celu
 *    nowa animacje obrazen (hurtTime rosnie - ustawiane przez pakiet statusu obrazen z serwera).
 *
 * Ograniczenie: klient nie dostaje od serwera informacji "to Ty zadales obrazenia". Jesli w oknie
 * potwierdzenia ten sam cel dostanie obrazenia od kogos innego, moze to zostac zaliczone. Jedna proba
 * ataku moze potwierdzic maksymalnie jedno trafienie (dedup), a bez proby ataku nic sie nie nalicza.
 */
public final class ComboTracker {
    public static final class State {
        public int hits;
        public long lastHitTick;
        public Vec3d lastKnownPos = Vec3d.ZERO;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();
    private static UUID current;
    private static long tick;

    // oczekujaca proba ataku
    private static int pendingEntityId = -1;
    private static long pendingTick;
    private static int pendingPrevHurt;

    private static int prevPlayerHurt;

    private ComboTracker() {}

    public static long tick() { return tick; }
    public static State currentState() { return current == null ? null : STATES.get(current); }
    public static Entity currentEntity() {
        ClientWorld w = MinecraftClient.getInstance().world;
        if (w == null || current == null) return null;
        for (Entity e : w.getEntities()) if (e.getUuid().equals(current)) return e;
        return null;
    }

    public static void reset() {
        STATES.clear();
        current = null;
        pendingEntityId = -1;
        prevPlayerHurt = 0;
    }

    /** Wywolywane z AttackEntityCallback: to tylko PROBA ataku. */
    public static void onAttackAttempt(Entity target) {
        if (!ComboConfig.INSTANCE.enabled || !(target instanceof LivingEntity le)) return;
        pendingEntityId = target.getId();
        pendingTick = tick;
        pendingPrevHurt = le.hurtTime;
    }

    public static void onTick(MinecraftClient mc) {
        tick++;
        ClientWorld world = mc.world;
        if (world == null || mc.player == null) { reset(); return; }
        ComboConfig cfg = ComboConfig.INSTANCE;
        if (!cfg.enabled) { reset(); return; }

        if (!mc.player.isAlive()) { reset(); return; }
        if (cfg.resetOnPlayerHurt) {
            if (mc.player.hurtTime > prevPlayerHurt) { reset(); }
        }
        prevPlayerHurt = mc.player.hurtTime;

        // --- potwierdzenie trafienia ---
        if (pendingEntityId != -1) {
            if (tick - pendingTick > cfg.confirmWindowTicks) {
                pendingEntityId = -1; // proba bez potwierdzonego skutku - nie liczymy
            } else {
                Entity e = world.getEntityById(pendingEntityId);
                if (!(e instanceof LivingEntity le) || !le.isAlive()) {
                    pendingEntityId = -1;
                } else if (le.hurtTime > pendingPrevHurt
                        && le.squaredDistanceTo(mc.player) <= cfg.maxConfirmDistance * cfg.maxConfirmDistance) {
                    pendingEntityId = -1; // jedna proba = max jedno trafienie
                    registerHit(le);
                } else if (le.hurtTime == 0) {
                    pendingPrevHurt = 0; // animacja obrazen sie skonczyla - kolejny wzrost to nowe obrazenia
                }
            }
        }

        // --- wygasanie / smierc / znikniecie celu ---
        Iterator<Map.Entry<UUID, State>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, State> en = it.next();
            Entity e = null;
            for (Entity x : world.getEntities()) if (x.getUuid().equals(en.getKey())) { e = x; break; }
            boolean dead = !(e instanceof LivingEntity le) || !le.isAlive() || le.getHealth() <= 0 || e.isRemoved();
            boolean expired = tick - en.getValue().lastHitTick > cfg.timeoutTicks;
            if (dead || expired) {
                if (en.getKey().equals(current)) current = null;
                it.remove();
            } else {
                en.getValue().lastKnownPos = e.getPos();
            }
        }

        ComboEffects.tick(world);
    }

    private static void registerHit(LivingEntity target) {
        ComboConfig cfg = ComboConfig.INSTANCE;
        UUID id = target.getUuid();
        if (current != null && !current.equals(id) && cfg.resetOnTargetSwitch) {
            STATES.clear();
        }
        current = id;
        State s = STATES.computeIfAbsent(id, k -> new State());
        s.hits++;
        s.lastHitTick = tick;
        s.lastKnownPos = target.getPos();
        ComboEffects.onHit(target, s.hits);
    }

    /** Pozycja napisu nad celem (z interpolacja). */
    public static Vec3d labelPos(Entity e, float tickDelta) {
        return e.getLerpedPos(tickDelta).add(0, e.getHeight() + 0.85, 0);
    }
}
