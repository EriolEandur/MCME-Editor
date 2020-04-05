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

import com.mcmiddleearth.mcme.editor.EditorPlugin;
import com.mcmiddleearth.mcme.editor.clipboard.Clipboard;
import com.mcmiddleearth.mcme.editor.clipboard.UndoData;
import com.mcmiddleearth.pluginutil.message.FancyMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Region and block selections and jobs of a player.
 * @author Eriol_Eandur
 */
public class EditPlayer extends EditCommandSender{
    
    @Getter @Setter
    private Clipboard clipboard;

    @Getter
    private final List<UndoData> undoData = new ArrayList<>();
    
    @Getter
    private final List<UndoData> redoData = new ArrayList<>();
    
    @Getter @Setter
    private BlockData incompleteBlockSelection = null;
    
    @Getter
    private final UUID uuid;
    
    public EditPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public CommandSender getSender() {
        return getPlayer();
    }
    
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
    
    public OfflinePlayer getOfflinePlayer() {
        return Bukkit.getOfflinePlayer(uuid);
    }
    
    public boolean hasClipboard() {
        return clipboard != null;
    }
    
    public boolean hasUndos() {
        return !undoData.isEmpty();
    }
    
    public boolean hasRedos() {
        return !redoData.isEmpty();
    }
    
    @Override
    public boolean isOnline() {
        return Bukkit.getOfflinePlayer(uuid).isOnline();
    }
    
    public void info(String message, String onClick, String onHover) {
        if(isOnline()){
            FancyMessage fancy = new FancyMessage(EditorPlugin.getMessageUtil());
            if(onClick==null && onHover==null) {
                fancy.addSimple(message);
            } else if (onClick==null) {
                fancy.addTooltipped(message, onHover);
            } else if (onHover==null) {
                fancy.addClickable(message, onClick);
            } else {
                fancy.addFancy(message, onClick, onHover);
            }
            fancy.send(Bukkit.getPlayer(uuid));
        }
    }
    
    /*@Override
    public ChatColor infoColor() {
        return EditorPlugin.getMessageUtil().INFO;
    }
    
    @Override
    public ChatColor stressedColor() {
        return EditorPlugin.getMessageUtil().STRESSED;
    }*/
    
    @Override
    public boolean equals(Object other) {
        return other instanceof EditPlayer 
                && uuid.equals(((EditPlayer)other).getUuid());
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.uuid);
        return hash;
    }


}
