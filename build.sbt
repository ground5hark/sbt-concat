lazy val `sbt-concat` = project in file(".")

organization := "net.ground5hark.sbt"
name := "sbt-concat"
description := "sbt-web asset concatenation plugin"

enablePlugins(SbtWebBase)
addSbtWeb("1.4.4")

licenses := Seq("MIT" -> url("https://opensource.org/licenses/mit-license.php"))
