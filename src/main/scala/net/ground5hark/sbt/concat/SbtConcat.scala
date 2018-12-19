package net.ground5hark.sbt.concat

import com.typesafe.sbt.web.{PathMapping, SbtWeb}
import sbt.Keys._
import sbt._
import com.typesafe.sbt.web.pipeline.Pipeline
import collection.mutable
import mutable.ListBuffer
import java.io.File

object Import {
  val concat = TaskKey[Pipeline.Stage]("web-concat", "Concatenates groups of web assets")

  object Concat {
    val groups = SettingKey[Seq[ConcatGroup]]("web-concat-groups", "List of ConcatGroup items")
    val parentDir = SettingKey[String]("web-concat-parent-dir", "Parent directory name in the target folder to write concatenated files to, default: \"\" (no parentDir)")
  }

  def group(o: AnyRef): Either[Seq[String], PathFinder] = o match {
    case o: Seq[_] => Left(o.asInstanceOf[Seq[String]])
    case o: PathFinder => Right(o)
    case u =>
      sys.error(s"Can't create a concat group from $u. Must provide either Seq[String] or a PathFinder for the concat group values")
  }
}

object NotHiddenFileFilter extends FileFilter {
  override def accept(f: File): Boolean = !HiddenFileFilter.accept(f)
}

object SbtConcat extends AutoPlugin {
  override def requires = SbtWeb

  override def trigger = AllRequirements

  val autoImport = Import

  import SbtWeb.autoImport._
  import WebKeys._
  import autoImport._
  import Concat._

  override def projectSettings = Seq(
    groups := ListBuffer.empty[ConcatGroup],
    includeFilter in concat := NotHiddenFileFilter,
    parentDir := "",
    concat := concatFiles.value
  )

  private def toFileNames(values: Seq[ConcatGroup],
                          srcDirs: Seq[File],
                          webModuleDirs: Seq[File]): Seq[(String, Iterable[String])] = values.map {
    case (groupName, fileNames) =>
      fileNames match {
        case Left(fileNamesSeq) => (groupName, fileNamesSeq)
        case Right(fileNamesPathFinder) =>
          val r = fileNamesPathFinder.pair(Path.relativeTo(srcDirs ++ webModuleDirs) | Path.basic)
          (groupName, r.map(_._2))
        case u => sys.error(s"Expected Seq[String] or PathFinder, but got $u")
      }
  }

  private def concatFiles: Def.Initialize[Task[Pipeline.Stage]] = Def.task {
    val logValue = streams.value.log
    mappings: Seq[PathMapping] =>
      val groupsValue = toFileNames(groups.value,
        (sourceDirectories in Assets).value,
        (webModuleDirectories in Assets).value)

      val groupMappings = if (groupsValue.nonEmpty) {
        logValue.info(s"Building ${groupsValue.size} concat group(s)")
        // Mutable map so we can pop entries we've already seen, in case there are similarly named files
        val reverseMapping = ReverseGroupMapping.get(groupsValue, logValue)
        val concatGroups = mutable.Map.empty[String, StringBuilder]
        val filteredMappings = mappings.filter(m => (includeFilter in concat).value.accept(m._1) && m._1.isFile)
        val targetDir = webTarget.value / parentDir.value

        groupsValue.foreach {
          case (groupName, fileNames) =>
            fileNames.foreach { fileName =>
              val separator = File.separatorChar
              def normalize(path: String) = path.replace('\\', separator).replace('/', separator)
              val mapping = filteredMappings.filter(entry => normalize(entry._2) == normalize(fileName))
              if (mapping.nonEmpty) {
                // TODO This is not as memory efficient as it could be, write to file instead
                concatGroups.getOrElseUpdate(groupName, new StringBuilder)
                  .append(s"\n/** $fileName **/\n")
                  .append(IO.read(mapping.head._1))
                reverseMapping.remove(fileName)
              } else logValue.warn(s"Unable to process $fileName. Not found.")
            }
        }

        concatGroups.map {
          case (groupName, concatenatedContents) =>
            val outputFile = targetDir / groupName
            IO.write(outputFile, concatenatedContents.toString())
            outputFile
        }.pair(Path.relativeTo(webTarget.value))
      } else {
        Seq.empty[PathMapping]
      }

      groupMappings ++ mappings
  }
}

private object ReverseGroupMapping {
  def get(groups: Seq[(String, Iterable[String])], logger: Logger): mutable.Map[String, String] = {
    val ret = mutable.Map.empty[String, String]
    groups.foreach {
      case (groupName, fileNames) => fileNames.foreach { fileName =>
        ret(fileName) = groupName
      }
    }
    ret
  }
}
