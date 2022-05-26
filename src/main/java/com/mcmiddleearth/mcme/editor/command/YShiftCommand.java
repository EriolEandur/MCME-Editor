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
import com.mcmiddleearth.mcme.editor.job.JobType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.builder.LiteralArgumentBuilder.literal;
import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

/**
 *
 * @author Eriol_Eandur
 */
public class YShiftCommand extends AbstractEditorCommand{

    @Override
    public LiteralArgumentBuilder getCommandTree() {
        LiteralArgumentBuilder builder = literal("yshift")
            .requires(s -> ((EditCommandSender)s).hasPermissions(Permissions.Y_SHIFT))
            .executes(c -> {return startJob(c, JobType.Y_SHIFT);});
        return builder;
    }
    
    @Override
    public int startJob(CommandContext c, JobType type) {
        return super.startJob(c,type);
    }

}
