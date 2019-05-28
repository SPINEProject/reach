package org.clulab.reach.context
import org.ml4ai.data.utils.{AggregatedRow, FoldMaker, CodeUtils}
import org.ml4ai.data.classifiers.LinearSVMWrapper
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import scala.io.Source
object PerformCrossValReach extends App with LazyLogging {
  val svmWrapper = new LinearSVMWrapper(null)
  val config = ConfigFactory.load()
  val untrainedConfigPath = config.getString("svmContext.untrainedSVMPath")
  val untrainedSVMInstance = svmWrapper.loadFrom(untrainedConfigPath)
  val foldsForSVMContextEngine = Source.fromFile(config.getString("svmContext.folds"))
  val groupedPath = config.getString("svmContext.groupedFeatures")
  val hardCodedFeaturesPath = config.getString("contextEngine.params.hardCodedFeatures")
  val (_,rows) = CodeUtils.loadAggregatedRowsFromFile(groupedPath, hardCodedFeaturesPath)

  val foldsFromCSV = FoldMaker.getFoldsPerPaper(foldsForSVMContextEngine)
  val trainValCombined = CodeUtils.combineTrainVal(foldsFromCSV)
  val filteredRows = rows.filter(_.PMCID != "b'PMC4204162'")
  val (truthTestSVM, predTestSVM) = FoldMaker.svmControllerLinearSVM(untrainedSVMInstance, trainValCombined, filteredRows)
  val svmResult = CodeUtils.scoreMaker("Linear SVM", truthTestSVM, predTestSVM)
  logger.info(svmResult+" : results obtained by performing cross validation on old data in the reach pipeline")
}
