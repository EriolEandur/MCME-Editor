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
package com.mcmiddleearth.mcme.editor.data.chunk;

import com.mcmiddleearth.mcme.editor.data.chunk.ChunkEditData;
import com.mcmiddleearth.pluginutil.NMSUtil;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public class ChunkLightEditData extends ChunkEditData {

    List<Vector> edits = new ArrayList<>();
    
    public ChunkLightEditData(int chunkX, int chunkZ) {
        super(chunkX, chunkZ);
    }
    
    @Override
    public void applyEdit(World world, boolean refreshChunks) {
        NMSUtil.calcLight(world.getChunkAt(getChunkX(),getChunkZ()), edits);
        if(refreshChunks) {
            world.refreshChunk(getChunkX(), getChunkZ());
        }
    }
    
    public void add(Vector vec) {
        edits.add(vec);
    }
    
}
