package com.raether.watchwordbot;

import java.util.ArrayList;
import java.util.List;

class Faction {
	private String factionName;
	// unique set
	private List<Player> players = new ArrayList<>();

	Faction(String factionName) {
		this.factionName = factionName;
	}

	@Override
	public String toString() {
		return (new StringBuilder().append(this.factionName)).toString();
	}

	void shiftMembership() {
		if (this.isEmpty()) {
			return;
		}
		Player lastPlayer = players.remove(players.size() - 1);
		players.add(0, lastPlayer);
	}

	private boolean isEmpty() {
		return players.isEmpty();
	}

	Integer getPlayerIndex(Player firstPlayer) {
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

	String getName() {
		return this.factionName;
	}

	boolean hasLeader() {
		return getLeader() != null;
	}

	Player getLeader() {
		if (players.isEmpty()) {
			return null;
		}

		return players.get(0);
	}

	List<Player> getAllPlayers() {
		return this.players;
	}

	boolean isLeader(Player player) {
		return hasLeader() && (player == getLeader());
	}

	public List<Player> getFollowers() {
		List<Player> followers = new ArrayList<Player>();
		followers.addAll(getAllPlayers());
		followers.remove(getLeader());
		return followers;
	}

	boolean addPlayer(Player player) {
		if (players.contains(player)) {
			return false;
		}
		players.add(player);
		return true;
	}

	boolean removePlayer(Player player) {
		return players.remove(player);
	}
}