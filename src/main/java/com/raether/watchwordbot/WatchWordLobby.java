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
		if (hasUser(user)) {
			return false;
		}
		Player player = new Player();
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
}
