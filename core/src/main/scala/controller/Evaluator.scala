/** Copyright 2014 TappingStone, Inc.
  *
  * Licensed under the Apache License, Version 2.0 (the "License");
  * you may not use this file except in compliance with the License.
  * You may obtain a copy of the License at
  *
  *     http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */

package io.prediction.controller

import io.prediction.core.BaseEvaluator

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.reflect._
import scala.reflect.runtime.universe._

/** Base class of evaluator.
  *
  * Evaluator compare predicted result with actual known values and produce numerical
  * comparisons.
  *
  * @tparam EP Evaluator parameters class.
  * @tparam PD Prepared data class.
  * @tparam Q Input query class.
  * @tparam P Output prediction class.
  * @tparam A Actual value class.
  * @tparam EU Evaluation unit class.
  * @tparam ES Evaluation set class.
  * @tparam ER Evaluation result class.
  * @group Evaluator
  */
abstract class Evaluator[
    EP <: Params : ClassTag, DP, Q, P, A, EU, ES, ER <: AnyRef]
  extends BaseEvaluator[EP, DP, Q, P, A, EU, ES, ER] {

  def evaluateUnitBase(input: (Q, P, A)): EU = {
    evaluateUnit(input._1, input._2, input._3)
  }

  /** Implement this method to calculate a unit of evaluation, comparing a pair
    * of predicted and actual values.
    *
    * @param query Input query that produced the prediction.
    * @param prediction The predicted value.
    * @param actual The actual value.
    */
  def evaluateUnit(query: Q, prediction: P, actual: A): EU

  def evaluateSetBase(dataParams: DP, metricUnits: Seq[EU]): ES = {
    evaluateSet(dataParams, metricUnits)
  }

  /** Implement this method to calculate an overall result of an evaluation set.
    *
    * @param dataParams Data parameters that were used to generate data for this
    *                   evaluation.
    * @param evaluationUnits A list of evaluation units from [[evaluateUnit]].
    */
  def evaluateSet(dataParams: DP, evaluationUnits: Seq[EU]): ES

  def evaluateAllBase(input: Seq[(DP, ES)]): ER = {
    evaluateAll(input)
  }

  /** Implement this method to aggregate all evaluation result set generated by
    * each evaluation's [[evaluateSet]] to produce the final result.
    *
    * @param input A list of data parameters and evaluation set pairs to aggregate.
    */
  def evaluateAll(input: Seq[(DP, ES)]): ER
}

/** Trait for nice evaluator results
  *
  * evaluator result can be rendered nicely by implementing toHTML and toJSON
  * methods. These results are rendered through dashboard.
  * @group Evaluator
  */
trait NiceRendering {
  /** HTML portion of the rendered evaluator results. */
  def toHTML(): String

  /** JSON portion of the rendered evaluator results. */
  def toJSON(): String
}

/** An implementation of mean square error evaluator. `DP` is `AnyRef`. This
  * support any kind of data parameters.
  *
  * @group Evaluator
  */
class MeanSquareError extends Evaluator[EmptyParams, AnyRef,
    AnyRef, Double, Double, (Double, Double), String, String] {
  def evaluateUnit(q: AnyRef, p: Double, a: Double): (Double, Double) = (p, a)

  def evaluateSet(ep: AnyRef, data: Seq[(Double, Double)]): String = {
    val units = data.map(e => math.pow(e._1 - e._2, 2))
    val mse = units.sum / units.length
    f"Set: $ep Size: ${data.length} MSE: ${mse}%8.6f"
  }

  def evaluateAll(input: Seq[(AnyRef, String)]): String = {
    input.map(_._2).mkString("\n")
  }
}
