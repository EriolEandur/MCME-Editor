package com.mcmiddleearth.mcme.editor.data.block;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;

import java.util.Objects;

public class SimpleBlockData implements EditBlockData {

    private final BlockData blockData;

    private final double probability;

    public static SimpleBlockData createBlockData(String data) {
        String[] dataList = data.split("::");
        BlockData blockData = Bukkit.createBlockData(dataList[0]);
        double probability = (dataList.length>1?Double.parseDouble(dataList[1])/100:1);
        return new SimpleBlockData(blockData, probability);
    }

    public SimpleBlockData(BlockData data) {
        this(data, 1);
    }

    public SimpleBlockData(BlockData data, double probability) {
        this.blockData = data;
        this.probability = probability;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleBlockData that = (SimpleBlockData) o;
        return Objects.equals(blockData, that.blockData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockData);
    }

    @Override
    public BlockData getBlockData() {
        return blockData;
    }

    @Override
    public String getAsString(boolean hideUnspecified) {
        return blockData.getAsString(hideUnspecified);
    }

    @Override
    public String getAsString() {
        return blockData.getAsString();
    }

    @Override
    public EditBlockData clone() {
        return new SimpleBlockData(blockData.clone(),probability);
    }

    @Override
    public double getProbability() {
        return probability;
    }
}
