package com.example.realismsurvival.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseCoralPlantTypeBlock;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.CoralFanBlock;
import net.minecraft.world.level.block.CoralPlantBlock;
import net.minecraft.world.level.block.CoralWallFanBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Waterlogged coral schedules {@link Fluids#WATER} ticks that try to spread vanilla water into
 * graded streams. Skip those ticks — open graded water already owns the surface.
 */
@Mixin(
        value = {
            BaseCoralPlantTypeBlock.class,
            BaseCoralWallFanBlock.class,
            CoralPlantBlock.class,
            CoralFanBlock.class,
            CoralWallFanBlock.class
        }
)
public abstract class CoralSkipWaterFluidTickMixin {

    @Redirect(
            method = "updateShape",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/ScheduledTickAccess;scheduleTick(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/material/Fluid;I)V"
            )
    )
    private void realismsurvival$skipVanillaWaterTick(
            ScheduledTickAccess ticks,
            BlockPos pos,
            Fluid fluid,
            int delay
    ) {
        if (fluid != Fluids.WATER && fluid != Fluids.FLOWING_WATER) {
            ticks.scheduleTick(pos, fluid, delay);
        }
    }
}
