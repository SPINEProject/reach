package org.clulab.polarity.ml

import java.nio.file.{Files, Paths}

import edu.cmu.dynet.Expression._
import edu.cmu.dynet._
import org.clulab.fatdynet.utils.CloseableModelSaver
import org.clulab.fatdynet.utils.Closer.AutoCloser
import org.clulab.polarity.{NegativePolarity, Polarity, PositivePolarity}
import org.clulab.reach.mentions.BioEventMention

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.io.Source
import scala.util.Random



class DeepLearningPolarityClassifier(val savedModelPath:String="SavedLSTM") extends PolarityClassifier{

  Initialize.initialize()

  val dictPath = "vocab.txt"
  val w2vDictPath = "w2vvoc.txt"

  //val lines = Source.fromFile(dictPath).getLines().toList
  //val lines2 = Source.fromFile(w2vDictPath).getLines().toList

  val (w2i, c2i) = mkVocabs()


  val VOC_SIZE = 3671
  val WEM_DIMENSIONS = 100
  val CEM_DIMENSIONS = 30
  val NUM_LAYERS = 1
  val HIDDEN_SIZE = 30
  val N_EPOCH = 5

  var loss: Float = 0
  val pc = new ParameterCollection
  val w2v_wemb_lp: LookupParameter = pc.addLookupParameters(w2i.size, /*1579375,*/ Dim(Seq(WEM_DIMENSIONS)))
  val c2v_cemb:LookupParameter = pc.addLookupParameters(c2i.size, /*1579375,*/ Dim(Seq(CEM_DIMENSIONS)))

  val p_W = pc.addParameters(Dim(1, 2*HIDDEN_SIZE+1))
  val p_b = pc.addParameters(Dim(1))


  val builderFwd = new LstmBuilder(NUM_LAYERS, WEM_DIMENSIONS+CEM_DIMENSIONS*2, HIDDEN_SIZE, pc)
  val builderBwd = new LstmBuilder(NUM_LAYERS, WEM_DIMENSIONS+CEM_DIMENSIONS*2, HIDDEN_SIZE, pc)
  val charFwRnnBuilder = new LstmBuilder(NUM_LAYERS, CEM_DIMENSIONS, CEM_DIMENSIONS, pc)
  val charBwRnnBuilder = new LstmBuilder(NUM_LAYERS, CEM_DIMENSIONS, CEM_DIMENSIONS, pc)


  //val sgd = new SimpleSGDTrainer(pc)
  val sgd = new AdamTrainer(pc)

  val missing_vec = new FloatVector(WEM_DIMENSIONS)
  val missing_charVec = new FloatVector(CEM_DIMENSIONS*2)

  private var _isFitted = false
  
  if (Files.exists(Paths.get(savedModelPath))){
    println("Loading saved model ...")
    _isFitted=true
    val modelLoader = new ModelLoader(savedModelPath)
    modelLoader.populateModel(pc, "/allParams")
    println("Loading model finished!")
  }
  
  

  /**
    * Trains the classifier. This method is meant to have side effects by fitting the parameters
    *
    * @param trainingPath Training data
    * @param trainingRatio Ratio of training samples in the dataset
    */
  override def fit(trainingPath:String = "SentencesInfo_all_label_final_ExactRecur.txt", trainRatio:Float=0.8.toFloat, saveFlag:Boolean=true): Unit = {
    
    if (!_isFitted){
      val (sens_train, labels_train, sens_test, labels_test) = this.readFromSpreadsheet(trainingPath, trainRatio)

      val N_EPOCH = this.N_EPOCH
      for (epoch <- 1 to N_EPOCH) {
        println(s"epoch $epoch")
        this.fitSingleEpoch(sens_train, labels_train)
        this.testSingleEpoch(sens_test, labels_test)
        sgd.learningRate=sgd.learningRate*0.3.toFloat
      }
      if (saveFlag) {save()}
      _isFitted=true
    }

  }

  /**
    * Returns whether fit has been called before. Mostly for control
    *
    * @return True if the parameters of the model have been fit previously
    */
  override def isFitted: Boolean = _isFitted

  /**
    * Gets the predicted polarity for the provided argument
    *
    * @param events Ordered sequence of events to get their polarity from
    * @return Predictions of Polarity subclasses
    */
  override def predict(events: Seq[BioEventMention]): Seq[Polarity] = {
    var predictions = Seq[Polarity]()
    for (event<-events) {
      val lemmas = event.lemmas.get
      val rule = event.label
      var rulePolarity = 0
      if (rule.startsWith("Neg")){
        rulePolarity=0
      }else{
        rulePolarity=1
      }
      ComputationGraph.renew()
      
      val y_pred = runInstance(lemmas, rulePolarity)
      if (y_pred.value().toFloat>0.5){
        predictions = predictions:+PositivePolarity
      }
      else{
        predictions = predictions:+NegativePolarity
      }
      
    }
    predictions
  }

  override def predict(event: BioEventMention): Polarity = {
    val lemmas = event.lemmas.get
    val rule = event.label
    var rulePolarity = 0
    if (rule.startsWith("Neg")){
      rulePolarity=0
    }else{
      rulePolarity=1
    }
    ComputationGraph.renew()

    val y_pred = runInstance(lemmas, rulePolarity)
    if (y_pred.value().toFloat>0.5){
      PositivePolarity
    }
    else{
      NegativePolarity
    }
  }

  /**
    * Saves the model parameter's to a file
    *
    * @param modelPath file path to save the model to.
    */
  override def save(modelPath: String="SavedLSTM"): Unit = {
    println("Saving model ...")
    new CloseableModelSaver(modelPath).autoClose { modelSaver =>
      modelSaver.addModel(pc, "/allParams")
    }
    println("Saving trained model finished!")

  }

  def runInstance(words:Seq[String], rulePolarityNum:Int):Expression= {
    val W = Expression.parameter(p_W)
    val b = Expression.parameter(p_b)

    val rulePolarity = Expression.input(rulePolarityNum)

    val inputsFwd = words map { word =>
      mkEmbedding(word.toLowerCase())
    }
    val inputsBwd = inputsFwd.reverse


    val statesFwd = transduce(inputsFwd, builderFwd)
    val statesBwd = transduce(inputsBwd, builderBwd)


    // Get the last embedding
    val selected = concatenate(statesFwd.last, statesBwd.last)
    val feedForwardInput = concatenate(selected, rulePolarity)

    // Run the FF network for classification
    Expression.logistic(W * feedForwardInput + b)
  }

  def transduce(embeddings:Iterable[Expression], builder:RnnBuilder): Iterable[Expression] = {
    builder.newGraph()
    builder.startNewSequence()
    val states = embeddings.map(builder.addInput)
    states
  }

  def mkEmbedding(word: String):Expression = {
    //
    // make sure you preprocess the word similarly to the embedding library used!
    //   GloVe large does not do any preprocessing
    //   GloVe small lowers the case
    //   Our Word2Vec uses Word2Vec.sanitizeWord
    //
    val sanitized = word // word.toLowerCase() // Word2Vec.sanitizeWord(word)

    val wordEmbedding =
      if (w2i.contains(word)){
        Expression.lookup(w2v_wemb_lp, w2i(word))
      }
      else {
        Expression.input(Dim(WEM_DIMENSIONS), missing_vec)
      }

    // biLSTM over character embeddings
    val charEmbedding =
      mkCharEmbedding(word)

    concatenate(wordEmbedding, charEmbedding)
  }

  def mkCharEmbedding(word: String): Expression = {
    //println(s"make embedding for word [$word]")
    val charEmbeddings = new ArrayBuffer[Expression]()
    if (word.length>0){
      for(i <- word.indices) {
        if(c2i.contains(word.charAt(i))){
          charEmbeddings += Expression.lookup(c2v_cemb, c2i(word.charAt(i)))
        }
      }
      if (charEmbeddings.length>0){
        val fwOut = transduce(charEmbeddings, charFwRnnBuilder).last
        val bwOut = transduce(charEmbeddings.reverse, charBwRnnBuilder).last
        concatenate(fwOut, bwOut)
      }
      else{
        Expression.input(Dim(CEM_DIMENSIONS*2), missing_vec)
      }
    }
    else{
      Expression.input(Dim(CEM_DIMENSIONS*2), missing_vec)
    }
  }

  def charToIndex(w2v_voc:Map[String, Int], special_voc:Map[String, Int]):Map[Char,Int]={
    println("Generating character embedding index ...")
    val chars = new mutable.HashSet[Char]()
    for (keyWord <- w2v_voc.keys) {
      for(i <- keyWord.indices) {
        chars += keyWord.charAt(i)
      }
    }
    for (keyWord <- special_voc.keys) {
      for(i <- keyWord.indices) {
        chars += keyWord.charAt(i)
      }
    }

    val c2i = chars.toList.sorted.zipWithIndex.toMap
    println("Character index finished!")
    c2i
  }

  def fitSingleEpoch(input_sens: Seq[(Seq[String],Int)],labels: Seq[Int]): Unit = {
    var total_loss = 0.toFloat
    for ((instance, label) <- input_sens zip labels) {

      ComputationGraph.renew()

      val y_value = label

      val y = Expression.input(y_value)
      val y_pred = runInstance(instance._1, instance._2)
      val loss_expr = Expression.binaryLogLoss(y_pred, y)
      loss = ComputationGraph.forward(loss_expr).toFloat
      ComputationGraph.backward(loss_expr)
      sgd.update()
      total_loss+=loss

    }
    var average_loss = total_loss/input_sens.length
    println(s"training loss ${average_loss}")

  }

  def testSingleEpoch(input_sens: Seq[(Seq[String],Int)],labels: Seq[Int]): Unit = {

    var total_loss = 0.toFloat
    var correct_count = 0
    for ((instance, label) <- input_sens zip labels) {
      ComputationGraph.renew()

      val y_value = label

      val y = Expression.input(y_value)
      val y_pred = runInstance(instance._1, instance._2)
      val loss_expr = Expression.binaryLogLoss(y_pred, y)
      loss = ComputationGraph.forward(loss_expr).toFloat
      total_loss+=loss

      if (y_pred.value().toFloat>0.5){
        if (label==1) {correct_count+=1}
      }
      else{
        if (label==0) {correct_count+=1}
      }

    }
    val average_loss = total_loss/input_sens.length
    val test_acc = correct_count.toFloat/labels.length
    println(s"number of testing samples ${labels.length}")
    println(s"testing loss ${average_loss}")
    println(s"testing acc ${test_acc}")
  }

  def loadModelEval(trainingPath:String = "SentencesInfo_all_label_final_ExactRecur.txt", trainRatio:Float=0.8.toFloat):Unit={
    val (sens_train, labels_train, sens_test, labels_test) = this.readFromSpreadsheet(trainingPath, trainRatio)

    this.testSingleEpoch(sens_test, labels_test)
  }

  def readFromSpreadsheet(spreadsheet_path:String, train_ratio:Float): (Seq[(Seq[String], Int)], Seq[Int], Seq[(Seq[String], Int)], Seq[Int]) ={
    println("Loading data from spreadsheet ...")
    var instances = Seq[(Seq[String],Int)]()
    var labels = Seq[Int]()

    val bufferedSource = Source.fromFile(spreadsheet_path)
    for (line <- bufferedSource.getLines.drop(1)) {
      val cols = line.split("\t").map(_.trim)
      // do whatever you want with the columns here
      val sentence = cols(0)
      val start = cols(2).toInt
      val end = cols(3).toInt

      var rulePolarity=0
      if (cols(5).startsWith("Pos")) {
        rulePolarity=1
      }else{
        rulePolarity=0
      }


      instances = instances :+ (sentence.split(' ').slice(start, end).toSeq, rulePolarity)
      if (cols(6).startsWith("Pos")) {
        val label = 1
        labels = labels :+label
      }else{
        val label=0
        labels = labels :+label
      }
    }
    bufferedSource.close

    println(s"Num. all samples: ${instances.length}")
    println(s"Num. all labels: ${labels.length}")


    val random_1 = new Random(1)
    val random_2 = new Random(1)

    val sens_shuffled = random_1.shuffle(instances)
    val labels_shuffle = random_2.shuffle(labels)

    //val sens_shuffled = sentences
    //val labels_shuffle = labels

    val n_training = (labels.length * train_ratio).toInt

    val sens_train = sens_shuffled.slice(0, n_training)
    val labels_train = labels_shuffle.slice(0, n_training)
    val sens_test = sens_shuffled.slice(n_training, labels.length)
    val labels_test = labels_shuffle.slice(n_training, labels.length)
    println("Loading data finished!")

    (sens_train, labels_train, sens_test, labels_test)

  }

  def mkVocabs(spreadSheetPath:String = "SentencesInfo_all_label_final_ExactRecur.txt"): (Map[String, Int], Map[Char, Int]) = {
    println("Making vocabulary for deep learning model ...")
    val (trainSentences, _, _,_) = readFromSpreadsheet(spreadSheetPath, 0.8.toFloat)

    val chars = new mutable.HashSet[Char]()
    val words = new mutable.HashSet[String]()
    for(instance <- trainSentences) {
      for(token <- instance._1) {
        val word = token.toLowerCase
        words += word
        for(i <- word.indices) {
          chars += word.charAt(i)
        }
      }
    }

    val w2i = words.zipWithIndex.toMap
    val c2i = chars.toList.sorted.zipWithIndex.toMap

    println(s"Vocabulary build finished! W2I size ${w2i.size},  C2I size ${c2i.size}")

    (w2i, c2i)
  }
}


// object DeepLearningPolarityClassifier extends App{
//   //def load(path:String):DeepLearningPolarityClassifier = ???
//   val lstmClassifier = new DeepLearningPolarityClassifier()
//   lstmClassifier.fit()
//   //lstmClassifier.loadModelEval()
// }
