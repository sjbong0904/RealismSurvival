package com.example.realismsurvival.environment;

import com.example.realismsurvival.api.ModBlockTags;
import com.example.realismsurvival.api.ModFluidTags;
import com.example.realismsurvival.api.environment.EnvironmentContext;
import com.example.realismsurvival.api.event.BreathCalculateEvent;
import com.example.realismsurvival.api.event.ShelterCalculateEvent;
import com.example.realismsurvival.api.event.WetnessCalculateEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Samples surroundings and fires shelter / wetness / breath events for addon climate & gear systems.
 *
 * <p>Core temperature uses its own contact/scan math; thirst swim restore reads
 * {@link EnvironmentContext#submerged()}. Shelter / wetness multipliers remain addon hooks.
 */
public final class EnvironmentProbe {

    private static final int SHELTER_SCAN_HEIGHT = 8;

    private EnvironmentProbe() {}

    public static EnvironmentContext probe(ServerPlayer player, ServerLevel level, Holder<Biome> biome) {
        BlockPos pos = player.blockPosition();
        int sky = level.getBrightness(LightLayer.SKY, pos);
        int block = level.getBrightness(LightLayer.BLOCK, pos);

        float shelterFactor = computeShelterFactor(level, pos);
        boolean sheltered = shelterFactor > 0.0F;

        boolean inWaterOrRain = player.isInWaterOrRain();
        float wetness = inWaterOrRain ? 1.0F : 0.0F;

        FluidState headFluid = level.getFluidState(BlockPos.containing(player.getX(), player.getEyeY(), player.getZ()));
        boolean submerged = !headFluid.isEmpty()
                && (headFluid.is(FluidTags.WATER)
                        || headFluid.is(FluidTags.LAVA)
                        || headFluid.is(ModFluidTags.DRINKABLE_FLUIDS)
                        || headFluid.is(ModFluidTags.SUBMERGING_FLUIDS));
        Fluid submergedFluid = submerged ? headFluid.getType() : Fluids.EMPTY;
        boolean canBreathe = !submerged
                || player.canBreatheUnderwater()
                || headFluid.is(ModFluidTags.BREATHABLE_FLUIDS);
        float airRatio = player.getMaxAirSupply() > 0
                ? (float) player.getAirSupply() / (float) player.getMaxAirSupply()
                : 1.0F;

        EnvironmentContext ctx = new EnvironmentContext(
                level,
                biome,
                pos,
                level.dimension(),
                level.isRaining(),
                level.isThundering(),
                sky,
                block,
                sheltered,
                shelterFactor,
                wetness > 0.0F,
                wetness,
                submerged,
                submergedFluid,
                canBreathe,
                airRatio
        );

        NeoForge.EVENT_BUS.post(new ShelterCalculateEvent(player, ctx));
        NeoForge.EVENT_BUS.post(new WetnessCalculateEvent(player, ctx));
        NeoForge.EVENT_BUS.post(new BreathCalculateEvent(player, ctx));
        return ctx;
    }

    /**
     * 0 if can see sky with no shelter blocks above; otherwise up to 1 from ceiling / tagged shelter.
     */
    static float computeShelterFactor(ServerLevel level, BlockPos pos) {
        if (!level.canSeeSky(pos)) {
            return 1.0F;
        }
        int solid = 0;
        BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int dy = 1; dy <= SHELTER_SCAN_HEIGHT; dy++) {
            cursor.set(pos.getX(), pos.getY() + dy, pos.getZ());
            if (cursor.getY() >= level.getMaxY()) {
                break;
            }
            BlockState state = level.getBlockState(cursor);
            if (state.isAir()) {
                continue;
            }
            if (state.is(ModBlockTags.SHELTER_BLOCKS) || state.canOcclude()) {
                solid++;
            }
        }
        if (solid <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, solid / 3.0F);
    }
}
