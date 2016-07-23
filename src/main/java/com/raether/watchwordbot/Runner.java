package com.raether.watchwordbot;

import java.util.Optional;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class Runner {

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err
					.println("Please pass in your slack token code as an argument.");
			System.exit(1);
		}

		Optional<SessionFactory> sessionFactory = Optional.empty();
		try {
			sessionFactory = Optional.of(new Configuration().configure()
					.buildSessionFactory());
		} catch (Exception e) {
			System.err.println("Could not initialize database!");
			e.printStackTrace();
		}
		WatchWordBot bot = new WatchWordBot(args[0], sessionFactory);
		try {
			bot.loadWordList();
			bot.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
