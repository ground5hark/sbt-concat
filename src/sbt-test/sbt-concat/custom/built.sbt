import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.Import.WebKeys._
organization := "net.ground5hark.sbt"

name := "sbt-concat-test"

version := "0.1.2"

scalaVersion := "2.10.4"

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

Concat.root := ""

Concat.groups := Seq(
  "css/style-group.css" -> Seq("style1.css", "style2.css"),
  "js/script-group.js" -> Seq("file1.js", "file2.js")
)

pipelineStages := Seq(concat)

val verifyConcatContents = taskKey[Unit]("Verify contents of concatenation groups")

verifyConcatContents := {
  val pub = (public in Assets).value
  val concatCss = (pub / "" ** "*style-group.css").get
  val concatJs = (pub / "" ** "*script-group.js").get
  def assertEqual(f: File, contains: Seq[String]): Unit = {
    val contents = IO.read(f)
    contains.foreach { s =>
      if (!contents.contains(s))
        sys.error(s"${f.getName}: Expected `$s` to be in `$contents`")
    }
  }
  val containsCss = Seq("background-color: #000;", "/** style2.css **/",
                        "/** style1.css **/", "font-face: Arial, sans-serif;")
  assertEqual(concatCss.head, containsCss)
  val containsJs = Seq("file1Callback = function() {", "/** file2.js **/",
                       "console.log('file2 - callback called');")
  assertEqual(concatJs.head, containsJs)
}
