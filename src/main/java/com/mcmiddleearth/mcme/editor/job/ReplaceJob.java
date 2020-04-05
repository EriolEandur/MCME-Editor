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

import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender.BlockSelectionMode;
import com.mcmiddleearth.mcme.editor.job.action.ReplaceAction;
import com.sk89q.worldedit.regions.Region;
import java.util.List;
import java.util.Map.Entry;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Eriol_Eandur
 */
public class ReplaceJob extends BlockSearchJob {
    
    public ReplaceJob(EditCommandSender owner, int id, YamlConfiguration config) {
        super(owner, id, config);
        //actions.putAll(config.getObject("replaces", HashMap.class));
        //loadResultsFromFile();
        
    }
    
    public ReplaceJob(EditCommandSender owner, int id, World world, Region extraRegion, List<Region> regions, 
                      boolean exactMatch, int size) {
        super(owner, id, world, extraRegion, regions, exactMatch, size, owner.hasItemBlocksSelected(BlockSelectionMode.REPLACE,BlockSelectionMode.SWITCH));
        int actionId = 0;
        for(Entry<BlockData,BlockData> entry: owner.getReplaces().entrySet()) {
            actions.put(entry.getKey(),new ReplaceAction(actionId,entry.getKey(),entry.getValue()));
            actionId++;
        }
        for(Entry<BlockData,BlockData> entry: owner.getSwitches().entrySet()) {
            actions.put(entry.getKey(),new ReplaceAction(actionId,entry.getKey(),entry.getValue()));
            actionId++;
            actions.put(entry.getValue(),new ReplaceAction(actionId,entry.getValue(),entry.getKey()));
            actionId++;
        }
        saveActionsToFile();
    }

    @Override
    public JobType getType() {
        return JobType.REPLACE;
    }

}
