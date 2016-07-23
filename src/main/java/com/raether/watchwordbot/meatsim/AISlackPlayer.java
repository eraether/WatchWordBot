package com.raether.watchwordbot.meatsim;

import java.util.UUID;

import com.ullink.slack.simpleslackapi.SlackUser;

public class AISlackPlayer implements SlackUser {
	private String id;
	private String username;

	public AISlackPlayer(String username) {
		this.id = generateRandomID();
		this.username = username;
	}

	protected static String generateRandomID() {
		return UUID.randomUUID().toString().replaceAll("-", "");
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getUserName() {
		return username;
	}

	@Override
	public String getRealName() {
		return getUserName();
	}

	@Override
	public String getUserMail() {
		return getUserName() + "@none.com";
	}

	@Override
	public boolean isDeleted() {
		return true;
	}

	@Override
	public boolean isAdmin() {
		return false;
	}

	@Override
	public boolean isOwner() {
		return false;
	}

	@Override
	public boolean isPrimaryOwner() {
		return false;
	}

	@Override
	public boolean isRestricted() {
		return true;
	}

	@Override
	public boolean isUltraRestricted() {
		return false;
	}

	@Override
	public boolean isBot() {
		return true;
	}

	@Override
	public String getTimeZone() {
		return "UTC";
	}

	@Override
	public String getTimeZoneLabel() {
		return "UTC";
	}

	@Override
	public Integer getTimeZoneOffset() {
		return 0;
	}

}
