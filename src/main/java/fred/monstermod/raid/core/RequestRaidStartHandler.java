package fred.monstermod.raid.core;

import fred.monstermod.core.BlockUtils;
import fred.monstermod.core.MessageUtil;
import fred.monstermod.core.RandomUtil;
import fred.monstermod.raid.Raid;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RequestRaidStartHandler {

    private Raid raid;

    public RequestRaidStartHandler(Raid _raid) {
        raid = _raid;
    }

    public void onRequestRaidStart(Player player)
    {
        if (!canStartRaid(player)) return;

        if (Bukkit.getServer().getWorld(RaidConfig.WORLD_NAME) == null)
        {
            player.sendMessage(Color.ORANGE + "Raid world generation starting... this can take a while");

            WorldCreator worldCreator = new WorldCreator(RaidConfig.WORLD_NAME);
            worldCreator.environment(World.Environment.NORMAL);
            worldCreator.seed((long) RandomUtil.random(0, 123456789));
            worldCreator.createWorld();
        }

        startRaid(player);
    }

    /**
     * Checks if the player can start the raid session it is a member of.
     * @param player The player that requests to start the raid session.
     * @return True when the raid session can be started, false when not.
     */
    private boolean canStartRaid(Player player)
    {
        RaidSession raidSession = raid.sessions.getCurrentRaidSession(player);
        if (raidSession == null)
        {
            player.sendMessage(ChatColor.RED + "You are not part of any raid.");
            return false;
        }

        if (!raidSession.isLeader(player))
        {
            String isNotLeaderMessage = ChatColor.RED + "You can not start the raid. You are not the raid leader of " + raidSession.getName() + ". ";

            Player leader = Bukkit.getPlayer(raidSession.getLeader());
            if (leader != null)
                isNotLeaderMessage += leader.getName() + " is the leader.";

            player.sendMessage(isNotLeaderMessage);
            return false;
        }

        if (raidSession.getStatus() != RaidSessionStatus.PREPARING)
        {
            player.sendMessage(ChatColor.RED + "You can not start raid '" + raidSession.getName()  + "' when it is already in progress.");
            return false;
        }

        return true;
    }

    private void startRaid(Player player)
    {
        RaidSession session = raid.sessions.getCurrentRaidSession(player);
        MessageUtil.broadcast(ChatColor.GREEN + "Raid session " + session.getName() +" is now starting...");

        Location spawnLocation = pickRandomLandLocation();

        session.activate();

        for (UUID playerUuid : session.getPlayers())
        {
            Player member = Bukkit.getPlayer(playerUuid);
            if (member == null) continue;

            member.teleport(spawnLocation.add(0, 1, 0));
            member.sendMessage(ChatColor.GREEN + "You have been teleported to the raid location. Good luck and have fun!");
        }
    }

    private Location pickRandomLandLocation()
    {
        Location location = null;
        while (location == null || location.getBlock().getBlockData().getMaterial().equals(Material.WATER))
        {
            Bukkit.getLogger().info("Picking new random location...");
            location = pickRandomLandLocationImpl();
        }

        return location;
    }

    private Location pickRandomLandLocationImpl()
    {
        World raidWorld = Bukkit.getServer().getWorld(RaidConfig.WORLD_NAME);
        final int RANDOM_X = (int)RandomUtil.random(-RaidConfig.X_SPREAD, RaidConfig.X_SPREAD);
        final int RANDOM_Z = (int)RandomUtil.random(-RaidConfig.Z_SPREAD, RaidConfig.Z_SPREAD);
        Block block = BlockUtils.getHighestYBlock(raidWorld, RANDOM_X, RANDOM_Z);
        return block.getLocation();
    }
}
