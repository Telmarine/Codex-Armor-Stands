package com.tac5studios.codexarmorstands.mixin;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** ArmorStand.disabledSlots is private in 1.21.1 (it's the DisabledSlots tag). */
@Mixin(ArmorStand.class)
public interface ArmorStandAccessor {

    @Accessor("disabledSlots")
    int codexArmorStands$getDisabledSlots();

    @Accessor("disabledSlots")
    void codexArmorStands$setDisabledSlots(int value);
}
