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

import com.mcmiddleearth.mcme.editor.EditorPlugin;
import com.mcmiddleearth.mcme.editor.Permissions;
import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.command.sender.EditPlayer;
import com.mcmiddleearth.mcme.editor.data.PluginData;
import com.mcmiddleearth.mcme.editor.data.block.EditBlockData;
import com.mcmiddleearth.mcme.editor.data.block.SimpleBlockData;
import com.mcmiddleearth.pluginutil.NumericUtil;
import com.mcmiddleearth.pluginutil.StringUtil;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

/**
 * Manage block rules for jobs. Subcommands: switch, replace, count
 * Get rules from selection tools or from argument
 * Save/load rule sets to file
 * @author Eriol_Eandur
 */
public class BlockCommand extends AbstractEditorCommand {

    
    @Override
    public LiteralArgumentBuilder getCommandTree() {
        return literal("block")
            .requires(s -> ((EditCommandSender)s).hasPermissions(Permissions.BLOCK))
            .executes(c -> { 
                ((EditCommandSender)c.getSource()).info("Manual at: ???.");
                return 0;
            })
            .then(literal("clear")
                .executes(c -> { 
                    clearAllBlockSelections(c);
                    ((EditCommandSender)c.getSource()).info("All block selection lists have been cleared.");
                    return 0;
                }))
            .then(literal("list")
                .requires(s -> ((EditCommandSender)s).hasPermissions(Permissions.BLOCK_FILES))
                .executes(c -> {return sendBlockSelectionFilesMessage((EditCommandSender)c.getSource(),1);})
                .then(argument("<page>", integer())
                    .executes(c -> {
                        int argument = 1;
                        try { argument = c.getArgument("<page>",Integer.class);
                        }catch(IllegalArgumentException ex){};
                        sendBlockSelectionFilesMessage((EditCommandSender)c.getSource(),argument);
                        return 0;
                    })))
            .then(literal("save")
                .requires(s -> ((EditCommandSender)s).hasPermissions(Permissions.BLOCK_FILES))
                .executes(c -> {return sendMissingFilename(c);})
                .then(argument("<filename>", word())
                    .executes(c -> {return saveBlockSelections(c,false);})
                    .then(literal("-o")
                        .executes(c -> {return saveBlockSelections(c,true);}))))
            .then(literal("load")
                .requires(s -> ((EditCommandSender)s).hasPermissions(Permissions.BLOCK_FILES))
                .executes(c -> {return sendMissingFilename(c);})
                .then(argument("<filename>", word())
                    .executes(c -> {return loadBlockSelections(c,false);})
                    .suggests((c,builder) -> {
/*Logger.getGlobal().info("context input: "+c.getInput());
Logger.getGlobal().info("builder input: "+builder.getInput());
Logger.getGlobal().info("builder remaining: "+builder.getRemaining());
Logger.getGlobal().info("builder start: "+builder.getStart());*/
                        return addBlockSelectionFileSuggestions(builder).buildFuture();
                    })
                        //return builder.buildFuture();})
                    .then(literal("-a")
                        .executes(c -> {return loadBlockSelections(c,true);}))))
            .then(literal("delete")
                .requires(s -> ((EditCommandSender)s).hasPermissions(Permissions.BLOCK_FILES))
                .executes(c -> {
                    sendMissingFilename(c);
                    return 0;})
                .then(argument("<filename>", word())
                    .executes(c -> {return deleteBlockSelections(c);})
                    .suggests((c,builder) -> {return addBlockSelectionFileSuggestions(builder).buildFuture();})))
            .then(blockSelectionModeSubcommand(Permissions.BLOCK_COUNT,
                                                  EditCommandSender.BlockSelectionMode.COUNT))
            .then(blockSelectionModeSubcommand(Permissions.BLOCK_REPLACE,
                                                  EditCommandSender.BlockSelectionMode.REPLACE))
            .then(blockSelectionModeSubcommand(Permissions.BLOCK_REPLACE,
                                                  EditCommandSender.BlockSelectionMode.SWITCH));
    }
    
    private LiteralArgumentBuilder<Object>
                blockSelectionModeSubcommand(Permissions perm, EditCommandSender.BlockSelectionMode mode) {
        return literal(mode.name().toLowerCase())
            .requires(s -> ((EditCommandSender)s).hasPermissions(perm))
            .executes(c -> {
                setBlockSelectionMode(c, mode);
                return 0;  
            })
            .then(literal("clear")
                .executes(c -> {
                    clearBlockSelection(c,mode);
                    ((EditCommandSender)c.getSource()).info("Cleared block selection list: "+mode.name());
                    setBlockSelectionMode(c,mode);
                    return 0;
                }))
            .then(argument("<blockData>",greedyString())
                .executes(c -> {
                    setBlockSelectionMode(c, mode);
                    return 0;
                }));
        }
                
    private void setBlockSelectionMode(CommandContext c, EditCommandSender.BlockSelectionMode mode) {
        EditCommandSender p = ((EditCommandSender)c.getSource());
        p.setBlockSelectionMode(mode);
        if(p instanceof EditPlayer) {
            ((EditPlayer)p).setIncompleteBlockSelection(null);
        }
        try {
            String args = (String) c.getArgument("<blockData>", String.class);
            /*if(args.startsWith("-c")) {
                clearBlockSelection(c, mode);
                p.info("Cleared block selection list: "+mode.name());
            } else {*/
            switch(mode) {
                case COUNT:
                    getBlockCountArguments(p,args).forEach(data -> p.addCount(data)); break;
                case REPLACE:
                    getBlockReplaceArguments(p,args,">").forEach(data -> p.addReplace(data[0],data[1])); break;
                case SWITCH:
                    getBlockReplaceArguments(p,args,"<>").forEach(data -> p.addSwitch(data[0],data[1])); break;
            }
            //}
        } catch(IllegalArgumentException ex) {}
        sendSelectedBlocksMessage(p);
    }
    
    private List<EditBlockData> getBlockCountArguments(EditCommandSender p, String args) {
//Logger.getGlobal().info("end: "+args);//c.getRange().getEnd());
        String[] argArray = args.trim().split(" ");
        List<EditBlockData> list = new ArrayList<>();
        for(String argument: argArray) {
            try {
                EditBlockData blockData = parseBlockData(argument);
                list.add(blockData);
            }catch(IllegalArgumentException ex) {
                p.error("Block data must have format id:dv or material[attribute1=value1,attribute2=value2]");
            }
        }
        return list;
        /*reader.setCursor(c.getRange().getEnd());
        while(reader.canRead()) {
            reader.skipWhitespace();
            String argument = reader.readUnquotedString();
Logger.getGlobal().info("Read argument: "+argument);
            String[] data = argument.split(":");
            int id = 0;
            int dv = 0;
            if(data.length>0 && NumericUtil.isInt(data[0])) {
                id = NumericUtil.getInt(data[0]);
            } else {
                ((EditPlayer)c.getSource()).error("Block data must have format id:dv");
                continue;
            }
            if(data.length>1 && NumericUtil.isInt(data[1])) {
                dv = NumericUtil.getInt(data[1]);
            }
Logger.getGlobal().info("Found argument: "+id+":"+dv);
        }
        return null;*/
    }
    
    private List<EditBlockData[]> getBlockReplaceArguments(EditCommandSender p, String args, String delimiter) {
//Logger.getGlobal().info("end: "+args);//c.getRange().getEnd());
        String[] argArray = args.trim().split(" ");
        List<EditBlockData[]> list = new ArrayList<>();
        for(String argument: argArray) {
            try {
                String[] argumentArray = argument.split(delimiter);
                if(argumentArray.length<2) {
                    p.error("You need to specify two sets of block data separated by '"+delimiter+"'");
                    continue;
                }
                EditBlockData blockDataReplace = parseBlockData(argumentArray[0]);
                EditBlockData blockDataPlace = parseBlockData(argumentArray[1]);
                list.add(new EditBlockData[]{blockDataReplace, blockDataPlace});
            }catch(IllegalArgumentException ex) {
                p.error("Block data must have format id:dv or material[attribute1=value1,attribute2=value2]");
            }
        }
        return list;
   }

    private EditBlockData parseBlockData(String argument) throws IllegalArgumentException {
        String[] data = argument.split(":");
        int id;
        int dv = 0;
        if(data.length>0 && NumericUtil.isInt(data[0])) {
            id = NumericUtil.getInt(data[0]);
            if(data.length>1 && NumericUtil.isInt(data[1])) {
                dv = NumericUtil.getInt(data[1]);
            }
//Logger.getGlobal().info("Found argument: "+id+":"+dv);
            throw new UnsupportedOperationException("Block definitions by ID and DV are no longer supported.");
            //return new SimpleBlockData(BlockDataManager.getBlockData(id,(byte)dv));
        } else {
            if(!argument.startsWith("minecraft:")) {
                argument = "minecraft:"+argument;
            }
//Logger.getGlobal().info("Found argument: "+argument);
            EditBlockData blockData = SimpleBlockData.createBlockData(argument);
            return blockData;
        }
    }
    
    private SuggestionsBuilder addBlockSelectionFileSuggestions(SuggestionsBuilder builder) {
        File[] files = PluginData.getBlockSelectionFolder()
                                 .listFiles(PluginData.getBlockSelectionFileFilter());
        for(File file: files) {
            String name = file.getName();
            if(name.startsWith(builder.getRemaining())) {
                builder.suggest(name.substring(0,
                                     name.length()-PluginData.getBlockSelectionFileExtension().length()));
            }
        }
        return builder;
    }
    
    public int sendBlockSelectionFilesMessage(EditCommandSender sender, int page) {
        if(sender instanceof EditPlayer) {
             EditorPlugin.getMessageUtil()
                         .sendFancyFileListMessage((Player)sender.getSender(), 
                                 new FancyMessage(EditorPlugin.getMessageUtil())
                                         .addSimple("Available block selection files:"), 
                                 PluginData.getBlockSelectionFolder(), 
                                 PluginData.getBlockSelectionFileFilter(),
                                 new String[]{""+page}, 
                                 "/block list", "/block load", false);
        } else {
            File[] files = PluginData.getBlockSelectionFolder().listFiles(PluginData.getBlockSelectionFileFilter());
            sender.info("Available block selection files:");
            for(File file: files) {
                sender.info(file.getName());
            }
        }
         return 0;
    //public void sendFancyFileListMessage(Player recipient, FancyMessage header,
      //                                              File baseDir, FileFilter filter, String[] args,
        //                                            String listCommand, String selectCommand, boolean showSubDir) {
        // args may be length 0 or include: [relative Dir] [#page]
        // list command must be like: /listCommand [relativeDirectory] [#page]
        // select command must be like: /selectCommand <filename>
    }
    
    private int loadBlockSelections(CommandContext c, boolean append) {
        //try {
            String filename = (String) c.getArgument("<filename>", String.class);
            File file = new File(PluginData.getBlockSelectionFolder(),
                                 filename+PluginData.getBlockSelectionFileExtension());
            if(file.exists()) {
                if(!append) {
                    clearAllBlockSelections(c);
                }
                ((EditCommandSender)c.getSource()).loadBlockSelections(file);
                ((EditCommandSender)c.getSource()).info("Block selections loaded.");
                sendSelectedBlocksMessage((EditCommandSender)c.getSource());
            } else {
                ((EditCommandSender)c.getSource()).error("File not found.");
            }
        /*} catch (IllegalArgumentException ex) {
            ((EditPlayer)c.getSource()).error("You need to specify a filename.");
        }*/
        return 0;
    }
    
    private int saveBlockSelections(CommandContext c, boolean overwrite) {
        //try{
            String filename = (String) c.getArgument("<filename>", String.class);
            File file = new File(PluginData.getBlockSelectionFolder(),
                                 filename+PluginData.getBlockSelectionFileExtension());
//Logger.getGlobal().info("overwrite: "+overwrite+" filename: "+file.getName());
            if(file.exists() && !overwrite) {
                ((EditCommandSender)c.getSource()).error("That file already exists. Use argument '-o' to overwrite");
            } else {
                ((EditCommandSender)c.getSource()).saveBlockSelections(file);
                ((EditCommandSender)c.getSource()).info("Block selections save.");
            }
        /*} catch (IllegalArgumentException ex) {
            ((EditPlayer)c.getSource()).error("You need to specify a filename.");
        }*/
        return 0;
    }
    
    public static void sendSelectedBlocksMessage(EditCommandSender p) {
        String selection = StringUtil.concat(p.getBlockSelections(p.getBlockSelectionMode()),"\n");
        if(p instanceof EditPlayer) {
            ((EditPlayer)p).info("Block selection mode: "
                        +EditorPlugin.getMessageUtil().STRESSED+p.getBlockSelectionMode().name()
                        +EditorPlugin.getMessageUtil().INFO+". Hover here for more info about selected blocks.",
                    null,(selection.equals("")?"<none>":selection));
        } else {
            p.info("Block selection mode: "
                        +EditorPlugin.getMessageUtil().STRESSED+p.getBlockSelectionMode().name()
                        +EditorPlugin.getMessageUtil().INFO+".\n"+(selection.equals("")?"<none>":selection));
        }
    }

    private int deleteBlockSelections(CommandContext<Object> c) {
        String filename = (String) c.getArgument("<filename>", String.class);
        File file = new File(PluginData.getBlockSelectionFolder(),
                             filename+PluginData.getBlockSelectionFileExtension());
        if(file.exists()) {
            file.delete();
        }
        return 0;
    }
    

}
