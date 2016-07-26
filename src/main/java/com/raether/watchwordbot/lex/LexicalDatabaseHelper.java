package com.raether.watchwordbot.lex;

import com.google.common.base.Optional;

import edu.cmu.lti.lexical_db.ILexicalDatabase;
import edu.cmu.lti.lexical_db.NictWordNet;

public class LexicalDatabaseHelper {
	private static Optional<ILexicalDatabase> singletonDB = Optional.absent();

	public static Optional<ILexicalDatabase> getDatabase() {
		if (singletonDB.isPresent()) {
			return singletonDB;
		}
		try {
			singletonDB = Optional.of(new NictWordNet());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return singletonDB;
	}

	public static boolean canBeCreated() {
		return getDatabase().isPresent();
	}
}
