package com.paulorchard.islandcraft.weatherofarrakis.storms;

import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Our own code (not adapted from upstream): applies none of the copied mixins when the real Immersive Storms is on the classpath, because the two
 * cannot run together (our copy then registers nothing, so the mixins would touch unregistered attributes).
 */
public final class StormsMixinPlugin implements IMixinConfigPlugin {
    private static final String REAL_MOD_CLASS = "com/thedeathlycow/immersive/storms/ImmersiveStorms.class";
    private boolean realModPresent;

    @Override
    public void onLoad(String mixinPackage) {
        ClassLoader loader = StormsMixinPlugin.class.getClassLoader();
        realModPresent = loader != null && loader.getResource(REAL_MOD_CLASS) != null;
        if (realModPresent) {
            System.out.println("[IslandCraft Weather of Arrakis] The real Immersive Storms is on the classpath: the copied mixins in " + mixinPackage + " are not applied.");
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !realModPresent;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
