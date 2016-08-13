package com.raether.watchwordbot.lex;

import java.util.List;

import org.hibernate.Session;

public class BanishedWordHelper {
	@SuppressWarnings({ "deprecation", "unchecked" })
	public static List<BanishedWord> getBanishedWordList(Session session) {
		return session.createCriteria(BanishedWord.class).list();
	}
}
