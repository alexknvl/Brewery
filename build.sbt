name := "brewery"

organization := "com.dre"

version := "1.4.0"

javacOptions ++= Seq("-Xlint:deprecation")

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

scalacOptions += "-target:jvm-1.7"

libraryDependencies ++= Seq(
  "org.bukkit" % "bukkit" % "1.8.8-R0.1-SNAPSHOT",
  "org.spigotmc" % "spigot-api" % "1.8.8-R0.1-SNAPSHOT",
  "net.milkbowl.vault" % "VaultAPI" % "1.5" % "provided",
  "com.sk89q" % "worldguard" % "6.0.0-SNAPSHOT")

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "Plugin Metrics" at "http://repo.mcstats.org/content/repositories/public",
  "Bukkit" at "http://repo.bukkit.org/content/groups/public/",
  "Spigot" at "https://hub.spigotmc.org/nexus/content/groups/public/",
  "sk89q" at "http://maven.sk89q.com/repo/",
  "Vault" at "http://nexus.theyeticave.net/content/repositories/pub_releases")
