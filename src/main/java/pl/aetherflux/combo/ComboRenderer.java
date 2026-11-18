package pl.aetherflux.combo;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Renderuje napis HYPER COMBO jako billboard w swiecie 3D, nad aktualnym celem. */
public final class ComboRenderer {
    public static final int AETHER = 0x3FD8FF;   // kolor AETHERFLUX (zmien tutaj)
    public static final int VIOLET = 0x9B5CFF;
    private static final String LABEL = "HYPER COMBO";

    private ComboRenderer() {}

    public static Text buildText(int hits, float time) {
        if (hits < 11) { // 4-10: zwykly, wyrazny
            return Text.literal(LABEL).setStyle(Style.EMPTY.withColor(AETHER).withBold(true));
        }
        // 11+: plynne przejscie fiolet <-> AETHERFLUX
        MutableText out = Text.empty();
        for (int i = 0; i < LABEL.length(); i++) {
            char ch = LABEL.charAt(i);
            float t = 0.5f + 0.5f * MathHelper.sin(time * 0.12f - i * 0.45f);
            out.append(Text.literal(String.valueOf(ch)).setStyle(Style.EMPTY.withColor(lerp(VIOLET, AETHER, t)).withBold(true)));
        }
        return out;
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) MathHelper.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) MathHelper.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) MathHelper.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }

    public static void render(WorldRenderContext ctx) {
        ComboConfig cfg = ComboConfig.INSTANCE;
        if (!cfg.enabled) return;
        ComboTracker.State s = ComboTracker.currentState();
        Entity target = ComboTracker.currentEntity();
        if (s == null || target == null || s.hits < 4) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        float td = mc.getRenderTickCounter().getTickProgress(false);
        Camera cam = ctx.camera();
        Vec3d cp = cam.getPos();
        Vec3d p = ComboTracker.labelPos(target, td);

        double dist = p.distanceTo(cp);
        // skalowanie z odlegloscia, zeby napis byl czytelny z daleka (jak nametag, z limitem)
        float distScale = (float) MathHelper.clamp(dist / 6.0, 1.0, 3.5);
        float sinceHit = (ComboTracker.tick() - s.lastHitTick) + td;
        float pulse = 1f + 0.25f * Math.max(0f, 1f - sinceHit / 6f);
        float scale = 0.025f * cfg.textScale * distScale * pulse;

        float time = ComboTracker.tick() + td;
        Text text = buildText(s.hits, time);
        TextRenderer tr = mc.textRenderer;

        MatrixStack m = ctx.matrices();
        m.push();
        m.translate(p.x - cp.x, p.y - cp.y, p.z - cp.z);
        m.multiply(cam.getRotation());          // billboard: zawsze do kamery
        m.scale(scale, -scale, scale);
        m.scale(-1f, 1f, 1f);                   // tak jak w nametagach (tekst nie jest lustrzany)
        Matrix4f mat = m.peek().getPositionMatrix();
        float x = -tr.getWidth(text) / 2f;
        tr.draw(text, x, 0, 0xFFFFFFFF, false, mat, ctx.consumers(),
                TextRenderer.TextLayerType.SEE_THROUGH, 0x66000000, 0xF000F0);
        m.pop();
    }
}
