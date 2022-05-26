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

import com.mcmiddleearth.mcme.editor.EditorPlugin;
import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.command.sender.EditConsoleSender;
import com.mcmiddleearth.mcme.editor.command.sender.EditPlayer;
import com.mcmiddleearth.mcme.editor.data.chunk.ChunkEditData;
import com.mcmiddleearth.mcme.editor.data.ChunkPosition;
import com.mcmiddleearth.mcme.editor.data.EditChunkSnapshot;
import com.mcmiddleearth.mcme.editor.data.PluginData;
import com.mcmiddleearth.mcme.editor.queue.ReadingQueue;
import com.mcmiddleearth.mcme.editor.queue.WritingQueue;
import com.mcmiddleearth.mcme.editor.util.Profiler;
import com.mcmiddleearth.mcme.editor.util.ProgressMessenger;
import com.mcmiddleearth.mcme.editor.util.RegionUtil;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author Eriol_Eandur
 */
public abstract class AbstractJob implements Comparable<AbstractJob>{
    
    @Getter
    @Setter
    private boolean sendUpdates = true;
    
    @Setter
    @Getter
    private static int maxChunkTickets;
    
    protected ReadingQueue readingQueue;
    protected WritingQueue writingQueue;
    
    @Getter
    private File jobDataFile;
    public final static String jobDataFileExt = ".job";
    
    private File chunkFile;
    private final static String chunkFileExt = ".chk";
    private DataInputStream chunkIn;
    
    private File progresFile;
    private final static String progresFileExt = ".pgr";
    //private DataOutputStream progresOut;
    
    @Getter
    private final EditCommandSender owner;
    
    @Getter
    private final int id;
    
    @Getter
    private final World world;
    
    @Getter
    @Setter
    private JobStatus status;
    
    @Getter
    private JobStatus statusRequested;
    
    @Getter
    private int size;
    
    @Getter
    private int current=0;
    private int lastCurrent; //For processing speed calculation
    private long lastCurrentTime; //For processing speed calculation
    private int unrequested;
    
    @Getter
    private long startTime;
    @Getter
    private long endTime;
    @Getter
    @Setter
    private long lastMessaged;
    
    @Getter
    private boolean includeItemBlocks = false;
    
    @Getter
    private YamlConfiguration config;
    
    @Getter
    private static EnumSet<JobStatus> runnableStates = EnumSet.of(JobStatus.RUNNING,JobStatus.SUSPENDED,JobStatus.WAITING);
    @Getter
    private static EnumSet<JobStatus> dequeueableStates = EnumSet.of(JobStatus.CANCELLED,JobStatus.FINISHED,JobStatus.FAILED);

    private Region extraRegion;
    private final List<Region> regions = new ArrayList<>();

    @Getter
    private int maxY, minY;
    
    private boolean refreshChunks;
    
    public static AbstractJob loadJob(File jobFile) {
        int id = Integer.parseInt(jobFile.getName().substring(0, jobFile.getName().indexOf(jobDataFileExt)));
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(new File(PluginData.getJobFolder(),id+jobDataFileExt));
            if(config.getString("status").equals(JobStatus.FINISHED.name())) {
                jobFile.delete();
                return null;
            }
            String ownerUuid = config.getString("owner","console");
            EditCommandSender owner;
            if(ownerUuid.equals("console")) {
                owner = PluginData.getConsoleSender();
            } else {
                 owner = PluginData.getOrCreateEditPlayer(UUID.fromString(ownerUuid));
            }
            switch(JobType.valueOf(config.getString("type"))) {
                case COUNT:
                    return new CountJob(owner,id, config);
                case REPLACE:
                    return new ReplaceJob(owner,id, config);
                case LIGHT:
                    return new LightJob(owner,id, config);
                case SURVIVAL_PREP:
                    return new SurvivalPrepJob(owner,id, config);
                case Y_SHIFT:
                    return new YShiftJob(owner,id,config);
                default:
                    Logger.getLogger(AbstractJob.class.getName()).log(Level.SEVERE, "Invalid job type.");
            }
        } catch (IOException | InvalidConfigurationException ex) {
            Logger.getLogger(AbstractJob.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public AbstractJob(EditCommandSender owner, int id, YamlConfiguration config) {
        status = JobStatus.CREATION;
        statusRequested = status;
        this.config = config;
        includeItemBlocks = config.getBoolean("includeItemBlocks",false);
        startTime = config.getLong("start");
        endTime = config.getLong("end");
        refreshChunks = config.getBoolean("refreshChunks",false);
        this.owner = owner;
        this.id = id;
        this.world = Bukkit.getWorld(config.getString("world"));
        if(world==null) {
            fail(null);
        }
        //weRegion = null;
        createFilesObjects();
        readSizeFromFile();
        createQueues();
        try(DataInputStream in = new DataInputStream(new FileInputStream(progresFile))) {
            current = in.readInt();
        } catch (IOException ex) {
//Logger.getGlobal().info("no Progress file found");
            current = 0;
        }
        unrequested=size-current;
        statusRequested = JobStatus.valueOf(config.getString("status", JobStatus.SUSPENDED.name()));
//Logger.getGlobal().info("job status: "+id+" "+statusRequested);
        loadRegionsFromFile();
        setYRange();
    }
    
    public AbstractJob(EditCommandSender owner, int id, World world, Region extraRegion, List<Region> regions, int size, 
                       boolean includeItemBlocks, boolean refreshChunks) {
        status = JobStatus.CREATION;
        statusRequested = status;
        startTime = System.currentTimeMillis();
        this.includeItemBlocks = includeItemBlocks;
        this.owner = owner;
        this.id = id;
        this.world = world;
        createFilesObjects();
        //readSizeFromFile();
        this.size = size;
        unrequested = size;
        this.refreshChunks = refreshChunks;
        createQueues();
        //initJobData(owner);
        config = new YamlConfiguration();
        try {
            if(owner instanceof EditPlayer) {
                config.set("owner", ((EditPlayer)owner).getUuid().toString());
            } else {
                config.set("owner", "console");
            }
            config.set("includeItemBlocks",includeItemBlocks);
            config.set("start",startTime);
            config.set("end", endTime);
            config.set("refreshChunks",refreshChunks);
            config.set("world", world.getName());
            config.set("type",getType().name());
            config.set("status", status);
            config.save(jobDataFile);
        } catch (IOException ex) {
            fail(ex);
        }
        this.extraRegion = extraRegion;
        this.regions.addAll(regions);
        setYRange();
        saveRegionsToFile();
    }
    
    /*protected void initialize() {
        openFileStreams();
        saveProgresToFile();
    }*/
    
    public abstract JobType getType();
    
    public int getRemaining() {
        return size - current;
    }

    private void createQueues() {
        readingQueue = new ReadingQueue(world);
        writingQueue = new WritingQueue(world);
    }    
    
    public static File getChunkFile(int id) {
        return new File(PluginData.getJobFolder(),id+chunkFileExt);
    }

    public void suspend() {
        if(!runnableStates.contains(status)) return;
        statusRequested = JobStatus.SUSPENDED;
        saveJobStatus();
    }

    public void resume() {
        if(!runnableStates.contains(status)) return;
        statusRequested = JobStatus.WAITING;
        saveJobStatus();
    }

    public void cancel() {
        if(!runnableStates.contains(status)) return;
        statusRequested = JobStatus.CANCELLED;
        endTime = System.currentTimeMillis();
        saveJobStatus();
    }
    
    public void start() {
        if(!status.equals(JobStatus.CREATION)) return;
        statusRequested = JobStatus.WAITING;
        saveJobStatus();
    }
    
    public boolean isRunnable() {
        return runnableStates.contains(status);
    }
    
    public boolean isDequeueable() {
        return dequeueableStates.contains(status);
    }
    
    @Override
    public int compareTo(AbstractJob other) {
        return (size-current < other.size-other.current?-1:
                    (size-current > other.size-other.current?1:0));
    }
    
    public static boolean saveChunksToFile(int id, Set<ChunkPosition> chunks, CommandSender receiver) {
        try(DataOutputStream out = new DataOutputStream(new FileOutputStream(getChunkFile(id)))) {
//Logger.getGlobal().info("write chunkFile: "+getChunkFile(id).getName());
//Logger.getGlobal().info("write size: "+chunks.size());
            out.writeInt(chunks.size());
            ProgressMessenger progress = new ProgressMessenger(receiver,2,"Writing chunk information: %1 of "+chunks.size());
            for(ChunkPosition chunk: chunks) {
                out.writeInt(chunk.getX());
//Logger.getGlobal().info("write x: "+chunk.getX());
                out.writeInt(chunk.getZ());
//Logger.getGlobal().info("write z: "+chunk.getZ());
                progress.step();
            }
            out.flush();
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(AbstractJob.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    private void readSizeFromFile() {
        try(DataInputStream in = new DataInputStream(new FileInputStream(chunkFile))) {
//Logger.getGlobal().info("read chunkFile: "+chunkFile);
           size = in.readInt();
//Logger.getGlobal().info("read size: "+size);
        } catch (IOException ex) {
            size = 0;
            fail(ex);
        }
    }
    
    private void saveProgresToFile() {
        try(DataOutputStream progresOut = new DataOutputStream(new FileOutputStream(progresFile))) {
            progresOut.writeInt(current);
            progresOut.flush();
            progresOut.close();
        } catch (IOException ex) {
            fail(ex);
        }
    }

    private void createFilesObjects() {
        jobDataFile = new File(PluginData.getJobFolder(),id+jobDataFileExt);
        chunkFile = new File(PluginData.getJobFolder(),id+chunkFileExt);
        progresFile = new File(PluginData.getJobFolder(),id+progresFileExt);
    }
    
    public void openFileStreams() {
        try {
            chunkIn = new DataInputStream(new FileInputStream(chunkFile));
            chunkIn.skipBytes(4*(1+current*2));
            //current++;
            //progresOut = new DataOutputStream(new FileOutputStream(progresFile));
        } catch (IOException ex) {
            size = 0;
            fail(ex);
        }
    }
    
    public void cleanup() {
        closeFileStreams();
        jobDataFile.delete();
        chunkFile.delete();
        progresFile.delete();
    }
    
    public void closeFileStreams() {
        try {
            if(chunkIn!=null) {
                chunkIn.close();
            }
        } catch (IOException ex) {
            fail(ex);
        }
        /*try {
            progresOut.flush();
            progresOut.close();
        } catch (IOException ex) {
            fail(ex);
        }*/
    }
    
    protected final void fail(Exception ex) {
        statusRequested = JobStatus.FAILED;
        endTime = System.currentTimeMillis();
        Logger.getLogger(AbstractJob.class.getName()).log(Level.SEVERE, null, ex);
    }

    public abstract ChunkEditData handle(EditChunkSnapshot chunk);
    
    public void work() {
//Logger.getGlobal().info("work:");
        while(readingQueue.hasChunk()) {
//Logger.getGlobal().info("handle chunk (current unrequested):" +current+" "+unrequested);
            ChunkEditData data = handle(readingQueue.pollChunk());
            writingQueue.put(data);
            saveResultsToFile();
        }
    }

    public void requestChunks() {
//Logger.getGlobal().info("request:");
        int remaining = readingQueue.remainingRequestsCapacity();
        List<ChunkPosition> request = new ArrayList<>();
        for(int i = 0; i < remaining && i < unrequested; i++) {
            try {
//Logger.getGlobal().info("request chunk (still unrequested):"+unrequested);
                request.add(new ChunkPosition(chunkIn.readInt(),chunkIn.readInt()));
            } catch (IOException ex) {
                fail(ex);
            }
        }
        readingQueue.request(request);
        unrequested -= request.size();
    }
    
    public boolean hasEdit() {
        return writingQueue.hasEdit();
    }

    public void editChunk() {
        ChunkEditData edit = writingQueue.poll();
        //if(world.getChunkAt(edit.getChunkX(), edit.getChunkZ()).isLoaded()) {
        try {
            edit.applyEdit(world, refreshChunks);
        } finally {
            new BukkitRunnable() {
                @Override
                public void run() {
                    world.removePluginChunkTicket(edit.getChunkX(), edit.getChunkZ(), EditorPlugin.getInstance());
                }
            }.runTaskLater(EditorPlugin.getInstance(), 6);
        }
//Logger.getGlobal().info("Edit Chunk: "+current);
        current++;
        saveProgresToFile();
        if(current == size) {
            statusRequested = JobStatus.FINISHED;
            endTime = System.currentTimeMillis();
            saveJobStatus();
        }
    }

    public boolean hasRequests() {
        return readingQueue.hasRequest();
    }

    public boolean needsChunks() {
        return readingQueue.remainingChunkCapacity()>0
                && (world.getPluginChunkTickets().get(EditorPlugin.getInstance()) == null
                        || world.getPluginChunkTickets().get(EditorPlugin.getInstance()).size() <= maxChunkTickets);
    }
    
    public void serveChunkRequest() {
//Logger.getGlobal().info("serveing Chunk request: ");
//Collection collection = world.getPluginChunkTickets().get(EditorPlugin.getInstance());
//Logger.getGlobal().info("Open chunk tickes: "+(collection!=null?collection.size():0));
        //Profiler.start("request");
        ChunkPosition chunk = readingQueue.nextRequest();
        //Profiler.stop("request");
        //Profiler.start("ticket");
        world.addPluginChunkTicket(chunk.getX(),chunk.getZ(), EditorPlugin.getInstance());
        //Profiler.stop("ticket");
        Profiler.start("put");
        readingQueue.putChunk(new EditChunkSnapshot(world.getChunkAt(chunk.getX(),chunk.getZ()),includeItemBlocks));
        Profiler.stop("put");
    }
    
    public void releaseChunkTickets() {
        world.removePluginChunkTickets(EditorPlugin.getInstance());
    }

    public void resetProcessingSpeed() {
        lastCurrent = current;
        lastCurrentTime = System.currentTimeMillis();
    }
       
    public double getProcessingSpeed() {
        long time = System.currentTimeMillis();
        double result = ((current - lastCurrent)/((time-lastCurrentTime)/1e3));
        lastCurrent = current;
        lastCurrentTime = time;
        return result;
    }
    
    public double getTimeToFinish(double processingSpeed) {
        return (getSize()-getCurrent())/processingSpeed;
    }
    
    public String progresMessage() {
//        Collection collection = world.getPluginChunkTickets().get(EditorPlugin.getInstance());
//Logger.getGlobal().info("Open chunk tickes: "+(collection!=null?collection.size():0));
        double ratio = (getCurrent()*1.0/getSize()*100);
        double speed;
        double timeToFinish;
        if(status.equals(JobStatus.RUNNING)) {
            speed = getProcessingSpeed();
            timeToFinish = getTimeToFinish(speed);
        } else {
            speed = 0;
            timeToFinish = 0;
        }
        speed = (speed<0.001?0:speed);
        long timeElapsed = (System.currentTimeMillis()-startTime)/1000;
        return String.format(ChatColor.BLACK+"..._n_Processing rate: _h_%1$.3g_n_ chunks/second\n"
                            +ChatColor.BLACK+"..._n_Done: _h_%2$.3g%%_n_ (_h_%3$d_n_ of _h_%4$d_n_ chunks)\n"
                            +ChatColor.BLACK+"..._n_Elapsed: _h_%5$d_n_ seconds Left: _h_"+(timeToFinish>0?"%6$.0f_n_ seconds":"unknown_n_"),
                              speed,ratio,getCurrent(),getSize(),timeElapsed,timeToFinish);
                     //.replace("_n_", ""+getOwner().infoColor()).replace("_h_", ""+getOwner().stressedColor()); moved to AsyncJobScheduler
    }
    
    public abstract String getResultMessage();
    
    private void saveJobStatus() {
        try {
//Logger.getGlobal().info("saveJobStatus: "+statusRequested.name());
            config.set("status", statusRequested.name());
            config.save(jobDataFile);
        } catch (IOException ex) {
            fail(ex);
        }
    }
    
    public abstract void saveResultsToFile();
    
    public abstract void saveLogsToFile();
    
    public boolean isOwner(EditCommandSender sender) {
        return (sender instanceof EditConsoleSender && owner instanceof EditConsoleSender)
                || (sender instanceof EditPlayer 
                    && owner instanceof EditPlayer
                    && ((EditPlayer)sender).getUuid().equals(((EditPlayer)owner).getUuid()));
    }
    
    private void setYRange() {
        if(regions.isEmpty() && extraRegion!=null) {
            maxY = extraRegion.getMaximumPoint().getBlockY();
            minY = extraRegion.getMinimumPoint().getBlockY();
        } else {
            maxY = 0;
            minY = getWorld().getMaxHeight();
            for(Region region: regions) {
                if(region.getMaximumPoint().getBlockY()>maxY) {
                    maxY = region.getMaximumPoint().getBlockY();
                }
                if(region.getMinimumPoint().getBlockY()<minY) {
                    minY = region.getMinimumPoint().getBlockY();
                }
            }
            if(extraRegion!=null) {
                maxY = Math.min(maxY, extraRegion.getMaximumPoint().getBlockY());
                minY = Math.max(minY, extraRegion.getMinimumPoint().getBlockY());
            }
        }
    }
    
    protected final boolean isInside(int chunkX, int chunkZ, int x, int y, int z) {
        x = 16*chunkX + x;
        z = 16*chunkZ + z;
        if(extraRegion!=null && !extraRegion.contains(BlockVector3.at(x,y,z))) {
//Logger.getGlobal().info("not in extra region: "+chunkX+ " "+ chunkZ+" "+x+" "+y+" "+z+" "+extraRegion);
            return false;
        }
        if(regions.isEmpty()) {
//Logger.getGlobal().info("in extra region: "+chunkX+ " "+ chunkZ+" "+x+" "+y+" "+z+" "+extraRegion);
            return true;
        }
        for(Region region:regions) {
            if(region.contains(BlockVector3.at(x, y, z))) {
//Logger.getGlobal().info("is inside: "+chunkX+ " "+ chunkZ+" "+x+" "+y+" "+z+" "+region);
                return true;
            }
        }
        return false;
    }
    
    private void loadRegionsFromFile() {
        if(getConfig().contains("extraRegion")) {
            extraRegion = RegionUtil.loadFromMap(getConfig().getConfigurationSection("extraRegion")
                                                            .getValues(true));
        } else {
            extraRegion = null;
        }
        List<Map<?,?>> regionMaps = getConfig().getMapList("regions");
        for(Map<?,?> map: regionMaps) {
            helper(map);
        }
    }
    
    private <T,V> void helper(Map<T,V> map) {
        regions.add(RegionUtil.loadFromMap(map));
    }
    
    private void saveRegionsToFile() {
        if(extraRegion!=null) {
            getConfig().set("extraRegion", RegionUtil.saveToMap(extraRegion));
        }
        List<Map<String,Object>> regionMaps = new ArrayList<>();
        for(Region region: regions) {
            regionMaps.add(RegionUtil.saveToMap(region));
        }
        getConfig().set("regions", regionMaps);
    }
    

}
