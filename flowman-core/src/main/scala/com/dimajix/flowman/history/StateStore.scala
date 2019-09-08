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

package com.dimajix.flowman.history

import com.dimajix.flowman.execution.Phase
import com.dimajix.flowman.execution.Status
import com.dimajix.flowman.spec.target.BatchInstance
import com.dimajix.flowman.spec.target.TargetInstance


abstract class BatchToken

abstract class TargetToken


abstract class StateStore {
    /**
      * Returns the state of a job, or None if no information is available
      * @param batch
      * @return
      */
    def getBatchState(batch:BatchInstance, phase:Phase) : Option[BatchState]

    /**
      * Starts the run and returns a token, which can be anything
      * @param batch
      * @return
      */
    def startBatch(batch:BatchInstance, phase:Phase) : BatchToken

    /**
      * Sets the status of a job after it has been started
      * @param token The token returned by startJob
      * @param status
      */
    def finishBatch(token:BatchToken, status:Status) : Unit

    /**
      * Returns the state of a specific target on its last run, or None if no information is available
      * @param target
      * @return
      */
    def getTargetState(target:TargetInstance, phase:Phase) : Option[TargetState]

    /**
      * Performs some checkJob, if the run is required
      * @param target
      * @return
      */
    def checkTarget(target:TargetInstance, phase:Phase) : Boolean

    /**
      * Starts the run and returns a token, which can be anything
      * @param target
      * @return
      */
    def startTarget(target:TargetInstance, phase:Phase, parent:Option[BatchToken]) : TargetToken

    /**
      * Sets the status of a job after it has been started
      * @param token The token returned by startJob
      * @param status
      */
    def finishTarget(token:TargetToken, status:Status) : Unit

    /**
      * Returns a list of job matching the query criteria
      * @param query
      * @param limit
      * @param offset
      * @return
      */
    def findBundles(query:BatchQuery, order:Seq[BatchOrder], limit:Int, offset:Int) : Seq[BatchState]

    /**
      * Returns a list of job matching the query criteria
      * @param query
      * @param limit
      * @param offset
      * @return
      */
    def findTargets(query:TargetQuery, order:Seq[TargetOrder], limit:Int, offset:Int) : Seq[TargetState]
}
