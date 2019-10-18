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
package com.mcmiddleearth.mcme.editor.command;

import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.command.sender.EditPlayer;
import com.mcmiddleearth.mcme.editor.data.PluginData;
import com.mcmiddleearth.mcme.editor.job.JobManager;
import com.mcmiddleearth.mcme.editor.job.JobType;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 * @author Eriol_Eandur
 */
public abstract class AbstractEditorCommand {
    
    //TODO Help feature
    //TODO Common messages
    
    public abstract LiteralArgumentBuilder<EditCommandSender> getCommandTree();
    
    public boolean isPlayer(EditCommandSender sender) {
//Logger.getGlobal().info("checking player: "+(sender instanceof EditPlayer));
        return sender instanceof EditPlayer;
    }
    
    public int sendMissingFilename(CommandContext c) {
        ((EditPlayer)c.getSource()).error("You need to specify a filename.");
        return 0;
    }

    public int startJob(CommandContext c, JobType type) {
Logger.getGlobal().info("startJob: "+type.name());
        boolean weSelection = false;
        boolean exactMatch = true;
        Set<String> worlds = new HashSet<>();
        Set<String> rps = new HashSet<>();
        String filename = null;
        try {
            String[] options = ((String) c.getArgument("<options>", String.class)).split(" ");
            for(String option: options) {
                if(option.startsWith("-we")) {
                    weSelection = true;
                } else if(option.startsWith("-m")) {
                    exactMatch = false;
                } else if(option.startsWith("-f:")) {
                    filename = option.substring(3);
                } else if(option.startsWith("-p:")) {
                    rps.add(option.substring(3));
                } else if(option.startsWith("-m:")) {
                    worlds.add(option.substring(3));
                } else {
                    ((EditCommandSender)c.getSource()).error("Invalid option:"+option);
                }
            }
        } catch (IllegalArgumentException ex) {}
        if(worlds.isEmpty() && rps.isEmpty()) {
            if(c.getSource() instanceof EditPlayer) {
                weSelection = true;
            } else {
                ((EditCommandSender)c.getSource()).error("You need to select a job area using options -p or -m");
                return 0;
            }
        }
        if(filename!=null) {
            File file = new File(PluginData.getBlockSelectionFolder(),
                                 filename+PluginData.getBlockSelectionFileExtension());
            if(file.exists()) {
                clearAllBlockSelections(c);
                ((EditCommandSender)c.getSource()).loadBlockSelections(file);
            }
        }
        if(JobManager.enqueueBlockJob((EditCommandSender)c.getSource(),weSelection,worlds,
                                      rps,type,exactMatch)) {
            ((EditCommandSender)c.getSource()).info("Job was added to editor queue.");
        } else {
            ((EditCommandSender)c.getSource()).error("Enqueue job failed. Your may need to make a valid area selection.");
        }
        return 0;
    }
    
    protected void clearAllBlockSelections(CommandContext c) {
        clearBlockSelection(c,EditCommandSender.BlockSelectionMode.COUNT);
        clearBlockSelection(c,EditCommandSender.BlockSelectionMode.REPLACE);
        clearBlockSelection(c,EditCommandSender.BlockSelectionMode.SWITCH);
    }
    
    protected void clearBlockSelection(CommandContext c, EditCommandSender.BlockSelectionMode mode) {
        EditCommandSender p = ((EditCommandSender)c.getSource());
        p.clearBlockSelection(mode);
    }
    
    protected SuggestionsBuilder addJobOptionSuggestions(SuggestionsBuilder builder) {
        builder.suggest("-we", new LiteralMessage("Limits the job to your WE selection."))
               .suggest("-m:worldname", new LiteralMessage("Limits the job to the given world."))
               .suggest("-p:rpname", new LiteralMessage("Limits the job to areas using the given rp."));
        return builder;
    }
}
