package wustendorf;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.src.Block;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.RenderBlocks;
import net.minecraft.src.Tessellator;
import net.minecraftforge.client.ForgeHooksClient;

public class FlagRenderer implements ISimpleBlockRenderingHandler {
    public static final int ID = 3595001;

    public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {
        return;
    }

    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        ForgeHooksClient.bindTexture("/fm_wustendorf.png", 0);

        double tex_x_min = (0/16d);
        double tex_x_max = tex_x_min + (1/16d);
        double tex_y_min = (0/16d);
        double tex_y_max = tex_y_min + (1/16d);

        Tessellator tess = Tessellator.instance;
        tess.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));

        double x_min = x + 0.05;
        double x_max = x + 0.95;
        double z_min = z + 0.05;
        double z_max = z + 0.95;

        tess.addVertexWithUV(x_min, y + 1.0D, z_min, tex_x_min, tex_y_min);
        tess.addVertexWithUV(x_min, y + 0.0D, z_min, tex_x_min, tex_y_max);
        tess.addVertexWithUV(x_max, y + 0.0D, z_max, tex_x_max, tex_y_max);
        tess.addVertexWithUV(x_max, y + 1.0D, z_max, tex_x_max, tex_y_min);

        tess.addVertexWithUV(x_max, y + 1.0D, z_max, tex_x_max, tex_y_min);
        tess.addVertexWithUV(x_max, y + 0.0D, z_max, tex_x_max, tex_y_max);
        tess.addVertexWithUV(x_min, y + 0.0D, z_min, tex_x_min, tex_y_max);
        tess.addVertexWithUV(x_min, y + 1.0D, z_min, tex_x_min, tex_y_min);

        return true;
    }

    public boolean shouldRender3DInInventory() {
        return false;
    }

    public int getRenderId() {
        return ID;
    }
}
