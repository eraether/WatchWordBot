package com.raether.watchwordbot;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public class Runner {

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err
					.println("Please pass in your slack token code as an argument.");
			System.exit(1);
		}

		Optional<SessionFactory> sessionFactory = readSessionFactory();
		Optional<GHRepository> ghRepo = readGHRepo();

		WatchWordBot bot = new WatchWordBot(args[0], sessionFactory, ghRepo);
		try {
			bot.loadResources();
			bot.connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Optional<GHRepository> readGHRepo() {
		try {
			GitHub github = GitHub.connect();
			String desiredRepo = System.getProperty("GITHUB_REPOSITORY");
			System.out.println("Desired Repo:" + desiredRepo);
			GHRepository repo = github.getRepository(desiredRepo);
			return Optional.of(repo);
		} catch (Exception e) {
			for (Object o : System.getProperties().keySet()) {
				System.out.println(o + ":" + System.getProperty(o + ""));
			}

			for (String env : System.getenv().keySet()) {
				System.out.println(env + ":" + System.getenv(env));
			}
			e.printStackTrace();
			return Optional.empty();
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
