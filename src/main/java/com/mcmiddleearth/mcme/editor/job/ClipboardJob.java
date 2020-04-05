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
import com.mcmiddleearth.mcme.editor.command.sender.EditPlayer;
import com.sk89q.worldedit.regions.Region;
import java.util.ArrayList;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Eriol_Eandur
 */
public abstract class ClipboardJob extends AbstractJob{
    
    public ClipboardJob(EditCommandSender owner, int id, YamlConfiguration config) {
        super(owner, id, config);
    }
    
    public ClipboardJob(EditPlayer owner, int id, World world, int size, boolean includeItemBlocks) {
        super(owner, id, world, owner.getClipboard().getWeRegion(), new ArrayList<Region>(), size, includeItemBlocks);
    }

    @Override
    public void saveLogsToFile() {
    }
}
