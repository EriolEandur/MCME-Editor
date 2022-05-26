package com.mcmiddleearth.mcme.editor.data.chunk;

import com.mcmiddleearth.architect.copyPaste.Clipboard;
import com.mcmiddleearth.architect.copyPaste.CopyPasteException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.World;

public class ChunkYShiftEditData extends ChunkEditData {


    public ChunkYShiftEditData(int chunkX, int chunkZ) {
        super(chunkX, chunkZ);
    }

    @Override
    public void applyEdit(World world, boolean refreshChunks) {
        try {
            world.getChunkAt(getChunkX(),getChunkZ()).getEntities();
            Clipboard clipboard = new Clipboard(new Location(world, getChunkX() * 16, 0, getChunkZ() * 16),
                    new CuboidRegion(new BukkitWorld(world),
                            BlockVector3.at(getChunkX() * 16, 0, getChunkZ() * 16),
                            BlockVector3.at(getChunkX() * 16 + 15, 255, getChunkZ() * 16 + 15)));
            if (clipboard.cutToClipboard()) {
                clipboard.paste(clipboard.getReferencePoint().clone().subtract(0, 64, 0), true, true);
            }
        } catch (CopyPasteException e) {
            e.printStackTrace();
        }
    }
}

        /*IStoragePlot storagePlot = new IStoragePlot() {
            @Override
            public World getWorld() {
                return world;
            }
            @Override
            public Location getLowCorner() {
                return new Location(world, getChunkX()*16,0,getChunkZ()*16);
            }
            @Override
            public Location getHighCorner() {
                return new Location(world, getChunkX()*16+15,255,getChunkZ()*16+15);
            }
            @Override
            public boolean isInside(Location location) {
                return true;
            }
        };
        StoragePlotSnapshot snapshot = new StoragePlotSnapshot(storagePlot);
        byte[] nbtData = null;
        try(ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            DataOutputStream outStream = new DataOutputStream(
                    new BufferedOutputStream(
                            new GZIPOutputStream(
                                    byteOut)))) {
            new MCMEPlotFormat().save(storagePlot, outStream, snapshot);
            outStream.flush();
            outStream.close();
            nbtData = byteOut.toByteArray();
        } catch (IOException ex) {
            Logger.getLogger(Clipboard.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        Collection<Entity> entities = lowCorner.getWorld()
                .getNearbyEntities(new BoundingBox(lowCorner.getBlockX(),
                                lowCorner.getBlockY(),
                                lowCorner.getBlockZ(),
                                highCorner.getBlockX()+1,
                                highCorner.getBlockY()+1,
                                highCorner.getBlockZ()+1),
                        new MCMEEntityFilter());
        entities.forEach((entity) -> {
            entity.remove();
        });
        BlockData air = Bukkit.createBlockData(Material.AIR);
        World world = lowCorner.getWorld();
        for(int i = lowCorner.getBlockX(); i <= highCorner.getBlockX();i++) {
            for(int j = lowCorner.getBlockY(); j <= highCorner.getBlockY();j++) {
                for(int k = lowCorner.getBlockZ(); k <= highCorner.getBlockZ();k++) {
                    world.getBlockAt(i,j,k).setBlockData(air,false);
                }
            }
        }

        try(DataInputStream in = new DataInputStream(
                new BufferedInputStream(
                        new GZIPInputStream(
                                new ByteArrayInputStream(nbtData))))) {
            new MCMEPlotFormat().load(storagePlot.getLowCorner().add(0,-60,0), 0,
                                      new boolean[]{false,false,false}, true, true, null, in);
        } catch (IOException | InvalidRestoreDataException ex) {
            Logger.getLogger(Clipboard.class.getName()).log(Level.SEVERE, null, ex);
        }
        for(int x = 0; x < 16; x++) {
            for(int z = 0; z < 16; z++) {
                for(int y = 0; y < 327; y++) {
                    world.getChunkAt(getChunkX(),getChunkZ()).getBlock(x,y,z).setBlockData(Bukkit.createBlockData(Material.AIR));
                }
            }
        }
    }
}*/
