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
package com.mcmiddleearth.mcme.editor.data.chunk;

import lombok.Getter;
import org.bukkit.World;

/**
 * Collection of Block changes in one chunk.
 * @author Eriol_Eandur
 */
public abstract class ChunkEditData {
    
    @Getter
    private int chunkX;
    
    @Getter
    private int chunkZ;
            
    public ChunkEditData(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }
    
    public abstract void applyEdit(World world, boolean refreshChunks);
}
