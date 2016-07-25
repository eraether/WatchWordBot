package com.raether.watchwordbot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.config.ConfigurationHelper;

public class Runner {

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err
					.println("Please pass in your slack token code as an argument.");
			System.exit(1);
		}

		Optional<SessionFactory> sessionFactory = readSessionFactory();

		WatchWordBot bot = new WatchWordBot(args[0], sessionFactory);
		try {
			bot.loadWordList();
			bot.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Optional<SessionFactory> readSessionFactory() {
		Optional<SessionFactory> sessionFactory = Optional.empty();
		try {
			Configuration config = new Configuration().configure();
			final Map settingsCopy = new HashMap();
			settingsCopy.putAll(config.getProperties());
			ConfigurationHelper.resolvePlaceHolders(settingsCopy);
			for (Object o : settingsCopy.keySet()) {
				System.out.println("Hibernate property: " + o + "="
						+ settingsCopy.get(o));
			}

			sessionFactory = Optional.of(new Configuration().configure()
					.buildSessionFactory());
		} catch (Exception e) {
			System.err.println("Could not initialize database!");
			e.printStackTrace();
		}
		return sessionFactory;
	}
}
