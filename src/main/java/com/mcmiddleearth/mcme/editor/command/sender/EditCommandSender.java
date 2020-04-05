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
package com.mcmiddleearth.mcme.editor.command.sender;

import com.mcmiddleearth.architect.specialBlockHandling.data.ItemBlockData;
import com.mcmiddleearth.mcme.editor.EditorPlugin;
import com.mcmiddleearth.mcme.editor.Permissions;
import com.mcmiddleearth.mcme.editor.data.PluginData;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 *
 * @author Eriol_Eandur
 */
public abstract class EditCommandSender {
    
    public abstract CommandSender getSender();
    
    @Getter
    private Map<BlockData,BlockData> replaces = new HashMap<>();
    @Getter
    private Map<BlockData,BlockData> switches = new HashMap<>();
    @Getter
    private Set<BlockData> counts = new HashSet<>();

    @Getter @Setter
    private BlockSelectionMode blockSelectionMode = BlockSelectionMode.COUNT;
    
    public static EditCommandSender wrap(CommandSender sender) {
        if(sender instanceof ConsoleCommandSender) {
            return PluginData.getConsoleSender();
        } else if(sender instanceof Player) {
            return PluginData.getOrCreateEditPlayer((Player) sender);
        } else {
            throw new IllegalArgumentException("Sender needs to be Player or ConsoleCommandSender");
        }
    }
    
    public abstract boolean isOnline();
    
    public boolean hasPermissions(Permissions... permissions) {
        for(Permissions permission: permissions) {
//Logger.getGlobal().info("checking permission: "+permission.getPermissionNode());
            for(Permissions perm : permission.getWithChildren()) {
//Logger.getGlobal().info("checking child permission: "+perm.getPermissionNode());
                if(getSender().hasPermission(perm.getPermissionNode())) {
//Logger.getGlobal().info("found");
                   return true;
                } else {
//Logger.getGlobal().info("not found.");
                }
            }
        }
//Logger.getGlobal().info("no permission.");
        return false;
    }
    
    public void info(String message) {
        if(isOnline()) {
            EditorPlugin.getMessageUtil().sendInfoMessage(getSender(), message);
        }
    }
    
    public void error(String message) {
        if(isOnline()) {
            EditorPlugin.getMessageUtil().sendErrorMessage(getSender(), message);
        }
    }
    
    public void sendNoPermissionMessage() {
        if(isOnline()) {
            EditorPlugin.getMessageUtil().sendErrorMessage(getSender(), "You don't have permission to do this.");
        }
    }
    
    public List<String> getBlockSelections(BlockSelectionMode mode) {
        List<String> result = new ArrayList<>();
        switch(mode) {
            case COUNT:
                boolean secondColor = false;
                for(BlockData data: counts) {
                    result.add((secondColor?ChatColor.LIGHT_PURPLE:ChatColor.DARK_PURPLE)
                               +data.getAsString(true));
                    secondColor = !secondColor;
                } break;
            case REPLACE:
                replaces.forEach((blockData1, blockData2) 
                        -> { result.add(ChatColor.RED+blockData1.getAsString(true));
                             result.add(ChatColor.GREEN+blockData2.getAsString(true));}); break;
            case SWITCH:
                switches.forEach((blockData1, blockData2) 
                        -> { result.add(ChatColor.GOLD+blockData1.getAsString(true));
                             result.add(ChatColor.YELLOW+blockData2.getAsString(true));}); break;
        }
        return result;
    }
    
    public void clearBlockSelection(BlockSelectionMode mode) {
        switch(mode) {
            case COUNT:
                counts.clear();
                break;
            case REPLACE:
                replaces.clear();
                break;
            case SWITCH:
                switches.clear();
                break;
        }
    }
    
    public void addReplace(BlockData data1, BlockData data2) {
        replaces.put(data1, data2);
    }
    
    public void addSwitch(BlockData data1, BlockData data2) {
        switches.put(data1, data2);
    }
    
    public void addCount(BlockData data) {
        counts.add(data);
    }
    
    public void loadBlockSelections(File file) {
        try(Scanner reader = new Scanner(file)) {
            reader.nextLine();
            while(reader.hasNext()) {
                String line = reader.nextLine();
                if(line.startsWith("-")) {
                    break;
                }
                if(line.startsWith("minecraft:")) {
                    BlockData data = Bukkit.createBlockData(line);
                    counts.add(data);
                }
            }
            while(reader.hasNext()) {
                String line = reader.nextLine();
                if(line.startsWith("-")) {
                    break;
                }
                if(line.startsWith("minecraft:")) {
                    BlockData data1 = Bukkit.createBlockData(line);
                    BlockData data2 = Bukkit.createBlockData(reader.nextLine().substring(2));
                    replaces.put(data1,data2);
                } else if(line.startsWith("mcme:")) {
                    BlockData data1 = ItemBlockData.createItemBlockData(line);
                    BlockData data2 = Bukkit.createBlockData(reader.nextLine().substring(2));
                    replaces.put(data1,data2);
                }
            }
            while(reader.hasNext()) {
                String line = reader.nextLine();
                if(line.startsWith("-")) {
                    break;
                }
                if(line.startsWith("minecraft:")) {
                    BlockData data1 = Bukkit.createBlockData(line);
                    BlockData data2 = Bukkit.createBlockData(reader.nextLine().substring(2));
                    switches.put(data1,data2);
                }
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(EditPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void saveBlockSelections(File file) {
        try(PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("-"+BlockSelectionMode.COUNT);
            counts.forEach(data -> writer.println(data.getAsString(true)));
            writer.println("-"+BlockSelectionMode.REPLACE);
            replaces.forEach((key, value) -> {writer.println(key.getAsString(true));
                                              writer.println("> "+value.getAsString(true));});
            writer.println("-"+BlockSelectionMode.SWITCH);
            switches.forEach((key, value) -> {writer.println(key.getAsString(true));
                                              writer.println("<> "+value.getAsString(true));});
        } catch (IOException ex) {
            Logger.getLogger(EditPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
 
    public boolean hasItemBlocksSelected(BlockSelectionMode... modes) {
        for(BlockSelectionMode mode: modes) {
            switch(mode) {
                case REPLACE:
                    if(checkForItemBlocks(replaces)) return true;
                    break;
                case COUNT:
                    if(counts.stream().anyMatch((entry) -> (entry instanceof ItemBlockData))) return true;
                    break;
                case SWITCH:
                    if(checkForItemBlocks(switches)) return true;
                    break;
            }
        }
        return false;
    }
    
    private boolean checkForItemBlocks(Map<BlockData,BlockData> selections) {
        return selections.entrySet().stream().anyMatch((entry) -> (entry.getKey() instanceof ItemBlockData || entry.getValue() instanceof ItemBlockData));
    }
    
    public static enum BlockSelectionMode {
        REPLACE, COUNT, SWITCH;
    }
    
    /*public ChatColor infoColor() {
        return ChatColor.WHITE;
    }
    
    public ChatColor stressedColor() {
        return ChatColor.WHITE;
    }*/
    
}
