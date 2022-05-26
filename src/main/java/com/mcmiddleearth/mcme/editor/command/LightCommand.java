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

import com.mcmiddleearth.mcme.editor.Permissions;
import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.command.sender.EditPlayer;
import com.mcmiddleearth.mcme.editor.job.JobType;
import com.mcmiddleearth.pluginutil.NMSUtil;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;
import com.mojang.brigadier.context.CommandContext;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Server;

/**
 *
 * @author Eriol_Eandur
 */
public class LightCommand extends AbstractEditorCommand{

    @Override
    public LiteralArgumentBuilder getCommandTree() {
        LiteralArgumentBuilder builder = literal("fixlight")
            .requires(s -> ((EditCommandSender)s).hasPermissions(Permissions.FIX_LIGHT))
            .executes(c -> {return startJob(c, JobType.LIGHT);})
            .then(argument("<options>", greedyString())
                .suggests((c,optionBuilder) -> { return addJobOptionSuggestions(optionBuilder).buildFuture();})
                .executes(c -> {return startJob(c, JobType.LIGHT);}));
        return builder;
    }
    
    @Override
    public int startJob(CommandContext c, JobType type) {
        return super.startJob(c,type);
/*Logger.getGlobal().info("calclight");
        Location loc = ((EditPlayer)c.getSource()).getPlayer().getLocation();
        Object blockPosition = NMSUtil.createNMSObject("BlockPosition", new Class[]{int.class,int.class,int.class}, 
                                                       loc.getBlockX(),loc.getBlockY(), loc.getBlockZ());
Logger.getGlobal().info("blocPosition "+blockPosition);
        Chunk chunk = loc.getChunk();
        Object nmsChunk = NMSUtil.invokeCraftBukkit("CraftChunk", "getHandle", new Class[0], chunk);
Logger.getGlobal().info("nmsChunk"+nmsChunk);
        Object lightEngine = NMSUtil.invokeNMS("Chunk", "e", new Class[0], nmsChunk);
Logger.getGlobal().info("sync "+Bukkit.isPrimaryThread());
Logger.getGlobal().info("lightEngine "+lightEngine);
        boolean flag = true;
        Logger.getGlobal().info("getLight: "+NMSUtil.invokeNMS("LightEngine", "b", new Class[]{blockPosition.getClass(),int.class}, lightEngine, blockPosition, 0));
        NMSUtil.invokeNMS("LightEngine", "a", new Class[]{blockPosition.getClass()}, lightEngine, blockPosition);
        //NMSUtil.invokeNMS("LightEngine", "a", new Class[]{blockPosition.getClass()}, lightEngine, blockPosition, 15);
        //chunk.getWorld().refreshChunk(chunk.getX(), chunk.getZ());
        Object nmsWorldServer = NMSUtil.invokeCraftBukkit("CraftWorld", "getHandle", new Class[0], chunk.getWorld());
Logger.getGlobal().info("worldServer "+nmsWorldServer);
        Object nmsChunkProviderServer = NMSUtil.invokeNMS("WorldServer", "getChunkProvider", new Class[0], nmsWorldServer);
Logger.getGlobal().info("chunkProvider "+nmsChunkProviderServer);
        NMSUtil.invokeNMS("ChunkProviderServer", "flagDirty", new Class[]{blockPosition.getClass()}, nmsChunkProviderServer, blockPosition);
        return 0;*/
    }

}
