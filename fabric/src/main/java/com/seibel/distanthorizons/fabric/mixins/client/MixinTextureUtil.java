package com.seibel.distanthorizons.fabric.mixins.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.seibel.distanthorizons.core.config.Config;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Enhanced texture utility with aggressive LOD bias optimizations for maximum FPS
 * This version implements more aggressive texture optimization settings
 *
 * @author coolGi
 * @author kawashirov (FPS optimizations)
 */
@Mixin(TextureUtil.class)
public class MixinTextureUtil
{
	@Redirect(method = "Lcom/mojang/blaze3d/platform/TextureUtil;prepareImage(Lcom/mojang/blaze3d/platform/NativeImage$InternalGlFormat;IIII)V",
			at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/GlStateManager;_texParameter(IIF)V", remap = false))
	private static void setLodBias(int target, int pname, float param)
	{
		// Get user-configured LOD bias value
		float biasValue = Config.Client.Advanced.Graphics.AdvancedGraphics.lodBias.get().floatValue();
		
		if (target == GL11.GL_TEXTURE_2D) {
			// Apply ultra-aggressive LOD bias for maximum FPS
			if (pname == GL14.GL_TEXTURE_LOD_BIAS) {
				if (biasValue != 0) {
					// Use an even more aggressive bias multiplier for performance
					float aggressiveBias = Math.max(-6.0f, Math.min(4.0f, biasValue * 1.5f));
					GlStateManager._texParameter(target, pname, aggressiveBias);
				} else {
					// Apply more negative bias for maximum sharpness and FPS
					GlStateManager._texParameter(target, pname, -1.25f);
				}
			} else {
				// Apply ultra-performance-oriented texture parameters
				switch (pname) {
					case GL11.GL_TEXTURE_MIN_FILTER:
						// Use the most efficient mipmapping for maximum FPS
						GlStateManager._texParameter(target, pname, GL11.GL_LINEAR_MIPMAP_NEAREST);
						break;
					case GL11.GL_TEXTURE_MAG_FILTER:
						// Use nearest filtering for maximum performance when quality isn't critical
						GlStateManager._texParameter(target, pname, GL11.GL_NEAREST);
						break;
					default:
						// Use the original parameter for everything else
						GlStateManager._texParameter(target, pname, param);
						break;
				}
			}
		} else {
			// For non-2D textures, use original parameters
			GlStateManager._texParameter(target, pname, param);
		}
	}
}
