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
package com.mcmiddleearth.mcme.editor.job;

import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.data.block.EditBlockData;
import com.mcmiddleearth.mcme.editor.data.block.SimpleBlockData;
import com.mcmiddleearth.mcme.editor.data.chunk.ChunkBlockEditData;
import com.mcmiddleearth.mcme.editor.data.chunk.ChunkEditData;
import com.mcmiddleearth.mcme.editor.data.EditChunkSnapshot;
import com.sk89q.worldedit.regions.Region;
import java.io.IOException;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public class SetJob extends AbstractJob{

    private EditBlockData setData;
    
    public SetJob(EditCommandSender owner, int id, YamlConfiguration config) {
        super(owner, id, config);
        setData = SimpleBlockData.createBlockData(config.getString("setData"));
    }
    
    public SetJob(EditCommandSender owner, int id, World world, Region region, List<Region> regions, 
                      EditBlockData data, int size, boolean includeItemBlocks, boolean refreshChunks) {
        super(owner, id, world, region, regions, size, includeItemBlocks,refreshChunks);
        setData = data;
        saveSetDataToFile();
    }

    @Override
    public ChunkEditData handle(EditChunkSnapshot editChunk) {
        ChunkSnapshot chunk = editChunk.getChunkSnapshot();
        ChunkBlockEditData edit = new ChunkBlockEditData(chunk.getX(),chunk.getZ());
        for(int i=0; i<16; i++) {
            for(int j=0; j<16; j++) {
//Logger.getGlobal().info("maxY: "+maxY);
                if(isInside(chunk.getX(),chunk.getZ(),i,getMinY(),j)) {
                    for(int k=getMinY(); k<=Math.min(chunk.getHighestBlockYAt(i, j),getMaxY());k++) {
//Logger.getGlobal().info("inside: "+isInside(chunk.getX(),chunk.getZ(),i,k,j));
                        edit.add(new Vector(i,k,j), setData);
                    }
                }
            }
        }
        return edit;
    }
    
    private void saveSetDataToFile() {
        try {
            getConfig().set("setData", setData.getAsString());
            getConfig().save(getJobDataFile());
        } catch (IOException ex) {
            fail(ex);
        }
    }
    
    @Override
    public JobType getType() {
        return JobType.SET;
    }

    @Override
    public String getResultMessage() {
        String result = "\nPlaced "+setData+" in "+getSize()+" chunks.";
        return result;
    }
    
    @Override
    public void saveResultsToFile() {
    }
    
    @Override
    public void saveLogsToFile() {
    }

}
