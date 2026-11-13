package com.example.realismsurvival.water;

import com.example.realismsurvival.api.water.WaterApi;
import com.example.realismsurvival.api.water.WaterQuality;
import com.example.realismsurvival.registry.ModWaterQualities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import org.jspecify.annotations.Nullable;

/**
 * Single water-like fluid pair shared by all {@link WaterQuality} grades. Grade is stored on
 * {@link WaterQualityStates#QUALITY} and copied through spread / source formation.
 *
 * <p>Distilled quality never forms infinite sources (still/loot product, not natural springs).
 */
public abstract class GradedFlowingFluid extends BaseFlowingFluid {

    protected GradedFlowingFluid(Properties properties) {
        super(properties);
    }

    /** Vanilla {@link net.minecraft.world.level.material.WaterFluid#animateTick} parity. */
    @Override
    public void animateTick(Level level, BlockPos pos, FluidState fluidState, RandomSource random) {
        if (!fluidState.isSource() && !fluidState.getValue(FALLING)) {
            if (random.nextInt(64) == 0) {
                level.playLocalSound(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        SoundEvents.WATER_AMBIENT,
                        SoundSource.AMBIENT,
                        random.nextFloat() * 0.25F + 0.75F,
                        random.nextFloat() + 0.5F,
                        false
                );
            }
        } else if (random.nextInt(10) == 0) {
            level.addParticle(
                    ParticleTypes.UNDERWATER,
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0.0,
                    0.0,
                    0.0
            );
        }
    }

    @Override
    public @Nullable ParticleOptions getDripParticle() {
        return ParticleTypes.DRIPPING_WATER;
    }

    @Override
    protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier) {
        effectApplier.apply(InsideBlockEffectType.EXTINGUISH);
    }

    @Override
    protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
        super.createFluidStateDefinition(builder);
        builder.add(WaterQualityStates.QUALITY);
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        BlockState block = super.createLegacyBlock(state);
        if (block.hasProperty(WaterQualityStates.QUALITY)) {
            block = block.setValue(WaterQualityStates.QUALITY, WaterQualityStates.qualityId(state));
        }
        return block;
    }

    public FluidState sourceOf(WaterQuality quality, boolean falling) {
        return WaterQualityStates.withQuality(getSource(falling), quality);
    }

    public FluidState flowingOf(WaterQuality quality, int level, boolean falling) {
        return WaterQualityStates.withQuality(getFlowing(level, falling), quality);
    }

    public FluidState sourceOfId(int qualityId, boolean falling) {
        return WaterQualityStates.withQualityId(getSource(falling), qualityId);
    }

    public FluidState flowingOfId(int qualityId, int level, boolean falling) {
        return WaterQualityStates.withQualityId(getFlowing(level, falling), qualityId);
    }

    /**
     * Vanilla logic with quality: strongest horizontal neighbour chooses quality; infinite sources
     * need two+ sources of that same quality.
     */
    @Override
    protected FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState blockState) {
        int bestAmount = 0;
        int chosenQuality = WaterQualityStates.idOf(ModWaterQualities.FRESH.get());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            cursor.setWithOffset(pos, direction);
            FluidState neighbor = level.getFluidState(cursor);
            if (!isFlowNeighbour(neighbor)) {
                continue;
            }
            int amount = neighbor.getAmount();
            if (amount > bestAmount) {
                bestAmount = amount;
                chosenQuality = qualityIdForFlow(level, cursor, neighbor);
            }
        }

        FluidState above = level.getFluidState(pos.above());
        if (!above.isEmpty() && isFlowNeighbour(above)) {
            return flowingOfId(qualityIdForFlow(level, pos.above(), above), 8, true);
        }

        int matchingSources = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            cursor.setWithOffset(pos, direction);
            FluidState neighbor = level.getFluidState(cursor);
            if (isFlowNeighbour(neighbor)
                    && neighbor.isSource()
                    && qualityIdForFlow(level, cursor, neighbor) == chosenQuality) {
                matchingSources++;
            }
        }

        if (canConvertToSource(getSource(false), level, pos)
                && matchingSources >= 2
                && canFormInfiniteSource(chosenQuality)) {
            return sourceOfId(chosenQuality, false);
        }

        int newAmount = bestAmount - getDropOff(level);
        if (newAmount <= 0) {
            return Fluids.EMPTY.defaultFluidState();
        }
        return flowingOfId(chosenQuality, newAmount, false);
    }

    /** Graded fluid or vanilla / waterlogged water — one surface for spread maths. */
    private boolean isFlowNeighbour(FluidState neighbor) {
        return !neighbor.isEmpty()
                && (neighbor.getType().isSame(this) || WaterSurfaceCompat.isWaterSurface(neighbor));
    }

    /** Distilled never renews as an infinite spring (machine/loot only). */
    public static boolean canFormInfiniteSource(int qualityId) {
        return WaterQualityStates.byId(qualityId) != ModWaterQualities.DISTILLED.get();
    }

    public static boolean canFormInfiniteSource(WaterQuality quality) {
        return quality != ModWaterQualities.DISTILLED.get();
    }

    private static int qualityIdForFlow(ServerLevel level, BlockPos pos, FluidState neighbor) {
        WaterQuality graded = WaterApi.fromFluidState(neighbor);
        if (graded != null) {
            return WaterQualityStates.idOf(graded);
        }
        if (WaterSurfaceCompat.isWaterSurface(neighbor)) {
            return WaterQualityStates.idOf(WaterApi.qualityAt(level, pos, neighbor));
        }
        return WaterQualityStates.idOf(ModWaterQualities.FRESH.get());
    }

    @Override
    protected boolean canBeReplacedWith(
            FluidState state,
            BlockGetter level,
            BlockPos pos,
            Fluid fluidIn,
            Direction direction
    ) {
        if (isSame(fluidIn)) {
            return false;
        }
        if (WaterSurfaceCompat.canDisplaceFlowingWaterLike(state, fluidIn)) {
            return true;
        }
        return direction == Direction.DOWN && !isSame(fluidIn);
    }

    public static class Source extends GradedFlowingFluid {
        public Source(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any()
                    .setValue(WaterQualityStates.QUALITY, 0));
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends GradedFlowingFluid {
        public Flowing(Properties properties) {
            super(properties);
            registerDefaultState(getStateDefinition().any()
                    .setValue(LEVEL, 7)
                    .setValue(WaterQualityStates.QUALITY, 0));
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
