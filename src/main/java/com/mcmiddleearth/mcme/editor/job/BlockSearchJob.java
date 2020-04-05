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

import com.mcmiddleearth.architect.specialBlockHandling.data.ItemBlockData;
import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.data.ChunkEditData;
import com.mcmiddleearth.mcme.editor.data.EditChunkSnapshot;
import com.mcmiddleearth.mcme.editor.data.PluginData;
import com.mcmiddleearth.mcme.editor.job.action.CountAction;
import com.sk89q.worldedit.regions.Region;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public abstract class BlockSearchJob extends AbstractJob{
    
    private File resultsFile,resultsTempFile,logFile;
    private final static String resultsFileExt = ".res";
    private final static String resultsTempFileExt = ".tmp";
    private final static String logFileExt = ".log";
    //private DataOutputStream resultsOut;
    
    private boolean exactMatch;
    
    protected final Map<BlockData,CountAction> actions = new HashMap<>();
    
    public BlockSearchJob(EditCommandSender owner, int id, YamlConfiguration config) {
        super(owner, id, config);
        createFileObjects();
        ///extraRegion=null;
        List list = config.getList("actions");
        exactMatch = config.getBoolean("exactMatch",true);
        list.forEach(action->actions.put(((CountAction)action).getSearchData(), (CountAction)action));
        loadResultsFromFile();
    }
    
    public BlockSearchJob(EditCommandSender owner, int id, World world, Region extraRegion, List<Region> regions, 
                          boolean exactMatch, int size, boolean includeItemBlocks) {//boolean weSelection, Set<String> worlds, Set<String> rps) {
        super(owner, id, world, extraRegion, regions, size, includeItemBlocks);
        this.exactMatch = exactMatch;
        saveJobDataToFile();
        createFileObjects();
    }
 
    protected final void saveActionsToFile() {
        try {
            List<Object> list = new ArrayList<>();
            actions.values().forEach(action->list.add(action));
            getConfig().set("actions", list);
            getConfig().save(getJobDataFile());
        } catch (IOException ex) {
            fail(ex);
        }
    }
    
    @Override
    public synchronized void saveLogsToFile() {
        try(PrintWriter fw = new PrintWriter(new FileWriter(logFile))) {
            actions.values().stream().map((action) -> {
                fw.println(action.getSearchData().getAsString(true).replace("minecraft:", "")
                        + "->" + action.getApplicationCount());
                return action;
            }).forEachOrdered((action) -> {
                action.getLocations().forEach((loc) -> {
                    fw.println(" "+loc.getBlockX()+" | "+loc.getBlockY()+" | "+loc.getBlockZ());
                });
            });
        } catch (IOException ex) {
            Logger.getLogger(BlockSearchJob.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public synchronized void saveResultsToFile() {
//Logger.getGlobal().info("save!");
        try(final DataOutputStream resultsOut = new DataOutputStream(new FileOutputStream(resultsTempFile))) {
            for(CountAction action: actions.values()) {
//Logger.getGlobal().info(""+action.getId());
                resultsOut.writeInt(action.getId());
//Logger.getGlobal().info(""+action.getApplicationCount());
                resultsOut.writeInt(action.getApplicationCount());
            }
            resultsOut.flush();
            resultsOut.close();
            resultsTempFile.renameTo(resultsFile);
        } catch (IOException ex) {
            fail(ex);
        }
    }
    
    protected synchronized final void loadResultsFromFile() {
//Logger.getGlobal().info("Load");
        if(resultsFile.exists()) {
            try(final DataInputStream resultsIn = new DataInputStream(new FileInputStream(resultsFile))) {
                Map<Integer,Integer> results = new HashMap<>();
                for(int i=0; i<actions.size();i++) {
//Logger.getGlobal().info("read");
                    results.put(resultsIn.readInt(),resultsIn.readInt());
                }
                actions.values().forEach(action -> action.setApplicationCount(results.get(action.getId())));
                resultsIn.close();
            } catch (IOException ex) {
                fail(ex);
            }
        }
    }
    
    public String getResults() {
        return "not implemented";
    }

    private void createFileObjects() {
        resultsFile = new File(PluginData.getJobFolder(),getId()+resultsFileExt);
        resultsTempFile = new File(PluginData.getJobFolder(),getId()+resultsTempFileExt);
        logFile = new File(PluginData.getJobFolder(),getId()+logFileExt);
    }
    
    /*@Override
    protected void openFileStreams() {
        super.openFileStreams();
        try {
            resultsOut = new DataOutputStream(new FileOutputStream(resultsFile).);
        } catch (FileNotFoundException ex) {
            fail(ex);
        }
    }*/
    
    @Override
    public void cleanup() {
        super.cleanup();
        resultsFile.delete();
        resultsTempFile.delete();
    }
    
    /*@Override
    public void closeFileStreams() {
        super.closeFileStreams();
        try {
            resultsOut.flush();
            resultsOut.close();
        } catch (IOException ex) {
            fail(ex);
        }
    }*/
    
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
        for(int i=0; i<16; i++) {
            for(int j=0; j<16; j++) {
//Logger.getGlobal().info("maxY: "+getMaxY());
                for(int k=getMinY(); k<=Math.min(chunk.getHighestBlockYAt(i, j),getMaxY());k++) {
//Logger.getGlobal().info("inside: "+isInside(chunk.getX(),chunk.getZ(),i,k,j));
                    if(complete || isInside(chunk.getX(),chunk.getZ(),i,k,j)) {
//Logger.getGlobal().info("checking: "+i+" "+k+" "+j);
                        BlockData data = chunk.getBlockData(i, k, j);
                        CountAction action = null;
                        if(exactMatch && !isIncludeItemBlocks()) {
//Logger.getGlobal().info("exact Search without item blocks");
                            action = actions.get(data);
                        } else {
                            for(Entry<BlockData,CountAction> search: actions.entrySet()) {
//Logger.getGlobal().info("search: "+search.getKey().toString());
                                if(search.getKey() instanceof ItemBlockData) {
//Logger.getGlobal().info("search for itemBlock! "+((ItemBlockData)search.getKey()).getBlockData().getAsString(false));
//Logger.getGlobal().info("data! "+data.getAsString(false));
                                    if(search.getKey().matches(data)) {
//Logger.getGlobal().info("first match! ");
                                        ItemBlockData itemBlock = editChunk.getItemBlockData(new Vector(i,k,j));
//Logger.getGlobal().info("search: "+((ItemBlockData)search.getKey()).getAsString());
//Logger.getGlobal().info("found : "+itemBlock.getAsString());
                                        if(search.getKey().equals(itemBlock)) {
//Logger.getGlobal().info("match found! ");
                                            action = search.getValue();
                                            break;
                                        }
                                    }
                                } else {
//Logger.getGlobal().info(" no itemBlock! "+search.getKey().toString());
    //Logger.getGlobal().info("search: "+search.getValue().toString());
    //Logger.getGlobal().info("data: "+data.toString());
                                    if(data.matches(search.getKey())) {
    //Logger.getGlobal().warning("Match!");
                                        action = search.getValue();
                                        break;
                                    }
                                }
                            }
                        }
//Logger.getGlobal().info("action: "+action);
                        if(action!=null) {
                            BlockData replace = action.apply(data, new Vector(chunk.getX()*16+i,k,chunk.getZ()*16+j));
//Logger.getGlobal().info("replace: "+replace);
                            if(replace!=null) {
                                edit.add(new Vector(i,k,j), replace);
                            }
                            if(action.getSearchData() instanceof ItemBlockData) {
                                edit.addRemoval(new Vector(i,k,j), (ItemBlockData) action.getSearchData());
                            }
                        }
                    }
                }
            }
        }
        return edit;
    }
    
    @Override
    public String getResultMessage() {
        String result = "\nFound blocks:\n";
        for(CountAction action: actions.values()) {
            result = result + ChatColor.AQUA+action.getSearchData().getAsString(true).replace("minecraft:", "")
                            + ChatColor.GRAY+"->"
                            + ChatColor.GREEN+action.getApplicationCount()+"\n";
        }
        return result;
    }
    
    private void saveJobDataToFile() {
        getConfig().set("exactMatch", exactMatch);
    }
}
