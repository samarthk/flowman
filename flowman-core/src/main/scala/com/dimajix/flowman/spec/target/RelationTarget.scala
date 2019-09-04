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

package com.dimajix.flowman.spec.target

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.spark.sql.DataFrame
import org.slf4j.LoggerFactory

import com.dimajix.flowman.execution.Context
import com.dimajix.flowman.execution.Executor
import com.dimajix.flowman.spec.MappingIdentifier
import com.dimajix.flowman.spec.MappingOutputIdentifier
import com.dimajix.flowman.spec.RelationIdentifier
import com.dimajix.flowman.spec.model.Relation
import com.dimajix.flowman.history.TargetInstance
import com.dimajix.flowman.types.SingleValue


case class RelationTarget(
    instanceProperties: Target.Properties,
    mapping:MappingOutputIdentifier,
    relation: RelationIdentifier,
    mode: String,
    partition: Map[String,String],
    parallelism: Int,
    rebalance: Boolean
) extends BaseTarget {
    private val logger = LoggerFactory.getLogger(classOf[RelationTarget])

    /**
      * Returns an instance representing this target with the context
      * @return
      */
    override def instance : TargetInstance = {
        TargetInstance(
            Option(namespace).map(_.name).getOrElse(""),
            Option(project).map(_.name).getOrElse(""),
            name,
            partition
        )
    }

    /**
      * Builds the target using the given input tables
      *
      * @param executor
      */
    override def build(executor:Executor) : Unit = {
        val partition = this.partition.mapValues(v => SingleValue(v))

        logger.info(s"Writing mapping '${this.mapping}' to relation '$relation' into partition $partition")
        val mapping = context.getMapping(this.mapping.mapping)
        val dfIn = executor.instantiate(mapping, this.mapping.output)
        val table = if (rebalance)
            dfIn.repartition(parallelism)
        else
            dfIn.coalesce(parallelism)

        val rel = context.getRelation(relation)
        rel.write(executor, table, partition, mode)
    }

    /**
      * Cleans the target. This will remove any data in the target for the current partition
      * @param executor
      */
    override def truncate(executor: Executor): Unit = {
        val partition = this.partition.mapValues(v => SingleValue(v))

        logger.info(s"Cleaning partition $partition of relation '$relation'")
        val rel = context.getRelation(relation)
        rel.clean(executor, partition)
    }
}




class RelationTargetSpec extends TargetSpec {
    @JsonProperty(value="input", required=true) private var input:String = _
    @JsonProperty(value="relation", required=true) private var relation:String = _
    @JsonProperty(value="mode", required=false) private var mode:String = "overwrite"
    @JsonProperty(value="partition", required=false) private var partition:Map[String,String] = Map()
    @JsonProperty(value="parallelism", required=false) private var parallelism:String = "16"
    @JsonProperty(value="rebalance", required=false) private var rebalance:String = "false"

    override def instantiate(context: Context): Target = {
        RelationTarget(
            instanceProperties(context),
            MappingOutputIdentifier.parse(context.evaluate(input)),
            RelationIdentifier.parse(context.evaluate(relation)),
            context.evaluate(mode),
            context.evaluate(partition),
            context.evaluate(parallelism).toInt,
            context.evaluate(rebalance).toBoolean
        )
    }
}
