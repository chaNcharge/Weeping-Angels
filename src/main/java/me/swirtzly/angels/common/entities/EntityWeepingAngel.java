package me.swirtzly.angels.common.entities;

import me.swirtzly.angels.WeepingAngels;
import me.swirtzly.angels.client.models.poses.PoseManager;
import me.swirtzly.angels.common.WAObjects;
import me.swirtzly.angels.common.misc.WAConstants;
import me.swirtzly.angels.config.WAConfig;
import me.swirtzly.angels.utils.AngelUtils;
import me.swirtzly.angels.utils.Teleporter;
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBreakDoor;
import net.minecraft.entity.ai.EntityAIMoveTowardsRestriction;
import net.minecraft.entity.ai.EntityAIWanderAvoidWater;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;

import static net.minecraft.block.BlockLever.FACING;
import static net.minecraft.block.BlockLever.POWERED;

public class EntityWeepingAngel extends EntityQuantumLockBase {
	
    private static final DataParameter<Integer> TYPE = EntityDataManager.createKey(EntityWeepingAngel.class, DataSerializers.VARINT);
    private static final DataParameter<Boolean> IS_CHILD = EntityDataManager.createKey(EntityWeepingAngel.class, DataSerializers.BOOLEAN);
    private static final DataParameter<String> CURRENT_POSE = EntityDataManager.createKey(EntityWeepingAngel.class, DataSerializers.STRING);
    private static final DataParameter<Integer> HUNGER_LEVEL = EntityDataManager.createKey(EntityWeepingAngel.class, DataSerializers.VARINT);
    
    public static ResourceLocation LOOT_TABLE = new ResourceLocation(WeepingAngels.MODID,"weepingangel");
    
    private SoundEvent[] SEEN_SOUNDS = new SoundEvent[]{WAObjects.Sounds.ANGEL_SEEN_1, WAObjects.Sounds.ANGEL_SEEN_2, WAObjects.Sounds.ANGEL_SEEN_3, WAObjects.Sounds.ANGEL_SEEN_4, WAObjects.Sounds.ANGEL_SEEN_5, WAObjects.Sounds.ANGEL_SEEN_6, WAObjects.Sounds.ANGEL_SEEN_7, WAObjects.Sounds.ANGEL_SEEN_8};
    private SoundEvent[] CHILD_SOUNDS = new SoundEvent[]{SoundEvents.ENTITY_VEX_AMBIENT, WAObjects.Sounds.LAUGHING_CHILD};

    public EntityWeepingAngel(World world) {
        super(world);
        tasks.addTask(0, new EntityAIBreakDoor(this));
        tasks.addTask(5, new EntityAIMoveTowardsRestriction(this, 1.0D));
        tasks.addTask(7, new EntityAIWanderAvoidWater(this, 1.0D));
        tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 50.0F));
        experienceValue = WAConfig.angels.xpGained;
    }



    @Override
    protected void entityInit() {
        super.entityInit();
        getDataManager().register(IS_CHILD, rand.nextInt(10) == 4);
        getDataManager().register(TYPE, AngelUtils.randomType().getId());
        getDataManager().register(CURRENT_POSE, PoseManager.randomEnum(PoseManager.AngelPoses.class).name());
        getDataManager().register(HUNGER_LEVEL, 50);
    }

    @Override
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, IEntityLivingData livingdata) {
        playSound(WAObjects.Sounds.ANGEL_AMBIENT, 0.5F, 1.0F);
        return super.onInitialSpawn(difficulty, livingdata);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.BLOCK_STONE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return WAObjects.Sounds.ANGEL_DEATH;
    }
    
    @Override
    protected SoundEvent getAmbientSound() {
        if (isCherub() && ticksExisted % AngelUtils.secondsToTicks(2) == 0) {
            return CHILD_SOUNDS[rand.nextInt(CHILD_SOUNDS.length)];
        }
        return null;
    }

    @Override
    public float getEyeHeight() {
        return isCherub() ? height : 1.3F;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(WAConfig.angels.damage);
        getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(50.0D);
        getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).setBaseValue(9999999.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.23000000417232513D);
        this.getEntityAttribute(SharedMonsterAttributes.ARMOR).setBaseValue(2.0D);
    }

    @Override
    public boolean attackEntityAsMob(Entity entity) {

        if (entity instanceof EntityPlayerMP) {

            EntityPlayerMP playerMP = (EntityPlayerMP) entity;

            //Blowing out light items from the players hand
            if (WAConfig.angels.torchBlowOut && isCherub()) {
                AngelUtils.removeLightFromHand(playerMP, this);
            }

            //Steals keys from the player
            if (getHeldItemMainhand().isEmpty() && rand.nextBoolean()) {
                for (int i = 0; i < playerMP.inventory.getSizeInventory(); i++) {
                    ItemStack stack = playerMP.inventory.getStackInSlot(i);
                    for (String regName : WAConstants.KEYS) {
                        if (regName.matches(stack.getItem().getRegistryName().toString())) {
                            setHeldItem(EnumHand.MAIN_HAND, playerMP.inventory.getStackInSlot(i).copy());
                            playerMP.inventory.getStackInSlot(i).setCount(0);
                            playerMP.inventoryContainer.detectAndSendChanges();
                        }
                    }
                }
            }


            //Teleporting and damage
            if (WAConfig.teleport.justTeleport) {
                if (!isCherub()) {
                    teleportInteraction(playerMP);
                    return false;
                } else {
                    dealDamage(playerMP);
                    return true;
                }
            } else {
                boolean shouldTeleport = rand.nextInt(10) < 5 && !isWeak();
                if (shouldTeleport) {
                    teleportInteraction(playerMP);
                    return false;
                } else {
                    dealDamage(playerMP);
                    return true;
                }
            }

        }
        return true;
    }
    
    @Override
    protected ResourceLocation getLootTable()
    {
        return LOOT_TABLE;
    }


    public void dealDamage(EntityPlayer playerMP) {
        if (getHealth() > 5) {
            playerMP.attackEntityFrom(WAObjects.ANGEL, 4.0F);
            heal(4.0F);
        } else {
            playerMP.attackEntityFrom(WAObjects.ANGEL_NECK_SNAP, 4.0F);
            heal(2.0F);
        }
    }
    
    
    public void dropAngelStuff() {
        ResourceLocation resourcelocation = this.getLootTable();
        LootTable loottable = this.world.getMinecraftServer().getEntityWorld().getLootTableManager().getLootTableFromLocation(resourcelocation);
        LootContext.Builder lootcontext$builder = (new LootContext.Builder((WorldServer)this.world)).withLootedEntity(this).withDamageSource(DamageSource.STARVE);
        loottable.generateLootForPools(this.rand, lootcontext$builder.build());
		entityDropItem(getHeldItemMainhand(), getHeldItemMainhand().getCount());
		entityDropItem(getHeldItemOffhand(), getHeldItemOffhand().getCount());
    }
    
	/*Drops Tardis Keys on Death + uses loot table drops
	 * Used to allow for config value defined tardis keys to be dropped
	 * Used instead of adding loot table functions
	 * 	N.B.There is a loot table function that does the same thing, but it requires:
	 *  -Hardcoded item registry names
	 *  -New entry for each tardis key (There could be many Tardis keys/items the player wants the angel to steal and drop on death
	 */
	
	@Override
	public void onDeath(DamageSource cause) {
		super.onDeath(cause);
		entityDropItem(getHeldItemMainhand(), getHeldItemMainhand().getCount());
		entityDropItem(getHeldItemOffhand(), getHeldItemOffhand().getCount());
	}

//    @Override
//    protected void dropFewItems(boolean wasRecentlyHit, int lootingModifier) {
//        dropItem(Item.getItemFromBlock(Blocks.STONE), rand.nextInt(3));
//    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setString(WAConstants.POSE, getPose());
        compound.setInteger(WAConstants.TYPE, getType());
        compound.setBoolean(WAConstants.ANGEL_CHILD, isCherub());
        compound.setInteger(WAConstants.HUNGER_LEVEL, getHungerLevel());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);

        if (compound.hasKey(WAConstants.POSE)) setPose(compound.getString(WAConstants.POSE));

        if (compound.hasKey(WAConstants.TYPE)) setType(compound.getInteger(WAConstants.TYPE));

        if (compound.hasKey(WAConstants.ANGEL_CHILD)) setChild(compound.getBoolean(WAConstants.ANGEL_CHILD));

        if (compound.hasKey(WAConstants.HUNGER_LEVEL)) setHungerLevel(compound.getInteger(WAConstants.HUNGER_LEVEL));
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (ticksExisted % 2400 == 0 && !world.isRemote) {
            setHungerLevel(getHungerLevel() - 2);
            if (isWeak()) {
                attackEntityFrom(DamageSource.STARVE, 2);
            }
        }
    }

    @Override
    public void invokeSeen(EntityPlayer player) {
        super.invokeSeen(player);

        if (player instanceof EntityPlayerMP && getSeenTime() == 1 && getPrevPos().toLong() != getPosition().toLong()) {
            setPrevPos(getPosition());
            if (WAConfig.angels.playSeenSounds) {
                ((EntityPlayerMP) player).connection.sendPacket(new SPacketSoundEffect(getSeenSound(), SoundCategory.HOSTILE, player.posX, player.posY, player.posZ, 0.2F, 1F));
            }
            if (getType() != AngelEnums.AngelType.ANGEL_THREE.getId()) {
                setPose(PoseManager.randomEnum(PoseManager.AngelPoses.class).name());
            } else {
                setPose(rand.nextBoolean() ? PoseManager.AngelPoses.ANGRY.name() : PoseManager.AngelPoses.HIDING_FACE.name());
            }
        }
    }

    public SoundEvent getSeenSound() {
        return SEEN_SOUNDS[rand.nextInt(SEEN_SOUNDS.length)];
    }


    @Override
    protected void playStepSound(BlockPos pos, Block blockIn) {
        SoundType soundtype = blockIn.getSoundType(world.getBlockState(pos), world, pos, this);

        if (this.world.getBlockState(pos.up()).getBlock() == Blocks.SNOW_LAYER) {
            soundtype = Blocks.SNOW_LAYER.getSoundType();
            this.playSound(soundtype.getStepSound(), soundtype.getVolume() * 0.15F, soundtype.getPitch());
        }

        if (!blockIn.getDefaultState().getMaterial().isLiquid()) {
            if (WAConfig.angels.playScrapSounds && !isCherub()) {
                playSound(WAObjects.Sounds.STONE_SCRAP, soundtype.getVolume() * 0.15F, soundtype.getPitch());
            }

            if (isCherub()) {
                if (world.rand.nextInt(5) == 5) {
                    playSound(WAObjects.Sounds.CHILD_RUN, soundtype.getVolume() * 0.15F, soundtype.getPitch());
                }
            }
        }
    }


    @Override
    public void moveTowards(EntityLivingBase entity) {
        super.moveTowards(entity);
        if (isQuantumLocked()) return;
        if (WAConfig.angels.playScrapSounds && !isCherub()) {
            playSound(WAObjects.Sounds.STONE_SCRAP, 0.2F, 1.0F);
        }

        if (isCherub()) {
            if (world.rand.nextInt(5) == 5) {
                playSound(WAObjects.Sounds.CHILD_RUN, 1.0F, 1.0F);
            }
        }
    }

    public boolean isWeak() {
        return getHungerLevel() < 15;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (getSeenTime() == 0 || world.getLight(getPosition()) == 0 || world.isAirBlock(getPosition().down())) {
            setNoAI(false);
        }

        if (ticksExisted % 500 == 0 && getAttackTarget() == null && !isQuantumLocked() && getSeenTime() == 0) {
            setPose(PoseManager.AngelPoses.HIDING_FACE.name());
        }

        replaceBlocks(getEntityBoundingBox().grow(WAConfig.angels.blockBreakRange));

    }

    @Override
    public void onKillEntity(EntityLivingBase entityLivingIn) {
        super.onKillEntity(entityLivingIn);

        if (entityLivingIn instanceof EntityPlayer) {
            playSound(WAObjects.Sounds.ANGEL_NECK_SNAP, 1, 1);
        }
    }

    @Override
    protected PathNavigate createNavigator(World worldIn) {
        PathNavigateGround navigator = new PathNavigateGround(this, worldIn);
        navigator.setCanSwim(false);
        navigator.setBreakDoors(true);
        navigator.setAvoidSun(false);
        return navigator;
    }

    private void replaceBlocks(AxisAlignedBB box) {
        if (world.isRemote || !WAConfig.angels.blockBreaking || ticksExisted % 100 != 0 || isQuantumLocked()) return;

        if (world.getLight(getPosition()) == 0) {
            return;
        }

        for (BlockPos pos : BlockPos.getAllInBox(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
            IBlockState blockState = world.getBlockState(pos);
            if (world.getGameRules().getBoolean("mobGriefing") && getHealth() > 5) {


                //Button
                if (blockState.getBlock() instanceof BlockButton && rand.nextInt(5) < 2) {
                    world.setBlockState(pos, blockState.withProperty(POWERED, Boolean.TRUE), 3);
                    world.markBlockRangeForRenderUpdate(pos, pos);
                    world.notifyNeighborsOfStateChange(pos, blockState.getBlock(), false);
                    world.notifyNeighborsOfStateChange(pos.offset(blockState.getValue(BlockButton.FACING).getOpposite()), blockState.getBlock(), false);
                    world.scheduleUpdate(pos, blockState.getBlock(), blockState.getBlock() == Blocks.WOODEN_BUTTON ? 20 : 30);
                }

                //Lever
                if (blockState.getBlock() instanceof BlockLever && rand.nextInt(5) < 2) {
                    blockState = blockState.cycleProperty(POWERED);
                    world.setBlockState(pos, blockState, 3);
                    float f = blockState.getValue(POWERED) ? 0.6F : 0.5F;
                    world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.3F, f);
                    world.notifyNeighborsOfStateChange(pos, blockState.getBlock(), false);
                    EnumFacing enumfacing = blockState.getValue(FACING).getFacing();
                    world.notifyNeighborsOfStateChange(pos.offset(enumfacing.getOpposite()), blockState.getBlock(), false);
                }

                if (!canBreak(blockState) || blockState.getBlock() == Blocks.LAVA || blockState.getBlock() == Blocks.AIR) {
                    continue;
                }

                if (blockState.getBlock() == Blocks.TORCH || blockState.getBlock() == Blocks.REDSTONE_TORCH || blockState.getBlock() == Blocks.GLOWSTONE) {
                    AngelUtils.playBreakEvent(this, pos, Blocks.AIR);
                    return;
                }

                if (blockState.getBlock() == Blocks.LIT_PUMPKIN) {
                    AngelUtils.playBreakEvent(this, pos, Blocks.PUMPKIN);
                    return;
                }

                if (blockState.getBlock() == Blocks.LIT_REDSTONE_LAMP) {
                    AngelUtils.playBreakEvent(this, pos, Blocks.REDSTONE_LAMP);
                    return;
                }

                if (blockState.getBlock() instanceof BlockPortal || blockState.getBlock() instanceof BlockEndPortal) {
                    if (getHealth() < getMaxHealth()) {
                        heal(1.5F);
                        world.setBlockToAir(pos);
                    }
                } else
                    continue;

                return;
            }
        }
    }

    private boolean canBreak(IBlockState blockState) {
        for (String regName : WAConfig.angels.disAllowedBlocks) {
            if (blockState.getBlock().getRegistryName().toString().equals(regName)) {
                return false;
            }
        }
        return true;
    }

    private void teleportInteraction(EntityPlayer player) {
        if (world.isRemote || player.isCreative() && !WAConfig.angels.teleportInCreative) return;

        AngelUtils.EnumTeleportType type = WAConfig.teleport.teleportType;

        switch (type) {
            case STRUCTURES:
                Teleporter.handleStructures(player);
                break;
            case RANDOM_PLACE:
                if (rand.nextBoolean()) {
                    BlockPos pos = new BlockPos(player.posX + rand.nextInt(WAConfig.teleport.teleportRange), 0, player.posZ + rand.nextInt(WAConfig.teleport.teleportRange));
                    if (AngelUtils.isPosOutsideBorder(pos, player.world)) {
                        pos = world.getSpawnPoint().add(rand.nextInt(WAConfig.teleport.teleportRange), 0, rand.nextInt(WAConfig.teleport.teleportRange));
                    }
                    Teleporter.moveSafeAcrossDim(player, pos);
                } else {
                    Teleporter.handleStructures(player);
                }
            case DONT:
                dealDamage(player);
                break;
            default:
                break;
        }
    }

    public String getPose() {
        return getDataManager().get(CURRENT_POSE);
    }

    public void setPose(String newPose) {
        getDataManager().set(CURRENT_POSE, newPose);
    }

    public boolean isCherub() {
        return getDataManager().get(IS_CHILD);
    }

    public void setChild(boolean child) {
        getDataManager().set(IS_CHILD, child);
    }

    public int getType() {
        return getDataManager().get(TYPE);
    }

    public void setType(int angelType) {
        getDataManager().set(TYPE, angelType);
    }

    public int getHungerLevel() {
        return getDataManager().get(HUNGER_LEVEL);
    }

    public void setHungerLevel(int hunger) {
        getDataManager().set(HUNGER_LEVEL, hunger);
    }
}

