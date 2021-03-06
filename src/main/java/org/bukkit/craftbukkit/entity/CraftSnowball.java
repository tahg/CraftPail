package org.bukkit.craftbukkit.entity;

import net.minecraft.server.EntitySnowball;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Snowball;

/**
 * A snowball.
 *
 * @author sk89q
 */
public class CraftSnowball extends CraftEntity implements Snowball {
    public CraftSnowball(CraftServer server, EntitySnowball entity) {
        super(server, entity);
    }

    @Override
    public String toString() {
        return "CraftSnowball";
    }
}
