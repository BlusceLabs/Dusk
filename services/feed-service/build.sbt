val scala3Version = "3.3.4"

lazy val root = project
  .in(file("."))
  .settings(
    name := "feed-service",
    version := "0.1.0",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "cask"     % "0.9.2",
      "com.lihaoyi" %% "upickle"  % "3.3.1",
      "com.lihaoyi" %% "requests" % "0.8.0",
    ),
    assembly / mainClass     := Some("com.dusk.feed.Main"),
    assembly / assemblyJarName := "feed-service.jar",
  )
