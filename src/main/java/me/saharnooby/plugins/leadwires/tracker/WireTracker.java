package me.saharnooby.plugins.leadwires.tracker;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import me.saharnooby.plugins.leadwires.chunk.ChunkCoord;
import me.saharnooby.plugins.leadwires.chunk.LoadedChunkTracker;
import me.saharnooby.plugins.leadwires.wire.Vector;
import me.saharnooby.plugins.leadwires.wire.Wire;
import me.saharnooby.plugins.leadwires.wire.WireStorage;
import me.saharnooby.plugins.leadwires.util.EntityId;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * @author saharNooby
 * @since 10:20 25.03.2020
 */
@RequiredArgsConstructor
public final class WireTracker {

	private final WireStorage storage;
	private final LoadedChunkTracker chunkTracker;
	private final Map<Player, PlayerTrackerData> map = new HashMap<>();

	public void checkPlayer(@NonNull Player player) {
		PlayerTrackerData data = this.map.computeIfAbsent(player, k -> new PlayerTrackerData());

		Set<ChunkCoord> loaded = this.chunkTracker.getLoaded(player);

		for (Iterator<SentWire> i = data.getSentWires().values().iterator(); i.hasNext(); ) {
			SentWire wire = i.next();

			if (!isLoaded(loaded, wire.getWire()) || !this.storage.containsWire(wire.getWire().getUuid())) {
				ProtocolUtil.despawn(player, wire);
				i.remove();
			}
		}

		// todo optimize: get by world, by chunk
		String world = player.getWorld().getName();

		for (Wire wire : this.storage.getWires().values()) {
			if (!wire.getWorld().equals(world)) {
				continue;
			}

			if (data.getSentWires().containsKey(wire.getUuid())) {
				continue;
			}

			if (isLoaded(loaded, wire)) {
				SentWire sent = new SentWire(wire, EntityId.next(), EntityId.next());
				data.getSentWires().put(wire.getUuid(), sent);
				ProtocolUtil.spawn(player, sent);
			}
		}
	}

	private static boolean isLoaded(@NonNull Set<ChunkCoord> loaded, @NonNull Wire wire) {
		return loaded.contains(getChunk(wire.getA())) && loaded.contains(getChunk(wire.getB()));
	}

	private static ChunkCoord getChunk(@NonNull Vector vector) {
		return new ChunkCoord(((int) Math.floor(vector.getX())) >> 4, ((int) Math.floor(vector.getZ())) >> 4);
	}

	public void removePlayer(@NonNull Player player) {
		PlayerTrackerData data = this.map.remove(player);

		if (data != null) {
			data.getSentWires().values().forEach(w -> ProtocolUtil.despawn(player, w));
		}
	}

	public void removeAllPlayers() {
		this.map.forEach((player, data) -> data.getSentWires().values().forEach(w -> ProtocolUtil.despawn(player, w)));
		this.map.clear();
	}

}
