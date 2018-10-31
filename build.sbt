name := "scala-reactive"

version := "1.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.17",
  "com.typesafe.akka" %% "akka-testkit" % "2.5.17" % "test",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.apache.commons" % "commons-collections4" % "4.2")
