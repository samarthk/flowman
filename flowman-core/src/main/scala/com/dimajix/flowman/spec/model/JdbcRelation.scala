package com.dimajix.flowman.spec.model

import java.util.Properties

import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StructType
import org.slf4j.LoggerFactory

import com.dimajix.flowman.execution.Context
import com.dimajix.flowman.execution.Executor
import com.dimajix.flowman.spec.ConnectionIdentifier
import com.dimajix.flowman.spec.schema.SingleValue
import com.dimajix.flowman.spec.schema.FieldValue
import com.dimajix.flowman.util.SchemaUtils


class JdbcRelation extends BaseRelation {
    private val logger = LoggerFactory.getLogger(classOf[JdbcRelation])

    @JsonProperty(value="connection") private var _connection: String = _
    @JsonProperty(value="properties") private var _properties:Map[String,String] = Map()
    @JsonProperty(value="database") private var _database: String = _
    @JsonProperty(value="table") private var _table: String = _

    def connection(implicit context: Context) : ConnectionIdentifier = ConnectionIdentifier.parse(context.evaluate(_connection))
    def properties(implicit context: Context) : Map[String,String] = _properties.mapValues(context.evaluate)
    def database(implicit context:Context) : String = context.evaluate(_database)
    def table(implicit context:Context) : String = context.evaluate(_table)

    /**
      * Reads the configured table from the source
      * @param executor
      * @param schema
      * @return
      */
    override def read(executor:Executor, schema:StructType, partitions:Map[String,FieldValue] = Map()) : DataFrame = {
        implicit val context = executor.context

        val tableName = database + "." + table

        logger.info(s"Reading data from JDBC source $tableName in database $connection")

        // Get Connection
        val (db,props) = createProperties(context)

        // Connect to database
        val reader = this.reader(executor)
        val df = reader.jdbc(db.url, tableName, props)
        SchemaUtils.applySchema(df, schema)
    }

    /**
      * Writes a given DataFrame into a JDBC connection
      *
      * @param executor
      * @param df
      * @param partition
      * @param mode
      */
    override def write(executor:Executor, df:DataFrame, partition:Map[String,SingleValue], mode:String) : Unit = {
        implicit val context = executor.context

        val tableName = database + "." + table

        logger.info(s"Writing data to JDBC source $tableName in database $connection")

        val writer = df.write
                .options(options)

        val (db,props) = createProperties(context)
        writer.jdbc(db.url, tableName, props)
    }

    override def create(executor:Executor) : Unit = ???
    override def destroy(executor:Executor) : Unit = ???
    override def migrate(executor:Executor) : Unit = ???

    private def createProperties(implicit context: Context) = {
        // Get Connection
        val db = context.getConnection(connection)
        val props = new Properties()
        props.setProperty("user", db.username)
        props.setProperty("password", db.password)
        props.setProperty("driver", db.driver)

        db.properties.foreach(kv => props.setProperty(kv._1, kv._2))
        properties.foreach(kv => props.setProperty(kv._1, kv._2))

        logger.info("Connecting to jdbc source at {}", db.url)

        (db,props)
    }
}
