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

import com.mcmiddleearth.mcme.editor.data.block.EditBlockData;
import com.mcmiddleearth.mcme.editor.data.block.SimpleBlockData;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.util.Vector;

/**
 *
 * @author Eriol_Eandur
 */
public class CountAction implements ConfigurationSerializable {
    
    private static final int MAX_LOG = 1000;
    @Getter
    private final EditBlockData searchData;
    
    @Getter
    private final List<Vector> locations = new ArrayList<>();
    
    @Getter
    @Setter
    protected int applicationCount=0;
    
    @Getter
    private final int id;
    
    public CountAction(int id, EditBlockData search) {
        this.id = id;
        this.searchData = search.clone();
    }
    
    public EditBlockData apply(BlockData found, Vector loc) {
        if(applicationCount < MAX_LOG) {
            locations.add(loc);
        }
        applicationCount++;
        return null;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String,Object> map = new HashMap<>();
        map.put("id",id);
        map.put("search", searchData.getAsString(true));
        return map;
    }
    
    public static CountAction deserialize(Map<String,Object> map) {
        String dataString = (String)map.get("search");
        EditBlockData data;
        if(dataString.startsWith(ItemBlockData.NAMESPACE)) {
            data = new SimpleBlockData(ItemBlockData.createItemBlockData(dataString));
        } else {
            data = SimpleBlockData.createBlockData(dataString);
        }
        return new CountAction((int)map.get("id"),data);
    }
}
