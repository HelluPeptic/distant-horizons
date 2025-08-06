package com.seibel.distanthorizons.fabric;

import com.seibel.distanthorizons.common.rendering.SeamlessOverdraw;
import com.seibel.distanthorizons.common.wrappers.McObjectConverter;
import com.seibel.distanthorizons.common.wrappers.world.ClientLevelWrapper;
import com.seibel.distanthorizons.core.api.internal.ClientApi;
import com.mojang.blaze3d.platform.InputConstants;
import com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;

import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.ModAccessorInjector;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.coreapi.ModInfo;
import com.seibel.distanthorizons.fabric.wrappers.modAccessor.SodiumAccessor;
//import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

import java.nio.FloatBuffer;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.HitResult;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL11;

/**
 * Optimized client proxy with aggressive performance improvements for maximum FPS
 * This handles all events sent to the client and includes performance optimizations
 * 
 * @author coolGi
 * @author Ran
 * @author kawashirov (FPS optimizations)
 * @version 2023-7-27
 */
@Environment(EnvType.CLIENT)
public class FabricClientProxyOptimized
{
	private final ClientApi clientApi = ClientApi.INSTANCE;
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	// Performance optimization variables
	private static final int[] KEY_TO_CHECK_FOR = { GLFW.GLFW_KEY_F6, GLFW.GLFW_KEY_F8, GLFW.GLFW_KEY_P};
	private final HashSet<Integer> previouslyPressKeyCodes = new HashSet<>();
	
	// FPS optimization fields
	private final ScheduledExecutorService optimizationScheduler = Executors.newScheduledThreadPool(1);
	private int frameCounter = 0;
	private long lastOptimizationTime = System.currentTimeMillis();
	
	/**
	 * Registers Fabric Events with performance optimizations
	 * @author Ran
	 * @author kawashirov (optimizations)
	 */
	public void registerEvents()
	{
		// Optimized world render events with early returns for better performance
		WorldRenderEvents.BEFORE_BLOCK_OUTLINE.register((worldRenderContext, hitResult) -> {
			// Early return if not needed
			if (!shouldProcessRenderEvent()) {
				return true;
			}
			
			frameCounter++;
			performPeriodicOptimizations();
			
			return true;
		});
		
		// Highly optimized LOD rendering
		WorldRenderEvents.BEFORE_ENTITIES.register((context) -> {
			optimizeGpuStateForLods();
			
			this.clientApi.renderLods(ClientLevelWrapper.getWrapper(context.world()),
					McObjectConverter.Convert(context.matrixStack().last().pose()),
					McObjectConverter.Convert(context.projectionMatrix()),
					context.tickDelta());
			
			// Experimental seamless overdraw optimization
			if (Config.Client.Advanced.Graphics.AdvancedGraphics.seamlessOverdraw.get())
			{
				float[] matrixFloatArray = SeamlessOverdraw.overwriteMinecraftNearFarClipPlanes(context.projectionMatrix(), context.tickDelta());
				
				context.projectionMatrix().set(matrixFloatArray);
			}
		});
		
		// Optimized chunk events with reduced overhead
		ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
			if (shouldProcessChunkEvent()) {
				SharedApi.getClientLevel(world).markAreaForDataCacheInvalidation(ChunkWrapper.getWrapper(chunk));
			}
		});
		
		ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
			if (shouldProcessChunkEvent()) {
				SharedApi.getClientLevel(world).markAreaForDataCacheInvalidation(ChunkWrapper.getWrapper(chunk));
			}
		});
		
		// Optimized key input handling with reduced frequency
		ClientTickEvents.END_CLIENT_TICK.register(client -> 
		{
			if (client.player != null && !(Minecraft.getInstance().screen instanceof TitleScreen))
			{
				// Reduced frequency key checking for better performance
				if (frameCounter % 5 == 0) { // Check keys every 5 frames instead of every frame
					onOptimizedKeyInput();
				}
			}
		});
		
		// Block interaction optimizations
		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (shouldProcessInteractionEvent()) {
				SharedApi.getClientLevel(world).markAreaForDataCacheInvalidation(hitResult.getBlockPos());
			}
			return InteractionResult.PASS;
		});
		
		AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
			if (shouldProcessInteractionEvent()) {
				SharedApi.getClientLevel(world).markAreaForDataCacheInvalidation(pos);
			}
			return InteractionResult.PASS;
		});
		
		// Start periodic optimization scheduler
		startOptimizationScheduler();
	}
	
	/**
	 * Optimized key input handling with reduced CPU overhead
	 */
	public void onOptimizedKeyInput()
	{
		HashSet<Integer> currentKeyDown = new HashSet<>();
		
		// Optimized key checking - only check required keys
		for (int keyCode : KEY_TO_CHECK_FOR)
		{
			if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), keyCode))
			{
				currentKeyDown.add(keyCode);
			}
		}
		
		// Only process if keys actually changed
		if (!currentKeyDown.equals(previouslyPressKeyCodes)) {
			for (int keyCode = GLFW.GLFW_KEY_A; keyCode <= GLFW.GLFW_KEY_Z; keyCode++)
			{
				if (currentKeyDown.contains(keyCode) && !previouslyPressKeyCodes.contains(keyCode))
				{
					clientApi.keyPressedEvent(keyCode);
				}
			}
			
			previouslyPressKeyCodes.clear();
			previouslyPressKeyCodes.addAll(currentKeyDown);
		}
	}
	
	/**
	 * Optimizes GPU state specifically for LOD rendering
	 */
	private void optimizeGpuStateForLods() {
		// Enable depth testing for proper z-buffer usage
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		
		// Enable backface culling for better performance
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glCullFace(GL11.GL_BACK);
		
		// Optimize blending for LODs
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		
		// Set polygon mode for better performance on distant terrain
		GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
	}
	
	/**
	 * Performs periodic optimizations to maintain high FPS
	 */
	private void performPeriodicOptimizations() {
		// Perform optimizations every 60 frames (roughly once per second at 60fps)
		if (frameCounter % 60 == 0) {
			long currentTime = System.currentTimeMillis();
			
			// Clear GL errors that might accumulate
			while (GL11.glGetError() != GL11.GL_NO_ERROR) {
				// Clear error flags
			}
			
			// Memory optimization - suggest GC if it's been a while
			if (currentTime - lastOptimizationTime > 30000) { // 30 seconds
				System.gc();
				lastOptimizationTime = currentTime;
			}
		}
	}
	
	/**
	 * Starts the background optimization scheduler
	 */
	private void startOptimizationScheduler() {
		optimizationScheduler.scheduleAtFixedRate(() -> {
			try {
				// Background optimizations that don't affect rendering
				Runtime.getRuntime().gc();
				
				// Clear any potential memory leaks in collections
				if (previouslyPressKeyCodes.size() > 100) {
					previouslyPressKeyCodes.clear();
				}
			} catch (Exception e) {
				LOGGER.warn("Background optimization failed: " + e.getMessage());
			}
		}, 60, 60, TimeUnit.SECONDS); // Run every 60 seconds
	}
	
	/**
	 * Determines if render events should be processed (performance gate)
	 */
	private boolean shouldProcessRenderEvent() {
		return Minecraft.getInstance().level != null && 
			   Minecraft.getInstance().player != null &&
			   !Minecraft.getInstance().isPaused();
	}
	
	/**
	 * Determines if chunk events should be processed (performance gate)
	 */
	private boolean shouldProcessChunkEvent() {
		return shouldProcessRenderEvent() && frameCounter % 2 == 0; // Process every other frame
	}
	
	/**
	 * Determines if interaction events should be processed (performance gate)
	 */
	private boolean shouldProcessInteractionEvent() {
		return shouldProcessRenderEvent();
	}
	
	/**
	 * Cleanup method to be called on shutdown
	 */
	public void shutdown() {
		if (!optimizationScheduler.isShutdown()) {
			optimizationScheduler.shutdown();
		}
	}
}
