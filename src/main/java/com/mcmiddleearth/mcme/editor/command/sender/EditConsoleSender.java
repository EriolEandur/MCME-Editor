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

import com.mcmiddleearth.mcme.editor.Permissions;
import java.util.Objects;
import lombok.Getter;
import org.bukkit.command.ConsoleCommandSender;

/**
 *
 * @author Eriol_Eandur
 */
public class EditConsoleSender extends EditCommandSender{
    
    @Getter
    private ConsoleCommandSender sender;
    
    public EditConsoleSender(ConsoleCommandSender sender) {
        this.sender = sender;
    }

    @Override
    public boolean isOnline() {
        return true;
    }
    
    @Override
    public boolean hasPermissions(Permissions... permissions) {
        return true;
    }
    
    @Override
    public boolean equals(Object other) {
        return other instanceof EditConsoleSender;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.sender);
        return hash;
    }


}
