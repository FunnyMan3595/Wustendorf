package wustendorf;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.ITinyPacketHandler;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.PacketDispatcher;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import ic2.api.Ic2Recipes;
import ic2.api.Items;
import ic2.common.ContainerElectricMachine;
import ic2.common.Ic2Items;
import ic2.common.IHasGui;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.src.Block;
import net.minecraft.src.DamageSource;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityDragon;
import net.minecraft.src.EntityFlying;
import net.minecraft.src.EntityLiving;
import net.minecraft.src.EntityLightningBolt;
import net.minecraft.src.EntityMob;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.EntitySlime;
import net.minecraft.src.EnumSkyBlock;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MathHelper;
import net.minecraft.src.ModLoader;
import net.minecraft.src.NBTBase;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NetHandler;
import net.minecraft.src.NetServerHandler;
import net.minecraft.src.Packet131MapData;
import net.minecraft.src.Packet1Login;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraft.src.WorldServer;

@Mod(
    modid="Wustendorf",
    name="Wustendorf",
    version="%conf:VERSION%",
    dependencies=""
)
@NetworkMod(
    clientSideRequired=true,
    serverSideRequired=false,
    tinyPacketHandler=Wustendorf.PacketHandler.class,
    versionBounds="%conf:VERSION_BOUNDS%"
)
public class Wustendorf implements ITickHandler {

    public Random random = new Random();
    private Connection masterDB = null;
    private Map<WorldServer, WustendorfDB> worldDBs = new HashMap<WorldServer, WustendorfDB>();
    private Map<WorldServer, Boolean> activeInWorld = new HashMap<WorldServer, Boolean>();

    public static boolean ranUpdate = false;
    public static final boolean DEBUG = false;
    public static Configuration config;
    public static Wustendorf instance = null;
    public static Set<World> lightUpdated = new HashSet<World>();

    @SidedProxy(clientSide = "wustendorf.ClientProxy", serverSide = "wustendorf.CommonProxy")
    public static CommonProxy proxy;

    public static int real_mod(int number, int modulus) {
        int mod = number % modulus;
        if (mod < 0) {
            // Java is a fucking idiot.
            mod += modulus;
        }

        return mod;
    }

    @Mod.PreInit
    public void preInit(FMLPreInitializationEvent event) {
        instance = this;

        // Load config file.
        config = new Configuration(event.getSuggestedConfigurationFile());

        File dbFile = new File(event.getModConfigurationDirectory(),
                               "wustendorf");

        try {
            Class.forName("org.h2.Driver");
            masterDB = DriverManager.getConnection("jdbc:h2:" +
                                                   dbFile.getCanonicalPath());

        } catch (Exception e) {
            System.out.println("Wustendorf: Unable to initialize database.");
        }
    }

    @Mod.Init
    public void init(FMLInitializationEvent event) {
        Side side = getSide();

        try {
            config.load();
        } catch (RuntimeException e) {} // Just regenerate the config if it's
                                        // broken.
        String id_strs[] = {
            config.get("wustendorf.marker", config.CATEGORY_BLOCK, 195).value,
        };
        config.save();

        int ids[] = new int[id_strs.length];
        try {
            for (int i=0; i<id_strs.length; i++) {
                ids[i] = Integer.parseInt(id_strs[i]);
            }
        } catch (NumberFormatException e) {
        }

        // Register with ModLoader.
        WustendorfMarker marker = new WustendorfMarker(ids[0]);
        ItemStack one_marker = new ItemStack(marker, 1);
        GameRegistry.registerBlock(marker);
        //GameRegistry.registerBlock(marker, MarkerItem.class);
        //GameRegistry.registerTileEntity(MarkerTileEntity.class, "House Flag");

        proxy.init(marker);

        // Add crafting recipes
        Ic2Recipes.addCraftingRecipe(one_marker, new Object[] {
            "|# ", "|##", "|  ",
            Character.valueOf('|'), Item.stick,
            Character.valueOf('#'), Block.cloth,
        });

        // Register the GUI handler.
        //NetworkRegistry.instance().registerGuiHandler(this, new WustendorfGuiHandler());

        //TickRegistry.registerTickHandler(this, Side.CLIENT);
        TickRegistry.registerTickHandler(this, Side.SERVER);
        //TickRegistry.registerTickHandler(this, Side.BUKKIT);
    }

    public static Side getSide() {
        return FMLCommonHandler.instance().getEffectiveSide();
    }

    public static Wustendorf instance() {
        return instance;
    }

    @Mod.ServerStarting
    public void beforeServerStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandWustendorf());
    }

    @Mod.ServerStarted
    public void onServerStart(FMLServerStartedEvent event) {
        for (WustendorfDB worldDB : worldDBs.values()) {
            worldDB.close();
        }
        worldDBs.clear();
        activeInWorld.clear();
    }

    @Mod.ServerStopping
    public void onServerStopping(FMLServerStoppingEvent event) {
        for (WustendorfDB worldDB : worldDBs.values()) {
            worldDB.close();
        }
        worldDBs.clear();
        activeInWorld.clear();
    }

    public static void updateLightAt(World world, int x, int y, int z) {
        boolean change = false;

        int existing = world.getSavedLightValue(EnumSkyBlock.Block, x, y, z);
        int override = overrideBlockLight(world, x, y, z);

        if (override != -1) {
            if (existing != override) {
                world.setLightValue(EnumSkyBlock.Block, x, y, z, override);
                change = true;
            }
        } else {
            world.updateLightByType(EnumSkyBlock.Block, x, y, z);

            int after = world.getSavedLightValue(EnumSkyBlock.Block, x, y, z);
            if (after != existing) {
                change = true;
            }
        }

        if (change) {
            world.markBlockNeedsUpdate(x, y, z);
        }
    }

    public static boolean inSafeRegion(World world, int x, int z) {
        return false;
    }

    public static boolean inLitRegion(World world, int x, int z) {
        return false;
    }

    public static int overrideBlockLight(World world, int x, int y, int z) {
        if (!(world instanceof WorldServer)) {
            return -1;
        }

        WorldServer ws = (WorldServer) world;

        if (instance.activeInWorld.get(ws) != Boolean.TRUE) {
            return -1;
        }

        WustendorfDB worldDB = getWorldDB(ws);

        return worldDB.getRegionLight(x, z);
    }

    public static int overrideSkyLight(World world, int x, int y, int z) {
        // XXX: There's no hook for this right now.
        //      One would need to be added.
        return -1;
    }

    public static boolean isHostile(EntityLiving mob) {
        if (mob instanceof EntityMob) { // Spider, Zombie, etc.
            return true;
        } else if (mob instanceof EntityDragon) { // Dragon
            return true;
        } else if (mob instanceof EntityFlying) { // Ghast
            return true;
        } else if (mob instanceof EntitySlime) { // Slime, Magma Cube
            return true;
        } else {
            return false;
        }
    }

    public static void considerKillingCritter(EntityLiving mob) {
        if (!isHostile(mob)) {
            return;
        }

        int x = MathHelper.floor_double(mob.posX);
        int z = MathHelper.floor_double(mob.posZ);

        if (mob.isEntityAlive() && inSafeRegion(mob.worldObj, x, z)) {
            zot(mob);
        }
    }

    public static void zot(Entity entity) {
        if (entity.worldObj instanceof WorldServer) {
            if (instance.activeInWorld.get(entity.worldObj) != Boolean.TRUE) {
                return;
            }

            // TODO: Check/charge energy.

            // Mostly-cosmetic lightning bolt.
            WorldServer world = (WorldServer) entity.worldObj;
            world.addWeatherEffect(
                new EntityLightningBolt(world, entity.posX, entity.posY,
                                               entity.posZ));

            // Ensure the monster will take full damage.
            entity.hurtResistantTime = 0;

            // This should one-shot anything but a boss.
            entity.attackEntityFrom(DamageSource.magic, 100);
        }
    }

    public static House.BlockType getTypeOverride(int id, int meta) {
        return null;
    }

    public static int randint(int min, int max) {
        int values = (max - min) + 1;
        return instance.random.nextInt(values) - min;
    }

    public static WustendorfDB getWorldDB(WorldServer world) {
        WustendorfDB worldDB = instance.worldDBs.get(world);
        if (worldDB != null) {
            return worldDB;
        } else {
            worldDB = new WustendorfDB(world);
            instance.worldDBs.put(world, worldDB);
            return worldDB;
        }
    }

    public void tickStart(EnumSet<TickType> type, Object... tickData) {
        if (!(tickData[0] instanceof WorldServer)) {
            return;
        }

        WorldServer world = (WorldServer) tickData[0];
        WustendorfDB worldDB = getWorldDB(world);
        int markerCount = worldDB.getMarkerCount();

        activeInWorld.put(world, (markerCount > 0));

        // Tick updates.
        for (int i=0; i<markerCount; i++) {
            if (random.nextInt(100) == 0) {
                int[] coords = worldDB.getMarker(i);

                if (coords.length == 3) {
                    WustendorfMarker.randomTick(world, coords[0], coords[1], coords[2]);
                }
            }
        }
    }

    public void tickEnd(EnumSet<TickType> type, Object... tickData) { }

    public EnumSet<TickType> ticks() {
        return EnumSet.of(TickType.WORLD);
    }

    public String getLabel() {
        return "Wustendorf.lighting";
    }

    public static class PacketHandler implements ITinyPacketHandler {
        public void handle(NetHandler handler, Packet131MapData packet) {
            short id = packet.uniqueID;
            byte[] data = packet.itemData;

            if (DEBUG) {
                System.out.println("Got packet " + id + ":");
                String data_str = "";
                for (byte datum : data) {
                    data_str += datum + ",";
                }
                System.out.println(data_str);
            }

            if (Wustendorf.getSide() == Side.CLIENT) {
            } else { // Server/bukkit side
            }
        }
    }
}
