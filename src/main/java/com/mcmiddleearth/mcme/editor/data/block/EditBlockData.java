package com.mcmiddleearth.mcme.editor.data.block;

import org.bukkit.block.data.BlockData;

public interface EditBlockData {

    public BlockData getBlockData();

    public String getAsString(boolean hideUnspecified);
    public String getAsString();

    public EditBlockData clone();

    public double getProbability();

}
