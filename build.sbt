name := "CathBooksScraper"
version := "0.1"
scalaVersion := "2.13.16"

libraryDependencies += "net.ruippeixotog" %% "scala-scraper" % "3.2.0"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.10" % Test
libraryDependencies += "org.mockito" % "mockito-scala_2.13" % "2.0.0" % Test
libraryDependencies += "org.typelevel" %% "cats-core" % "2.13.0"

inThisBuild(List(
  scalaVersion := "2.13.16",
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision, // only required for Scala 2.x
))

scalacOptions += {
  if (scalaVersion.value.startsWith("2.12")) "-Ywarn-unused"
  else "-Wunused"
}
