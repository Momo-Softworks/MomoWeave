package com.momosoftworks.momoweave.mixin;

import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ChunkGenerator.class)
public class MixinBiomeOrePassthrough
{
    //@Inject(method = "applyBiomeDecoration", at = @At(value = "INVOKE", target = "Ljava/util/Set;retainAll(Ljava/util/Collection;)Z", shift = At.Shift.AFTER),
    //        locals = LocalCapture.CAPTURE_FAILHARD)
    //private void passthroughOreFeatures(WorldGenLevel pLevel, ChunkAccess pChunk, StructureManager pStructureManager, CallbackInfo ci,
    //                                    // locals
    //                                    ChunkPos chunkpos, SectionPos sectionpos, BlockPos blockpos, Registry<Structure> registry, Map<Integer, List<Structure>> map,
    //                                    List<FeatureSorter.StepFeatureData> list, WorldgenRandom worldgenrandom, long i, Set<Holder<Biome>> set)
    //{
    //    set.removeIf(biome -> biome.is(Tags.Biomes.IS_UNDERGROUND));
    //}
}
