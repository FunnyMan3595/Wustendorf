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
}
