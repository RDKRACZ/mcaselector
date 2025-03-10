package net.querz.mcaselector.version.anvil119;

import net.querz.mcaselector.io.registry.BiomeRegistry;
import net.querz.mcaselector.point.Point2i;
import net.querz.mcaselector.point.Point3i;
import net.querz.mcaselector.tiles.Tile;
import net.querz.mcaselector.version.Helper;
import net.querz.mcaselector.version.anvil117.Anvil117ChunkFilter;
import net.querz.nbt.tag.*;
import java.util.*;

public class Anvil119ChunkFilter extends Anvil117ChunkFilter {

	@Override
	public boolean matchBlockNames(CompoundTag data, Collection<String> names) {
		ListTag<CompoundTag> sections = Helper.tagFromCompound(data, "sections");
		if (sections == null) {
			return false;
		}

		int c = 0;
		nameLoop:
		for (String name : names) {
			for (CompoundTag t : sections) {
				ListTag<CompoundTag> palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette) {
					if (name.equals(Helper.stringFromCompound(p, "Name"))) {
						c++;
						continue nameLoop;
					}
				}
			}
		}
		return names.size() == c;
	}

	@Override
	public boolean matchAnyBlockName(CompoundTag data, Collection<String> names) {
		ListTag<CompoundTag> sections = Helper.tagFromCompound(data, "sections");
		if (sections == null) {
			return false;
		}

		for (String name : names) {
			for (CompoundTag t : sections) {
				ListTag<CompoundTag> palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
				if (palette == null) {
					continue;
				}
				for (CompoundTag p : palette) {
					if (name.equals(Helper.stringFromCompound(p, "Name"))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean paletteEquals(CompoundTag data, Collection<String> names) {
		ListTag<CompoundTag> sections = Helper.tagFromCompound(data, "sections");
		if (sections == null) {
			return false;
		}

		Set<String> blocks = new HashSet<>();
		for (CompoundTag t : sections) {
			ListTag<CompoundTag> palette = Helper.tagFromCompound(Helper.tagFromCompound(t, "block_states"), "palette");
			if (palette == null) {
				continue;
			}
			for (CompoundTag p : palette) {
				String n;
				if ((n = Helper.stringFromCompound(p, "Name")) != null) {
					if (!names.contains(n)) {
						return false;
					}
					blocks.add(n);
				}
			}
		}
		if (blocks.size() != names.size()) {
			return false;
		}
		for (String name : names) {
			if (!blocks.contains(name)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean matchBiomes(CompoundTag data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
		ListTag<CompoundTag> sections = Helper.tagFromCompound(data, "sections");
		if (sections == null) {
			return false;
		}

		Set<String> names = new HashSet<>(biomes.size());

		filterLoop:
		for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
			for (CompoundTag section : sections) {
				ListTag<StringTag> biomePalette = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
				if (biomePalette == null) {
					continue filterLoop;
				}
				for (StringTag biomeName : biomePalette) {
					if (identifier.matches(biomeName.getValue())) {
						names.add(biomeName.getValue());
						continue filterLoop;
					}
				}
			}
		}
		return biomes.size() == names.size();
	}

	@Override
	public boolean matchAnyBiome(CompoundTag data, Collection<BiomeRegistry.BiomeIdentifier> biomes) {
		ListTag<CompoundTag> sections = Helper.tagFromCompound(data, "sections");
		if (sections == null) {
			return false;
		}

		for (BiomeRegistry.BiomeIdentifier identifier : biomes) {
			for (CompoundTag section : sections) {
				ListTag<StringTag> biomePalette = Helper.tagFromCompound(Helper.tagFromCompound(section, "biomes"), "palette");
				if (biomePalette == null) {
					continue;
				}
				for (StringTag biomeName : biomePalette) {
					if (identifier.matches(biomeName.getValue())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void changeBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome) {
		ListTag<CompoundTag> sections = Helper.tagFromCompound(data, "sections");
		if (sections == null) {
			return;
		}

		for (CompoundTag section : sections) {
			CompoundTag biomes = Helper.tagFromCompound(section, "biomes");
			if (biomes == null) {
				continue;
			}

			ListTag<StringTag> newBiomePalette = new ListTag<>(StringTag.class);
			newBiomePalette.addString(biome.getName());
			biomes.put("palette", newBiomePalette);
			biomes.putLongArray("data", new long[1]);
		}
	}

	@Override
	public void forceBiome(CompoundTag data, BiomeRegistry.BiomeIdentifier biome) {
		ListTag<CompoundTag> sections = Helper.tagFromCompound(data, "sections");
		if (sections == null) {
			return;
		}

		for (CompoundTag section : sections) {
			CompoundTag biomes = new CompoundTag();
			ListTag<StringTag> newBiomePalette = new ListTag<>(StringTag.class);
			newBiomePalette.addString(biome.getName());
			biomes.put("palette", newBiomePalette);
			biomes.putLongArray("data", new long[1]);
			section.put("biomes", new CompoundTag());
		}
	}

	@Override
	public void replaceBlocks(CompoundTag data, Map<String, BlockReplaceData> replace) {
		ListTag<CompoundTag> sections = Helper.tagFromCompound(data, "sections");
		if (sections == null) {
			return;
		}

		Point2i pos = Helper.point2iFromCompound(data, "xPos", "zPos");
		if (pos == null) {
			return;
		}
		pos = pos.chunkToBlock();

		// handle the special case when someone wants to replace air with something else
		if (replace.containsKey("minecraft:air")) {
			Map<Integer, CompoundTag> sectionMap = new HashMap<>();
			List<Integer> heights = new ArrayList<>(26);
			for (CompoundTag section : sections) {
				sectionMap.put(section.getNumber("Y").intValue(), section);
				heights.add(section.getNumber("Y").intValue());
			}

			for (int y = -4; y < 20; y++) {
				if (!sectionMap.containsKey(y)) {
					sectionMap.put(y, completeSection(new CompoundTag(), y));
					heights.add(y);
				} else {
					CompoundTag section = sectionMap.get(y);
					if (!section.containsKey("block_states")) {
						completeSection(sectionMap.get(y), y);
					}
				}
			}

			heights.sort(Integer::compareTo);
			sections.clear();

			for (int height : heights) {
				sections.add(sectionMap.get(height));
			}
		}

		ListTag<CompoundTag> tileEntities = Helper.tagFromCompound(data, "block_entities");
		if (tileEntities == null) {
			tileEntities = new ListTag<>(CompoundTag.class);
		}

		for (CompoundTag section : sections) {
			CompoundTag blockStatesTag = section.getCompoundTag("block_states");
			ListTag<CompoundTag> palette = Helper.tagFromCompound(blockStatesTag, "palette");
			long[] blockStates = Helper.longArrayFromCompound(blockStatesTag, "data");
			if (palette == null) {
				continue;
			}

			if (palette.size() == 1 && blockStates == null) {
				blockStates = new long[256];
			}

			int y = Helper.numberFromCompound(section, "Y", -5).intValue();
			if (y < -4 || y > 19) {
				continue;
			}

			section.remove("BlockLight");
			section.remove("SkyLight");

			for (int i = 0; i < 4096; i++) {
				CompoundTag blockState = getBlockAt(i, blockStates, palette);

				for (Map.Entry<String, BlockReplaceData> entry : replace.entrySet()) {
					if (!blockState.getString("Name").matches(entry.getKey())) {
						continue;
					}
					BlockReplaceData replacement = entry.getValue();

					try {
						blockStates = setBlockAt(i, replacement.getState(), blockStates, palette);
					} catch (Exception ex) {
						throw new RuntimeException("failed to set block in section " + y, ex);
					}

					Point3i location = indexToLocation(i).add(pos.getX(), y * 16, pos.getZ());

					if (replacement.getTile() != null) {
						CompoundTag tile = replacement.getTile().clone();
						tile.putInt("x", location.getX());
						tile.putInt("y", location.getY());
						tile.putInt("z", location.getZ());
						tileEntities.add(tile);
					} else if (tileEntities.size() != 0) {
						for (int t = 0; t < tileEntities.size(); t++) {
							CompoundTag tile = tileEntities.get(t);
							if (tile.getInt("x") == location.getX()
								&& tile.getInt("y") == location.getY()
								&& tile.getInt("z") == location.getZ()) {
								tileEntities.remove(t);
								break;
							}
						}
					}

				}
			}

			try {
				blockStates = cleanupPalette(blockStates, palette);
			} catch (Exception ex) {
				throw new RuntimeException("failed to cleanup section " + y, ex);
			}

			if (blockStates == null) {
				blockStatesTag.remove("data");
			} else {
				blockStatesTag.putLongArray("data", blockStates);
			}
		}

		data.put("block_entities", tileEntities);
	}

	@Override
	protected long[] adjustBlockStateBits(ListTag<CompoundTag> palette, long[] blockStates, Map<Integer, Integer> oldToNewMapping) {
		if (palette.size() == 1) {
			return null;
		}
		return super.adjustBlockStateBits(palette, blockStates, oldToNewMapping);
	}

	@Override
	protected int getPaletteIndex(int blockIndex, long[] blockStates) {
		if (blockStates == null) {
			return 0;
		}
		return super.getPaletteIndex(blockIndex, blockStates);
	}

	protected CompoundTag completeSection(CompoundTag section, int y) {
		section.putByte("Y", (byte) y);
		if (!section.containsKey("block_states")) {
			CompoundTag newBlockStates = new CompoundTag();
			section.put("block_states", newBlockStates);
		}
		CompoundTag blockStates = section.getCompoundTag("block_states");

		if (!blockStates.containsKey("data")) {
			blockStates.putLongArray("data", new long[256]);
		}
		if (!blockStates.containsKey("palette")) {
			ListTag<CompoundTag> newPalette = new ListTag<>(CompoundTag.class);
			CompoundTag newBlockState = new CompoundTag();
			newBlockState.putString("Name", "minecraft:air");
			newPalette.add(newBlockState);
			blockStates.put("palette", newPalette);
		}

		if (!section.containsKey("biomes")) {
			CompoundTag newBiomes = new CompoundTag();
			section.put("biomes", newBiomes);
		}
		CompoundTag biomes = section.getCompoundTag("biomes");

		if (!biomes.containsKey("palette")) {
			ListTag<StringTag> biomePalette = new ListTag<>(StringTag.class);
			biomePalette.addString("minecraft:plains");
			biomes.put("palette", biomePalette);
		}
		if (!biomes.containsKey("data")) {
			biomes.putLongArray("data", new long[1]);
		}
		return section;
	}

	@Override
	public int getAverageHeight(CompoundTag data) {
		ListTag<CompoundTag> sections = Helper.tagFromCompound(data, "sections");
		if (sections == null) {
			return 0;
		}

		sections.sort(this::filterSections);

		int totalHeight = 0;

		for (int cx = 0; cx < Tile.CHUNK_SIZE; cx++) {
			zLoop:
			for (int cz = 0; cz < Tile.CHUNK_SIZE; cz++) {
				for (CompoundTag section : sections) {
					ListTag<CompoundTag> palette = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
					long[] blockStates = Helper.longArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
					if (palette == null) {
						continue;
					}

					Number height = Helper.numberFromCompound(section, "Y", null);
					if (height == null) {
						continue;
					}

					for (int cy = Tile.CHUNK_SIZE - 1; cy >= 0; cy--) {
						int index = cy * Tile.CHUNK_SIZE * Tile.CHUNK_SIZE + cz * Tile.CHUNK_SIZE + cx;
						CompoundTag block = getBlockAt(index, blockStates, palette);
						if (!isEmpty(block)) {
							totalHeight += height.intValue() * 16 + cy;
							continue zLoop;
						}
					}
				}
			}
		}
		return totalHeight / (Tile.CHUNK_SIZE * Tile.CHUNK_SIZE);
	}

	@Override
	public int getBlockAmount(CompoundTag data, String[] blocks) {
		ListTag<CompoundTag> sections = Helper.tagFromCompound(data, "sections");
		if (sections == null) {
			return 0;
		}

		int result = 0;

		for (CompoundTag section : sections) {
			ListTag<CompoundTag> palette = Helper.tagFromCompound(Helper.tagFromCompound(section, "block_states"), "palette");
			long[] blockStates = Helper.longArrayFromCompound(Helper.tagFromCompound(section, "block_states"), "data");
			if (palette == null) {
				continue;
			}

			for (int i = 0; i < palette.size(); i++) {
				CompoundTag blockState = palette.get(i);
				String name = Helper.stringFromCompound(blockState, "Name");
				if (name == null) {
					continue;
				}

				for (String block : blocks) {
					if (name.equals(block)) {
						// count blocks of this type
						for (int k = 0; k < 4096; k++) {
							if (blockState == getBlockAt(k, blockStates, palette)) {
								result++;
							}
						}
						break;
					}
				}
			}
		}
		return result;
	}

	@Override
	public ListTag<CompoundTag> getTileEntities(CompoundTag data) {
		return Helper.tagFromCompound(data, "block_entities");
	}

	@Override
	public CompoundTag getStructureReferences(CompoundTag data) {
		return Helper.tagFromCompound(Helper.tagFromCompound(data, "structures"), "References");
	}

	@Override
	public CompoundTag getStructureStarts(CompoundTag data) {
		return Helper.tagFromCompound(Helper.tagFromCompound(data, "structures"), "starts", new CompoundTag());
	}

	@Override
	public ListTag<CompoundTag> getSections(CompoundTag data) {
		return Helper.tagFromCompound(data, "sections");
	}

	@Override
	public LongTag getInhabitedTime(CompoundTag data) {
		return Helper.tagFromCompound(data, "InhabitedTime");
	}

	@Override
	public void setInhabitedTime(CompoundTag data, long inhabitedTime) {
		if (data != null) {
			data.putLong("InhabitedTime", inhabitedTime);
		}
	}

	@Override
	public StringTag getStatus(CompoundTag data) {
		return Helper.tagFromCompound(data, "Status");
	}

	@Override
	public void setStatus(CompoundTag data, String status) {
		if (data != null) {
			data.putString("Status", status);
		}
	}

	@Override
	public LongTag getLastUpdate(CompoundTag data) {
		return Helper.tagFromCompound(data, "LastUpdate");
	}

	@Override
	public void setLastUpdate(CompoundTag data, long lastUpdate) {
		if (data != null) {
			data.putLong("LastUpdate", lastUpdate);
		}
	}

	@Override
	public IntTag getXPos(CompoundTag data) {
		return Helper.tagFromCompound(data, "xPos");
	}

	@Override
	public IntTag getYPos(CompoundTag data) {
		return Helper.tagFromCompound(data, "yPos");
	}

	@Override
	public IntTag getZPos(CompoundTag data) {
		return Helper.tagFromCompound(data, "zPos");
	}

	@Override
	public ByteTag getLightPopulated(CompoundTag data) {
		return Helper.tagFromCompound(data, "isLightOn");
	}

	@Override
	public void setLightPopulated(CompoundTag data, byte lightPopulated) {
		if (data != null) {
			data.putByte("isLightOn", lightPopulated);
		}
	}

	@Override
	public void forceBlending(CompoundTag data) {
		int min = 0, max = 0;
		ListTag<CompoundTag> sections = Helper.tagFromCompound(data, "sections");
		for (CompoundTag section : sections) {
			int y = Helper.numberFromCompound(section, "Y", 0).intValue();
			min = Math.min(y, min);
			max = Math.max(y, max);
		}
		min = Math.min(min, -4);
		max = Math.max(max, 20);
		CompoundTag blendingData = new CompoundTag();
		blendingData.putInt("min_section", min);
		blendingData.putInt("max_section", max);
		data.put("blending_data", blendingData);
		data.remove("Heightmaps");
		data.remove("isLightOn");
	}
}
