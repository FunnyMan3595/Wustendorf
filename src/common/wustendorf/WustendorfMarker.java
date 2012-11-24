package wustendorf;

import java.util.*;
import net.minecraft.src.*;

public class WustendorfMarker extends Block {
    public static WustendorfMarker instance=null;
    public WustendorfMarker(int block_id) {
        super(block_id, Material.cloth);
        setHardness(0.8F);
        setStepSound(soundClothFootstep);
        setBlockName("wustendorf_marker");
        this.setCreativeTab(CreativeTabs.tabDecorations);

        instance = this;
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

    public static void popFlag(World world, int x, int y, int z) {
        instance.dropBlockAsItemWithChance(world, x, y, z, 0, 1.0F, 0);

        if (world.setBlockAndMetadata(x, y, z, 0, 0))
        {
            world.notifyBlocksOfNeighborChange(x, y, z, 0);
        }
    }

    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLiving agent) {
        if (!(world instanceof WorldServer)) {
            return;
        }

        WustendorfDB worldDB = Wustendorf.getWorldDB((WorldServer) world);

        House house = House.detectHouse(world, x, y, z);

        if (!house.valid) {
            popFlag(world, x, y, z);
            System.out.println("Invalid house.");
            return;
        }

        int id = worldDB.addMarker(x, y, z);

        if (id == -1) {
            System.out.println("Unable to insert marker.  Aborting.");
            popFlag(world, x, y, z);
            return;
        }

        System.out.println("House with " + house.rooms.size() + " rooms detected:");

        int room_index = 0;
        for (House.Room room : house.rooms) {
            int[] otherRoom = worldDB.roomAt(room.origin.x, room.origin.y, room.origin.z);
            if (otherRoom.length > 0 && otherRoom[0] != id) {
                System.out.println("Encountered existing house at (" + room.origin.x + "," + room.origin.y + "," + room.origin.z + ")");
                popFlag(world, x, y, z);
                return;
            }

            int x_min = room.origin.x;
            int x_max = room.origin.x;
            int y_min = room.origin.y;
            int y_max = room.origin.y;
            int z_min = room.origin.z;
            int z_max = room.origin.z;
            int important_blocks = 0;
            for (Map.Entry<House.Location, House.BlockType> entry : room.blocks.entrySet()) {
                House.Location  loc  = entry.getKey();
                House.BlockType type = entry.getValue();
                if (type.isInteresting()) {
                    worldDB.addImportantBlock(id, room_index, loc.x, loc.y, loc.z, type.flags);
                    important_blocks += 1;
                }

                if (loc.x < x_min) {
                    x_min = loc.x;
                }

                if (loc.x > x_max) {
                    x_max = loc.x;
                }

                if (loc.y < y_min) {
                    y_min = loc.y;
                }

                if (loc.y > y_max) {
                    y_max = loc.y;
                }

                if (loc.z < z_min) {
                    z_min = loc.z;
                }

                if (loc.z > z_max) {
                    z_max = loc.z;
                }
            }
            worldDB.addRoom(id, room_index, x_min, x_max, y_min, y_max, z_min, z_max, room.origin.x, room.origin.y, room.origin.z);

            System.out.println(String.format("Room %d contains %d important blocks in (%d,%d,%d)-(%d,%d,%d)", room_index, important_blocks, x_min, y_min, z_min, x_max, y_max, z_max));

            //System.out.println("  Room " + room_index + " has " + room.blocks.size()
            //                   + " blocks and " + room.connections.size() + " connections:");

            //for (House.Location block : room.blocks.keySet()) {
            //    System.out.println("    B(" + block.x + "," + block.y + ","
            //                       + block.z + ")");
            //}

            //for (House.Location connection : room.connections.keySet()) {
            //    System.out.println("    C(" + connection.x + "," + connection.y + ","
            //                       + connection.z + ")");
            //}
            room_index++;
        }
    }

     public void breakBlock(World world, int x, int y, int z, int id, int meta) {
        super.breakBlock(world, x, y, z, id, meta);

        if (!(world instanceof WorldServer)) {
            return;
        }

        WustendorfDB worldDB = Wustendorf.getWorldDB((WorldServer) world);
        worldDB.removeMarker(x, y, z);
    }

    public static void tick(WorldServer world, int x, int y, int z) {
        WustendorfDB worldDB = Wustendorf.instance.getWorldDB(world);

        int food = worldDB.getTag("food_level", x, y, z);

        if (food < 0) {
            food = 0;
        }

        int food_use = worldDB.getTag("food_use", x, y, z);

        if (food_use <= 0) {
            return;
        }

        food -= food_use;

        if (food < 0) {
            int food_multiplier = worldDB.getTag("food_multiplier", x, y, z);
            if (food_multiplier <= 0) {
                food_multiplier = 6;
            }

            for (List<Integer> tile : worldDB.getImportantBlocks(x, y, z)) {
                TileEntity te = world.getBlockTileEntity(tile.get(0), tile.get(1), tile.get(2));

                if (te instanceof IInventory) {
                    IInventory inv = (IInventory) te;

                    for (int i=0; i<inv.getSizeInventory(); i++) {
                        ItemStack stack = inv.getStackInSlot(i);

                        if (stack == null || stack.stackSize <= 0) {
                            continue;
                        }

                        Item item = stack.getItem();
                        if (item instanceof ItemFood) {
                            ItemFood fooditem = (ItemFood) item;

                            int food_value = fooditem.getHealAmount() * food_multiplier;

                            int decr = 0;
                            while (food < 0 && stack.stackSize > decr) {
                                decr++;
                                food += food_value;
                            }

                            inv.decrStackSize(i, decr);

                            if (food >= 0) {
                                break;
                            }
                        }
                    }

                    if (food >= 0) {
                        break;
                    }
                }
            }
        }

        if (food >= 0) {
            worldDB.setTag(food, "food_level", x, y, z);
        } else {
            popFlag(world, x, y, z);
        }
    }
}
