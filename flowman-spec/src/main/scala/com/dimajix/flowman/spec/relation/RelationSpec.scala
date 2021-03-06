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

package com.dimajix.flowman.spec.relation

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.util.StdConverter

import com.dimajix.common.TypeRegistry
import com.dimajix.flowman.annotation.RelationType
import com.dimajix.flowman.execution.Context
import com.dimajix.flowman.model.Relation
import com.dimajix.flowman.spec.NamedSpec
import com.dimajix.flowman.spi.ClassAnnotationHandler


object RelationSpec extends TypeRegistry[RelationSpec] {
    class NameResolver extends StdConverter[Map[String, RelationSpec], Map[String, RelationSpec]] {
        override def convert(value: Map[String, RelationSpec]): Map[String, RelationSpec] = {
            value.foreach(kv => kv._2.name = kv._1)
            value
        }
    }
}

/**
  * Interface class for declaring relations (for sources and sinks) as part of a model
  */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", visible=true)
@JsonSubTypes(value = Array(
    new JsonSubTypes.Type(name = "jdbc", value = classOf[JdbcRelationSpec]),
    new JsonSubTypes.Type(name = "table", value = classOf[HiveTableRelationSpec]),
    new JsonSubTypes.Type(name = "view", value = classOf[HiveViewRelationSpec]),
    new JsonSubTypes.Type(name = "generic", value = classOf[GenericRelationSpec]),
    new JsonSubTypes.Type(name = "hiveTable", value = classOf[HiveTableRelationSpec]),
    new JsonSubTypes.Type(name = "hiveUnionTable", value = classOf[HiveUnionTableRelationSpec]),
    new JsonSubTypes.Type(name = "hiveView", value = classOf[HiveViewRelationSpec]),
    new JsonSubTypes.Type(name = "file", value = classOf[FileRelationSpec]),
    new JsonSubTypes.Type(name = "local", value = classOf[LocalRelationSpec]),
    new JsonSubTypes.Type(name = "provided", value = classOf[ProvidedRelationSpec]),
    new JsonSubTypes.Type(name = "template", value = classOf[TemplateRelationSpec]),
    new JsonSubTypes.Type(name = "null", value = classOf[NullRelationSpec])
))
abstract class RelationSpec extends NamedSpec[Relation] {
    @JsonProperty(value="description", required = false) private var description: Option[String] = None
    @JsonProperty(value="options", required=false) private var options:Map[String,String] = Map()

    override def instantiate(context:Context) : Relation

    /**
      * Returns a set of common properties
      * @param context
      * @return
      */
    override protected def instanceProperties(context:Context) : Relation.Properties = {
        require(context != null)
        Relation.Properties(
            context,
            context.namespace,
            context.project,
            name,
            kind,
            context.evaluate(labels),
            description.map(context.evaluate),
            context.evaluate(options)
        )
    }
}



class RelationSpecAnnotationHandler extends ClassAnnotationHandler {
    override def annotation: Class[_] = classOf[RelationType]

    override def register(clazz: Class[_]): Unit =
        RelationSpec.register(clazz.getAnnotation(classOf[RelationType]).kind(), clazz.asInstanceOf[Class[_ <: RelationSpec]])
}
