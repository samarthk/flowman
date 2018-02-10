package com.dimajix.dataflow.spec.runner

import scala.util.Failure
import scala.util.Success
import scala.util.Try

import org.slf4j.LoggerFactory

import com.dimajix.dataflow.execution.Context
import com.dimajix.dataflow.execution.Executor
import com.dimajix.dataflow.spec.Job
import com.dimajix.dataflow.spec.JobStatus


abstract class AbstractRunner extends Runner {
    private val logger = LoggerFactory.getLogger(classOf[JdbcLoggedRunner])

    /**
      * Executes a given job with the given executor. The runner will take care of
      * logging and monitoring
      *
      * @param executor
      * @param job
      * @return
      */
    def execute(executor: Executor, job:Job) : Boolean = {
        implicit val context = executor.context

        // Get Monitor
        val present = check(context)
        val token = start(context)

        val shutdownHook = new Thread() { override def run() : Unit = failure(context, token) }
        withShutdownHook(shutdownHook) {
            // First check if execution is really required
            if (present) {
                logger.info("Everything up to date, skipping execution")
                skipped(context, token)
                true
            }
            else {
                val result = Try {
                    job.execute(executor)
                }
                result match {
                    case Success(JobStatus.SUCCESS) =>
                        logger.info("Successfully finished execution of Job")
                        success(context, token)
                        true
                    case Success(JobStatus.FAILURE) =>
                        logger.error("Execution of Job failed")
                        failure(context, token)
                        false
                    case Success(JobStatus.ABORTED) =>
                        logger.error("Execution of Job aborted")
                        aborted(context, token)
                        false
                    case Success(JobStatus.SKIPPED) =>
                        logger.error("Execution of Job skipped")
                        skipped(context, token)
                        true
                    case Failure(e) =>
                        logger.error("Caught exception while executing job: {}", e.getMessage)
                        logger.error(e.getStackTrace.mkString("\n    at "))
                        false
                }
            }
        }
    }

    /**
      * Performs some check, if the run is required
      * @param context
      * @return
      */
    protected def check(context:Context) = false

    /**
      * Starts the run and returns a token, which can be anything
      *
      * @param context
      * @return
      */
    protected def start(context:Context) : Object = null

    /**
      * Marks a run as a success
      *
      * @param context
      * @param token
      */
    protected def success(context: Context, token:Object) : Unit = {}

    /**
      * Marks a run as a failure
      *
      * @param context
      * @param token
      */
    protected def failure(context: Context, token:Object) : Unit = {}

    /**
      * Marks a run as a failure
      *
      * @param context
      * @param token
      */
    protected def aborted(context: Context, token:Object) : Unit = {}

    /**
      * Marks a run as being skipped
      *
      * @param context
      * @param token
      */
    protected def skipped(context: Context, token:Object) : Unit = {}

    private def withShutdownHook[T](shutdownHook:Thread)(block: => T) : T = {
        Runtime.getRuntime.addShutdownHook(shutdownHook)
        val result = block
        Runtime.getRuntime.removeShutdownHook(shutdownHook)
        result
    }

}