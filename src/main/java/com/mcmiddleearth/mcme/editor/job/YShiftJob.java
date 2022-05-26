package com.mcmiddleearth.mcme.editor.job;

import com.mcmiddleearth.mcme.editor.command.sender.EditCommandSender;
import com.mcmiddleearth.mcme.editor.data.EditChunkSnapshot;
import com.mcmiddleearth.mcme.editor.data.chunk.ChunkEditData;
import com.mcmiddleearth.mcme.editor.data.chunk.ChunkYShiftEditData;
import com.sk89q.worldedit.regions.Region;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;

public class YShiftJob extends AbstractJob {

    public YShiftJob(EditCommandSender owner, int id, YamlConfiguration config) {
        super(owner, id, config);
    }

    public YShiftJob(EditCommandSender owner, int id, World world, Region extraRegion, List<Region> regions, int size) {
        super(owner, id, world, extraRegion, regions, size, true, false);
    }

    @Override
    public JobType getType() {
        return JobType.Y_SHIFT;
    }

    @Override
    public ChunkEditData handle(EditChunkSnapshot chunk) {
        return new ChunkYShiftEditData(chunk.getChunkSnapshot().getX(), chunk.getChunkSnapshot().getZ());
    }

    @Override
    public String getResultMessage() {
        return "Done!";
    }

    @Override
    public void saveResultsToFile() {

    }

    @Override
    public void saveLogsToFile() {

    }
}
