package com.dimajix.flowman.spec.schema

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.spark.sql.types.DataType
import org.apache.spark.sql.types.StructField

import com.dimajix.flowman.execution.Context

class PartitionField {
    @JsonProperty(value="name", required = true) private var _name: String = _
    @JsonProperty(value="type", required = false) private var _type: FieldType = _
    @JsonProperty(value="description", required = false) private var _description: String = _
    @JsonProperty(value="granularity", required = false) private var _granularity: String = _

    def name : String = _name
    def ftype : FieldType = _type
    def description(implicit context: Context) : String = context.evaluate(_description)
    def granularity(implicit context: Context) : String = context.evaluate(_granularity)

    def sparkType : DataType = _type.sparkType
    def sparkField : StructField = StructField(name, sparkType, false)

    def interpolate(value: FieldValue)(implicit context: Context) : Iterable[Any] = _type.interpolate(value, granularity)
}