package ltguide.minebackup.threads;

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
			for (final Player player : world.getPlayers())
				if (player.isOnline()) player.saveData();
			
			final Location spawn = world.getSpawnLocation();
			x = (int) spawn.getX() >> 4;
			z = (int) spawn.getZ() >> 4;
			
			int chunks = 0;
			for (final Chunk chunk : world.getLoadedChunks())
				if (!keepChunk(chunk) && world.unloadChunk(chunk.getX(), chunk.getZ(), true, true)) chunks++;
			
			plugin.debug(" | unloaded " + chunks + " chunks @ " + plugin.stopTime());
			
			world.save();
		}
		else if ("count".equals(action)) return plugin.persist.hasPlayers(world);
		
		return false;
	}
	
	private boolean keepChunk(final Chunk chunk) {
		if (!world.getKeepSpawnInMemory()) return false;
		
		return x - 13 <= chunk.getX() && chunk.getX() <= x + 13 && z - 13 <= chunk.getZ() && chunk.getZ() <= z + 13;
	}
}
