name := "SpraySample"

organization := "ly.inoueyu"

version := "0.0.1"

scalaVersion := "2.10.2"
//scalaVersion := "2.9.1"

resolvers ++= Seq(
   "Sonatype release" at "http://oss.sonatype.org/content/repositories/releases"
  ,"Typesafe repo"    at "http://repo.typesafe.com/typesafe/releases/"
  ,"Spray repo"       at "http://repo.spray.io"
  ,"Spray nightlies"  at "http://nightlies.spray.io/"
)

libraryDependencies ++= Seq(
//   "org.specs2"     %  "specs2_2.10"             % "2.2"
   "ch.qos.logback"    %  "logback-core"           % "latest.integration"
  ,"ch.qos.logback"    %  "logback-classic"        % "latest.integration"
  ,"io.spray"          %% "spray-json"             % "1.2+"
  ,"io.spray"          %  "spray-http"             % "1.2+"
  ,"io.spray"          %  "spray-httpx"            % "1.2+"
  ,"io.spray"          %  "spray-can"              % "1.2+"
  ,"com.typesafe.akka" %% "akka-slf4j"             % "2.2.0-RC1"
  ,"com.typesafe.akka" %% "akka-actor"             % "2.2.0-RC1"
//  ,"com.scalapenos"    %  "riak-scala-client_2.10" % "0.8+"
  ,"com.basho.riak"    %  "riak-client"            % "1.4+"
)


initialCommands := "import ly.inoueyu.spraysample._"
