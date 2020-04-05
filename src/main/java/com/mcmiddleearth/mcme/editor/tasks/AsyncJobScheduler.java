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
import com.mcmiddleearth.mcme.editor.data.PluginData;
import com.mcmiddleearth.mcme.editor.job.AbstractJob;
import com.mcmiddleearth.mcme.editor.job.JobManager;
import com.mcmiddleearth.mcme.editor.job.JobStatus;
import com.mcmiddleearth.mcme.editor.util.Profiler;
import com.mcmiddleearth.mcme.editor.util.ProgressMessenger;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Asynchronous job execution.
 * @author Eriol_Eandur
 */
public class AsyncJobScheduler {

    private BukkitTask task;
    
    private boolean cancel = false;
    
    public AsyncJobScheduler() {
        task = new BukkitRunnable() {
            @Override
            public synchronized void run() {
                while(!cancel) {
                    Profiler.update();
//Logger.getGlobal().info("Starting asycScheduler loop");
                    Iterator<AbstractJob> jobIterator = JobManager.getJobs();
                    Profiler.start("status");
                    while(jobIterator.hasNext()) {
//Logger.getGlobal().info("Starting next job status");
                        AbstractJob job = jobIterator.next();
                        if(!checkFail(job)) {
                            switch(job.getStatus()) {
                                case CREATION:
                                    if(!job.getStatusRequested().equals(JobStatus.CREATION)) {
                                        job.setStatus(job.getStatusRequested());
                                        if(job.isRunnable()) {
                                            job.openFileStreams();
                                            checkFail(job);
                                        }
                                    }
                                    break;
                                case RUNNING:
                                case WAITING:
                                    if(!job.getStatus().equals(job.getStatusRequested())) {
                                        switch(job.getStatusRequested()) {
                                            case CANCELLED:
                                                cancelJob(job);
                                                break;
                                            case SUSPENDED:
                                                suspendJob(job);
                                                break;
                                            case FINISHED:
                                                finishJob(job);
                                        }
                                    }
                                    break;
                                case SUSPENDED:
                                    if(!job.getStatus().equals(job.getStatusRequested())) {
                                        switch(job.getStatusRequested()) {
                                            case CANCELLED:
                                                cancelJob(job);
                                                break;
                                            case WAITING:
                                                resumeJob(job);
                                                break;
                                            case FINISHED:
                                                finishJob(job);
                                        }
                                    }
                            }
                        }
                    }
                    jobIterator = JobManager.getJobs();
                    boolean doneSomething = false;
                    Profiler.stop("status");
                    Profiler.start("work");
                    while(jobIterator.hasNext()) {
//Logger.getGlobal().info("Starting next job working");
                        AbstractJob  job = jobIterator.next();
                        switch(job.getStatus()) {
                            case WAITING:
                            case RUNNING:
                                if(!doneSomething) {
                                    if(job.getStatus().equals(JobStatus.WAITING)) {
                                        job.getOwner().info(ProgressMessenger
                                                .formatProgressMessage("Working on your job _h_#"+job.getId()+"_n_ now.\n"+job.progresMessage()));
                                        job.setLastMessaged(System.currentTimeMillis());
                                        job.resetProcessingSpeed();
                                    } else {
                                        if(job.isSendUpdates() && System.currentTimeMillis()>job.getLastMessaged()+10000) {
                                            job.getOwner().info(ProgressMessenger
                                                .formatProgressMessage("Working on your job _h_#"+job.getId()+"_n_.\n"+job.progresMessage()));
                                            job.setLastMessaged(System.currentTimeMillis());
                                        }
                                    }
                                    job.setStatus(JobStatus.RUNNING);
                                    job.work();
                                    //job.saveResultsToFile();
                                    job.requestChunks();
                                    doneSomething = true;
                                } else {
                                    if(job.getStatus().equals(JobStatus.RUNNING)) {
                                        job.getOwner().info("Your job #"+job.getId()+" is now waiting for another job to finish.\n"
                                                +ProgressMessenger.formatProgressMessage(job.progresMessage()));
                                    }
                                    job.setStatus(JobStatus.WAITING);
                                }
                        }
                    }
                    Profiler.stop("work");
                    Profiler.start("idle");
                    if(!doneSomething) {
//Logger.getGlobal().info("No work");
                        try {
                            jobIterator = JobManager.getJobs();
                            while(jobIterator.hasNext()) {
//Logger.getGlobal().info("Starting next job dequeue");
                                AbstractJob job = jobIterator.next();
                                if(!job.isRunnable() && job.getEndTime() > System.currentTimeMillis() - PluginData.getJobStorageTime()) {
                                    JobManager.dequeueJob(job.getId());
                                }
                            }
                            this.wait(500);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(AsyncJobScheduler.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                    Profiler.stop("idle");
                    try {
                        this.wait(50);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(AsyncJobScheduler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.runTaskLaterAsynchronously(EditorPlugin.getInstance(),5);
    }
    
    private boolean checkFail(AbstractJob job) {
        if(job.getStatusRequested().equals(JobStatus.FAILED)
           && !job.getStatus().equals(JobStatus.FAILED)) {
            job.closeFileStreams();
            job.setStatus(JobStatus.FAILED);
            job.getOwner().error("Your job #"+job.getId()+" failed. See console for details.");
            return true;
        }
        return false;
    }
    
    private void cancelJob(AbstractJob job) {
        job.setStatus(JobStatus.CANCELLED);
        job.saveLogsToFile();
        job.closeFileStreams();
        if(!checkFail(job)) {
            job.getOwner().info("Your job #"+job.getId()+" was cancelled.");
        }
    }
    
    private void suspendJob(AbstractJob job) {
        job.setStatus(JobStatus.SUSPENDED);
        job.saveLogsToFile();
            job.getOwner().info("Your job #"+job.getId()+" was suspended.");
    }
    
    private void resumeJob(AbstractJob job) {
        job.setStatus(JobStatus.WAITING);
            job.getOwner().info("Your job #"+job.getId()+" was resumed.");
    }

    private void finishJob(AbstractJob job) {
        job.setStatus(JobStatus.FINISHED);
        job.saveLogsToFile();
        job.closeFileStreams();
        if(!checkFail(job)) {
            job.getOwner().info("Your job #"+job.getId()+" was finished."+job.getResultMessage());
        }
    }
    
    public void cancel() {
        cancel = true;
    }
    
    public boolean isCancelled() {
        return task.isCancelled();
    }

    public boolean isEnded() {
        return !(   Bukkit.getScheduler().isCurrentlyRunning(task.getTaskId()) 
                 || Bukkit.getScheduler().isQueued(task.getTaskId()));
    }
    
    public boolean isRunning() {
        return Bukkit.getScheduler().isCurrentlyRunning(task.getTaskId());
    }
    
    public boolean isQueued() {
        return Bukkit.getScheduler().isQueued(task.getTaskId());
    }
    
}
