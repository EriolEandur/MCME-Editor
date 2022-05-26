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
package com.mcmiddleearth.mcme.editor;

import com.mcmiddleearth.mcme.editor.command.*;
import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.command.sender.EditConsoleSender;
import com.mcmiddleearth.mcme.editor.job.JobManager;
import com.mcmiddleearth.mcme.editor.job.action.CountAction;
import com.mcmiddleearth.mcme.editor.job.action.ReplaceAction;
import com.mcmiddleearth.mcme.editor.listener.BlockSelectionListener;
import com.mcmiddleearth.mcme.editor.listener.LightBrush;
import com.mcmiddleearth.mcme.editor.queue.QueueConfiguration;
import com.mcmiddleearth.pluginutil.message.MessageUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;


/**
 *
 * @author Eriol_Eandur
 */
public class EditorPlugin extends JavaPlugin {
    
    @Getter
    private static MessageUtil messageUtil;
        
    @Getter
    private static EditorPlugin instance;
    
    private static CommandDispatcher<EditCommandSender> dispatcher = new CommandDispatcher<>();
    
    @Override
    public void onEnable() {
        instance = this; 
        messageUtil = new MessageUtil();
        messageUtil.setPluginName(this.getName());
        ConfigurationSerialization.registerClass(CountAction.class);
        ConfigurationSerialization.registerClass(ReplaceAction.class);
        //necessary??? this.getCommand("block").setExecutor(this);
        dispatcher.register(new BlockCommand().getCommandTree());
        dispatcher.register(new CountCommand().getCommandTree());
        dispatcher.register(new ReplaceCommand().getCommandTree());
        dispatcher.register(new QueueCommand().getCommandTree());
        dispatcher.register(new LightCommand().getCommandTree());
        dispatcher.register(new YShiftCommand().getCommandTree());
        getServer().getPluginManager().registerEvents(new BlockSelectionListener(), this);
        getServer().getPluginManager().registerEvents(new LightBrush(), this);
        Permissions.register();
        QueueConfiguration.loadConfig();
        JobManager.loadJobs();
        JobManager.startJobScheduler();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command,
                             String alias, String[] args) {
        String input = getInput(command,args);
        ParseResults<EditCommandSender> parseResult = dispatcher.parse(input, EditCommandSender.wrap(sender));
        if(parseResult.getExceptions().size()>0) {
            parseResult.getContext().getSource().getSender().sendMessage("Invalid command syntax.");
            return true;
        }
        /*Logger.getGlobal().info("Parse Result: "+parseResult.getContext().getCommand());
        for(CommandNode node: parseResult.getContext().getNodes().keySet()) {
            Logger.getGlobal().info("Node: "+node.getName()); 
                                  //+ " Error: "+parseResult.getExceptions().get(node).getMessage());
        }*/
        try {
            dispatcher.execute(parseResult);
        } catch (CommandSyntaxException ex) {
            if(parseResult.getContext().getSource() instanceof EditConsoleSender) {
                parseResult.getContext().getSource().error("Command can be used by players only.");
            } else {
                parseResult.getContext().getSource().error("Command unknown or you don't have permission to use.");
            }
            Logger.getLogger(EditorPlugin.class.getName()).log(Level.SEVERE, null, ex);
            //return false;
        }
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        EditCommandSender editSender = EditCommandSender.wrap(sender);
        try {
            CompletableFuture<Suggestions> completionSuggestions = dispatcher.getCompletionSuggestions(dispatcher.parse(getInput(command, args), editSender));
            return completionSuggestions.get().getList().stream().map(Suggestion::getText).collect(Collectors.toList());
        } catch (InterruptedException | ExecutionException e) {
            //e.printStackTrace();
        }
        return new ArrayList<>();
    }
    /*
    @Override
    public List<String> onTabComplete​(CommandSender sender, Command command, 
                                 String alias, String[] args) {
        String input = getInput(command, args);
        ParseResults<EditCommandSender> parseResult = dispatcher.parse(input, EditCommandSender.wrap(sender));
        if(parseResult.getExceptions().size()>0) {
            parseResult.getContext().getSource().getSender().sendMessage("Invalid command syntax.");
            return new ArrayList<>();
        }
        CompletableFuture<Suggestions> future = dispatcher.getCompletionSuggestions(parseResult);
        try {
            List<String> suggestions = new ArrayList<>();
            future.get().getList().forEach(line -> {
//Logger.getGlobal().info("sug: "+line);
                suggestions.add(line.getText());
            });
            return suggestions;
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EditorPlugin.class.getName()).log(Level.SEVERE, null, ex);
            return new ArrayList<>();
        }
        /*
//Logger.getGlobal().info("range "+parseResult.getContext().getRange());
        CommandContextBuilder<EditCommandSender> lastChild = parseResult.getContext().getLastChild();
        List<String> suggestions = new ArrayList<>();
//parseResult.getContext().getNodes().forEach((node, range) ->
//Logger.getGlobal().info("nodes: "+node.getName()+" "+range.toString()));
//Logger.getGlobal().info("last child range "+lastChild.getRange());
        String partial = new StringRange(lastChild.getRange().getEnd(),
                                                                parseResult.getReader().getTotalLength())
                                                    .get(parseResult.getReader()).trim();
//Logger.getGlobal().info("partial: "+partial);        
        Entry<CommandNode<EditCommandSender>,StringRange> entry = lastChild.getNodes().entrySet().stream()
                 .max((c1,c2) -> (c1.getValue().getStart()>c2.getValue().getStart()?1:
                                   (c1.getValue().getStart()<c2.getValue().getStart()?-1:0)))
                 .get();
            CommandNode<EditCommandSender> node = entry.getKey();
            node.getChildren().stream()
                    .filter(child -> child.getName()
                                    .startsWith(partial))
                    .forEach(child -> suggestions.add(child.getName()));
        return suggestions;*/
    //}
    
    @Override
    public void onDisable() {
        JobManager.stopJobScheduler();
        //TODO close all job file streams
    }
    
    private String getInput(Command command, String[] args) {
        String input =  command.getName();
        for(String arg: args) {
            input = input+CommandDispatcher.ARGUMENT_SEPARATOR+arg;
        }
        return input;
    }
    
}
