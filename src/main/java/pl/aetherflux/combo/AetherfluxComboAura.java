package pl.aetherflux.combo;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AetherfluxComboAura implements ClientModInitializer {
    public static final String NAME = "AETHERFLUX Combo Aura";
    public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

    @Override
    public void onInitializeClient() {
        ComboConfig.load();

        // Tylko PROBA ataku - nic nie atakuje automatycznie, nie zmieniamy wyniku zdarzenia.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient()) ComboTracker.onAttackAttempt(entity);
            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(ComboTracker::onTick);
        ClientPlayConnectionEvents.JOIN.register((h, s, c) -> ComboTracker.reset());
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> ComboTracker.reset());

        WorldRenderEvents.AFTER_TRANSLUCENT.register(ComboRenderer::render);

        ClientCommandRegistrationCallback.EVENT.register((d, reg) -> d.register(
            ClientCommandManager.literal("aetherflux_combo")
                .then(ClientCommandManager.literal("toggle").executes(c -> {
                    ComboConfig.INSTANCE.enabled = !ComboConfig.INSTANCE.enabled;
                    ComboConfig.save();
                    c.getSource().sendFeedback(Text.literal(NAME + ": " + (ComboConfig.INSTANCE.enabled ? "wlaczony" : "wylaczony")));
                    return 1;
                }))
                .then(ClientCommandManager.literal("timeout")
                    .then(ClientCommandManager.argument("sekundy", DoubleArgumentType.doubleArg(0.5, 30)).executes(c -> {
                        double sec = DoubleArgumentType.getDouble(c, "sekundy");
                        ComboConfig.INSTANCE.timeoutTicks = (int) Math.round(sec * 20);
                        ComboConfig.save();
                        c.getSource().sendFeedback(Text.literal(NAME + ": czas wygasania = " + sec + " s"));
                        return 1;
                    })))
                .then(ClientCommandManager.literal("targetreset").executes(c -> {
                    ComboConfig.INSTANCE.resetOnTargetSwitch = !ComboConfig.INSTANCE.resetOnTargetSwitch;
                    ComboConfig.save();
                    c.getSource().sendFeedback(Text.literal(NAME + ": reset przy zmianie celu = " + ComboConfig.INSTANCE.resetOnTargetSwitch));
                    return 1;
                }))
                .then(ClientCommandManager.literal("particles").executes(c -> {
                    ComboConfig.INSTANCE.particles = !ComboConfig.INSTANCE.particles;
                    ComboConfig.save();
                    c.getSource().sendFeedback(Text.literal(NAME + ": czasteczki = " + ComboConfig.INSTANCE.particles));
                    return 1;
                }))
        ));
        LOGGER.info("{} zaladowany", NAME);
    }
}
