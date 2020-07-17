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

package com.dimajix.flowman.tools.exec.info

import scala.collection.JavaConverters._

import com.dimajix.flowman.execution.Session
import com.dimajix.flowman.model.Project
import com.dimajix.flowman.tools.ToolConfig
import com.dimajix.flowman.tools.exec.Command


class InfoCommand extends Command {
    override def execute(project:Project, session: Session): Boolean = {
        super.execute(project, session)

        // Create project specific executor
        val context = session.getContext(project)

        println(s"Flowman home directory: ${ToolConfig.homeDirectory.getOrElse("")}")
        println(s"Flowman config directory: ${ToolConfig.confDirectory.getOrElse("")}")
        println(s"Flowman plugin directory: ${ToolConfig.pluginDirectory.getOrElse("")}")

        println("Namespace:")
        session.namespace.foreach { ns =>
            println(s"    name: ${ns.name}")
            println(s"    plugins: ${ns.plugins.mkString(",")}")
        }

        println("Project:")
        println(s"    name: ${project.name}")
        println(s"    version: ${project.version.getOrElse("")}")
        println(s"    basedir: ${project.basedir.getOrElse("")}")
        println(s"    filename: ${project.filename.map(_.toString).getOrElse("")}")

        println("Environment:")
        context.environment
            .toSeq
            .sortBy(_._1)
            .foreach{ case(k,v) => println(s"    $k=$v") }

        println("Configuration:")
        context.config
            .toSeq
            .sortBy(_._1)
            .foreach{ case(k,v) => println(s"    $k=$v") }

        true
    }

}