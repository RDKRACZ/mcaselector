package net.querz.mcaselector.version;

import net.querz.mcaselector.io.registry.BiomeRegistry;
import net.querz.nbt.io.SNBTUtil;
import net.querz.nbt.tag.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

public interface ChunkFilter {

	// returns true if ALL block names are present
	boolean matchBlockNames(CompoundTag data, Collection<String> names);

	// returns true if ALL biomes are present
	boolean matchBiomes(CompoundTag data, Collection<BiomeRegistry.BiomeIdentifier> biomes);

	// returns true if AT LEAST ONE block name is present
	boolean matchAnyBlockName(CompoundTag data, Collection<String> names);

	// returns true if the palette ONLY contains the block names, ignoring air
	boolean paletteEquals(CompoundTag data, Collection<String> names);

	// returns true if AT LEAST ONE biome is present
	boolean matchAnyBiome(CompoundTag data, Collection<BiomeRegistry.BiomeIdentifier> biomes);

	void changeBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome);

	void forceBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome);

	void replaceBlocks(CompoundTag data, Map<String, BlockReplaceData> replace);

	int getAverageHeight(CompoundTag data);

	int getBlockAmount(CompoundTag data, String[] blocks);

	ListTag<CompoundTag> getTileEntities(CompoundTag data);

	CompoundTag getStructureStarts(CompoundTag data);

	CompoundTag getStructureReferences(CompoundTag data);

	ListTag<CompoundTag> getSections(CompoundTag data);

	LongTag getInhabitedTime(CompoundTag data);

	void setInhabitedTime(CompoundTag data, long inhabitedTime);

	StringTag getStatus(CompoundTag data);

	void setStatus(CompoundTag data, String status);

	LongTag getLastUpdate(CompoundTag data);

	void setLastUpdate(CompoundTag data, long lastUpdate);

	IntTag getXPos(CompoundTag data);

	IntTag getYPos(CompoundTag data);

	IntTag getZPos(CompoundTag data);

	ByteTag getLightPopulated(CompoundTag data);

	void setLightPopulated(CompoundTag data, byte lightPopulated);

	void forceBlending(CompoundTag data);

	class BlockReplaceData {

		private String name;
		private CompoundTag state;
		private CompoundTag tile;
		private final BlockReplaceType type;

		public BlockReplaceData(String name) {
			type = BlockReplaceType.NAME;
			this.name = name;
			state = new CompoundTag();
			state.putString("Name", name);
		}

		public BlockReplaceData(String name, CompoundTag tile) {
			type = BlockReplaceType.NAME_TILE;
			this.name = name;
			this.tile = tile;
			state = new CompoundTag();
			state.putString("Name", name);
		}

		public BlockReplaceData(CompoundTag state) {
			type = BlockReplaceType.STATE;
			this.state = state;
			name = state.getString("Name");
		}

		public BlockReplaceData(CompoundTag state, CompoundTag tile) {
			type = BlockReplaceType.STATE_TILE;
			this.state = state;
			this.tile = tile;
			name = state.getString("Name");
		}

		public BlockReplaceType getType() {
			return type;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setState(CompoundTag state) {
			this.state = state;
		}

		public CompoundTag getState() {
			return state;
		}

		public void setTile(CompoundTag tile) {
			this.tile = tile;
		}

		public CompoundTag getTile() {
			return tile;
		}

		@Override
		public String toString() {
			try {
				switch (type) {
					case NAME:
						if (name.startsWith("minecraft:")) {
							return name;
						} else {
							return "'" + name + "'";
						}
					case STATE:
						return SNBTUtil.toSNBT(state);
					case STATE_TILE:
						return SNBTUtil.toSNBT(state) + ";" + SNBTUtil.toSNBT(tile);
					case NAME_TILE:
						if (name.startsWith("minecraft:")) {
							return name + ";" + SNBTUtil.toSNBT(tile);
						} else {
							return "'" + name + "';" + SNBTUtil.toSNBT(tile);
						}
					default:
						return null;
				}
			} catch (IOException ex) {
				return null;
			}
		}
	}

	enum BlockReplaceType {
		NAME, STATE, STATE_TILE, NAME_TILE
	}
}
