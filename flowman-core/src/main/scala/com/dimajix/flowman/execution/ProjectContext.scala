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

package com.dimajix.flowman.execution

import org.slf4j.LoggerFactory

import com.dimajix.flowman.spec.Connection
import com.dimajix.flowman.spec.ConnectionIdentifier
import com.dimajix.flowman.spec.JobIdentifier
import com.dimajix.flowman.spec.OutputIdentifier
import com.dimajix.flowman.spec.Profile
import com.dimajix.flowman.spec.Project
import com.dimajix.flowman.spec.RelationIdentifier
import com.dimajix.flowman.spec.TableIdentifier
import com.dimajix.flowman.spec.flow.Mapping
import com.dimajix.flowman.spec.model.Relation
import com.dimajix.flowman.spec.output.Output
import com.dimajix.flowman.spec.runner.Runner
import com.dimajix.flowman.spec.task.Job


class ProjectContext(parent:RootContext, _project:Project) extends AbstractContext {
    override protected val logger = LoggerFactory.getLogger(classOf[ProjectContext])

    updateFrom(parent)

    def project : Project = _project

    /**
      * Returns the appropriate runner for this project.
      *
      * @return
      */
    override def runner : Runner = {
        if (_project.runner != null)
            _project.runner
        else
            parent.runner
    }

    /**
      * Returns a specific named Transform. The Transform can either be inside this Contexts project or in a different
      * project within the same namespace
      *
      * @param identifier
      * @return
      */
    override def getMapping(identifier: TableIdentifier): Mapping = {
        if (identifier.project.forall(_ == _project.name))
            _project.mappings.getOrElse(identifier.name, throw new NoSuchElementException(s"Mapping '$identifier' not found in project ${_project.name}"))
        else
            parent.getMapping(identifier)
    }

    /**
      * Returns a specific named RelationType. The RelationType can either be inside this Contexts project or in a different
      * project within the same namespace
      *
      * @param identifier
      * @return
      */
    override def getRelation(identifier: RelationIdentifier): Relation = {
        if (identifier.project.forall(_ == _project.name))
            _project.relations.getOrElse(identifier.name, throw new NoSuchElementException(s"Relation '$identifier' not found in project ${_project.name}"))
        else
            parent.getRelation(identifier)
    }

    /**
      * Returns a specific named OutputType. The OutputType can either be inside this Contexts project or in a different
      * project within the same namespace
      *
      * @param identifier
      * @return
      */
    override def getOutput(identifier: OutputIdentifier): Output = {
        if (identifier.project.forall(_ == _project.name))
            _project.outputs.getOrElse(identifier.name, throw new NoSuchElementException(s"Output '$identifier' not found in project ${_project.name}"))
        else
            parent.getOutput(identifier)
    }

    /**
      * Try to retrieve the specified database. Performs lookups in parent context if required
      *
      * @param identifier
      * @return
      */
    override def getConnection(identifier:ConnectionIdentifier) : Connection = {
        if (identifier.project.forall(_ == _project.name)) {
            databases.getOrElse(identifier.name, throw new NoSuchElementException(s"Database '$identifier' not found in project ${_project.name}"))
        }
        else {
            parent.getConnection(identifier)
        }
    }

    /**
      * Returns a specific named Job. The JobType can either be inside this Contexts project or in a different
      * project within the same namespace
      *
      * @param identifier
      * @return
      */
    override def getJob(identifier: JobIdentifier): Job = {
        if (identifier.project.forall(_ == _project.name))
            _project.jobs.getOrElse(identifier.name, throw new NoSuchElementException(s"Job $identifier not found in project ${_project.name}"))
        else
            parent.getJob(identifier)
    }

    /**
      * Creates a new chained context with additional environment variables
      * @param env
      * @return
      */
    override def withEnvironment(env:Map[String,String]) : Context = {
        withEnvironment(env.toSeq)
    }
    override def withEnvironment(env:Seq[(String,String)]) : Context = {
        setEnvironment(env, SettingLevel.PROJECT_SETTING)
        this
    }

    /**
      * Creates a new chained context with additional Spark configuration variables
      * @param env
      * @return
      */
    override def withConfig(env:Map[String,String]) : Context = {
        withConfig(env.toSeq)
    }
    override def withConfig(env:Seq[(String,String)]) : Context = {
        setConfig(env, SettingLevel.PROJECT_SETTING)
        this
    }

    /**
      * Creates a new chained context with additional properties from a profile
      * @param profile
      * @return
      */
    override def withProfile(profile:Profile) : Context = {
        setConfig(profile.config, SettingLevel.PROJECT_PROFILE)
        setEnvironment(profile.environment, SettingLevel.PROJECT_PROFILE)
        setDatabases(profile.databases, SettingLevel.PROJECT_PROFILE)
        this
    }
}