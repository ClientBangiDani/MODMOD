package pl.aetherflux.combo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Konfiguracja AETHERFLUX Combo Aura. Licznik kombinacji NIE jest tu zapisywany. */
public final class ComboConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("aetherflux_combo_aura.json");
    public static ComboConfig INSTANCE = new ComboConfig();

    public boolean enabled = true;
    /** Po ilu tickach (20 = 1 s) bez kolejnego potwierdzonego trafienia kombinacja wygasa. */
    public int timeoutTicks = 60;
    /** Zmiana celu zeruje kombinacje (true) albo kazdy cel ma osobny licznik (false). */
    public boolean resetOnTargetSwitch = true;
    /** Zerowanie kombinacji, gdy gracz sam dostanie obrazenia. */
    public boolean resetOnPlayerHurt = false;
    /** Okno (w tickach), w ktorym po ataku czekamy na potwierdzenie skutku (animacja obrazen celu). */
    public int confirmWindowTicks = 8;
    /** Maks. odleglosc celu od gracza przy potwierdzeniu trafienia. */
    public double maxConfirmDistance = 7.0;
    public float textScale = 1.0f;
    public boolean particles = true;
    /** Limit czasteczek wybuchu (ochrona wydajnosci). */
    public int maxBurstParticles = 48;

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                ComboConfig c = GSON.fromJson(Files.readString(FILE), ComboConfig.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception e) {
            AetherfluxComboAura.LOGGER.warn("Nie mozna wczytac konfiguracji, uzywam domyslnej", e);
        }
        INSTANCE.timeoutTicks = Math.max(10, INSTANCE.timeoutTicks);
        INSTANCE.confirmWindowTicks = Math.max(2, Math.min(40, INSTANCE.confirmWindowTicks));
        INSTANCE.maxBurstParticles = Math.max(0, Math.min(200, INSTANCE.maxBurstParticles));
        save();
    }

    public static void save() {
        try { Files.writeString(FILE, GSON.toJson(INSTANCE)); }
        catch (Exception e) { AetherfluxComboAura.LOGGER.warn("Nie mozna zapisac konfiguracji", e); }
    }
}
