package com.raether.watchwordbot.feat;

import java.util.List;

import org.hibernate.Session;

public class UserFeatureRequestHelper {

	@SuppressWarnings({ "deprecation", "unchecked" })
	public static List<UserFeatureRequest> getFeatureRequests(Session session) {
		return session.createCriteria(UserFeatureRequest.class).list();
	}

	public static void saveFeatureRequest(UserFeatureRequest featureRequest,
			Session session) {
		session.saveOrUpdate(featureRequest);
	}
}
