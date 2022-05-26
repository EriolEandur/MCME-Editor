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
package com.mcmiddleearth.mcme.editor.queue;

import com.mcmiddleearth.mcme.editor.data.chunk.ChunkEditData;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.World;

/**
 * Features asynchronous write only access to chunks.
 * @author Eriol_Eandur
 */
public class WritingQueue {
    
    private final World world;
    
    private final LinkedBlockingQueue<ChunkEditData> queue = new LinkedBlockingQueue<>();
    
    public WritingQueue(World world) {
        this.world = world;
    }
    
    public void put(ChunkEditData data) {
        try {
            queue.put(data);
        } catch (InterruptedException ex) {
            Logger.getLogger(WritingQueue.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public ChunkEditData poll() {
        try {
            return queue.take();
        } catch (InterruptedException ex) {
            Logger.getLogger(WritingQueue.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public ChunkEditData peek() {
        return queue.element();
    }
    
    public boolean isFull() {
        return queue.remainingCapacity()==0;
    }
    
    public boolean hasEdit() {
        if(!queue.isEmpty()) {
            ChunkEditData edit = queue.peek();
            if(world.getChunkAt(edit.getChunkX(),edit.getChunkZ()).isLoaded()) {
                return true;
            } else {
                Logger.getLogger(WritingQueue.class.getName()).log(Level.INFO, "Chunk not loaded yet! {0} {1}", new Object[]{edit.getChunkX(), edit.getChunkZ()});
                return false;
            }
        }
        return false;
    }
}
