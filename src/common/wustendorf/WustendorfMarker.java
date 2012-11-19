package wustendorf;

import net.minecraft.src.*;

public class WustendorfMarker extends Block {
    public WustendorfMarker(int block_id) {
        super(block_id, Material.cloth);
        setHardness(0.8F);
        setStepSound(soundClothFootstep);
        setBlockName("wustendorf_marker");
        this.setCreativeTab(CreativeTabs.tabDecorations);
    }

    public int getRenderType() {
        return FlagRenderer.ID;
    }

    public String getTextureFile() {
        return "/fm_wustendorf.png";
    }

    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    public boolean isBlockNormalCube(World world, int x, int y, int z) {
        return false;
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean renderAsNormalBlock() {
        return false;
    }

    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving agent) {
        if (!(world instanceof WorldServer)) {
            return;
        }

        WustendorfDB worldDB = Wustendorf.getWorldDB((WorldServer) world);
        worldDB.addMarker(x, y, z);

        House house = House.detectHouse(world, x, y, z);

        System.out.println("House with " + house.rooms.size() + " rooms detected:");

        int room_index = 0;
        for (House.Room room : house.rooms) {
            System.out.println("  Room " + room_index + " has " + room.blocks.size()
                               + " blocks and " + room.connections.size() + " connections:");

            //for (House.Location block : room.blocks.keySet()) {
            //    System.out.println("    B(" + block.x + "," + block.y + ","
            //                       + block.z + ")");
            //}

            for (House.Location connection : room.connections.keySet()) {
                System.out.println("    C(" + connection.x + "," + connection.y + ","
                                   + connection.z + ")");
            }
            room_index++;
        }
    }

    public void dropBlockAsItemWithChance(World world, int x, int y, int z, int meta, float chance, int fortune) {
        super.dropBlockAsItemWithChance(world, x, y, z, meta, chance, fortune);

        if (!(world instanceof WorldServer)) {
            return;
        }

        WustendorfDB worldDB = Wustendorf.getWorldDB((WorldServer) world);
    }

    public static void randomTick(WorldServer world, int x, int y, int z) {
        WustendorfDB worldDB = Wustendorf.instance.getWorldDB(world);

        if (worldDB.getTag("light", x, y, z) == -1) {
            return;
        }


        int range = worldDB.getRange(x, y, z);

        int dx = Wustendorf.randint(-range, range);
        int new_y = Wustendorf.randint(0, 255);
        int dz = Wustendorf.randint(-range, range);

        Wustendorf.instance.updateLightAt(world, x+dx, new_y, z+dz);
        worldDB.removeMarker(x, y, z);
    }
}
