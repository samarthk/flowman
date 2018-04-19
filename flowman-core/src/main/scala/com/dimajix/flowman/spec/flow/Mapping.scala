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

package com.dimajix.flowman.spec.flow

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.apache.spark.sql.DataFrame
import org.apache.spark.storage.StorageLevel

import com.dimajix.flowman.execution.Context
import com.dimajix.flowman.execution.Executor
import com.dimajix.flowman.spec.TableIdentifier
import com.dimajix.flowman.spec.output.Output
import com.dimajix.flowman.spi.Scanner


object Mapping {
    def subtypes : Seq[(String,Class[_ <: Mapping])] = Scanner.mappings

}

/**
  * Interface class for specifying a transformation (mapping)
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = Array(
    new JsonSubTypes.Type(name = "aggregate", value = classOf[AggregateMapping]),
    new JsonSubTypes.Type(name = "alias", value = classOf[AliasMapping]),
    new JsonSubTypes.Type(name = "deduplicate", value = classOf[DeduplicateMapping]),
    new JsonSubTypes.Type(name = "read", value = classOf[InputMapping]),
    new JsonSubTypes.Type(name = "write", value = classOf[Output]),
    new JsonSubTypes.Type(name = "repartition", value = classOf[RepartitionMapping]),
    new JsonSubTypes.Type(name = "sort", value = classOf[SortMapping]),
    new JsonSubTypes.Type(name = "extend", value = classOf[ExtendMapping]),
    new JsonSubTypes.Type(name = "filter", value = classOf[FilterMapping]),
    new JsonSubTypes.Type(name = "project", value = classOf[ProjectMapping]),
    new JsonSubTypes.Type(name = "provided", value = classOf[ProvidedMapping]),
    new JsonSubTypes.Type(name = "select", value = classOf[SelectMapping]),
    new JsonSubTypes.Type(name = "sql", value = classOf[SqlMapping]),
    new JsonSubTypes.Type(name = "union", value = classOf[UnionMapping])
))
abstract class Mapping {
    /**
      * This method should return true, if the resulting dataframe should be broadcast for map-side joins
      * @param context
      * @return
      */
    def broadcast(implicit context: Context) : Boolean

    /**
      * Returns the desired storage level. Default should be StorageLevel.NONE
      * @param context
      * @return
      */
    def cache(implicit context: Context) : StorageLevel

    /**
      * Returns the dependencies (i.e. names of tables in the Dataflow model)
      * @param context
      * @return
      */
    def dependencies(implicit context:Context) : Array[TableIdentifier]

    /**
      * Executes this MappingType and returns a corresponding DataFrame
      *
      * @param executor
      * @param input
      * @return
      */
    def execute(executor:Executor, input:Map[TableIdentifier,DataFrame]) : DataFrame
}