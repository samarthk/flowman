/*
 * Copyright 2018 Kaya Kupferschmidt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dimajix.flowman.hadoop

import java.io.FileNotFoundException
import java.io.StringWriter

import scala.math.Ordering

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FileStatus
import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.{FileSystem => HadoopFileSystem}
import org.apache.spark.sql.SparkSession
import org.apache.velocity.VelocityContext
import org.slf4j.LoggerFactory

import com.dimajix.flowman.catalog.PartitionSpec
import com.dimajix.flowman.templating.Velocity


object FileCollector {
    class Builder(hadoopConf:Configuration) {
        private var _pattern:Option[String] = None
        private var _path:Path = _
        private var _defaults:Map[String,Any] = Map()
        private var _context:VelocityContext = _

        def this(spark:SparkSession) = {
            this(spark.sparkContext.hadoopConfiguration)
        }

        /**
         * Sets the pattern which will be used for generating directory and/or file names from partition information
         *
         * @param pattern
         * @return
         */
        def pattern(pattern:String) : Builder = {
            require(pattern != null)
            this._pattern = Some(pattern)
            this
        }
        def pattern(pattern:Option[String]) : Builder = {
            require(pattern != null)
            this._pattern = pattern
            this
        }

        /**
         * Set default values for partitions not specified in `resolve`
         * @param defaults
         * @return
         */
        def defaults(defaults:Map[String,Any]) : Builder = {
            this._defaults = defaults
            this
        }

        def context(context:VelocityContext) : Builder = {
            this._context = context
            this
        }

        /**
         * Sets the base directory which is used for retrieving the file system. The base location must not contain
         * any pattern variable
         *
         * @param path
         * @return
         */
        def path(path:Path) : Builder = {
            require(path != null)
            this._path = path
            this
        }

        /**
         * Creates a FileCollector with the specified configuration
         * @return
         */
        def build() : FileCollector = {
            require(_path != null)
            new FileCollector(
                hadoopConf,
                _path,
                _pattern,
                _defaults
            )
        }
    }

    def builder(hadoopConf:Configuration) : Builder = new Builder(hadoopConf)
}


/**
  * Helper class for collecting files from a file system, which also support pattern substitution
  *
  * @param hadoopConf
  */
case class FileCollector(
    hadoopConf:Configuration,
    path:Path,
    pattern:Option[String],
    defaults:Map[String,Any]
) {
    private val logger = LoggerFactory.getLogger(classOf[FileCollector])

    private implicit val fileStatusOrder = new Ordering[FileStatus] {
        def compare(x: FileStatus, y: FileStatus): Int = x.getPath compareTo y.getPath
    }

    private lazy val templateEngine = Velocity.newEngine()
    private lazy val templateContext = Velocity.newContext()

    def resolve() : Path = {
        resolve(Seq())
    }
    def resolve(partition:PartitionSpec) : Path = {
        resolve(partition.toSeq)
    }
    def resolve(partition:Map[String,Any]) : Path = {
        resolve(partition.toSeq)
    }
    def resolve(partition:Seq[(String,Any)]) : Path = {
        if (pattern.exists(_.nonEmpty)) {
            val context = new VelocityContext(templateContext)
            val partitionValues = defaults ++ partition.toMap
            partitionValues.foreach(kv => context.put(kv._1, kv._2))
            val output = new StringWriter()
            templateEngine.evaluate(context, output, "FileCollector", pattern.get)
            new Path(path, output.getBuffer.toString)
        }
        else {
            path
        }
    }

    /**
      * Collects files from the given partitions
      *
      * @param partitions
      * @return
      */
    def collect(partitions:Iterable[PartitionSpec]) : Iterable[Path] = {
        requirePathAndPattern()

        logger.debug(s"Collecting files in location ${path} with pattern '${pattern.get}'")
        flatMap(partitions)(collectPath)
    }

    def collect(partition:PartitionSpec) : Seq[Path] = {
        requirePathAndPattern()

        logger.debug(s"Collecting files in location ${path} for partition ${partition.spec} using pattern '${pattern.get}'")
        map(partition)(collectPath)
    }

    /**
      * Collects files from the configured directory. Does not perform partition resolution
      *
      * @return
      */
    def collect() : Seq[Path] = {
        logger.debug(s"Collecting files in location ${path}, for all partitions ignoring any pattern")
        map(collectPath)
    }

    /**
      * Deletes all files and directories from the given partitions
      *
      * @param partitions
      * @return
      */
    def delete(partitions:Iterable[PartitionSpec]) : Unit = {
        requirePathAndPattern()

        logger.info(s"Deleting files in location ${path} with pattern '${pattern.get}'")
        foreach(partitions)(deletePath)
    }

    /**
      * Deletes files from the configured directory. Does not perform partition resolution
      *
      * @return
      */
    def delete() : Unit = {
        logger.info(s"Deleting files in location ${path}, for all partitions ignoring any pattern")
        foreach(deletePath _)
    }

    /**
     * Deletes files from the configured directory. Does not perform partition resolution
     *
     * @return
     */
    def truncate() : Unit = {
        logger.info(s"Deleting files in location ${path}, for all partitions ignoring any pattern")
        foreach(truncatePath _)
    }

    /**
      * FlatMaps all partitions using the given function
      * @param partitions
      * @param fn
      * @tparam T
      * @return
      */
    def flatMap[T](partitions:Iterable[PartitionSpec])(fn:(HadoopFileSystem,Path) => Iterable[T]) : Iterable[T] = {
        requirePathAndPattern()

        val fs = path.getFileSystem(hadoopConf)
        partitions.flatMap(p => fn(fs, resolve(p)))
    }

    /**
      * Maps all partitions using the given function
      * @param partitions
      * @param fn
      * @tparam T
      * @return
      */
    def map[T](partitions:Iterable[PartitionSpec])(fn:(HadoopFileSystem,Path) => T) : Iterable[T] = {
        requirePathAndPattern()

        val fs = path.getFileSystem(hadoopConf)
        partitions.map(p => fn(fs, resolve(p)))
    }

    def map[T](partition:PartitionSpec)(fn:(HadoopFileSystem,Path) => T) : T = {
        requirePathAndPattern()

        val fs = path.getFileSystem(hadoopConf)
        fn(fs, resolve(partition))
    }

    def map[T](fn:(HadoopFileSystem,Path) => T) : T = {
        requirePath()

        val fs = path.getFileSystem(hadoopConf)
        fn(fs,path)
    }

    def foreach(partitions:Iterable[PartitionSpec])(fn:(HadoopFileSystem,Path) => Unit) : Unit = {
        map(partitions)(fn)
    }

    def foreach(fn:(HadoopFileSystem,Path) => Unit) : Unit = {
        map(fn)
    }

    private def truncatePath(fs:HadoopFileSystem, path:Path) : Unit = {
        val isDirectory = try fs.getFileStatus(path).isDirectory catch { case _:FileNotFoundException => false }

        if (isDirectory) {
            logger.info(s"Truncating directory '$path'")
            val files = try fs.listStatus(path) catch { case _:FileNotFoundException => null }
            if (files != null)
                files.foreach(f => fs.delete(f.getPath, true))
        }
        else {
            deletePath(fs, path)
        }
    }

    private def deletePath(fs:HadoopFileSystem, path:Path) : Unit = {
        if (!isGlobPath(path)) {
          logger.info(s"Deleting directory '$path'")
          fs.delete(path, true)
        }
        else {
          logger.info(s"Deleting file(s) '$path'")
          val files = try fs.globStatus(path) catch { case _:FileNotFoundException => null }
          if (files != null)
            files.foreach(f => fs.delete(f.getPath, true))
        }
    }


    private def collectPath(fs:HadoopFileSystem, path:Path) : Seq[Path] = {
        if (isGlobPath(path)) {
            globPath(fs, path)
        }
        else {
            if (fs.exists(path))
                Seq(path)
            else
                Seq()
        }
    }

    private def isGlobPath(pattern: Path): Boolean = {
        pattern.toString.exists("{}[]*?\\".toSet.contains)
    }
    private def globPath(fs:HadoopFileSystem, pattern: Path): Seq[Path] = {
        Option(fs.globStatus(pattern)).map { statuses =>
            statuses.map(_.getPath.makeQualified(fs.getUri, fs.getWorkingDirectory)).toSeq
        }.getOrElse(Seq.empty[Path])
    }

    private def requirePathAndPattern() : Unit = {
        if (path.toString.isEmpty)
            throw new IllegalArgumentException("path needs to be defined for collecting partitioned files")
        if (!pattern.exists(_.nonEmpty))
            throw new IllegalArgumentException("pattern needs to be defined for collecting partitioned files")
    }

    private def requirePath() : Unit = {
        if (path.toString.isEmpty)
            throw new IllegalArgumentException("path needs to be defined for collecting files")
    }
}
