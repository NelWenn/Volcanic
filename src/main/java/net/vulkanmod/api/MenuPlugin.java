package net.vulkanmod.api;

import java.util.List;

/**
 * PROVISOIRE — à supprimer quand la pipeline de Revo saura déclarer ses paramètres.
 *
 * <p>Pourquoi cette interface existe. Le SPI actuel, {@code net.vulkanmod.render.plugin
 * .RenderPipelinePlugin}, expose quatre méthodes : {@code id()}, {@code name()},
 * {@code createPipelineManager()} et {@code createFrameGraph()}. Il ne permet ni de déclarer
 * des paramètres, ni de dire si le plugin est un shader. Le menu sait déjà tout afficher —
 * cartes, onglets, curseurs, favoris, recherche — il lui manque uniquement la source.
 *
 * <p>REVO, CE QU'IL FAUT FAIRE DE TON CÔTÉ, et cette interface disparaît :
 *
 * <ol>
 *   <li><b>Ajoute un discriminant sur {@code RenderPipelinePlugin}.</b> Un {@code kind()},
 *       une interface marqueur, la forme t'appartient. Le menu doit pouvoir répondre à
 *       « est-ce un shader ? » sans deviner :
 *       <ul>
 *         <li>shader → il apparaît dans la liste des shaders, ses paramètres vont sous
 *             {@code shaders.settings}</li>
 *         <li>pas un shader → il a sa propre page dans la catégorie Plugins (cas de Caldera,
 *             qui est un plugin sans shader ni frame graph)</li>
 *       </ul>
 *   </li>
 *   <li><b>Ajoute la déclaration de paramètres sur {@code RenderPipelinePlugin}</b>, pas sur un
 *       type propre aux shaders. Tout plugin doit pouvoir en exposer ; être un shader est un cas
 *       particulier, pas la règle. La forme de {@link MenuSetting} ci-dessous est une proposition,
 *       pas une contrainte — remplace-la par ce qui t'arrange, on adaptera le menu.</li>
 *   <li><b>Si tu passes par des annotations, mets-les en {@code RetentionPolicy.RUNTIME}.</b>
 *       En {@code CLASS}, qui est le défaut, la réflexion ne voit rien et l'échec est totalement
 *       silencieux — on a déjà perdu du temps là-dessus.</li>
 *   <li><b>N'encode pas de chemin d'interface dans l'identifiant.</b> Un {@code group} logique
 *       suffit, le menu fait la correspondance vers une page ou un onglet. Si l'id porte le
 *       placement, déplacer un paramètre casse les favoris enregistrés et les liens de recherche.</li>
 * </ol>
 *
 * <p>OÙ C'EST BRANCHÉ CÔTÉ MENU, pour que tu saches quoi remplacer :
 * {@code net.vulkanmod.config.ui.settings.MenuPlugins} charge les implémentations par
 * {@link java.util.ServiceLoader} et les convertit en réglages du menu. Le jour où tu livres,
 * cette classe lit ton SPI à la place et ce fichier est supprimé, sans rien changer d'autre.
 *
 * <p>Un plugin qui ne déclare rien reste valable : il obtient une page qui affiche son nom et son
 * état, et rien d'autre. C'est volontaire — le menu ne doit jamais dépendre de ce qu'un tiers a
 * pensé à remplir.
 */
public interface MenuPlugin {

    String id();

    String displayName();

    default boolean enabled() {
        return true;
    }

    /**
     * Un plugin qui peut être coupé depuis le menu renvoie {@code true} et implémente
     * {@link #setEnabled(boolean)}. Par défaut le menu n'affiche aucun interrupteur : un plugin
     * qu'on ne peut pas éteindre ne doit pas faire semblant.
     */
    default boolean toggleable() {
        return false;
    }

    default void setEnabled(boolean enabled) {
    }

    default List<MenuSetting> settings() {
        return List.of();
    }

    /**
     * Une ligne libre sous le titre de la vitrine — auteur, version, ce que le plugin veut.
     * {@code null} : la ligne n'est pas affichée.
     */
    default String byline() {
        return null;
    }

    /**
     * Description courte affichée dans la vitrine du plugin. {@code null} : le menu affiche
     * sa note par défaut (nombre de groupes de réglages).
     */
    default String description() {
        return null;
    }

    /**
     * Jusqu'à quatre étiquettes libres, rendues en pastilles (LOD, Terrain, …). Le menu ajoute
     * lui-même « Requires Volcanic » ; inutile de le déclarer.
     */
    default List<String> tags() {
        return List.of();
    }

    /**
     * Icône carrée de la vitrine, en chemin texture complet, par exemple
     * {@code "caldera:textures/gui/plugin_icon.png"}. 64×64 recommandé — le filtrage est
     * toujours nearest, fournir la taille réellement affichée. {@code null} : le menu dessine
     * un glyphe à la première lettre du nom.
     */
    default String iconTexture() {
        return null;
    }

    /**
     * Bannière 16:9 de la vitrine, en chemin texture complet, par exemple
     * {@code "caldera:textures/gui/plugin_banner.png"}. 432×243 recommandé, nearest oblige.
     * Le voile de lisibilité est ajouté par le menu, la bannière n'a pas à le contenir.
     * {@code null} : fond généré depuis l'identifiant du plugin.
     */
    default String bannerTexture() {
        return null;
    }

    default void onApply() {
    }
}
