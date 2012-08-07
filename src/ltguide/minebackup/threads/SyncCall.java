package ltguide.minebackup.threads;

import java.util.List;
import java.util.concurrent.Callable;

import ltguide.minebackup.MineBackup;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SyncCall implements Callable<Boolean> {
	private final MineBackup plugin;
	private final String action;
	private final World world;
	private List<Player> players;
	private int x;
	private int z;
	
	public SyncCall(final MineBackup plugin, final String action, final World world) {
		this.plugin = plugin;
		this.action = action;
		this.world = world;
	}
	
	@Override
	public Boolean call() {
		if ("save".equals(action)) {
			for (final Player player : players = world.getPlayers())
				if (player.isOnline()) player.saveData();
			
			final Location spawn = world.getSpawnLocation();
			x = (int) spawn.getX();
			z = (int) spawn.getZ();
			
			final Chunk[] chunks = world.getLoadedChunks();
			int count = 0;
			for (final Chunk chunk : chunks)
				if (!isSpawnChunk(chunk) && !isChunkInUse(chunk)) {
					world.unloadChunk(chunk.getX(), chunk.getZ(), true);
					count++;
				}
			
			plugin.debug(" | unloaded " + count + " of " + chunks.length + " chunks @ " + plugin.stopTime());
			
			if (world.getLoadedChunks().length != chunks.length - count) plugin.debug(" | however, something caused some chunks to stay loaded (" + world.getLoadedChunks().length + ")");
			
			world.save();
		}
		else if ("count".equals(action)) return plugin.persist.hasPlayers(world);
		
		return false;
	}
	
	private boolean isChunkInUse(final Chunk chunk) {
		for (final Player player : players) {
			final Location locaction = player.getLocation();
			final int range = player.isOnline() ? 256 : 16;
			
			if (Math.abs(locaction.getBlockX() - (chunk.getX() << 4)) <= range && Math.abs(locaction.getBlockZ() - (chunk.getZ() << 4)) <= range) return true;
		}
		
		return false;
	}
	
	private boolean isSpawnChunk(final Chunk chunk) {
		if (!world.getKeepSpawnInMemory()) return false;
		
		final int xSpawn = chunk.getX() * 16 + 8 - x;
		final int zSpawn = chunk.getZ() * 16 + 8 - z;
		final int radius = 128;
		
		if (xSpawn < -radius || xSpawn > radius || zSpawn < -radius || zSpawn > radius) return false;
		
		return true;
	}
}
