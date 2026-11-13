package com.example.realismsurvival.mixin;

import com.example.realismsurvival.component.SurvivalAttachments;
import com.example.realismsurvival.water.VanillaWaterConverter;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * After FEATURES (structures included) and before lighting: rewrite vanilla water to graded fluids
 * on the ProtoChunk so player load skips migration.
 */
@Mixin(ChunkStatusTasks.class)
public abstract class ChunkStatusTasksMixin {

    @Inject(method = "initializeLight", at = @At("HEAD"))
    private static void realismsurvival$convertGradedWater(
            WorldGenContext context,
            ChunkStep step,
            StaticCache2D<GenerationChunkHolder> chunks,
            ChunkAccess chunk,
            CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir
    ) {
        if (chunk.getData(SurvivalAttachments.WATER_CONVERTED)) {
            return;
        }
        VanillaWaterConverter.convertChunkAccess(chunk);
    }
}
