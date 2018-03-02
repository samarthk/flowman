package com.dimajix.flowman

import org.apache.spark.sql.SparkSession
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite
import org.scalatest.mockito.MockitoSugar

/**
  * Created by kaya on 18.01.17.
  */
trait LocalSparkSession extends BeforeAndAfterAll  with MockitoSugar { this:Suite =>
    var spark: SparkSession = _

    override def beforeAll() : Unit = {
        spark = SparkSession.builder()
            .master("local[2]")
            .config("spark.ui.enabled", "false")
            .config("spark.sql.warehouse.dir", "file:///tmp/spark-warehouse")
            .config("spark.sql.shuffle.partitions", "8")
            //.enableHiveSupport()
            .getOrCreate()
        spark.sparkContext.setLogLevel("WARN")
    }
    override def afterAll() : Unit = {
        if (spark != null) {
            spark.stop()
            spark = null
        }
    }
}
