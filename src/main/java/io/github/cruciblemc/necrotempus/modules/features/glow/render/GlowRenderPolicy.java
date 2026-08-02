package io.github.cruciblemc.necrotempus.modules.features.glow.render;

/** Rendering decisions shared by the glow silhouette mixins and their tests. */
public final class GlowRenderPolicy {

    private GlowRenderPolicy() {}

    /**
     * Keeps vanilla invisibility in the regular scene, while allowing the glow pass to capture the
     * entity's actual model as an outline source.
     */
    public static boolean shouldRenderBaseModel(boolean invisible, boolean silhouettePassActive) {
        return !invisible || silhouettePassActive;
    }
}
