/*
 * Copyright (C) 2020 MCME
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mcmiddleearth.mcme.editor.data;

import com.mcmiddleearth.architect.specialBlockHandling.data.ItemBlockData;
import com.mcmiddleearth.architect.specialBlockHandling.data.SpecialBlockInventoryData;
import com.mcmiddleearth.architect.specialBlockHandling.specialBlocks.SpecialBlockItemBlock;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public class EditChunkSnapshot {
    
    private ChunkSnapshot vanillaBlockData;
    
    private Map<Vector, ItemBlockData> itemBlockData = new HashMap<>();
    
    public EditChunkSnapshot(Chunk chunk, boolean enableItemBlocks) {
        vanillaBlockData = chunk.getChunkSnapshot(true, true, true);
        if(enableItemBlocks) {
            for(Entity entity: chunk.getEntities()) {
                if(entity instanceof ArmorStand && SpecialBlockItemBlock.isItemBlockArmorStand((ArmorStand)entity)) {
                    //String id = SpecialBlockItemBlock.getIdFromArmorStand((ArmorStand)entity);
                    int contentDamage = SpecialBlockItemBlock.getContentDamage((ArmorStand)entity);
                    //int[] coords = SpecialBlockItemBlock.getCoordinatesFromArmorStand((ArmorStand)entity); Doesn't work for WE copies.
                    Location armorLoc = entity.getLocation();
Logger.getGlobal().info("Armor stand found: "+armorLoc.getX()+" "+armorLoc.getY()+" "+armorLoc.getZ());
                    SpecialBlockItemBlock specialBlock = (SpecialBlockItemBlock) SpecialBlockInventoryData
                                                                 .getSpecialBlock(SpecialBlockItemBlock
                                                                                  .getIdFromArmorStand((ArmorStand)entity));
                    if(specialBlock!=null) {
Logger.getGlobal().info("specialBlock: "+specialBlock);
                        Block block = specialBlock.getBlock(armorLoc);
Logger.getGlobal().info("Block found: "+block.getX()+" "+block.getY()+" "+block.getZ());
                        if(!specialBlock.isArmorStandChanged((ArmorStand)entity, block)) {
    Logger.getGlobal().info("adding item block data!");
                            //BlockData blockData = chunk.getBlock(toChunkCoord(coords[0]), coords[1], toChunkCoord(coords[2])).getBlockData();
                            itemBlockData.put(new Vector(toChunkCoord(block.getX()),block.getY(), toChunkCoord(block.getZ())),
                                              new ItemBlockData(block.getBlockData(),specialBlock,contentDamage,entity.getLocation().getYaw()));
                        }
                    }
                }
            }
        }
            
    }
    
    private int toChunkCoord(int coord) {
        while(coord<0) {
            coord+=1024;
        }
        return coord%16;
    }
    
    public ChunkSnapshot getChunkSnapshot() {
        return vanillaBlockData;
    }
    
    public ItemBlockData getItemBlockData(Vector location) {
        return itemBlockData.get(location);
    }
}
