package wustendorf;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraftforge.client.MinecraftForgeClient;

public class ClientProxy extends CommonProxy {
    public void init(WustendorfMarker marker) {
        // Preload the in-world texture.
        MinecraftForgeClient.preloadTexture("/fm_wustendorf.png");

        // Add naming.
        LanguageRegistry.addName(marker, "House Flag");

        // Bind the renderer.
        RenderingRegistry.registerBlockHandler(new FlagRenderer());
    }

    public EntityPlayer getLocalPlayer() {
        return Minecraft.getMinecraft().thePlayer;
    }
}
