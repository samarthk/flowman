/*
 * Copyright 2018-2019 Kaya Kupferschmidt
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

package com.dimajix.flowman.spec.model

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.StructType

import com.dimajix.flowman.execution.Context
import com.dimajix.flowman.execution.Executor
import com.dimajix.flowman.spec.ResourceIdentifier
import com.dimajix.flowman.spec.schema.PartitionField
import com.dimajix.flowman.spec.schema.PartitionSchema
import com.dimajix.flowman.spec.schema.Schema
import com.dimajix.flowman.types.FieldValue
import com.dimajix.flowman.types.SingleValue


class NullRelation(
    override val instanceProperties:Relation.Properties,
    override val schema:Schema = null,
    override val partitions: Seq[PartitionField] = Seq()
) extends BaseRelation with SchemaRelation with PartitionedRelation {
    /**
      * Returns the list of all resources which will be created by this relation.
      *
      * @return
      */
    override def provides : Seq[ResourceIdentifier] = Seq()

    /**
      * Returns the list of all resources which will be required by this relation
      *
      * @return
      */
    override def requires : Seq[ResourceIdentifier] = Seq()

    /**
      * Returns the list of all resources which will be required by this relation for reading a specific partition.
      * The list will be specifically  created for a specific partition, or for the full relation (when the partition
      * is empty)
      *
      * @param partitions
      * @return
      */
    override def resources(partitions: Map[String, FieldValue]): Seq[ResourceIdentifier] = Seq()

    /**
      * Reads data from the relation, possibly from specific partitions
      *
      * @param executor
      * @param schema
      * @param partitions
      * @return
      */
    override def read(executor:Executor, schema:Option[StructType], partitions:Map[String,FieldValue] = Map()) : DataFrame = {
        require(executor != null)
        require(schema != null)
        require(partitions != null)

        if (inputSchema == null && schema.isEmpty)
            throw new IllegalArgumentException("Null relation either needs own schema or a desired input schema")

        // Add partitions values as columns
        val fullSchema = Option(inputSchema).map(s => StructType(s.fields ++ this.partitions.map(_.sparkField)))
        val readSchema = schema.orElse(fullSchema).get
        val rdd = executor.spark.sparkContext.emptyRDD[Row]
        executor.spark.createDataFrame(rdd, readSchema)
    }

    /**
      * Writes data into the relation, possibly into a specific partition
      *
      * @param executor
      * @param df
      * @param partition
      */
    override def write(executor:Executor, df:DataFrame, partition:Map[String,SingleValue], mode:String) : Unit = {
        require(executor != null)
        require(partition != null)
    }

    override def truncate(executor: Executor, partitions: Map[String, FieldValue]): Unit = {
        require(executor != null)
    }

    /**
      * Returns true if the relation already exists, otherwise it needs to be created prior usage
      * @param executor
      * @return
      */
    override def exists(executor:Executor) : Boolean = true

    override def create(executor: Executor, ifNotExists:Boolean=false): Unit = {
        require(executor != null)
    }
    override def destroy(executor: Executor, ifExists:Boolean=false): Unit = {
        require(executor != null)
    }
    override def migrate(executor: Executor): Unit = {
        require(executor != null)
    }
}



class NullRelationSpec extends RelationSpec with SchemaRelationSpec with PartitionedRelationSpec {
    /**
      * Creates the instance of the specified Relation with all variable interpolation being performed
      * @param context
      * @return
      */
    override def instantiate(context: Context): NullRelation = {
        new NullRelation(
            instanceProperties(context),
            if (schema != null) schema.instantiate(context) else null,
            partitions.map(_.instantiate(context))
        )
    }
}
