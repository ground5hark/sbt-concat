sbtPlugin := true

organization := "net.ground5hark.sbt"

name := "sbt-concat"

version := "0.1.9"

scalaVersion := "2.10.4"

resolvers ++= Seq(
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("sbt snapshot plugins", url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
  Resolver.sonatypeRepo("snapshots"),
  "Typesafe Snapshots Repository" at "http://repo.typesafe.com/typesafe/snapshots/",
  Resolver.mavenLocal
)

addSbtPlugin("com.typesafe.sbt" %% "sbt-web" % "1.3.0")

publishMavenStyle := true

scriptedSettings

scriptedLaunchOpts ++= Seq(
  "-Xmx1024M",
  "-XX:MaxPermSize=256M",
  s"-Dproject.version=${version.value}"
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  Some(if (isSnapshot.value) {
    "snapshots" at nexus + "content/repositories/snapshots"
  } else {
    "releases" at nexus + "service/local/staging/deploy/maven2"
  })
}

pomExtra := (
  <url>https://github.com/ground5hark/sbt-concat</url>
  <licenses>
    <license>
      <name>MIT</name>
      <url>http://opensource.org/licenses/MIT</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>git@github.com:ground5hark/sbt-concat.git</url>
    <connection>scm:git:git@github.com:ground5hark/sbt-concat.git</connection>
  </scm>
  <developers>
    <developer>
      <id>ground5hark</id>
      <name>John Bernardo</name>
      <url>https://github.com/ground5hark</url>
    </developer>
  </developers>)
