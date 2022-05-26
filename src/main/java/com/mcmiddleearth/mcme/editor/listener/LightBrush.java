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
package com.mcmiddleearth.mcme.editor.listener;

import com.mcmiddleearth.mcme.editor.EditorPlugin;
import com.mcmiddleearth.mcme.editor.Permissions;
import com.mcmiddleearth.mcme.editor.command.sender.EditPlayer;
import com.mcmiddleearth.mcme.editor.job.JobManager;
import com.mcmiddleearth.mcme.editor.job.JobType;
import com.mcmiddleearth.pluginutil.WEUtil;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;

/**
 *
 * @author Eriol_Eandur
 */
public class LightBrush implements Listener {
    
    int radius = 32;
    
    boolean cooldown = false;
    
    @EventHandler
    public void onBrush(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if(!cooldown 
                && event.getHand() != null
                && event.getHand().equals(EquipmentSlot.HAND)
                && (event.getAction().equals(Action.RIGHT_CLICK_AIR)
                    || event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
                && event.hasItem()
                && event.getItem().getType().equals(Material.PRISMARINE_CRYSTALS)
                && player.hasPermission(Permissions.LIGHT_BRUSH.getPermissionNode())) {
            Block block = player.getTargetBlockExact(160);
            Region region = new CuboidRegion(new BukkitWorld(player.getWorld()),BlockVector3.at(block.getX()-radius, 
                                                                              block.getY()-radius, 
                                                                              block.getZ()-radius),
                                                              BlockVector3.at(block.getX()+radius, 
                                                                              block.getY()+radius, 
                                                                              block.getZ()+radius));
            Set<String> worlds = new HashSet<>();
            Set<String> rps = new HashSet<>();
            worlds.add(player.getWorld().getName());
            JobManager.enqueueBlockJob(EditPlayer.wrap(player),region,true,worlds,
                                       rps, JobType.LIGHT, false, true);
            cooldown = true;
            new BukkitRunnable() {
                @Override
                public void run() {
                    cooldown = false;
                }
            }.runTaskLater(EditorPlugin.getInstance(), 20);
        }
    }
}
