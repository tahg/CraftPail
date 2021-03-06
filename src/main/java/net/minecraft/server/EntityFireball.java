package net.minecraft.server;

import java.util.List;

// CraftBukkit start
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.event.entity.EntityDamageByProjectileEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.ExplosionPrimeEvent;
// CraftBukkit end

public class EntityFireball extends Entity {

    private int f = -1;
    private int g = -1;
    private int h = -1;
    private int i = 0;
    private boolean j = false;
    public int a = 0;
    public EntityLiving shooter;
    private int k;
    private int l = 0;
    public double c;
    public double d;
    public double e;

    public EntityFireball(World world) {
        super(world);
        this.b(1.0F, 1.0F);
    }

    protected void b() {}

    public EntityFireball(World world, EntityLiving entityliving, double d0, double d1, double d2) {
        super(world);
        this.shooter = entityliving;
        this.b(1.0F, 1.0F);
        this.setPositionRotation(entityliving.locX, entityliving.locY, entityliving.locZ, entityliving.yaw, entityliving.pitch);
        this.setPosition(this.locX, this.locY, this.locZ);
        this.height = 0.0F;
        this.motX = this.motY = this.motZ = 0.0D;
        d0 += this.random.nextGaussian() * 0.4D;
        d1 += this.random.nextGaussian() * 0.4D;
        d2 += this.random.nextGaussian() * 0.4D;
        double d3 = (double) MathHelper.a(d0 * d0 + d1 * d1 + d2 * d2);

        this.c = d0 / d3 * 0.1D;
        this.d = d1 / d3 * 0.1D;
        this.e = d2 / d3 * 0.1D;
    }

    public void o_() {
        super.o_();
        this.fireTicks = 10;
        if (this.a > 0) {
            --this.a;
        }

        if (this.j) {
            int i = this.world.getTypeId(this.f, this.g, this.h);

            if (i == this.i) {
                ++this.k;
                if (this.k == 1200) {
                    this.die();
                }

                return;
            }

            this.j = false;
            this.motX *= (double) (this.random.nextFloat() * 0.2F);
            this.motY *= (double) (this.random.nextFloat() * 0.2F);
            this.motZ *= (double) (this.random.nextFloat() * 0.2F);
            this.k = 0;
            this.l = 0;
        } else {
            ++this.l;
        }

        Vec3D vec3d = Vec3D.create(this.locX, this.locY, this.locZ);
        Vec3D vec3d1 = Vec3D.create(this.locX + this.motX, this.locY + this.motY, this.locZ + this.motZ);
        MovingObjectPosition movingobjectposition = this.world.a(vec3d, vec3d1);

        vec3d = Vec3D.create(this.locX, this.locY, this.locZ);
        vec3d1 = Vec3D.create(this.locX + this.motX, this.locY + this.motY, this.locZ + this.motZ);
        if (movingobjectposition != null) {
            vec3d1 = Vec3D.create(movingobjectposition.f.a, movingobjectposition.f.b, movingobjectposition.f.c);
        }

        Entity entity = null;
        List list = this.world.b((Entity) this, this.boundingBox.a(this.motX, this.motY, this.motZ).b(1.0D, 1.0D, 1.0D));
        double d0 = 0.0D;

        for (int j = 0; j < list.size(); ++j) {
            Entity entity1 = (Entity) list.get(j);

            if (entity1.n_() && (entity1 != this.shooter || this.l >= 25)) {
                float f = 0.3F;
                AxisAlignedBB axisalignedbb = entity1.boundingBox.b((double) f, (double) f, (double) f);
                MovingObjectPosition movingobjectposition1 = axisalignedbb.a(vec3d, vec3d1);

                if (movingobjectposition1 != null) {
                    double d1 = vec3d.a(movingobjectposition1.f);

                    if (d1 < d0 || d0 == 0.0D) {
                        entity = entity1;
                        d0 = d1;
                    }
                }
            }
        }

        if (entity != null) {
            movingobjectposition = new MovingObjectPosition(entity);
        }

        if (movingobjectposition != null) {
            if (!this.world.isStatic) {
                // CraftBukkit start
                if (movingobjectposition.entity != null) {
                    boolean stick;
                    if (movingobjectposition.entity instanceof EntityLiving) {
                        CraftServer server = ((WorldServer) this.world).getServer();
                        org.bukkit.entity.Entity shooter = (this.shooter == null) ? null : this.shooter.getBukkitEntity();
                        org.bukkit.entity.Entity damagee = movingobjectposition.entity.getBukkitEntity();
                        org.bukkit.entity.Entity projectile = this.getBukkitEntity();
                        DamageCause damageCause = EntityDamageEvent.DamageCause.ENTITY_ATTACK;
                        int damage = 0;

                        // TODO @see EntityArrow#162
                        EntityDamageByProjectileEvent event = new EntityDamageByProjectileEvent(shooter, damagee, projectile, damageCause, damage);
                        server.getPluginManager().callEvent(event);

                        if (!event.isCancelled()) {
                            // this function returns if the fireball should stick or not, i.e. !bounce
                            stick = movingobjectposition.entity.damageEntity(this.shooter, event.getDamage());
                        } else {
                            // event was cancelled, get if the fireball should bounce or not
                            stick = !event.getBounce();
                        }
                    } else {
                        stick = movingobjectposition.entity.damageEntity(this.shooter, 0);
                    }
                    if (stick) {
                        ;
                    }
                }

            CraftServer server = ((WorldServer) this.world).getServer();

            ExplosionPrimeEvent event = new ExplosionPrimeEvent(CraftEntity.getEntity(server, this), 1.0F, false);
            server.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                // give 'this' instead of (Entity) null so we know what causes the damage
                this.world.createExplosion(this, this.locX, this.locY, this.locZ, event.getRadius(), event.getFire());
                this.die();
            }
            // CraftBukkit end
            }

            // this.die() // # CraftBukkit
        }

        this.locX += this.motX;
        this.locY += this.motY;
        this.locZ += this.motZ;
        float f1 = MathHelper.a(this.motX * this.motX + this.motZ * this.motZ);

        this.yaw = (float) (Math.atan2(this.motX, this.motZ) * 180.0D / 3.1415927410125732D);

        for (this.pitch = (float) (Math.atan2(this.motY, (double) f1) * 180.0D / 3.1415927410125732D); this.pitch - this.lastPitch < -180.0F; this.lastPitch -= 360.0F) {
            ;
        }

        while (this.pitch - this.lastPitch >= 180.0F) {
            this.lastPitch += 360.0F;
        }

        while (this.yaw - this.lastYaw < -180.0F) {
            this.lastYaw -= 360.0F;
        }

        while (this.yaw - this.lastYaw >= 180.0F) {
            this.lastYaw += 360.0F;
        }

        this.pitch = this.lastPitch + (this.pitch - this.lastPitch) * 0.2F;
        this.yaw = this.lastYaw + (this.yaw - this.lastYaw) * 0.2F;
        float f2 = 0.95F;

        if (this.ac()) {
            for (int k = 0; k < 4; ++k) {
                float f3 = 0.25F;

                this.world.a("bubble", this.locX - this.motX * (double) f3, this.locY - this.motY * (double) f3, this.locZ - this.motZ * (double) f3, this.motX, this.motY, this.motZ);
            }

            f2 = 0.8F;
        }

        this.motX += this.c;
        this.motY += this.d;
        this.motZ += this.e;
        this.motX *= (double) f2;
        this.motY *= (double) f2;
        this.motZ *= (double) f2;
        this.world.a("smoke", this.locX, this.locY + 0.5D, this.locZ, 0.0D, 0.0D, 0.0D);
        this.setPosition(this.locX, this.locY, this.locZ);
    }

    public void b(NBTTagCompound nbttagcompound) {
        nbttagcompound.a("xTile", (short) this.f);
        nbttagcompound.a("yTile", (short) this.g);
        nbttagcompound.a("zTile", (short) this.h);
        nbttagcompound.a("inTile", (byte) this.i);
        nbttagcompound.a("shake", (byte) this.a);
        nbttagcompound.a("inGround", (byte) (this.j ? 1 : 0));
    }

    public void a(NBTTagCompound nbttagcompound) {
        this.f = nbttagcompound.d("xTile");
        this.g = nbttagcompound.d("yTile");
        this.h = nbttagcompound.d("zTile");
        this.i = nbttagcompound.c("inTile") & 255;
        this.a = nbttagcompound.c("shake") & 255;
        this.j = nbttagcompound.c("inGround") == 1;
    }

    public boolean n_() {
        return true;
    }

    public boolean damageEntity(Entity entity, int i) {
        this.ae();
        if (entity != null) {
            Vec3D vec3d = entity.Y();

            if (vec3d != null) {
                this.motX = vec3d.a;
                this.motY = vec3d.b;
                this.motZ = vec3d.c;
                this.c = this.motX * 0.1D;
                this.d = this.motY * 0.1D;
                this.e = this.motZ * 0.1D;
            }

            return true;
        } else {
            return false;
        }
    }
}
