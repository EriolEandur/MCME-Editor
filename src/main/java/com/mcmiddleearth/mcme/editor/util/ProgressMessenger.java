/*
 * Copyright (C) 2020 MCME
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
package com.mcmiddleearth.mcme.editor.util;

import com.mcmiddleearth.mcme.editor.EditorPlugin;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;

/**
 *
 * @author Eriol_Eandur
 */
public class ProgressMessenger {

    private int counter = 0;

    private long interval;

    private long lastMessage;

    private CommandSender receiver; 

    private String message;

    public ProgressMessenger(CommandSender receiver, int interval, String message) {
        this.interval = interval*(long)1e3;
        this.receiver = receiver;
        lastMessage = System.currentTimeMillis();
        this.message = message;
    }

    public void step() {
        counter++;
        long time = System.currentTimeMillis();
        if(time>lastMessage+interval) {
            EditorPlugin.getMessageUtil().scheduleInfoMessage(receiver, message.replace("%1", ""+counter));
            lastMessage = time;
        }
    }
    
    public static String formatProgressMessage(String message) {
        return message.replace("_n_", infoColor()).replace("_h_", stressedColor());
    }
    
    public static String infoColor() {
        return ""+EditorPlugin.getMessageUtil().INFO;
    }
    public static String stressedColor() {
        return ""+EditorPlugin.getMessageUtil().STRESSED;
    }
}

