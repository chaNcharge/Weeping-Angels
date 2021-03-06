package me.swirtzly.angels.common.blocks;

import me.swirtzly.angels.client.models.poses.PoseManager;
import me.swirtzly.angels.common.tileentities.TileEntityPlinth;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public class BlockAngelPlinth extends Block {

    public BlockAngelPlinth() {
        super(Material.CORAL);
        translucent = true;
        setHardness(1.0F);
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityPlinth();
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    /**
     * Called by ItemBlocks after a block is set in the world, to allow post-place logic
     */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);

        if (world.getTileEntity(pos) instanceof TileEntityPlinth) {
            int rotation = MathHelper.floor(placer.rotationYaw);
            TileEntityPlinth plinth = (TileEntityPlinth) world.getTileEntity(pos);
            plinth.setRotation(rotation);
            plinth.setPose(PoseManager.randomEnum(PoseManager.AngelPoses.class).name());
            plinth.sendUpdates();
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (worldIn.isRemote) return false;
        if (playerIn.isSneaking()) {
            if (worldIn.getTileEntity(pos) instanceof TileEntityPlinth) {
                TileEntityPlinth plinth = (TileEntityPlinth) worldIn.getTileEntity(pos);
                plinth.setPose(PoseManager.randomEnum(PoseManager.AngelPoses.class).name());
                plinth.sendUpdates();
            }
            return false;
        }
        return super.onBlockActivated(worldIn, pos, state, playerIn, hand, facing, hitX, hitY, hitZ);
    }
}
