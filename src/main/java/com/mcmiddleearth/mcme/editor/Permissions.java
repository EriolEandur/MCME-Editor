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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;

/**
 *
 * @author Eriol_Eandur
 */
public enum Permissions {
    
    BLOCK               ("mcmeeditor.command.block",            PermissionDefault.OP),
    BLOCK_FILES         ("mcmeeditor.command.block.files",      PermissionDefault.OP),
    BLOCK_COUNT         ("mcmeeditor.command.block.count",      PermissionDefault.OP),
    BLOCK_REPLACE       ("mcmeeditor.command.block.replace",    PermissionDefault.OP),
    BLOCK_PLACE_ITEMBLOCK ("mcmeeditor.command.block.place.itemblock", PermissionDefault.OP),
    QUEUE_RESTART       ("mcmeeditor.command.queue.restart",    PermissionDefault.OP),
    QUEUE_CONFIG        ("mcmeeditor.command.queue.config",     PermissionDefault.OP),
    QUEUE               ("mcmeeditor.command.queue",            PermissionDefault.OP),
    QUEUE_SUSPEND       ("mcmeeditor.command.queue.suspend",    PermissionDefault.OP),
    QUEUE_CANCEL        ("mcmeeditor.command.queue.cancel",     PermissionDefault.OP),
    QUEUE_DELETE        ("mcmeeditor.command.queue.delete",     PermissionDefault.OP),
    QUEUE_RESUME        ("mcmeeditor.command.queue.resume",     PermissionDefault.OP),
    QUEUE_OTHER         ("mcmeeditor.command.queue.other",      PermissionDefault.OP),
    QUEUE_UPDATES       ("mcmeeditor.command.queue.updates",    PermissionDefault.OP),
    CLIPBOARD_LIMIT     ("mcmeeditor.clipboard.limit",          PermissionDefault.OP),
    CLIPBOARD_UNLIMITED ("mcmeeditor.clipboard.unlimited",      PermissionDefault.OP),
    CLIPBOARD_COPY      ("mcmeeditor.command.copy",             PermissionDefault.OP),
    CLIPBOARD_CUT       ("mcmeeditor.command.cut",              PermissionDefault.OP),
    CLIPBOARD_FLIP      ("mcmeeditor.command.flip",             PermissionDefault.OP),
    CLIPBOARD_ROTATE    ("mcmeeditor.command.rotate",           PermissionDefault.OP),
    CLIPBOARD_REDO      ("mcmeeditor.command.redo",             PermissionDefault.OP),
    CLIPBOARD_UNDO      ("mcmeeditor.command.undo",             PermissionDefault.OP),
    CLIPBOARD_PASTE     ("mcmeeditor.command.paste",            PermissionDefault.OP);
    
    
    @Getter
    private final String permissionNode;
    
    @Getter
    private final Permissions[] children;
    
    @Getter
    private final PermissionDefault defaultPerm;

    private Permissions(String permissionNode, PermissionDefault defaultPerm, Permissions... children) {
        this.permissionNode = permissionNode;
        this.children = children;
        this.defaultPerm = defaultPerm;
    }
    
    public Permissions[] getWithChildren() {
        Permissions[] result = Arrays.copyOf(children, children.length+1);
        result[children.length] = this;
        return result;
    }
    
    public static void register() {
        for(Permissions editorPermission: Permissions.values()) {
            Map<String, Boolean> children = new HashMap<>();
            for(Permissions child: editorPermission.getChildren()) {
                children.put(child.getPermissionNode(), Boolean.TRUE);
            }
            Permission bukkitPerm = new Permission(editorPermission.getPermissionNode(), 
                                                   editorPermission.getDefaultPerm(),
                                                   children);
            Bukkit.getServer().getPluginManager().addPermission(bukkitPerm);
//Logger.getGlobal().info("register: "+bukkitPerm.getName());
        }
    }
}
