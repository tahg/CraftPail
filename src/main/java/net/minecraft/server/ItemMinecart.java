package net.minecraft.server;

import org.bukkit.craftbukkit.CraftBlock;
import org.bukkit.craftbukkit.CraftItemStack;
import org.bukkit.craftbukkit.CraftPlayer;
import org.bukkit.event.Event.Type;
import org.bukkit.event.player.PlayerItemEvent;


public class ItemMinecart extends Item {

    public int a;

    public ItemMinecart(int i, int j) {
        super(i);
        aX = 1;
        a = j;
    }

    public boolean a(ItemStack itemstack, EntityPlayer entityplayer, World world, int i, int j, int k, int l) {
        int i1 = world.a(i, j, k);

        if (i1 == Block.aG.bh) {
            // Craftbukkit start
            // Minecarts
            CraftBlock blockClicked = (CraftBlock) ((WorldServer) world).getWorld().getBlockAt(i, j, k);
            CraftItemStack itemInHand = new CraftItemStack(itemstack);
            CraftPlayer thePlayer = new CraftPlayer(((WorldServer) world).getServer(), (EntityPlayerMP) entityplayer);
            PlayerItemEvent pie = new PlayerItemEvent(Type.PLAYER_ITEM, thePlayer, itemInHand, blockClicked, CraftBlock.notchToBlockFace(l));
            
            ((WorldServer) world).getServer().getPluginManager().callEvent(pie);
            
            if (pie.isCancelled()) return false;
            
            if (!world.z) {
                world.a(new EntityMinecart(world, (float) i + 0.5F, (float) j + 0.5F, (float) k + 0.5F, a));
            }
            itemstack.a--;
            return true;
        } else {
            return false;
        }
    }
}
