package Andrew6rant.tiered;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.*;
import com.anthonyhilyard.iceberg.util.GuiHelper;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Matrix4f;

import org.lwjgl.opengl.GL11;

import java.util.Timer;
import java.util.TimerTask;

public class Tooltip {
    // Thanks to Grend for permission to use the Iceberg Library and LegendaryTooltips code for the tooltips in this mod
    private static final Identifier TEXTURE_TOOLTIP_BORDERS = new Identifier("tiered", "textures/gui/tooltips.png");

    public static long timeInMilli = System.currentTimeMillis();
    public static int i = 0;

    public static void drawBorder(MatrixStack matrixStack, int x, int y, int width, int height, int[] frameArray, int startColor, int endColor) {

        matrixStack.push();
        Matrix4f mat = matrixStack.peek().getPositionMatrix();

        GuiHelper.drawGradientRect(mat, 400, x - 3, y - 3 + 1, x - 3 + 1, y + height + 3 - 1, startColor, endColor);
        GuiHelper.drawGradientRect(mat, 400, x + width + 2, y - 3 + 1, x + width + 3, y + height + 3 - 1, startColor, endColor);
        GuiHelper.drawGradientRect(mat, 400, x - 3, y - 3, x + width + 3, y - 3 + 1, startColor, endColor);
        GuiHelper.drawGradientRect(mat, 400, x - 3, y + height + 2, x + width + 3, y + height + 3, startColor, endColor);
        matrixStack.pop();

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TEXTURE_TOOLTIP_BORDERS);

        // We have to bind the texture to be able to query it
        MinecraftClient mc = MinecraftClient.getInstance();
        AbstractTexture borderTexture = mc.getTextureManager().getTexture(TEXTURE_TOOLTIP_BORDERS);
        borderTexture.bindTexture();

        int textureWidth = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH); // these should be equal and powers of 2 to render properly
        int textureHeight = GlStateManager._getTexLevelParameter(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

        int divisorValue = textureWidth / 32; // this allows texture size to scale larger than 256x (512x, 1024x...) for additional border styles (256x -> 16 styles, 512x -> 64 styles, etc)

        i = i > (frameArray.length - 1) ? 0 : i;
        drawBorderTextures(matrixStack, x, y, width, height, frameArray[i], divisorValue, textureWidth, textureHeight);
        if (frameArray.length > 1) {
            if (!(timeInMilli + frameArray[frameArray.length-1] > System.currentTimeMillis())) {
                timeInMilli = System.currentTimeMillis();
                i = (i + 1) > (frameArray.length - 2) ? 0 : i + 1;
                //drawBorderTextures(matrixStack, x, y, width, height, frameArray[i], divisorValue, textureWidth, textureHeight);
            }
        } //else {
        //    drawBorderTextures(matrixStack, x, y, width, height, frameArray[0], divisorValue, textureWidth, textureHeight);
        //}


    }

    public static void drawBorderTextures(MatrixStack matrixStack, int x, int y, int width, int height, int frameLevel, int divisorValue, int textureWidth, int textureHeight) {
        matrixStack.push();
        matrixStack.translate(0, 0, 410.0);
        // top left corner
        DrawableHelper.drawTexture(matrixStack, x - 10, y - 10, (frameLevel / divisorValue) * 128, (frameLevel * 32) % textureHeight, 16, 16, textureWidth, textureHeight);

        // top right corner
        DrawableHelper.drawTexture(matrixStack, x + width - 6, y - 10, 112 + (frameLevel / divisorValue) * 128, (frameLevel * 32) % textureHeight, 16, 16, textureWidth, textureHeight);

        // bottom left corner
        DrawableHelper.drawTexture(matrixStack, x - 10, y + height - 6, (frameLevel / divisorValue) * 128, 16 + (frameLevel * 32) % textureHeight, 16, 16, textureWidth, textureHeight);

        // bottom right corner
        DrawableHelper.drawTexture(matrixStack, x + width - 6, y + height - 6, 112 + (frameLevel / divisorValue) * 128, 16 + (frameLevel * 32) % textureHeight, 16, 16, textureWidth, textureHeight);

        if (width >= 64) {
            // top middle
            DrawableHelper.drawTexture(matrixStack, x + (width / 2) - 32, y - 12, 32 + (frameLevel / divisorValue) * 128, (frameLevel * 32) % textureHeight, 64, 16, textureWidth, textureHeight);

            // bottom middle
            DrawableHelper.drawTexture(matrixStack, x + (width / 2) - 32, y + height - 4, 32 + (frameLevel / divisorValue) * 128, (frameLevel * 32) % textureHeight + 16, 64, 16, textureWidth, textureHeight);
        }
        if (height >= 48) {
            // left side
            DrawableHelper.drawTexture(matrixStack, x - 12, y + (height / 2) - 16, 16 + (frameLevel / divisorValue) * 128, (frameLevel * 32) % textureHeight, 16, 32, textureWidth, textureHeight);

            // right side
            DrawableHelper.drawTexture(matrixStack, x + width - 4, y + (height / 2) - 16, 96 + (frameLevel / divisorValue) * 128, (frameLevel * 32) % textureHeight, 16, 32, textureWidth, textureHeight);
        }
        matrixStack.pop();
    }
}
