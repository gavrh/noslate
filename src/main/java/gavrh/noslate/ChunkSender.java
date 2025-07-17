package gavrh.noslate;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;

public class ChunkSender {

    private static class FilteredLevelChunk extends LevelChunk {
        private final LevelChunkSection[] filteredSections;

        public FilteredLevelChunk(LevelChunk original, LevelChunkSection[] filteredSections) {
            super(original.getLevel(), original.getPos());
            this.filteredSections = filteredSections;
        }

        @Override
        public LevelChunkSection[] getSections() {
            return filteredSections;
        }

        @Override
        public LevelChunkSection getSection(int index) {
            if (index < 0 || index >= filteredSections.length) return null;
            return filteredSections[index];
        }
    }

    public static ClientboundLevelChunkWithLightPacket createFilteredChunkPacket(ServerPlayer player, LevelChunk chunk) {
        Level level = chunk.getLevel();
        double playerY = player.getY();
        ChunkPos playerChunkPos = player.chunkPosition();
        ChunkPos chunkPos = chunk.getPos();
        int radius = 2;

        boolean insideRadius = Math.abs(chunkPos.x - playerChunkPos.x) <= radius
        && Math.abs(chunkPos.z - playerChunkPos.z) <= radius;

        Registry<Biome> biomeRegistry = level.registryAccess().lookupOrThrow(Registries.BIOME);
        LevelChunkSection[] originalSections = chunk.getSections();
        int minSectionY = chunk.getMinSectionY();
        LevelChunkSection[] filteredSections = new LevelChunkSection[originalSections.length];

        int revealThresholdY = 1;

        for (int i = 0; i < originalSections.length; i++) {
            int sectionY = minSectionY + i;
            int sectionTopY = (sectionY * 16) + 15;
            boolean isBelowThreshold = sectionTopY <= 0;

            if (playerY > revealThresholdY) {
                filteredSections[i] = isBelowThreshold
                ? createEmptySection(biomeRegistry, level, chunkPos, sectionY)
                : originalSections[i];
            } else {
                if (isBelowThreshold) {
                    filteredSections[i] = insideRadius
                    ? originalSections[i]
                    : createEmptySection(biomeRegistry, level, chunkPos, sectionY);
                } else {
                    filteredSections[i] = originalSections[i];
                }
            }

            if (filteredSections[i] == null) {
                filteredSections[i] = createEmptySection(biomeRegistry, level, chunkPos, sectionY);
            }
        }

        FilteredLevelChunk filteredChunk = new FilteredLevelChunk(chunk, filteredSections);
        LevelLightEngine lightEngine = level.getLightEngine();

        return new ClientboundLevelChunkWithLightPacket(filteredChunk, lightEngine, null, null, false);
    }

    private static LevelChunkSection createEmptySection(Registry<Biome> biomeRegistry, Level level, ChunkPos chunkPos, int sectionY) {
        return new LevelChunkSection(biomeRegistry, level, chunkPos, sectionY);
    }
}
