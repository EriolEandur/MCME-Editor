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
package com.mcmiddleearth.mcme.editor.job;

import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.data.ChunkEditData;
import com.mcmiddleearth.mcme.editor.data.EditChunkSnapshot;
import com.sk89q.worldedit.regions.Region;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public class SurvivalPrepJob extends BlockSearchJob {

    public SurvivalPrepJob(EditCommandSender owner, int id, YamlConfiguration config) {
        super(owner, id, config);
        loadResultsFromFile();
    }

    public SurvivalPrepJob(EditCommandSender owner, int id, World world, Region extraRegion, List<Region> regions, 
                      boolean exactMatch, int size) {
        super(owner, id, world, extraRegion, regions, exactMatch, size, false);
        saveActionsToFile();
    }
    
    @Override
    public ChunkEditData handle(EditChunkSnapshot editChunk) {
        ChunkSnapshot chunk = editChunk.getChunkSnapshot();
        boolean complete= false;
        //int maxY = getWorld().getMaxHeight();
//Logger.getGlobal().info("handle chunk: "+chunk);
        if(isInside(chunk.getX(), chunk.getZ(),0,getMinY(),0)
            && isInside(chunk.getX(), chunk.getZ(),0,getMinY(),15)
            && isInside(chunk.getX(), chunk.getZ(),15,getMinY(),0)
            && isInside(chunk.getX(), chunk.getZ(),15,getMinY(),15)
            && isInside(chunk.getX(), chunk.getZ(),0,getMaxY(),0)
            && isInside(chunk.getX(), chunk.getZ(),0,getMaxY(),15)
            && isInside(chunk.getX(), chunk.getZ(),15,getMaxY(),0)
            && isInside(chunk.getX(), chunk.getZ(),15,getMaxY(),15)) {
            complete = true;
        }
//Logger.getGlobal().info("complete: "+complete);
        ChunkEditData edit = new ChunkEditData(chunk.getX(),chunk.getZ());
//Logger.getGlobal().info("maxY: "+getMaxY());
        for(int i=0; i<16; i++) {
            for(int j=0; j<16; j++) {
//Logger.getGlobal().info("HighestBlockY("+i+"/"+j+"): "+chunk.getHighestBlockYAt(i, j));
                for(int k=Math.min(chunk.getHighestBlockYAt(i, j),getMaxY()); k>=getMinY();k--) {
//Logger.getGlobal().info("inside: "+isInside(chunk.getX(),chunk.getZ(),i,k,j));
                    if(complete || isInside(chunk.getX(),chunk.getZ(),i,k,j)) {
                        if(isSand(chunk, edit, i,k,j) 
                                && !isWaterOrAirAround(chunk, edit, i,k,j)) {
                            edit.add(new Vector(i,k,j),Bukkit.createBlockData(Material.STONE));
                        }
                    }
                }
            }
        }
        for(int i=0; i<16; i++) {
            for(int j=0; j<16; j++) {
//Logger.getGlobal().info("maxY: "+maxY);
                int top = 0;
                for(int k=Math.min(chunk.getHighestBlockYAt(i, j),getMaxY()); k>=getMinY();k--) {
//Logger.getGlobal().info("inside: "+isInside(chunk.getX(),chunk.getZ(),i,k,j));
                    int f = 1;
                    if(complete || isInside(chunk.getX(),chunk.getZ(),i,k,j)) {
                        if(isTerrain(chunk, edit, i,k,j) && top == 0) {
                            top = k;
                        } else if(k < top-4 && k > top - 40 && top < 60 && Math.random()<f*0.0012) {
                            placeVein(chunk,Material.COAL_ORE,i,k,j,12,edit);
                        } else if(k < top-25 && Math.random()<f*0.0007) {
                            placeVein(chunk,Material.IRON_ORE,i,k,j,7,edit);
                        } else if(k < top-35 && Math.random()<f*0.0002) {
                            placeVein(chunk,Material.GOLD_ORE,i,k,j,5,edit);
                        } else if(k < top-55 && Math.random()<f*0.0002) {
                            placeVein(chunk,Material.DIAMOND_ORE,i,k,j,5,edit);
                        } else if(k < top-55 && Math.random()<f*0.0005) {
                            placeVein(chunk,Material.REDSTONE_ORE,i,k,j,5,edit);
                        } else if(k < top-40 && k > top-70 && Math.random()<f*0.0003) {
                            placeVein(chunk,Material.LAPIS_ORE,i,k,j,5,edit);
                        } else if(k < top-60 && Math.random()<f*0.0003) {
                            placeVein(chunk,Material.EMERALD_ORE,i,k,j,1,edit);
                        } else if(k < top-65 && Math.random()<f*0.0005) {
                            placeVein(chunk,Material.NETHER_QUARTZ_ORE,i,k,j,7,edit);
                        } 
                    }
                }
            }
        }
        return edit;
    }
    
    private void placeVein(ChunkSnapshot chunk, Material mat, int x, int y, int z, int size, ChunkEditData edit) {
        Vector vec = new Vector(Math.random()*2-1,Math.random()*2-1,Math.random()*2-1);
        int step = 0;
        int counter = 0;
        while(counter<size) {
            if(isMaterial(chunk,edit,x,y,z,Material.STONE)) {
                if(edit.add(new Vector(x,y,z),Bukkit.createBlockData(mat))) {
                    counter++;
                }
            }
            if(step>0 && Math.random()>0.5 && isMaterial(chunk,edit,x-1,y,z,Material.STONE)) {
                if(edit.add(new Vector(x-1,y,z),Bukkit.createBlockData(mat))) {
                    counter++;
                }
            }
            if(step>0 && Math.random()>0.5 && isMaterial(chunk,edit,x,y,z-1,Material.STONE)) {
                if(edit.add(new Vector(x,y,z-1),Bukkit.createBlockData(mat))) {
                    counter++;
                }
            }
            if(step>0 && Math.random()>0.5 && isMaterial(chunk,edit,x-1,y,z-1,Material.STONE)) {
                if(edit.add(new Vector(x-1,y,z-1),Bukkit.createBlockData(mat))) {
                    counter++;
                }
            }
            if(counter<size-3) {
                if(step>0 && Math.random()>0.5 && isMaterial(chunk,edit,x-1,y-1,z,Material.STONE)) {
                    if(edit.add(new Vector(x-1,y-1,z),Bukkit.createBlockData(mat))) {
                        counter++;
                    }
                }
                if(step>0 && Math.random()>0.5 && isMaterial(chunk,edit,x,y-1,z-1,Material.STONE)) {
                    if(edit.add(new Vector(x,y-1,z-1),Bukkit.createBlockData(mat))) {
                        counter++;
                    }
                }
                if(step>0 && Math.random()>0.5 && isMaterial(chunk,edit,x-1,y-1,z-1,Material.STONE)) {
                    if(edit.add(new Vector(x-1,y-1,z-1),Bukkit.createBlockData(mat))) {
                        counter++;
                    }
                }
            }
            step++;
            x = x + (Math.random()<Math.abs(vec.getX())?(int) Math.signum(vec.getX()):0);
            y = y + (Math.random()<Math.abs(vec.getY())?(int) Math.signum(vec.getY()):0);
            z = z + (Math.random()<Math.abs(vec.getZ())?(int) Math.signum(vec.getZ()):0);
            if(x<0 || x > 15 || y < 0 || y > 255 || z < 0 || z > 15) break;
        }
    }
    
    private boolean isMaterial(ChunkSnapshot chunk, ChunkEditData edit, int x, int y, int z, Material mat) {
        if(x<0 || x > 15 || y < 0 || y > 255 || z < 0 || z > 15) return false;
        BlockData editData = edit.get(new Vector(x,y,z));
        if(editData!=null) {
            return editData.getMaterial().equals(mat);
        } else {
            return chunk.getBlockType(x, y, z).equals(mat);
        }
    }
    
    private boolean isTerrain(ChunkSnapshot chunk, ChunkEditData edit, int i, int k, int j) {
        return isMaterial(chunk,edit,i,k,j,Material.STONE)
            || isMaterial(chunk,edit,i,k,j,Material.DIORITE)
            || isMaterial(chunk,edit,i,k,j,Material.ANDESITE)
            || isMaterial(chunk,edit,i,k,j,Material.GRANITE)
            || isMaterial(chunk,edit,i,k,j,Material.GRAVEL)
            || isMaterial(chunk,edit,i,k,j,Material.GRASS_BLOCK)
            || isMaterial(chunk,edit,i,k,j,Material.DIRT)
            || isMaterial(chunk,edit,i,k,j,Material.COARSE_DIRT);
    }
    
    private boolean isSand(ChunkSnapshot chunk, ChunkEditData edit, int i, int k, int j) {
        return isMaterial(chunk,edit,i,k,j,Material.SAND);
    }
    
    private boolean isWaterOrAir(ChunkSnapshot chunk, ChunkEditData edit, int i, int k, int j) {
        return isMaterial(chunk,edit,i,k,j,Material.WATER)
            || isMaterial(chunk,edit,i,k,j,Material.AIR)
            || isMaterial(chunk,edit,i,k,j,Material.CAVE_AIR)
            || isMaterial(chunk,edit,i,k,j,Material.VOID_AIR);
    }
    
    private boolean isWaterOrAirAround(ChunkSnapshot chunk, ChunkEditData edit, int x, int y, int z) {
        for(int i = x-1; i<=x+1; i++) {
            for(int j = z-1; j<=z+1; j++) {
                for(int k = y-1; k<=y+1; k++) {
                    if(isWaterOrAir(chunk,edit,i,k,j)) {
                        return true;
                    }
                }
            }
        }
        return false;    
    }
    
    @Override
    public String getResultMessage() {
        String result = "\nPrepared region for survival:\nReplaced underground sand and added ores.";
        return result;
    }
    @Override
    public JobType getType() {
        return JobType.SURVIVAL_PREP;
    }
    
}
