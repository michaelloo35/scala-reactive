name := "scala-reactive"

version := "1.1"

scalaVersion := "2.12.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.5.6",
  "com.typesafe.akka" % "akka-testkit_2.12" % "2.5.6",
  "org.scalatest" % "scalatest_2.12" % "3.0.4" % "test",
  "com.typesafe.akka" %% "akka-persistence" % "2.5.17",
  "org.iq80.leveldb" % "leveldb" % "0.9",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8")