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

import com.mcmiddleearth.mcme.editor.data.ChunkPosition;
import com.mcmiddleearth.mcme.editor.data.EditChunkSnapshot;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;

/**
 * Features asynchronous read only access to chunks.
 * @author Eriol_Eandur
 */
public class ReadingQueue {
    
    private static final int TASK_SIZE = 20;

    private World world;
    
    private LinkedBlockingQueue<ChunkPosition> request = new LinkedBlockingQueue<>(TASK_SIZE);
    
    private LinkedBlockingQueue<EditChunkSnapshot> chunks = new LinkedBlockingQueue<>(TASK_SIZE);
    
    public ReadingQueue(World world) {
        this.world = world;
        //this.request = request;
    }
    
    public int remainingChunkCapacity() {
        return chunks.remainingCapacity();
    }
    
    public void putChunk(EditChunkSnapshot snapshot) {
        try {
            request.remove();
            chunks.put(snapshot);
        } catch (InterruptedException ex) {
            Logger.getLogger(ReadingQueue.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public boolean hasChunk() {
        return !chunks.isEmpty();
    }
    
    public EditChunkSnapshot pollChunk() {
        try {
            return chunks.take();
        } catch (InterruptedException ex) {
            Logger.getLogger(ReadingQueue.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public int remainingRequestsCapacity() {
        return request.remainingCapacity();
    }
    
    public void request(List<ChunkPosition> requestedChunks) {
        requestedChunks.forEach(chunk -> {
            try {
//Logger.getGlobal().info("enqueue request");
                request.put(chunk);
            } catch (InterruptedException ex) {
                Logger.getLogger(ReadingQueue.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    
    public boolean hasRequest() {
        return !request.isEmpty();
    }
    
    public ChunkPosition nextRequest() {
        return request.element();
    }
    
}
