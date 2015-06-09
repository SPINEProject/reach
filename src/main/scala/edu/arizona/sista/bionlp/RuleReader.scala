package edu.arizona.sista.bionlp

import java.io.File
import edu.arizona.sista.bionlp.reach.preprocessing.TemplateMap


/**
 * Utilities to read rule files
 */
object RuleReader {

  case class Rules(entities: String, modifications: String, events: String, coref: String)

  val resourcesDir = "/edu/arizona/sista/odin/domains/bigmechanism/summer2015/biogrammar"
  val templatesDir = s"$resourcesDir/templates"
  val entitiesDir = s"$resourcesDir/entities"
  val modificationsDir = s"$resourcesDir/modifications"
  val eventsDir = s"$resourcesDir/events"
  val corefDir = s"$resourcesDir/coref"
  // For shell
  val resourcesPath = s"src/main/resources$resourcesDir"

  def readRules(): String =
    readEntityRules() + "\n\n" + readModificationRules() + "\n\n" + readEventRules()

  def readEntityRules(): String = {
    val files = Seq(
      s"$entitiesDir/entities.yml")
    files map readResource mkString "\n\n"
  }

  def readModificationRules(): String = {
    val files = Seq(s"$modificationsDir/modifications.yml", s"$modificationsDir/mutants.yml")
    files map readResource mkString "\n\n"
  }

  def readEventRules(): String = {
    val files = Seq(
      // non-templatic event grammars
      s"$eventsDir/hydrolysis_events.yml",
      s"$eventsDir/bind_events.yml",
      //s"$eventsDir/transcription_events.yml",
      s"$eventsDir/translocation_events.yml")

    val ruleFiles = files map readResource mkString "\n\n"

    // Generate rules for templatic SIMPLE events
    val simpleEventTemplate = readResource(s"$templatesDir/simple-event_template.yml")
    val templaticEventRules = generateRulesFromTemplate(simpleEventTemplate, simpleEventMap)
    // println(templaticEventRules)

    // Generate rules for templatic ACTIVATION events
    val posActivationTemplate = readResource(s"$templatesDir/pos-reg_template.yml")
    val templaticPosActivationRules = generateRulesFromTemplateSingleEvent(posActivationTemplate, posActEventMap)
    val negActivationTemplate = readResource(s"$templatesDir/neg-reg_template.yml")
    val templaticNegActivationRules = generateRulesFromTemplateSingleEvent(negActivationTemplate, negActEventMap)

    // Generate rules for templatic REGULATION events
    val posRegTemplate = readResource(s"$templatesDir/pos-reg_template.yml")
    val templaticPosRegRules = generateRulesFromTemplateSingleEvent(posRegTemplate, posRegEventMap)
    val negRegTemplate = readResource(s"$templatesDir/neg-reg_template.yml")
    val templaticNegRegRules = generateRulesFromTemplateSingleEvent(negRegTemplate, negRegEventMap)


    ruleFiles +
      templaticEventRules +
      templaticPosActivationRules + templaticNegActivationRules +
      templaticPosRegRules + templaticNegRegRules
  }

  def readCorefRules(): String = {
    val files = Seq(
      // non-templatic grammars plus generics like "it" and "the protein"
      s"$modificationsDir/modifications.yml",
      s"$corefDir/generic_entities.yml",
      s"$corefDir/generic_events.yml")

    val ruleFiles = files map readResource mkString "\n\n"

    ruleFiles + readEventRules()
  }


  def readFile(filename: String) = {
    val source = io.Source.fromFile(filename)
    val data = source.mkString
    source.close()
    data
  }

  def readRuleFilesFromDir(file: File):String = {
    val ruleFiles =
      file.listFiles
      .filter(f => f.getName.endsWith(".yml"))
    val rules =
      ruleFiles
        .map{case f:File => readFile(f.getAbsolutePath)}
        .mkString("\n")
    rules + "\n"
  }

  def readResource(filename: String): String = {
    val source = io.Source.fromURL(getClass.getResource(filename))
    val data = source.mkString
    source.close()
    data
  }

  /**
   * Reads rules from for entities, modifications, and events
   * @return Rule instance
   */
  def reloadRules(): Rules  = {

    val entitiesPath = s"$resourcesPath/entities"
    val modificationsPath = s"$resourcesPath/modifications"
    val eventsPath = s"$resourcesPath/events"
    val templatesPath = s"$resourcesPath/templates"
    val corefPath = s"$resourcesPath/coref"

    val entitiesDir = new File(".", entitiesPath)
    val modificationsDir = new File(".", modificationsPath)
    val eventsDir = new File(".", eventsPath)
    val templatesDir = new File(".", templatesPath)
    val corefDir = new File(".", corefPath)

    println(s"\tentities\t=> ${entitiesDir.getCanonicalPath}")
    println(s"\tmodifications\t=> ${modificationsDir.getCanonicalPath}")
    println(s"\tevents\t\t=> ${eventsDir.getCanonicalPath}")
    println(s"\ttemplates\t=> ${templatesDir.getCanonicalPath}")
    println(s"\tcoref\t\t=> ${corefDir.getCanonicalPath}")

    val simpleEventTemplate = readFile(templatesDir.getAbsolutePath + "/simple-event_template.yml")
    val templaticEvents = generateRulesFromTemplate(simpleEventTemplate, simpleEventMap)

    val posActTemplate = readFile(templatesDir.getAbsolutePath + "/pos-reg_template.yml")
    val templaticPosActs = generateRulesFromTemplateSingleEvent(posActTemplate, posActEventMap)
    val negActTemplate = readFile(templatesDir.getAbsolutePath + "/neg-reg_template.yml")
    val templaticNegActs = generateRulesFromTemplateSingleEvent(negActTemplate, negActEventMap)

    val posRegTemplate = readFile(templatesDir.getAbsolutePath + "/pos-reg_template.yml")
    val templaticPosRegs = generateRulesFromTemplateSingleEvent(posRegTemplate, posRegEventMap)
    val negRegTemplate = readFile(templatesDir.getAbsolutePath + "/neg-reg_template.yml")
    val templaticNegRegs = generateRulesFromTemplateSingleEvent(negRegTemplate, negRegEventMap)

    val entityRules = readRuleFilesFromDir(entitiesDir)
    val modificationRules = readRuleFilesFromDir(modificationsDir)

    val eventRules = readRuleFilesFromDir(eventsDir) +
      templaticEvents +
      templaticPosActs +
      templaticNegActs +
      templaticPosRegs +
      templaticNegRegs

    val corefRules = eventRules + readRuleFilesFromDir(corefDir)
    Rules(entityRules, modificationRules, eventRules, corefRules)
  }

  /** Replaces rules variables.
    * 
    * @param rules A string with variables to replace
    * @param variables a map of (name -> value)
    * @return a string with the new text
    */
  def replaceVars(rules: String, variables: TemplateMap): String = {
    var text = rules
    for (name <- variables.keys)
      text = s"\\{\\{\\s*($name)\\s*\\}\\}".r.replaceAllIn(text, m => variables(m.group(1)))
    text
  }

  def generateRulesFromTemplate(template: String, varMap:Map[String, TemplateMap]):String = {
    varMap.values.map(m => replaceVars(template, m)) mkString "\n\n"
  }

  /** For when we have a single map */
  def generateRulesFromTemplateSingleEvent(template: String, varMap:TemplateMap):String = {
    replaceVars(template, varMap)
  }

  // Phosphorylation
  val phosphoMap: Map[String, String] =
    Map("eventName" -> "Phosphorylation",
        "actionFlow" -> "default",
        "labels" -> "Phosphorylation",
        "verbalTriggerLemma" -> "phosphorylate",
        "nominalTriggerLemma" -> "phosphorylation")

  // Ubiquitination
  val ubiqMap: Map[String, String] =
    Map("eventName" -> "Ubiquitination",
        "actionFlow" -> "mkUbiquitination",
        "labels" -> "Ubiquitination",
        "verbalTriggerLemma" -> "ubiquitinate",
        "nominalTriggerLemma" -> "ubiquitination")

  // Hydroxylation
  val hydroxMap: Map[String, String] =
    Map("eventName" -> "Hydroxylation",
        "actionFlow" -> "default",
        "labels" -> "Hydroxylation",
        "verbalTriggerLemma" -> "hydroxylate",
        "nominalTriggerLemma" -> "hydroxylation")

  // Sumoylation
  val sumoMap: Map[String, String] =
    Map("eventName" -> "Sumoylation",
        "actionFlow" -> "default",
        "labels" -> "Sumoylation",
        "verbalTriggerLemma" -> "sumoylate",
        "nominalTriggerLemma" -> "sumoylation")

  // Glycosylation
  val glycoMap: Map[String, String] =
    Map("eventName" -> "Glycosylation",
        "actionFlow" -> "default",
        "labels" -> "Glycosylation",
        "verbalTriggerLemma" -> "glycosylate",
        "nominalTriggerLemma" -> "glycosylation")

  // Acetylation
  val aceMap: Map[String, String] =
    Map("eventName" -> "Acetylation",
        "actionFlow" -> "default",
        "labels" -> "Acetylation",
        "verbalTriggerLemma" -> "acetylate",
        "nominalTriggerLemma" -> "acetylation")

  // Farnesylation
  val farneMap: Map[String, String] =
    Map("eventName" -> "Farnesylation",
      "actionFlow" -> "default",
      "labels" -> "Farnesylation",
      "verbalTriggerLemma" -> "farnesylate",
      "nominalTriggerLemma" -> "farnesylation")

  // Ribosylation
  val riboMap: Map[String, String] =
    Map("eventName" -> "Ribosylation",
        "actionFlow" -> "default",
        "labels" -> "Ribosylation",
        "verbalTriggerLemma" -> "ribosylate",
        "nominalTriggerLemma" -> "ribosylation")

  // Methylation
  val methMap: Map[String, String] =
    Map("eventName" -> "Methylation",
        "actionFlow" -> "default",
        "labels" -> "Methylation",
        "verbalTriggerLemma" -> "methylate",
        "nominalTriggerLemma" -> "methylation")

  val simpleEventMap: Map[String, Map[String, String]] =
    Map("Phosphorylation" -> phosphoMap,
        "Ubiquitination" -> ubiqMap,
        "Sumoylation" -> sumoMap,
        "Glycosylation" -> glycoMap,
        "Acetylation" -> aceMap,
        "Farnesylation" -> farneMap,
        "Hydroxylation" -> hydroxMap,
        "Ribosylation" -> riboMap,
        "Methylation" -> methMap)

  //
  // Please keep all triggers sorted alphabetically; otherwise it is hard to see what we have and what is missing
  //

  val POS_NOUNS = "acceler|activ|augment|cataly|caus|driv|elev|elicit|enhanc|express|facilit|increas|induc|induct|initi|produc|promot|promot|rais|reactiv|re-express|releas|stimul|trigger|up-regul|upregul"
  val NEG_NOUNS = "decreas|inhibit|loss|repress|suppress|supress"
  val AUXTRIGGERS = "activ|regul"

  val POS_REG_TRIGGERS = "acceler|accept|accumul|action|activat|aid|allow|associ|augment|cataly|caus|cleav|confer|contribut|convert|direct|driv|elev|elicit|enabl|enhanc|escort|export|express|facilit|gener|high|increas|induc|induct|initi|interact|interconvert|involv|lead|led|major|mediat|modul|necess|overexpress|potent|proce|produc|prolong|promot|rais|reactivat|re-express|releas|render|requir|rescu|respons|restor|result|retent|signal|stimul|support|synerg|synthes|target|trigger|underli|up-regul|upregul"
  val NEG_REG_TRIGGERS = "abolish|abrog|absenc|antagon|arrest|attenu|block|blunt|deactiv|decreas|defect|defici|degrad|delay|deplet|deregul|diminish|disengag|disrupt|down|down-reg|downreg|drop|dysregul|elimin|impair|imped|inactiv|inhibit|interf|knockdown|lack|limit|loss|lost|lower|negat|neutral|nullifi|oppos|overc|perturb|prevent|reduc|reliev|remov|repress|resist|restrict|revers|shutdown|slow|starv|suppress|supress|uncoupl"

  // These are a bit stricter than the POS_REG and NEG_REG because the context is more ambiguous for activations
  val POS_ACT_TRIGGERS = "acceler|activat|aid|allow|augment|direct|elev|elicit|enabl|enhanc|express|increas|induc|initi|modul|necess|overexpress|potenti|produc|prolong|promot|rais|reactivat|rescu|respons|restor|re-express|retent|sequest|signal|stimul|support|synerg|synthes|trigger|up-regul|upregul"
  val NEG_ACT_TRIGGERS = "attenu|block|deactiv|decreas|degrad|diminish|disrupt|impair|imped|inhibit|knockdown|limit|lower|negat|reduc|reliev|repress|restrict|revers|slow|starv|suppress|supress"

  // These are used to detect semantic inversions of regulations/activations. See DarpaActions.switchLabel
  val SEMANTIC_NEGATIVE_PATTERN = NEG_ACT_TRIGGERS.r

  val posRegEventMap: Map[String, String] =
    Map("labels" -> "Positive_regulation, ComplexEvent, Event",
        "ruleType" -> "regulation",
        "triggers" -> POS_REG_TRIGGERS,
        "auxtriggers" -> AUXTRIGGERS,
        "posnouns" -> POS_NOUNS,
        "negnouns" -> NEG_NOUNS, // needed for lookahead
        "actionFlow" -> "mkRegulation",
        "priority" -> "5",
        "controlledType" -> "SimpleEvent",
        "controllerType" -> "PossibleController")
  val posActEventMap: Map[String, String] =
    Map("labels" -> "Positive_activation, ActivationEvent, Event",
        "ruleType" -> "activation",
        "triggers" -> POS_ACT_TRIGGERS,
        "auxtriggers" -> AUXTRIGGERS,
        "posnouns" -> POS_NOUNS,
        "negnouns" -> NEG_NOUNS,
        "actionFlow" -> "mkActivation",
        "priority" -> "6", // must be 1 + priority of regulations!
        "controlledType" -> "BioChemicalEntity",
        "controllerType" -> "PossibleController")

  val negRegEventMap: Map[String, String] =
    Map("labels" -> "Negative_regulation, ComplexEvent, Event",
        "ruleType" -> "regulation",
        "triggers" -> NEG_REG_TRIGGERS,
        "auxtriggers" -> AUXTRIGGERS,
        "negnouns" -> NEG_NOUNS,
        "actionFlow" -> "mkRegulation",
        "priority" -> "5",
        "controlledType" -> "SimpleEvent",
        "controllerType" -> "PossibleController")
  val negActEventMap: Map[String, String] =
    Map("labels" -> "Negative_activation, ActivationEvent, Event",
        "ruleType" -> "activation",
        "triggers" -> NEG_ACT_TRIGGERS,
        "auxtriggers" -> AUXTRIGGERS,
        "negnouns" -> NEG_NOUNS,
        "actionFlow" -> "mkActivation",
        "priority" -> "6", // must be 1 + priority of regulations!
        "controlledType" -> "BioChemicalEntity",
        "controllerType" -> "PossibleController")

}
