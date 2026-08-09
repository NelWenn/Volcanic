package net.vulkanmod.api;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * PROVISOIRE — voir {@link MenuPlugin}. Un paramètre déclaré par un plugin.
 *
 * <p>Le {@code group} est logique, pas un chemin d'interface : le menu en fait un onglet sur la page
 * du plugin. Deux paramètres du même groupe se retrouvent côte à côte, et renommer le groupe ne casse
 * ni les favoris ni la recherche puisque l'identité d'un réglage est {@code pluginId + key}.
 *
 * <p>{@code titleKey} est une clé de traduction, jamais un littéral affiché tel quel — sinon le menu
 * n'est traduisible dans aucune langue.
 */
public final class MenuSetting {

    public enum Kind {
        TOGGLE,
        SLIDER,
        CHOICE
    }

    private final String key;
    private final String group;
    private final String titleKey;
    private final Kind kind;
    private final int min;
    private final int max;
    private final int step;
    private final List<String> choices;
    private final Supplier<Object> getter;
    private final Consumer<Object> setter;
    private final boolean restartRequired;
    private final Object defaultValue;
    private final String descriptionKey;

    private MenuSetting(String key, String group, String titleKey, Kind kind, int min, int max, int step,
                        List<String> choices, Supplier<Object> getter, Consumer<Object> setter,
                        boolean restartRequired, Object defaultValue, String descriptionKey) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (titleKey == null || titleKey.isBlank()) {
            throw new IllegalArgumentException("titleKey must not be blank");
        }
        if (getter == null || setter == null) {
            throw new IllegalArgumentException("a setting needs both a getter and a setter: " + key);
        }
        this.key = key;
        this.group = group == null || group.isBlank() ? "general" : group;
        this.titleKey = titleKey;
        this.kind = kind;
        this.min = min;
        this.max = max;
        this.step = step;
        this.choices = choices == null ? List.of() : List.copyOf(choices);
        this.getter = getter;
        this.setter = setter;
        this.restartRequired = restartRequired;
        this.defaultValue = defaultValue;
        this.descriptionKey = descriptionKey;
    }

    public static MenuSetting toggle(String key, String group, String titleKey,
                                     Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return new MenuSetting(key, group, titleKey, Kind.TOGGLE, 0, 1, 1, List.of(),
                getter::get, value -> setter.accept((Boolean) value), false, getter.get(), null);
    }

    public static MenuSetting slider(String key, String group, String titleKey, int min, int max, int step,
                                     Supplier<Integer> getter, Consumer<Integer> setter) {
        if (max <= min) {
            throw new IllegalArgumentException("max must be above min for " + key);
        }
        if (step <= 0) {
            throw new IllegalArgumentException("step must be positive for " + key);
        }
        return new MenuSetting(key, group, titleKey, Kind.SLIDER, min, max, step, List.of(),
                getter::get, value -> setter.accept((Integer) value), false, getter.get(), null);
    }

    public static MenuSetting choice(String key, String group, String titleKey, List<String> choices,
                                     Supplier<String> getter, Consumer<String> setter) {
        if (choices == null || choices.isEmpty()) {
            throw new IllegalArgumentException("a choice setting needs choices: " + key);
        }
        return new MenuSetting(key, group, titleKey, Kind.CHOICE, 0, 0, 1, choices,
                getter::get, value -> setter.accept((String) value), false, getter.get(), null);
    }

    public MenuSetting requiringRestart() {
        return new MenuSetting(key, group, titleKey, kind, min, max, step, choices, getter, setter,
                true, defaultValue, descriptionKey);
    }

    /** La valeur de repli du bouton « réinitialiser ». Par défaut, celle lue au démarrage. */
    public MenuSetting withDefault(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("default must not be null for " + key);
        }
        return new MenuSetting(key, group, titleKey, kind, min, max, step, choices, getter, setter,
                restartRequired, value, descriptionKey);
    }

    /** Clé de traduction affichée dans le panneau de détails. Sans elle, la fiche n'a que le titre. */
    public MenuSetting describedBy(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("descriptionKey must not be blank");
        }
        return new MenuSetting(this.key, group, titleKey, kind, min, max, step, choices, getter, setter,
                restartRequired, defaultValue, key);
    }

    public Object defaultValue() {
        return defaultValue;
    }

    public String descriptionKey() {
        return descriptionKey;
    }

    public String key() {
        return key;
    }

    public String group() {
        return group;
    }

    public String titleKey() {
        return titleKey;
    }

    public Kind kind() {
        return kind;
    }

    public int min() {
        return min;
    }

    public int max() {
        return max;
    }

    public int step() {
        return step;
    }

    public List<String> choices() {
        return choices;
    }

    public Object get() {
        return getter.get();
    }

    public void set(Object value) {
        setter.accept(value);
    }

    public boolean restartRequired() {
        return restartRequired;
    }
}
