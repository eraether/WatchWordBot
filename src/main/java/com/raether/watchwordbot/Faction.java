package com.raether.watchwordbot;

import java.util.ArrayList;
import java.util.List;

public class Faction {
	private String factionName;
	// unique set
	private List<Player> players = new ArrayList<Player>();

	public Faction(String factionName) {
		this.factionName = factionName;
	}

	public void shiftMembership() {
		if (this.isEmpty()) {
			return;
		}
		Player lastPlayer = players.remove(players.size() - 1);
		players.add(0, lastPlayer);
	}

	public boolean isEmpty() {
		return players.isEmpty();
	}

	public Integer getPlayerIndex(Player firstPlayer) {
		for (int x = 0; x < players.size(); x++) {
			if (players.get(x) == firstPlayer) {
				return x;
			}
		}
		return null;
	}

	public Faction(String factionName, List<Player> players) {
		this(factionName);
		this.players = players;
	}

	public String getName() {
		return this.factionName;
	}

	public boolean hasLeader() {
		return getLeader() != null;
	}

	public Player getLeader() {
		if (players.isEmpty()) {
			return null;
		}

		return players.get(0);
	}

	public List<Player> getAllPlayers() {
		return this.players;
	}

	public boolean isLeader(Player player) {
		return hasLeader() && (player == getLeader());
	}

	public List<Player> getFollowers() {
		List<Player> followers = new ArrayList<Player>();
		followers.addAll(getAllPlayers());
		followers.remove(getLeader());
		return followers;
	}

	public boolean addPlayer(Player player) {
		if (players.contains(player)) {
			return false;
		}
		players.add(player);
		return true;
	}

	public boolean removePlayer(Player player) {
		return players.remove(player);
	}
}