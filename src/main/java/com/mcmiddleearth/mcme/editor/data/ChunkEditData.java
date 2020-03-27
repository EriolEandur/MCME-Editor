/*
 * Copyright (C) 2019 MCME
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
import com.mcmiddleearth.architect.specialBlockHandling.specialBlocks.SpecialBlockItemBlock;
import com.mcmiddleearth.mcme.editor.EditorPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Collection of Block changes in one chunk.
 * @author Eriol_Eandur
 */
public class ChunkEditData {
    
    @Getter
    private int chunkX;
    
    @Getter
    private int chunkZ;
            
    @Getter
    private Map<Vector,BlockData> changes = new HashMap<>();
    
    private Map<Vector,ItemBlockData> removals = new HashMap<>();
    
    public ChunkEditData(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }
    
    public boolean add(Vector vector, BlockData data) {
        if(vector.getBlockX()>=0 && vector.getBlockX()<16
                && vector.getBlockY()>=0 && vector.getBlockY()<256
                && vector.getBlockZ()>=0 && vector.getBlockZ()<16) {
            changes.put(vector, data);
            return true;
        }
        return false;
    }
    
    public boolean addRemoval(Vector vector, ItemBlockData data) {
        if(vector.getBlockX()>=0 && vector.getBlockX()<16
                && vector.getBlockY()>=0 && vector.getBlockY()<256
                && vector.getBlockZ()>=0 && vector.getBlockZ()<16) {
            removals.put(vector, data);
            return true;
        }
        return false;
    }
    
    public BlockData get(Vector vector) {
        return changes.get(vector);
    }
    
    public boolean isEmpty() {
        return changes.isEmpty();
    }

    public void applyEdits(World world) {
//Logger.getGlobal().info("apply edits, size: "+changes.size());
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);
        if(Math.random()<0.01)
        {
            Logger.getLogger(EditorPlugin.class.getName()).log(Level.INFO,"Working at: "+chunkX+" "+chunkZ
                    +" ChunkTickets: "+world.getPluginChunkTickets().get(EditorPlugin.getInstance()).size());
        }
        removals.forEach((vector,data) -> {
            ArmorStand armorStand = SpecialBlockItemBlock.getArmorStand(new Location(chunk.getWorld(),
                                                                            chunkX*16+vector.getBlockX(),
                                                                            vector.getBlockY(),
                                                                            chunkZ*16+vector.getBlockZ()));
            if(armorStand!=null) {
                armorStand.remove();
            }
        });
        changes.forEach((vector,data) -> {
            chunk.getBlock(vector.getBlockX(),
                           vector.getBlockY(),
                           vector.getBlockZ())
                    .setType(Material.AIR);
        });
        new BukkitRunnable() {
            @Override
            public void run() {
                applyEditsUnchecked(chunk);
                /*new BukkitRunnable() {
                    @Override
                    public void run() {
                        applyEditsUnchecked(chunk);
                    }
                }.runTaskLater(EditorPlugin.getInstance(), 5);*/
            }
        }.runTaskLater(EditorPlugin.getInstance(), 5);
        /*if(!chunk.isLoaded()) {
Logger.getGlobal().info("Force load: "+chunkX + " "+chunkZ);
            chunk.load();
            chunk.setForceLoaded(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    applyEditsUnchecked(chunk);
                    chunk.setForceLoaded(false);
                    chunk.unload();
                }
            }.runTaskLater(EditorPlugin.getInstance(), 2);
        } else {
            applyEditsUnchecked(chunk);
        }*/
    }
        
    public void applyEditsUnchecked(Chunk chunk) {
        changes.forEach((vector, data) -> {
    //Logger.getGlobal().info("change: "+vector+" "+data+" "+world);
            Block block = chunk.getBlock(vector.getBlockX(),
                               vector.getBlockY(),
                               vector.getBlockZ());
            if(data instanceof ItemBlockData) {
                ItemBlockData itemBlockData = (ItemBlockData) data;
                block.setBlockData(itemBlockData.getBlockData());
                itemBlockData.getSpecialItemBlock().placeArmorStand(block, BlockFace.DOWN, 
                                                     new Location(null,0,0,0,itemBlockData.getYaw()+180,0),
                                                     itemBlockData.getCurrentDamage());
            } else {
                block.setBlockData(data, false);
                //No entity tile found error here.
            }
        });
    }
}
