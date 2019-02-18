package me.suff.angels.utils;

import me.suff.angels.common.entities.EntityAnomaly;
import me.suff.angels.config.WAConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ITeleporter;

import javax.annotation.Nullable;
import java.util.Random;

public final class Teleporter {
	
	@Nullable
	public static Entity move(Entity entity, int dimension, BlockPos pos) {
		return move(entity, dimension, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
	}
	
	@Nullable
	public static Entity move(Entity entity, int dimension, double x, double y, double z) {
		if (entity.world.isRemote || !entity.isNonBoss()) {
			return null;
		}
		
		if (entity.dimension == dimension) {
			if (entity instanceof EntityPlayerMP) {
				if (((EntityPlayerMP) entity).connection != null) {
					((EntityPlayerMP) entity).connection.setPlayerLocation(x, y, z, entity.rotationYaw, entity.rotationPitch);
				}
			} else {
				entity.setLocationAndAngles(x, y, z, entity.rotationYaw, entity.rotationPitch);
			}
			return entity;
		}
		return entity.changeDimension(dimension, new WATeleport(x, y, z));
	}
	
	public static Entity moveSafeAcrossDim(Entity entity, BlockPos pos) {
		
		EntityAnomaly anomaly = new EntityAnomaly(entity.world);
		BlockPos entityOldPos = entity.getPosition();
		anomaly.setLocationAndAngles(entityOldPos.getX(), entityOldPos.getY(), entityOldPos.getZ(), entity.rotationYaw, entity.rotationPitch);
		entity.world.spawnEntity(anomaly);
		
		if (entity instanceof EntityPlayerMP) {
			DimensionType newDimension = getRandomDimension(entity.world.provider.getDimensionType(), entity.world.rand);
			entity.changeDimension(newDimension.getId(), (world, en, yaw) -> entity.setLocationAndAngles(0, 0, 0, en.rotationYaw, en.rotationPitch));
			World world = entity.getEntityWorld();
			boolean beSafeFlag = newDimension == DimensionType.THE_END || newDimension == DimensionType.NETHER;
			BlockPos spawn = beSafeFlag ? pos : world.getSpawnPoint();
			
			while (!(world.isAirBlock(spawn) && world.isAirBlock(spawn.up())) && spawn.getY() < world.provider.getHeight() - 5)
				spawn = spawn.up();
			
			entity.setPositionAndUpdate(spawn.getX(), spawn.getY(), spawn.getZ());
			return entity;
		}
		return entity;
	}
	
	public static DimensionType getRandomDimension(DimensionType current, Random rand) {
		DimensionType[] dimensions = DimensionType.values();
		if (dimensions == null || dimensions.length <= 1)
			return DimensionType.OVERWORLD;
		
		DimensionType dim = dimensions[rand.nextInt(dimensions.length)];
		
		if (dim == null) {
			return DimensionType.OVERWORLD;
		}
		
		for (int notAllowedDimension : WAConfig.teleport.notAllowedDimensions) {
			if (notAllowedDimension == dim.getId()) {
				return current;
			}
		}
		return dim;
	}
	
	public static void handleStructures(EntityPlayer player) {
		
		String[] targetStructure = null;
		
		switch (player.world.provider.getDimension()) {
			case 0:
				targetStructure = AngelUtils.OVERWORLD_STRUCTURES;
				break;
			
			case 1:
				targetStructure = AngelUtils.END_STRUCTURES;
				break;
			
			case -1:
				targetStructure = AngelUtils.NETHER_STRUCTURES;
				break;
		}
		
		if (targetStructure != null) {
			BlockPos bPos = player.getEntityWorld().findNearestStructure(targetStructure[player.world.rand.nextInt(targetStructure.length)], player.getPosition(), false);
			if (bPos != null) {
				Teleporter.move(player, player.dimension, bPos);
			}
		}
	}
	
	public static final class WATeleport implements ITeleporter {
		private final double x, y, z;
		
		public WATeleport(double x, double y, double z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		@Override
		public void placeEntity(World world, Entity entity, float yaw) {
			entity.setLocationAndAngles(x, y, z, yaw, entity.rotationPitch);
		}
	}
	
}
