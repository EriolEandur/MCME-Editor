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
package com.mcmiddleearth.mcme.editor.job.action;

import com.mcmiddleearth.architect.specialBlockHandling.data.ItemBlockData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.mcmiddleearth.mcme.editor.data.block.BlockShiftData;
import com.mcmiddleearth.mcme.editor.data.block.EditBlockData;
import com.mcmiddleearth.mcme.editor.data.block.SimpleBlockData;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public class ReplaceAction extends CountAction {
    
    @Getter
    private final EditBlockData replaceData;
    
    private final String[] flexAttributes;
    
    private final boolean flex;

    //private final double probability;

    public ReplaceAction(int id, EditBlockData search, EditBlockData replace) {
        super(id, search);
        replaceData = replace.clone();
        //this.probability = probability;
        List<String> temp = new ArrayList<>();
        if(!(replace instanceof ItemBlockData)
                && !(replace instanceof BlockShiftData)) {
            String[] all = replaceData.getAsString(false).split("[\\[\\],]");
            String[] specified = replaceData.getAsString(true).split("[\\[\\],]");
            boolean found;
            for(String attrib: all) {
                found = false;
                for(String searchAttrib: specified) {
                    if(searchAttrib.equals(attrib)) {
                        found = true;
                        break;
                    }
                }
                if(!found) {
                    temp.add(attrib.substring(0,attrib.indexOf("=")));
    //Logger.getGlobal().info("flex: "+temp.get(temp.size()-1));
                }
            }
        }
        flexAttributes = temp.toArray(new String[temp.size()]);
        flex = flexAttributes.length>0;
    }
 
    @Override
    public EditBlockData apply(BlockData found, Vector loc) {
//Logger.getLogger("ReplaceAction").info("prop: "+replaceData.getProbability());
        if(Math.random()>replaceData.getProbability()) {
            return null;
        }
        applicationCount++;
        if(!flex) {
            return replaceData;
        } else {
//Logger.getGlobal().info("found: "+found.getAsString(false));
            String[] foundData = found.getAsString(false).split("[\\[\\],]");
            String repData = replaceData.getAsString(false);
//Logger.getGlobal().info("data first: "+repData);
            for(String flexAttrib : flexAttributes) {
                for(String foundAttribute : foundData) {
                    if(foundAttribute.startsWith(flexAttrib)) {
                        String oldValue = repData.substring(repData.indexOf(flexAttrib+"=")
                                                            +flexAttrib.length()+1);
                        int end = oldValue.indexOf(",");
                        if(end < 0 ) {
                            end = oldValue.indexOf("]");
                        }
                        oldValue = oldValue.substring(0,end);
//Logger.getGlobal().info("old: "+oldValue+" "+flexAttrib);
                        String value = foundAttribute.substring(foundAttribute.indexOf("=")+1);
//Logger.getGlobal().info("new: "+value+" "+foundAttribute);
                        repData = repData.replace(flexAttrib+"="+oldValue,flexAttrib+"="+value);
//Logger.getGlobal().info("data: "+repData);
                        break;
                    }
                }
            }
//Logger.getGlobal().info("data final: "+repData);
            try { 
                return new SimpleBlockData(Bukkit.createBlockData(repData));
            } catch(IllegalArgumentException ex) {
                return null;
            }
        }
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String,Object> map = new HashMap<>();
        map.put("id",getId());
        map.put("search", getSearchData().getAsString(true));
        map.put("replace", replaceData.getAsString(true));
        return map;
    }
    
    public static ReplaceAction deserialize(Map<String,Object> map) {
        String search = (String)map.get("search");
        String replace = (String)map.get("replace");
        EditBlockData searchData = createBlockData(search);
        EditBlockData replaceData;
        if(replace.startsWith(BlockShiftData.NAMESPACE)) {
            replaceData = BlockShiftData.createEditBlockData(searchData.getBlockData(),replace);
        } else {
            replaceData = createBlockData(replace);
        }
        return new ReplaceAction((int)map.get("id"),searchData,replaceData);
    }
    
    private static EditBlockData createBlockData(String data) {
        if(data.startsWith(ItemBlockData.NAMESPACE)) {
            return new SimpleBlockData(ItemBlockData.createItemBlockData(data));
        } else {
            return SimpleBlockData.createBlockData(data);
        }
    }
}
