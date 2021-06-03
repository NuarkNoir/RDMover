package xyz.nuark.rdmover;

import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityDrawers;
import com.refinedmods.refinedstorage.api.storage.StorageType;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDisk;
import com.refinedmods.refinedstorage.api.storage.disk.IStorageDiskProvider;
import com.refinedmods.refinedstorage.api.util.Action;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.refinedmods.refinedstorage.apiimpl.API;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;

@Mod.EventBusSubscriber
public class DriveRightClickHandler {
    static final Object lock = new Object();

    @SubscribeEvent(priority = EventPriority.NORMAL, receiveCanceled = true)
    public static void handleDriveRightClick(final PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() != LogicalSide.SERVER) {
            return;
        }

        ItemStack handIS = event.getItemStack();
        BlockPos blockPos = event.getPos();

        @SuppressWarnings("rawtypes")
        IStorageDisk disk = API.instance().getStorageDiskManager((ServerWorld) event.getWorld()).getByStack(handIS);
        if (disk == null) {
            return;
        }


        StorageType type = ((IStorageDiskProvider) handIS.getItem()).getType();
        if (type != StorageType.ITEM) {
            return;
        }

        @SuppressWarnings("unchecked")
        IStorageDisk<ItemStack> itemDisk = (IStorageDisk<ItemStack>) disk;

        Collection<ItemStack> stacks = itemDisk.getStacks();
        if (stacks.isEmpty()) {
            return;
        }

        TileEntity te = event.getWorld().getTileEntity(blockPos);
        if (!(te instanceof TileEntityDrawers)) {
            return;
        }


        synchronized (lock) {
            TileEntityDrawers dte = (TileEntityDrawers) te;
            boolean infiniteStorage = dte.getDrawerAttributes().isUnlimitedStorage() || dte.getDrawerAttributes().isVoid();
            for (int i = 0; i < dte.getGroup().getDrawerCount(); i++) {
                IDrawer drawer = dte.getGroup().getDrawer(i);
                stacks.stream().filter(itemStack -> itemStack.getItem() == drawer.getStoredItemPrototype().getItem()).findFirst().ifPresent(itemStack -> {
                    int remainingCap = drawer.getRemainingCapacity();
                    int stackAmount = itemStack.getCount();
                    int extractAmount = infiniteStorage? stackAmount : Math.min(stackAmount, remainingCap);

                    ItemStack extSim = itemDisk.extract(itemStack, extractAmount, IComparer.COMPARE_NBT, Action.SIMULATE);
                    if (extSim.getCount() > 0) {
                        itemDisk.extract(itemStack, extSim.getCount(), IComparer.COMPARE_NBT, Action.PERFORM);
                        drawer.setStoredItemCount(drawer.getStoredItemCount() + extSim.getCount());

                        event.getPlayer().sendStatusMessage(new StringTextComponent(
                                "Put " + extSim.getCount() + " items into drawer"
                        ), true);
                    }
                });
            }
        }
    }
}
