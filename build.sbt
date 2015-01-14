organization  := "com.octojon"

version       := "0.1"

scalaVersion  := "2.11.4"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

fork in console := true
fork in run := true

assemblyJarName in assembly := "resizer.jar"
test in assembly := {}

libraryDependencies ++= {
  val akkaV = "2.3.6"
  val sprayV = "1.3.2"
  Seq(
    "io.spray"            %%  "spray-can"     % sprayV,
    "io.spray"            %%  "spray-routing" % sprayV,
    "io.spray"            %%  "spray-testkit" % sprayV  % "test",
    "com.typesafe.akka"   %%  "akka-actor"    % akkaV,
    "com.typesafe.akka"   %%  "akka-testkit"  % akkaV   % "test",
    "org.specs2"          %%  "specs2-core"   % "2.3.11" % "test",
    "com.sksamuel.scrimage" %% "scrimage-core" % "1.4.1",
    "com.sksamuel.scrimage" %% "scrimage-canvas" % "1.4.1",
    "com.sksamuel.scrimage" %% "scrimage-filters" % "1.4.1"
  )
}

Revolver.settings
