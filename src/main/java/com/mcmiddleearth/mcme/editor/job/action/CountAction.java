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
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

/**
 *
 * @author Eriol_Eandur
 */
public class CountAction implements ConfigurationSerializable {
    
    @Getter
    private final BlockData searchData;
    
    @Getter
    @Setter
    protected int applicationCount=0;
    
    @Getter
    private final int id;
    
    public CountAction(int id, BlockData search) {
        this.id = id;
        this.searchData = search.clone();
    }
    
    public BlockData apply(BlockData found) {
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
        BlockData data;
        if(dataString.startsWith(ItemBlockData.NAMESPACE)) {
            data = ItemBlockData.createItemBlockData(dataString);
        } else {
            data = Bukkit.createBlockData(dataString);
        }
        return new CountAction((int)map.get("id"),data);
    }
}
