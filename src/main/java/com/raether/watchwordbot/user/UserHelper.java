package com.raether.watchwordbot.user;

import org.hibernate.Session;

import com.raether.watchwordbot.ranking.RatingHelper;

public class UserHelper {
	public static UserEntity readOrCreateUserEntity(String slackId,
			String username, Session session) {
		UserEntity entity = readUserEntity(slackId, session);
		if (entity == null) {
			entity = new UserEntity();
			entity.setUserId(slackId);
			entity.setUsername(username);
		}
		if (entity.getGuesserRating() == null) {
			RatingHelper.addDefaultRatingToUserIfUnset(entity, session);
		}
		return entity;
	}

	private static UserEntity readUserEntity(String slackId, Session session) {
		UserEntity entity = session.get(UserEntity.class, slackId);
		return entity;
	}
}
