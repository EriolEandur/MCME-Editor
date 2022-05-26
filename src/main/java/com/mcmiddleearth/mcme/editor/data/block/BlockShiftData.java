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
package com.mcmiddleearth.mcme.editor.data.block;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

import java.util.Objects;

/**
 *
 * @author Eriol_Eandur
 */
public class BlockShiftData implements EditBlockData {

    private final BlockData blockData;

    private final BlockData ambientBlockData;

    private final BlockFace direction;

    private final int shift;

    private final  double probability;

    public static final String NAMESPACE = "mcme_shift";

    public static BlockShiftData createEditBlockData(BlockData place, String data) {
        String[] firstSplit = data.split("::");
        BlockData ambient = Bukkit.createBlockData(firstSplit[1]);
        String[] BlockShiftData = firstSplit[0].split("[:=\\[,\\]]");
        BlockFace direction = BlockFace.valueOf(BlockShiftData[3].toUpperCase());
        int shift = Integer.parseInt(BlockShiftData[5]);
        double probability = (firstSplit.length>2?Double.parseDouble(firstSplit[2])/100:1);
        return new BlockShiftData(place, ambient, direction, shift, probability);
    }

    public BlockShiftData(BlockData blockData, BlockData ambient, BlockFace direction, int shift) {
        this(blockData,ambient,direction,shift,1);
    }

    public BlockShiftData(BlockData blockData, BlockData ambient, BlockFace direction, int shift, double probability) {
        this.blockData = blockData;
        this.ambientBlockData = ambient;
        this.shift = shift;
        this.direction = direction;
        this.probability = probability;
    }

    @Override
    public String getAsString() {
        return NAMESPACE+":[direction:"+direction.name().toLowerCase()+",shift:"+shift+"]::"+ambientBlockData.getAsString(false);
    }

    @Override
    public String getAsString(boolean bln) {
        return getAsString();
    }

    @Override
    public double getProbability() {
        return probability;
    }

    @Override
    public EditBlockData clone() {
        return new BlockShiftData(blockData.clone(),ambientBlockData.clone(),direction,shift,probability);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockShiftData that = (BlockShiftData) o;
        return shift == that.shift &&
                Objects.equals(blockData, that.blockData) &&
                Objects.equals(ambientBlockData, that.ambientBlockData) &&
                direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(blockData, ambientBlockData, direction, shift);
    }

    public BlockData getBlockData() {
        return blockData;
    }

    public BlockData getAmbient() {
        return ambientBlockData;
    }

    public BlockFace getDirection() {
        return direction;
    }

    public int getShift() {
        return shift;
    }
}
