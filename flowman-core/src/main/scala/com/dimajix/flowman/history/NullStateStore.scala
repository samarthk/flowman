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


class NullStateStore extends StateStore {
    /**
      * Returns the state of a job
      * @param job
      * @return
      */
    def getBundleState(job:BundleInstance, phase:Phase) : Option[BundleState] = None

    /**
      * Starts the run and returns a token, which can be anything
      * @param job
      * @return
      */
    override def startBundle(job:BundleInstance, phase:Phase) : BundleToken = null

    /**
      * Sets the status of a job after it has been started
      * @param token
      * @param status
      */
    override def finishBundle(token:BundleToken, status:Status) : Unit = {}

    /**
      * Returns the state of a target
      * @param target
      * @return
      */
    override def getTargetState(target:TargetInstance, phase:Phase) : Option[TargetState] = None

    /**
      * Performs some checkTarget, if the run is required
      * @param target
      * @return
      */
    override def checkTarget(target:TargetInstance, phase:Phase) : Boolean = false

    /**
      * Starts the run and returns a token, which can be anything
      * @param target
      * @return
      */
    override def startTarget(target:TargetInstance, phase:Phase, parent:Option[BundleToken]) : TargetToken = null

    /**
      * Sets the status of a target after it has been started
      * @param token
      * @param status
      */
    override def finishTarget(token:TargetToken, status:Status) : Unit = {}

    /**
      * Returns a list of job matching the query criteria
      * @param query
      * @param limit
      * @param offset
      * @return
      */
    override def findBundles(query:BundleQuery, order:Seq[BundleOrder], limit:Int, offset:Int) : Seq[BundleState] = Seq()

    /**
      * Returns a list of job matching the query criteria
      * @param query
      * @param limit
      * @param offset
      * @return
      */
    override def findTargets(query:TargetQuery, order:Seq[TargetOrder], limit:Int, offset:Int) : Seq[TargetState] = Seq()
}
