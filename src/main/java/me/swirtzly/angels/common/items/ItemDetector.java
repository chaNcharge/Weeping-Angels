package me.swirtzly.angels.common.items;

import me.swirtzly.angels.common.WAObjects;
import me.swirtzly.angels.common.entities.EntityWeepingAngel;
import me.swirtzly.angels.config.WAConfig;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.passive.EntityChicken;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.IItemPropertyGetter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.List;

public class ItemDetector extends Item {

    public ItemDetector() {
        setCreativeTab(CreativeTabs.TOOLS);
        setMaxStackSize(1);

        this.addPropertyOverride(new ResourceLocation("angle"), new IItemPropertyGetter() {
            @SideOnly(Side.CLIENT)
            double rotation;
            @SideOnly(Side.CLIENT)
            double rota;
            @SideOnly(Side.CLIENT)
            long lastUpdateTick;

            @SideOnly(Side.CLIENT)
            public float apply(ItemStack stack, @Nullable World worldIn, @Nullable EntityLivingBase entityIn) {
                if (entityIn == null && !stack.isOnItemFrame()) {
                    return 0.0F;
                } else {
                    boolean flag = entityIn != null;
                    Entity entity = flag ? entityIn : stack.getItemFrame();

                    if (worldIn == null) {
                        worldIn = entity.world;
                    }

                    double d0;

                    if (worldIn.provider.isSurfaceWorld()) {
                        double d1 = flag ? (double) entity.rotationYaw : this.getFrameRotation((EntityItemFrame) entity);
                        d1 = MathHelper.positiveModulo(d1 / 360.0D, 1.0D);
                        double d2 = this.getSpawnToAngle(worldIn, entity) / (Math.PI * 2D);
                        d0 = 0.5D - (d1 - 0.25D - d2);
                    } else {
                        d0 = Math.random();
                    }

                    if (flag) {
                        d0 = this.wobble(worldIn, d0);
                    }

                    return MathHelper.positiveModulo((float) d0, 1.0F);
                }
            }

            @SideOnly(Side.CLIENT)
            private double wobble(World worldIn, double p_185093_2_) {
                if (worldIn.getTotalWorldTime() != this.lastUpdateTick) {
                    this.lastUpdateTick = worldIn.getTotalWorldTime();
                    double d0 = p_185093_2_ - this.rotation;
                    d0 = MathHelper.positiveModulo(d0 + 0.5D, 1.0D) - 0.5D;
                    this.rota += d0 * 0.1D;
                    this.rota *= 0.8D;
                    this.rotation = MathHelper.positiveModulo(this.rotation + this.rota, 1.0D);
                }

                return this.rotation;
            }

            @SideOnly(Side.CLIENT)
            private double getFrameRotation(EntityItemFrame p_185094_1_) {
                return (double) MathHelper.wrapDegrees(180 + p_185094_1_.facingDirection.getHorizontalIndex() * 90);
            }

            @SideOnly(Side.CLIENT)
            private double getSpawnToAngle(World p_185092_1_, Entity p_185092_2_) {
                BlockPos blockpos = p_185092_1_.getSpawnPoint();
                return Math.atan2((double) blockpos.getZ() - p_185092_2_.posZ, (double) blockpos.getX() - p_185092_2_.posX);
            }
        });

    }

    @Override
    public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if (!entityIn.world.isRemote) {
            List<EntityWeepingAngel> angels = entityIn.world.getEntitiesWithinAABB(EntityWeepingAngel.class, entityIn.getEntityBoundingBox().grow(25, 25, 25));

            if (entityIn instanceof EntityPlayer) {
                if (!angels.isEmpty() && isSelected) {
                    {
                        if ((Math.cos(entityIn.ticksExisted) + 3) > 3.84F) {
                            worldIn.playSound(null, entityIn.posX, entityIn.posY, entityIn.posZ, WAObjects.Sounds.DING, SoundCategory.PLAYERS, 0.5F, 1.0F);
                        }

                        if (worldIn.rand.nextInt(5) == 3 && WAConfig.angels.chickenGoboom) {
                            for (EntityChicken chick : entityIn.world.getEntitiesWithinAABB(EntityChicken.class, entityIn.getEntityBoundingBox().grow(30, 30, 30))) {
                                if (entityIn.world.rand.nextBoolean()) {
                                    chick.getEntityWorld().createExplosion(chick, chick.getPosition().getX(), chick.getPosition().getY(), chick.getPosition().getZ(), 0.5F, false);
                                    chick.dropItem(Items.EGG, 1);
                                    chick.setDead();
                                }
                            }
                        }

                    }
                }
            }
        }
    }
}
