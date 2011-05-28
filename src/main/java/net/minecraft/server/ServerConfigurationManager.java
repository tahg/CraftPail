package net.minecraft.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

// CraftBukkit start
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.command.ColouredConsoleSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
// CraftBukkit end

public class ServerConfigurationManager {

    public static Logger a = Logger.getLogger("Minecraft");
    public List players = new ArrayList();
    public MinecraftServer server; // CraftBukkit - private -> public
    // private PlayerManager[] d = new PlayerManager[2]; // Craftbukkit - removed
    public int maxPlayers; // CraftBukkit - private -> public
    private Set banByName = new HashSet();
    private Set banByIP = new HashSet();
    private Set h = new HashSet();
    private Set i = new HashSet();
    private File j;
    private File k;
    private File l;
    private File m;
    public PlayerFileData playerFileData; // CraftBukkit - private - >public
    private boolean o;

    // CraftBukkit start
    private CraftServer cserver;

    public ServerConfigurationManager(MinecraftServer minecraftserver) {
        minecraftserver.server = new CraftServer(minecraftserver, this);
        minecraftserver.console = new ColouredConsoleSender(minecraftserver.server);
        cserver = minecraftserver.server;
        // CraftBukkit end

        this.server = minecraftserver;
        this.j = minecraftserver.a("banned-players.txt");
        this.k = minecraftserver.a("banned-ips.txt");
        this.l = minecraftserver.a("ops.txt");
        this.m = minecraftserver.a("white-list.txt");
        int i = minecraftserver.propertyManager.getInt("view-distance", 10);

        // Craftbukkit - removed playermanagers
        this.maxPlayers = minecraftserver.propertyManager.getInt("max-players", 20);
        this.o = minecraftserver.propertyManager.getBoolean("white-list", false);
        this.g();
        this.i();
        this.k();
        this.m();
        this.h();
        this.j();
        this.l();
        this.n();
    }

    public void setPlayerFileData(WorldServer[] aworldserver) {
        if (this.playerFileData != null) return; // CraftBukkit
        this.playerFileData = aworldserver[0].p().d();
    }

    public void a(EntityPlayer entityplayer) {
        // Craftbukkit - removed playermanagers
        this.a(entityplayer.dimension).addPlayer(entityplayer);
        WorldServer worldserver = this.server.a(entityplayer.dimension);

        worldserver.chunkProviderServer.getChunkAt((int) entityplayer.locX >> 4, (int) entityplayer.locZ >> 4);
    }

    public int a() {
        // Craftbukkit start
        if (this.server.worlds.size() == 0) {
            return this.server.propertyManager.getInt("view-distance", 10) * 16 - 16;
        } else {
            return this.server.worlds.get(0).manager.c();
        }
        // Craftbukkit end
    }

    private PlayerManager a(int i) {
        return server.a(i).manager; // Craftbukkit
    }

    public void b(EntityPlayer entityplayer) {
        this.playerFileData.b(entityplayer);
    }

    public void c(EntityPlayer entityplayer) {
        this.players.add(entityplayer);
        WorldServer worldserver = this.server.a(entityplayer.dimension);

        worldserver.chunkProviderServer.getChunkAt((int) entityplayer.locX >> 4, (int) entityplayer.locZ >> 4);

        while (worldserver.getEntities(entityplayer, entityplayer.boundingBox).size() != 0) {
            entityplayer.setPosition(entityplayer.locX, entityplayer.locY + 1.0D, entityplayer.locZ);
        }

        // CraftBukkit start
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(cserver.getPlayer(entityplayer), "\u00A7e" + entityplayer.name + " joined the game.");
        cserver.getPluginManager().callEvent(playerJoinEvent);

        String joinMessage = playerJoinEvent.getJoinMessage();

        if (joinMessage != null) {
            this.server.serverConfigurationManager.sendAll(new Packet3Chat(joinMessage));
        }
        // CraftBukkit end

        worldserver.addEntity(entityplayer);
        this.a(entityplayer.dimension).addPlayer(entityplayer);
    }

    public void d(EntityPlayer entityplayer) {
        this.a(entityplayer.dimension).movePlayer(entityplayer);
    }

    public String disconnect(EntityPlayer entityplayer) { // CraftBukkit - changed return type
        // CraftBukkit start
        // Quitting must be before we do final save of data, in case plugins need to modify it
        this.a(entityplayer.dimension).removePlayer(entityplayer);
        PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(cserver.getPlayer(entityplayer), "\u00A7e" + entityplayer.name + " left the game.");
        cserver.getPluginManager().callEvent(playerQuitEvent);
        // CraftBukkit end

        this.playerFileData.a(entityplayer);
        this.server.a(entityplayer.dimension).kill(entityplayer);
        this.players.remove(entityplayer);
        this.a(entityplayer.dimension).removePlayer(entityplayer);

        return playerQuitEvent.getQuitMessage(); // CraftBukkit
    }

    public EntityPlayer a(NetLoginHandler netloginhandler, String s) {
        // CraftBukkit start - note: this entire method needs to be changed
        // Instead of kicking then returning, we need to store the kick reason
        // in the event, check with plugins to see if it's ok, and THEN kick
        // depending on the outcome. Also change any reference to this.e.c to entity.world
        EntityPlayer entity = new EntityPlayer(this.server, this.server.a(0), s, new ItemInWorldManager(this.server.a(0)));
        Player player = (entity == null) ? null : (Player) entity.getBukkitEntity();
        PlayerLoginEvent event = new PlayerLoginEvent(player);

        String s1 = netloginhandler.networkManager.getSocketAddress().toString();

        s1 = s1.substring(s1.indexOf("/") + 1);
        s1 = s1.substring(0, s1.indexOf(":"));

        if (this.banByName.contains(s.trim().toLowerCase())) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, "You are banned from this server!");
            // return null // CraftBukkit
        } else if (!this.isWhitelisted(s)) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, "You are not white-listed on this server!");
        } else if (this.banByIP.contains(s1)) {
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, "Your IP address is banned from this server!");
        } else if (this.players.size() >= this.maxPlayers) {
            event.disallow(PlayerLoginEvent.Result.KICK_FULL, "The server is full!");
        } else {
            event.disallow(PlayerLoginEvent.Result.ALLOWED, s1);
        }

        cserver.getPluginManager().callEvent(event);
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            netloginhandler.disconnect(event.getKickMessage());
            return null;
        }

        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(i);

            if (entityplayer.name.equalsIgnoreCase(s)) {
                entityplayer.netServerHandler.disconnect("You logged in from another location");
            }
        }

        return new EntityPlayer(this.server, this.server.a(0), s, new ItemInWorldManager(this.server.a(0)));
        // CraftBukkit end
    }

    public EntityPlayer a(EntityPlayer entityplayer, int i) {
        this.server.b(entityplayer.dimension).trackPlayer(entityplayer);
        this.server.b(entityplayer.dimension).untrackEntity(entityplayer);
        this.a(entityplayer.dimension).removePlayer(entityplayer);
        this.players.remove(entityplayer);
        this.server.a(entityplayer.dimension).removeEntity(entityplayer);
        ChunkCoordinates chunkcoordinates = entityplayer.M();

        entityplayer.dimension = i;
        EntityPlayer entityplayer1 = new EntityPlayer(this.server, this.server.a(entityplayer.dimension), entityplayer.name, new ItemInWorldManager(this.server.a(entityplayer.dimension)));

        entityplayer1.id = entityplayer.id;
        entityplayer1.netServerHandler = entityplayer.netServerHandler;
        WorldServer worldserver = this.server.a(entityplayer.dimension);

        // CraftBukkit start - transfer internal variables
        entityplayer1.displayName = entityplayer.displayName;
        entityplayer1.compassTarget = entityplayer.compassTarget;
        entityplayer1.fauxSleeping = entityplayer.fauxSleeping;
        // CraftBukkit end

        if (chunkcoordinates != null) {
            ChunkCoordinates chunkcoordinates1 = EntityHuman.getBed(this.server.a(entityplayer.dimension), chunkcoordinates);

            if (chunkcoordinates1 != null) {
                entityplayer1.setPositionRotation((double) ((float) chunkcoordinates1.x + 0.5F), (double) ((float) chunkcoordinates1.y + 0.1F), (double) ((float) chunkcoordinates1.z + 0.5F), 0.0F, 0.0F);
                entityplayer1.a(chunkcoordinates);
            } else {
                entityplayer1.netServerHandler.sendPacket(new Packet70Bed(0));
            }
        }

        // CraftBukkit start
        Player respawnPlayer = cserver.getPlayer(entityplayer);
        Location respawnLocation = new Location(respawnPlayer.getWorld(), entityplayer1.locX, entityplayer1.locY, entityplayer1.locZ, entityplayer1.yaw, entityplayer1.pitch);

        PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, respawnLocation);
        cserver.getPluginManager().callEvent(respawnEvent);

        worldserver = ((CraftWorld)respawnEvent.getRespawnLocation().getWorld()).getHandle();
        entityplayer1.locX = respawnEvent.getRespawnLocation().getX();
        entityplayer1.locY = respawnEvent.getRespawnLocation().getY();
        entityplayer1.locZ = respawnEvent.getRespawnLocation().getZ();
        entityplayer1.yaw = respawnEvent.getRespawnLocation().getYaw();
        entityplayer1.pitch = respawnEvent.getRespawnLocation().getPitch();
        // CraftBukkit end

        worldserver.chunkProviderServer.getChunkAt((int) entityplayer1.locX >> 4, (int) entityplayer1.locZ >> 4);

        while (worldserver.getEntities(entityplayer1, entityplayer1.boundingBox).size() != 0) {
            entityplayer1.setPosition(entityplayer1.locX, entityplayer1.locY + 1.0D, entityplayer1.locZ);
        }

        entityplayer1.netServerHandler.sendPacket(new Packet9Respawn((byte) ((WorldServer) entityplayer1.world).getWorld().getEnvironment().getId()));
        entityplayer1.netServerHandler.a(entityplayer1.locX, entityplayer1.locY, entityplayer1.locZ, entityplayer1.yaw, entityplayer1.pitch);
        this.a(entityplayer1, worldserver);
        this.a(entityplayer1.dimension).addPlayer(entityplayer1);
        worldserver.addEntity(entityplayer1);
        this.players.add(entityplayer1);
        entityplayer1.syncInventory();
        entityplayer1.w();
        return entityplayer1;
    }

    public void f(EntityPlayer entityplayer) {
        WorldServer worldserver = this.server.a(entityplayer.dimension);
        boolean flag = false;
        byte b0;

        if (entityplayer.dimension == -1) {
            b0 = 0;
        } else {
            b0 = -1;
        }

        entityplayer.dimension = b0;
        WorldServer worldserver1 = this.server.a(entityplayer.dimension);

        // Craftbukkit
        entityplayer.netServerHandler.sendPacket(new Packet9Respawn((byte) ((WorldServer)entityplayer.world).getWorld().getEnvironment().getId()));
        worldserver.removeEntity(entityplayer);
        entityplayer.dead = false;
        double d0 = entityplayer.locX;
        double d1 = entityplayer.locZ;
        double d2 = 8.0D;

        if (entityplayer.dimension == -1) {
            d0 /= d2;
            d1 /= d2;
            entityplayer.setPositionRotation(d0, entityplayer.locY, d1, entityplayer.yaw, entityplayer.pitch);
            if (entityplayer.S()) {
                worldserver.entityJoinedWorld(entityplayer, false);
            }
        } else {
            d0 *= d2;
            d1 *= d2;
            entityplayer.setPositionRotation(d0, entityplayer.locY, d1, entityplayer.yaw, entityplayer.pitch);
            if (entityplayer.S()) {
                worldserver.entityJoinedWorld(entityplayer, false);
            }
        }

        if (entityplayer.S()) {
            worldserver1.addEntity(entityplayer);
            entityplayer.setPositionRotation(d0, entityplayer.locY, d1, entityplayer.yaw, entityplayer.pitch);
            worldserver1.entityJoinedWorld(entityplayer, false);
            worldserver1.chunkProviderServer.a = true;
            (new PortalTravelAgent()).a(worldserver1, entityplayer);
            worldserver1.chunkProviderServer.a = false;
        }

        this.a(entityplayer);
        entityplayer.netServerHandler.a(entityplayer.locX, entityplayer.locY, entityplayer.locZ, entityplayer.yaw, entityplayer.pitch);
        entityplayer.a((World) worldserver1);
        this.a(entityplayer, worldserver1);
        this.g(entityplayer);
    }

    public void b() {
        // Craftbukkit start
        for (int i = 0; i < this.server.worlds.size(); ++i) {
            this.server.worlds.get(i).manager.flush();
        }
        // Craftbukkit end
    }

    public void flagDirty(int i, int j, int k, int l) {
        this.a(l).flagDirty(i, j, k);
    }

    public void sendAll(Packet packet) {
        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(i);

            entityplayer.netServerHandler.sendPacket(packet);
        }
    }

    public void a(Packet packet, int i) {
        for (int j = 0; j < this.players.size(); ++j) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(j);

            if (entityplayer.dimension == i) {
                entityplayer.netServerHandler.sendPacket(packet);
            }
        }
    }

    public String c() {
        String s = "";

        for (int i = 0; i < this.players.size(); ++i) {
            if (i > 0) {
                s = s + ", ";
            }

            s = s + ((EntityPlayer) this.players.get(i)).name;
        }

        return s;
    }

    public void a(String s) {
        this.banByName.add(s.toLowerCase());
        this.h();
    }

    public void b(String s) {
        this.banByName.remove(s.toLowerCase());
        this.h();
    }

    private void g() {
        try {
            this.banByName.clear();
            BufferedReader bufferedreader = new BufferedReader(new FileReader(this.j));
            String s = "";

            while ((s = bufferedreader.readLine()) != null) {
                this.banByName.add(s.trim().toLowerCase());
            }

            bufferedreader.close();
        } catch (Exception exception) {
            a.warning("Failed to load ban list: " + exception);
        }
    }

    private void h() {
        try {
            PrintWriter printwriter = new PrintWriter(new FileWriter(this.j, false));
            Iterator iterator = this.banByName.iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();

                printwriter.println(s);
            }

            printwriter.close();
        } catch (Exception exception) {
            a.warning("Failed to save ban list: " + exception);
        }
    }

    public void c(String s) {
        this.banByIP.add(s.toLowerCase());
        this.j();
    }

    public void d(String s) {
        this.banByIP.remove(s.toLowerCase());
        this.j();
    }

    private void i() {
        try {
            this.banByIP.clear();
            BufferedReader bufferedreader = new BufferedReader(new FileReader(this.k));
            String s = "";

            while ((s = bufferedreader.readLine()) != null) {
                this.banByIP.add(s.trim().toLowerCase());
            }

            bufferedreader.close();
        } catch (Exception exception) {
            a.warning("Failed to load ip ban list: " + exception);
        }
    }

    private void j() {
        try {
            PrintWriter printwriter = new PrintWriter(new FileWriter(this.k, false));
            Iterator iterator = this.banByIP.iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();

                printwriter.println(s);
            }

            printwriter.close();
        } catch (Exception exception) {
            a.warning("Failed to save ip ban list: " + exception);
        }
    }

    public void e(String s) {
        this.h.add(s.toLowerCase());
        this.l();
    }

    public void f(String s) {
        this.h.remove(s.toLowerCase());
        this.l();
    }

    private void k() {
        try {
            this.h.clear();
            BufferedReader bufferedreader = new BufferedReader(new FileReader(this.l));
            String s = "";

            while ((s = bufferedreader.readLine()) != null) {
                this.h.add(s.trim().toLowerCase());
            }

            bufferedreader.close();
        } catch (Exception exception) {
            // CraftBukkit - corrected text
            a.warning("Failed to load ops: " + exception);
        }
    }

    private void l() {
        try {
            PrintWriter printwriter = new PrintWriter(new FileWriter(this.l, false));
            Iterator iterator = this.h.iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();

                printwriter.println(s);
            }

            printwriter.close();
        } catch (Exception exception) {
            // CraftBukkit - corrected text
            a.warning("Failed to save ops: " + exception);
        }
    }

    private void m() {
        try {
            this.i.clear();
            BufferedReader bufferedreader = new BufferedReader(new FileReader(this.m));
            String s = "";

            while ((s = bufferedreader.readLine()) != null) {
                this.i.add(s.trim().toLowerCase());
            }

            bufferedreader.close();
        } catch (Exception exception) {
            a.warning("Failed to load white-list: " + exception);
        }
    }

    private void n() {
        try {
            PrintWriter printwriter = new PrintWriter(new FileWriter(this.m, false));
            Iterator iterator = this.i.iterator();

            while (iterator.hasNext()) {
                String s = (String) iterator.next();

                printwriter.println(s);
            }

            printwriter.close();
        } catch (Exception exception) {
            a.warning("Failed to save white-list: " + exception);
        }
    }

    public boolean isWhitelisted(String s) {
        s = s.trim().toLowerCase();
        return !this.o || this.h.contains(s) || this.i.contains(s);
    }

    public boolean isOp(String s) {
        return this.h.contains(s.trim().toLowerCase());
    }

    public EntityPlayer i(String s) {
        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(i);

            if (entityplayer.name.equalsIgnoreCase(s)) {
                return entityplayer;
            }
        }

        return null;
    }

    public void a(String s, String s1) {
        EntityPlayer entityplayer = this.i(s);

        if (entityplayer != null) {
            entityplayer.netServerHandler.sendPacket(new Packet3Chat(s1));
        }
    }

    public void a(double d0, double d1, double d2, double d3, int i, Packet packet) {
        this.a((EntityHuman) null, d0, d1, d2, d3, i, packet);
    }

    public void a(EntityHuman entityhuman, double d0, double d1, double d2, double d3, int i, Packet packet) {
        for (int j = 0; j < this.players.size(); ++j) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(j);

            if (entityplayer != entityhuman && entityplayer.dimension == i) {
                double d4 = d0 - entityplayer.locX;
                double d5 = d1 - entityplayer.locY;
                double d6 = d2 - entityplayer.locZ;

                if (d4 * d4 + d5 * d5 + d6 * d6 < d3 * d3) {
                    entityplayer.netServerHandler.sendPacket(packet);
                }
            }
        }
    }

    public void j(String s) {
        Packet3Chat packet3chat = new Packet3Chat(s);

        for (int i = 0; i < this.players.size(); ++i) {
            EntityPlayer entityplayer = (EntityPlayer) this.players.get(i);

            if (this.isOp(entityplayer.name)) {
                entityplayer.netServerHandler.sendPacket(packet3chat);
            }
        }
    }

    public boolean a(String s, Packet packet) {
        EntityPlayer entityplayer = this.i(s);

        if (entityplayer != null) {
            entityplayer.netServerHandler.sendPacket(packet);
            return true;
        } else {
            return false;
        }
    }

    public void savePlayers() {
        for (int i = 0; i < this.players.size(); ++i) {
            this.playerFileData.a((EntityHuman) this.players.get(i));
        }
    }

    public void a(int i, int j, int k, TileEntity tileentity) {}

    public void k(String s) {
        this.i.add(s);
        this.n();
    }

    public void l(String s) {
        this.i.remove(s);
        this.n();
    }

    public Set e() {
        return this.i;
    }

    public void f() {
        this.m();
    }

    public void a(EntityPlayer entityplayer, WorldServer worldserver) {
        entityplayer.netServerHandler.sendPacket(new Packet4UpdateTime(worldserver.getTime()));
        if (worldserver.v()) {
            entityplayer.netServerHandler.sendPacket(new Packet70Bed(1));
        }
    }

    public void g(EntityPlayer entityplayer) {
        entityplayer.a(entityplayer.defaultContainer);
        entityplayer.B();
    }
}
