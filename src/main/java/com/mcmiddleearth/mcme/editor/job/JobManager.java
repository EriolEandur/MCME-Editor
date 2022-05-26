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

//import com.boydti.fawe.object.FawePlayer;
import com.mcmiddleearth.architect.serverResoucePack.RpManager;
import com.mcmiddleearth.architect.serverResoucePack.RpRegion;
import com.mcmiddleearth.mcme.editor.EditorPlugin;
import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.command.sender.EditPlayer;
import com.mcmiddleearth.mcme.editor.data.ChunkPosition;
import com.mcmiddleearth.mcme.editor.data.PluginData;
import com.mcmiddleearth.mcme.editor.tasks.AsyncJobScheduler;
import com.mcmiddleearth.mcme.editor.tasks.SyncJobScheduler;
import com.mcmiddleearth.mcme.editor.util.ProgressMessenger;
import com.mcmiddleearth.pluginutil.WEUtil;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Manages
 * @author Eriol_Eandur
 */
public class JobManager {
    
    private static AsyncJobScheduler asyncScheduler;
    private static SyncJobScheduler syncScheduler;
    
    private static PriorityQueue<AbstractJob> jobQueue = new PriorityQueue<>();
    
    private static int nextId = 1;

    public static boolean enqueueClipboardJob(EditPlayer owner, JobType type) {
        return false;
    }

    
    public static synchronized boolean enqueueBlockJob(EditCommandSender owner, boolean weSelection, Set<String> worlds, 
                                          Set<String> rps, JobType type, boolean exactMatch, boolean refreshChunks) {
        Region weRegion;
        if(weSelection && owner instanceof EditPlayer && owner.isOnline()) {
            /*WorldEditPlugin worldEdit = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
            LocalSession session = worldEdit.getSession(((EditPlayer)owner).getPlayer());
            try {
                tempWeRegion = session.getSelection(session.getSelectionWorld());
            } catch (IncompleteRegionException ex) {
                Logger.getLogger(JobManager.class.getName()).log(Level.SEVERE, null, ex);
            }*/
            weRegion = WEUtil.getSelection(((EditPlayer)owner).getPlayer());
        } else {
            weRegion = null;
        }
        return enqueueBlockJob(owner, weRegion, weSelection, worlds, rps, type, exactMatch, refreshChunks);
    }
    
    public static synchronized boolean enqueueBlockJob(EditCommandSender owner, Region weRegion, boolean weSelection, Set<String> worlds, 
                                          Set<String> rps, JobType type, boolean exactMatch, boolean refreshChunks) {
        boolean jobStarted = false;
        Set<World> validWorlds = new HashSet<>();
        if(worlds.isEmpty()) {
            validWorlds.addAll(Bukkit.getWorlds());
        } else {
            worlds.forEach(world -> {
                if(Bukkit.getWorld(world)!=null) {
                    validWorlds.add(Bukkit.getWorld(world));
                }});
        }
        Set<RpRegion> rpRegions = new HashSet<>();
        rps.forEach(rpName -> {
            RpManager.getRegions().entrySet().stream()
                    .filter(entry -> {
//Logger.getLogger(JobManager.class.getName()).log(Level.INFO,"JobManager - filter: "+entry.getKey()
//                                                                      +" "+entry.getValue().getName()
//                                                                      +" "+entry.getValue().getRp());
                        return entry.getValue().getRp().equalsIgnoreCase(rpName);
                            })
                                   //&& (validWorlds.isEmpty() || validWorlds.contains(entry.getValue().getRegion()
                                   //                                             .getWorld().getName())))
                    .forEach(entry -> rpRegions.add(entry.getValue()));
            });
        
        final Set<BlockVector2> weSelectionChunks = (weRegion!=null && worlds.contains(weRegion.getWorld().getName())?
                                                 weRegion.getChunks():new HashSet<>());
        ProgressMessenger chunkProgress = new ProgressMessenger(owner.getSender(),2,"Collecting chunks: %1");
        for(World world : validWorlds) {
            final Set<ChunkPosition> chunks = new HashSet<>();
            List<Region> rpWeRegions = new ArrayList<>();
            Region extraWeRegion = null;
            if(!rpRegions.isEmpty()) {
                rpRegions.stream().filter(rpRegion -> rpRegion.getRegion().getWorld().getName()
                                                              .equalsIgnoreCase(world.getName()))
                                  .forEach(rpRegion -> {
                    Region reg = rpRegion.getRegion();
                    rpWeRegions.add(reg);
                    if(weRegion!=null) {
                        EditorPlugin.getMessageUtil().scheduleInfoMessage(owner.getSender(), "Adding chunks of region: "+rpRegion.getName());
                        rpRegion.getRegion().getChunks().stream()
                                            .filter(chunk -> weSelectionChunks.contains(chunk))
                                            .forEach(chunk -> {
                                                chunks.add(new ChunkPosition(chunk.getBlockX(),
                                                                                           chunk.getBlockZ()));
                                                chunkProgress.step();
                                            });
                    } else {
                        EditorPlugin.getMessageUtil().scheduleInfoMessage(owner.getSender(), "Adding chunks of region: "+rpRegion.getName());
                        rpRegion.getRegion().getChunks().forEach(chunk -> {  //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                            chunks.add(new ChunkPosition(chunk.getBlockX(),chunk.getBlockZ()));
                            chunkProgress.step();
                        });
                    }
                });
            } else if(weRegion!=null && weRegion.getWorld().getName().equalsIgnoreCase(world.getName())){
                //getChunks(weRegion).forEach(chunk -> {
                weRegion.getChunks().forEach(chunk -> { //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
                   chunks.add(new ChunkPosition(chunk.getBlockX(),chunk.getBlockZ()));
                   chunkProgress.step();
                });
                extraWeRegion = weRegion;
            } else if(weRegion==null) {
                //TODO add all world chunks (stream to file)
            }
            if(!chunks.isEmpty()) {
                if(!AbstractJob.saveChunksToFile(nextId, chunks,owner.getSender())) {
                    return false;
                }
                AbstractJob job = null;
                switch(type) {
                    case COUNT:
                        job = new CountJob(owner, nextId, world, extraWeRegion, rpWeRegions, 
                                           exactMatch,chunks.size(),refreshChunks); break;
                    case REPLACE:
                        job = new ReplaceJob(owner, nextId, world, extraWeRegion, rpWeRegions, 
                                             exactMatch,chunks.size(),refreshChunks); break;
                    case LIGHT:
                        job = new LightJob(owner, nextId, world, extraWeRegion, rpWeRegions,
                                             chunks.size(),refreshChunks); break;
                    case SURVIVAL_PREP:
                        job = new SurvivalPrepJob(owner, nextId, world, extraWeRegion, rpWeRegions, 
                                             exactMatch,chunks.size()); break;
                    case Y_SHIFT:
                        job = new YShiftJob(owner, nextId, world, extraWeRegion, rpWeRegions, chunks.size());
                }
                nextId++;
                jobQueue.add(job);
                job.start();
                jobStarted = true;
            }
        }
        return jobStarted;
    }
    
    public static synchronized void loadJobs() {
        File[] files = PluginData.getJobFolder().listFiles(file -> file.getName()
                                                          .endsWith(AbstractJob.jobDataFileExt));
        for(File file: files) {
//Logger.getGlobal().info("File: "+file.getName());
            AbstractJob job = AbstractJob.loadJob(file);
            if(job!=null) {
                nextId = Math.max(nextId, job.getId()+1);
                jobQueue.add(job);
                //job.start();
            }
        }
    }
    
    public static synchronized void dequeueJob(int id) {
        AbstractJob job = getJob(id);
        if(job != null && AbstractJob.getDequeueableStates().contains(job.getStatus())) {
            job.cleanup();
            jobQueue.remove(job);
        }
    }
    
    public static synchronized Iterator<AbstractJob> getJobs() {
        List<AbstractJob> list = new ArrayList<>();
        list.addAll(jobQueue);
        return list.iterator();//Arrays.asList(jobQueue.toArray(new AbstractJob[0])).iterator();
    }

    public static synchronized AbstractJob getJob(int jobId) {
            return jobQueue.stream().filter(job -> job.getId()==jobId).findAny().orElse(null);
    }

    public static synchronized void suspendAllJobs(EditCommandSender sender, boolean all) {
        jobQueue.forEach(job -> job.suspend());
    }

    public static void suspendJob(EditCommandSender sender, int jobId) {
        getJob(jobId).suspend();
    }

    public static synchronized void resumeAllJobs(EditCommandSender sender, boolean all) {
        jobQueue.forEach(job -> job.resume());
    }

    public static void resumeJob(EditCommandSender sender, int jobId) {
        getJob(jobId).resume();
    }

    public static synchronized void cancelAllJobs(EditCommandSender sender, boolean all) {
        jobQueue.forEach(job -> job.cancel());
    }

    public static void cancelJob(EditCommandSender sender, int jobId) {
        getJob(jobId).cancel();
    }
  
    public static synchronized void dequeueAllJobs(EditCommandSender sender, boolean all) {
        jobQueue.forEach(job -> {
            dequeueJob(job.getId());
        });
    }

    /*public static void deleteJob(EditCommandSender sender, int jobId) {
        AbstractJob job = getJob(jobId);
        if(!job.isRunnable()) {
            job.cleanup();
            jobQueue.remove(job);
        }
    }*/
    
    public static boolean startJobScheduler() {
        if((syncScheduler==null && asyncScheduler==null)
                || syncScheduler.isCancelled() && asyncScheduler.isEnded()) {
            syncScheduler = new SyncJobScheduler();//.runTaskTimer(EditorPlugin.getInstance(), 10, 1);
            asyncScheduler = new AsyncJobScheduler();//.runTaskLaterAsynchronously(EditorPlugin.getInstance(),5);
            return true;
        } else {
            return false;
        }
    }
    
    public static synchronized void stopJobScheduler() {
        syncScheduler.cancel();
        asyncScheduler.cancel();
        jobQueue.forEach((job) -> {
            job.releaseChunkTickets();
        });
    }
    
    public static void restartJobScheduler(EditCommandSender sender) {
        suspendAllJobs(sender, true);
        new BukkitRunnable() {
            boolean stopped = false;
            @Override
            public void run() {
                if(!stopped) {
                    sender.info("Stopping Editor Queue...");
                    stopJobScheduler();
                    stopped = true;
                } else {
                    if(asyncScheduler.isEnded()) {
                        synchronized(this) {
                            jobQueue.clear();
                        }
                        loadJobs();
                        startJobScheduler();
                        cancel();
                        sender.info("Editor Queue restart complete.");
                    } else {
                        sender.info("Waiting for the Editor Queue to finish shutdown..."+asyncScheduler.isRunning()+" "+asyncScheduler.isQueued());
                    }
                }
            }
        }.runTaskTimer(EditorPlugin.getInstance(), 100, 100);
    }
    
    //replacement for broken method from WorldEdit Polygonal2DRegion in FAWE 1.14.151
    private static Set<BlockVector2> getChunks(Region region) { //don't use sync, takes a VERY long time for huge areas
        final Set<BlockVector2> chunks = new HashSet<>();

        final BlockVector3 min = region.getMinimumPoint();
        final BlockVector3 max = region.getMaximumPoint();

        boolean large = region.getLength()*region.getWidth()>1000000;
        int increment = (large?16:1);
        
        final int minY = (min.getBlockY()+max.getBlockY())/2;
//Logger.getGlobal().info("y: "+minY);

        for (int x = min.getBlockX(); x <= max.getBlockX(); x+=increment) {
//Logger.getGlobal().info("x: "+x);
            
            for (int z = min.getBlockZ(); z <= max.getBlockZ(); z+=increment) {
//Logger.getGlobal().info("z: "+z);
                if(large) {
                    int xChunk = (x >> 4) << 4;
                    int zChunk = (z >> 4) << 4;
                    if(!(  region.contains(BlockVector3.at(xChunk, minY, zChunk))
                        || region.contains(BlockVector3.at(xChunk+15, minY, zChunk))
                        || region.contains(BlockVector3.at(xChunk, minY, zChunk+15))
                        || region.contains(BlockVector3.at(xChunk+15, minY, zChunk+15)))) {
                        continue;
                    }
                } else if (!region.contains(BlockVector3.at(x, minY, z))) {
                    continue;
                }
//Logger.getGlobal().info("add!");

                chunks.add(BlockVector2.at(
                    x >> 4,
                    z >> 4
                ));
            }
        }

        return chunks;
    }
    
}
