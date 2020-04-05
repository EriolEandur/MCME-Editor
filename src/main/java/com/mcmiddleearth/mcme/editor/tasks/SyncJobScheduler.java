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
package com.mcmiddleearth.mcme.editor.tasks;

import com.mcmiddleearth.mcme.editor.EditorPlugin;
import com.mcmiddleearth.mcme.editor.job.AbstractJob;
import com.mcmiddleearth.mcme.editor.job.JobManager;
import com.mcmiddleearth.mcme.editor.job.JobStatus;
import com.mcmiddleearth.mcme.editor.util.Profiler;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Synchronous running Scheduler that fills ReadingQueues with requested 
 * Chunksnapshots and executes queued chunk changes in WritingQueues.
 * Runs each server tick for 20 ms, 10 ms, 5 ms or 0 ms depending on measured 
 * server tps.
 * 
 * @author Eriol_Eandur
 */
public class SyncJobScheduler{

    @Getter
    @Setter
    private static long serverTimeFullWork = (long)51e6, 
                        serverTimeHalfWork = (long)60e6,
                        fullWorkTime = (long) 15e6,
                        halfWorkTime = (long) 5e6;
    
    private static long lastStartTime=-1;
    
    private static final Average serverTickTime = new Average(10);
    
    BukkitTask task;
    
    public SyncJobScheduler() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
        //Logger.getGlobal().info("SyncJobScheduler");
                Profiler.start("Sync");
                if(lastStartTime == -1) {
                    lastStartTime = System.nanoTime();
                } else {
                    long startTime = System.nanoTime();
                    long serverTickDirect = startTime - lastStartTime;
                    long serverTick = serverTickTime.next(serverTickDirect);
                    long endTime;
                    if(serverTick > serverTimeHalfWork || serverTickDirect > serverTimeHalfWork) {
//Logger.getGlobal().info("sync: stop "+serverTick/1e6);
                        endTime = startTime;
                        Profiler.start("stop");
                        Profiler.stop("stop");
                    } else if(serverTick > serverTimeFullWork) {
//Logger.getGlobal().info("sync: half "+serverTick/1e6);
                        endTime = startTime + halfWorkTime;
                        Profiler.start("half");
                        Profiler.stop("half");
                    } else {
                        endTime = startTime + fullWorkTime;
                    }
                    Iterator<AbstractJob> jobs = JobManager.getJobs();
        //Logger.getGlobal().info("jobs: "+jobs.hasNext());
                    while(jobs.hasNext()) {
                        AbstractJob job = jobs.next();
        //Logger.getGlobal().info("job status: "+job.getId()+" "+job.getStatus().name());
                        if(job.getStatus().equals(JobStatus.RUNNING)) {
        //Logger.getGlobal().info("run job status: "+job.getId()+" hasEdit: "+job.hasEdit());
        //Logger.getGlobal().info("time: "+System.nanoTime()+" "+endTime);
                            //Profiler.start("eC");
                            while(System.nanoTime()<endTime && job.hasEdit()) {
                                job.editChunk();
        //Logger.getGlobal().info("time: "+System.nanoTime()+" "+endTime);
                            }
                            //Profiler.stop("eC");
        //Logger.getGlobal().info("run job status: "+job.getId()+" hasRequest: "+job.hasRequests());
        //Logger.getGlobal().info("time: "+System.nanoTime()+" "+endTime);
                            //Profiler.start("rC");
                            while(System.nanoTime()<endTime && job.hasRequests() && job.needsChunks()) {
                                job.serveChunkRequest();
        //Logger.getGlobal().info("time: "+System.nanoTime()+" "+endTime);
                            }
                            //Profiler.stop("rC");
                        }
                    }
                    lastStartTime = startTime;
                }
                Profiler.stop("Sync");
            }
        }.runTaskTimer(EditorPlugin.getInstance(), 10, 1);
    }
    
    public void cancel() {
        task.cancel();
    }
    
    public boolean isCancelled() {
        return task.isCancelled();
    }
    
    public static void setServerTimeAverage(int averagePeriods) {
        serverTickTime.setCapacity(averagePeriods);
    }
    
    public static int getServerTimeAverage() {
        return serverTickTime.getCapacity();
    }
    
    private static class Average {
        
        private long[] values;
        private long sum;
        
        private int current=0;
        private int max=0;
        
        
        public Average(int capacity) {
            values = new long[capacity];
        }
        
        public long next(long value) {
            if(current == max) {
                sum = sum + value;
                values[current] = value;
                max++;
            } else {
                sum = sum  - values[current] + value;
                values[current] = value;
            }
            if(current<values.length-1) {
                current++;
            } else {
                current = 0;
            }
            return sum / max;
        }
        
        public void setCapacity(int capacity) {
            values = Arrays.copyOf(values, capacity);
            if(current >= capacity) {
                current = 0;
                max = capacity;
                recalculateSum();
            } else if (max >= capacity) { 
                max = capacity;
                recalculateSum();
            }
        }
        
        public int getCapacity() {
            return values.length;
        }
        
        private void recalculateSum() {
            sum = 0;
            for(int i = 0; i<max; i++) {
                sum = sum + values[i];
            }
        }
    }
    
}
