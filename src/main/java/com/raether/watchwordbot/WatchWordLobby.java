package com.raether.watchwordbot;

import java.util.Set;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackUser;

public class WatchWordLobby {
	private SlackChannel channel;
	private TurnOrder turnOrder;
	private BidiMap<SlackUser, Player> playerMapping = new DualHashBidiMap<SlackUser, Player>();

	private double timerMultiplier = 1;

	public WatchWordLobby(SlackChannel channel, TurnOrder order) {
		this.channel = channel;
		this.turnOrder = order;
	}

	public SlackChannel getChannel() {
		return this.channel;
	}

	public boolean hasUser(SlackUser user) {
		return playerMapping.containsKey(user);
	}

	public boolean addUser(SlackUser user) {
		return addUser(user, new Player());
	}

	public boolean addUser(SlackUser user, Player player) {
		if (hasUser(user)) {
			return false;
		}
		playerMapping.put(user, player);
		turnOrder.getLeastFullFaction().addPlayer(player);
		return true;
	}

	public void removeUser(SlackUser user) {
		Player player = playerMapping.remove(user);
		turnOrder.removePlayer(player);
	}

	public Set<SlackUser> getUsers() {
		return playerMapping.keySet();
	}

	public Player getPlayer(SlackUser user) {
		return playerMapping.get(user);
	}

	public SlackUser getUser(Player player) {
		return playerMapping.getKey(player);
	}

	public TurnOrder getTurnOrder() {
		return this.turnOrder;
	}

	public void setTimerMultiplier(double multiplier) {
		this.timerMultiplier = multiplier;
	}

	public double getTimerMultiplier() {
		return this.timerMultiplier;
	}

	public int getAIPlayerCount() {
		int aiPlayerCount = 0;
		for (Faction faction : turnOrder.getAllFactions()) {
			for (Player player : faction.getAllPlayers()) {
				if (player.isAIControlled()) {
					aiPlayerCount += 1;
				}
			}
		}
		return aiPlayerCount;
	}
}
