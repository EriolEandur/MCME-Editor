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
import com.mcmiddleearth.mcme.editor.job.action.CountAction;
import com.sk89q.worldedit.regions.Region;
import java.util.List;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Eriol_Eandur
 */
public class CountJob extends BlockSearchJob {
    
    public CountJob(EditCommandSender owner, int id, YamlConfiguration config) {
        super(owner, id, config);
        //actions.putAll(config.getObject("counts", HashMap.class));
        //loadResultsFromFile();
        
    }
    
    public CountJob(EditCommandSender owner, int id, World world, Region extraRegion, List<Region> regions, 
                    boolean exactMatch, int size) {
        super(owner, id, world, extraRegion, regions, exactMatch, size, owner.hasItemBlocksSelected(EditCommandSender.BlockSelectionMode.COUNT));
        int actionId = 0;
        for(BlockData blockData: owner.getCounts()) {
            actions.put(blockData,new CountAction(actionId,blockData));
            actionId++;
        }
        saveActionsToFile();
    }

    @Override
    public JobType getType() {
        return JobType.COUNT;
    }

}
