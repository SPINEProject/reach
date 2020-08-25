package org.clulab.reach.assembly

import org.scalatest.{FlatSpec, Matchers}
import org.clulab.reach.TestUtils.{jsonStringToDocument, getMentionsFromDocument}
import org.clulab.reach.assembly.sieves.{AssemblySieve, DeduplicationSieves, PrecedenceSieves}


/**
  * Assembly sieve tests
  */
class TestAssemblySieves extends FlatSpec with Matchers {

  // Reichenbach rules

  val tamSent1 = "Once BEF had been phosphorylated, AFT was ubiquitinated"

  tamSent1 should "be annotated with the phosphorylation preceding the ubiquitination" in {

    val doc = jsonStringToDocument(""" {"id":"","text":"Once BEF had been phosphorylated, AFT was ubiquitinated","sentences":[{"words":["Once","BEF","had","been","phosphorylated",",","AFT","was","ubiquitinated"],"startOffsets":[0,5,9,13,18,32,34,38,42],"endOffsets":[4,8,12,17,32,33,37,41,55],"tags":["RB","NNP","VBD","VBN","VBN",",","NNP","VBD","VBN"],"lemmas":["once","BEF","have","be","phosphorylate",",","AFT","be","ubiquitinate"],"entities":["O","B-Gene_or_gene_product","O","O","O","O","B-Gene_or_gene_product","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":4,"destination":0,"relation":"advmod"},{"source":4,"destination":1,"relation":"nsubjpass"},{"source":4,"destination":2,"relation":"aux"},{"source":4,"destination":3,"relation":"auxpass"},{"source":8,"destination":4,"relation":"advcl"},{"source":8,"destination":6,"relation":"nsubjpass"},{"source":8,"destination":7,"relation":"auxpass"}],"roots":[8]},"stanford-collapsed":{"edges":[{"source":4,"destination":0,"relation":"advmod"},{"source":4,"destination":1,"relation":"nsubjpass"},{"source":4,"destination":2,"relation":"aux"},{"source":4,"destination":3,"relation":"auxpass"},{"source":8,"destination":4,"relation":"advcl"},{"source":8,"destination":6,"relation":"nsubjpass"},{"source":8,"destination":7,"relation":"auxpass"}],"roots":[8]}}}]} """)
    val mentions = getMentionsFromDocument(doc)

    val dedup = new DeduplicationSieves()
    val precedence = new PrecedenceSieves()

    val orderedSieves =
    // track relevant mentions
      AssemblySieve(dedup.trackMentions) andThen
        // find precedence using TAM rules
        AssemblySieve(precedence.reichenbachPrecedence)

    val am: AssemblyManager = orderedSieves.apply(mentions)

    val uRep = am.distinctSimpleEvents("Ubiquitination").head
    val pRep = am.distinctSimpleEvents("Phosphorylation").head

    am.distinctPredecessorsOf(uRep).size should be(1)
    val pr = am.getPrecedenceRelationsFor(uRep).head
    pr.before.equivalenceHash(ignoreMods = false) == pRep.equivalenceHash(ignoreMods = false) should be (true)
    pr.before.equivalenceHash(ignoreMods = true) == pRep.equivalenceHash(ignoreMods = true) should be (true)
    pr.after.equivalenceHash(ignoreMods = false) == uRep.equivalenceHash(ignoreMods = false) should be (true)
    pr.after.equivalenceHash(ignoreMods = true) == uRep.equivalenceHash(ignoreMods = true) should be (true)
  }

  val tamSent2 = "AFT will be ubiquitinated only if BEF is first phosphorylated"

  tamSent2 should "be annotated with the phosphorylation preceding the ubiquitination" in {

    val doc = jsonStringToDocument(""" {"id":"","text":"AFT will be ubiquitinated only if BEF is first phosphorylated","sentences":[{"words":["AFT","will","be","ubiquitinated","only","if","BEF","is","first","phosphorylated"],"startOffsets":[0,4,9,12,26,31,34,38,41,47],"endOffsets":[3,8,11,25,30,33,37,40,46,61],"tags":["NNP","MD","VB","VBN","RB","IN","NN","VBZ","JJ","VBN"],"lemmas":["AFT","will","be","ubiquitinate","only","if","bef","be","first","phosphorylate"],"entities":["B-Gene_or_gene_product","O","O","O","O","O","B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":3,"destination":0,"relation":"nsubjpass"},{"source":3,"destination":1,"relation":"aux"},{"source":3,"destination":2,"relation":"auxpass"},{"source":3,"destination":9,"relation":"advcl"},{"source":9,"destination":4,"relation":"advmod"},{"source":9,"destination":5,"relation":"mark"},{"source":9,"destination":6,"relation":"nsubjpass"},{"source":9,"destination":7,"relation":"auxpass"},{"source":9,"destination":8,"relation":"advmod"}],"roots":[3]},"stanford-collapsed":{"edges":[{"source":3,"destination":0,"relation":"nsubjpass"},{"source":3,"destination":1,"relation":"aux"},{"source":3,"destination":2,"relation":"auxpass"},{"source":3,"destination":9,"relation":"advcl"},{"source":9,"destination":4,"relation":"advmod"},{"source":9,"destination":5,"relation":"mark"},{"source":9,"destination":6,"relation":"nsubjpass"},{"source":9,"destination":7,"relation":"auxpass"},{"source":9,"destination":8,"relation":"advmod"}],"roots":[3]}}}]} """)
    val mentions = getMentionsFromDocument(doc)

    val dedup = new DeduplicationSieves()
    val precedence = new PrecedenceSieves()

    val orderedSieves =
    // track relevant mentions
      AssemblySieve(dedup.trackMentions) andThen
        // find precedence using TAM rules
        AssemblySieve(precedence.reichenbachPrecedence)

    val am: AssemblyManager = orderedSieves.apply(mentions)

    val uRep = am.distinctSimpleEvents("Ubiquitination").head
    val pRep = am.distinctSimpleEvents("Phosphorylation").head

    am.distinctPredecessorsOf(uRep).size should be(1)
    val pr = am.getPrecedenceRelationsFor(uRep).head
    pr.before.isEquivalentTo(pRep, ignoreMods = false) should be (true)
    pr.before.isEquivalentTo(pRep, ignoreMods = true) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = false) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = true) should be (true)
  }

  val tamSent3 = "AFT was ubiquitinated when BEF had been phosphorylated"

  tamSent3 should "be annotated with the phosphorylation preceding the ubiquitination" in {

    val doc = jsonStringToDocument(""" {"id":"","text":"AFT was ubiquitinated when BEF had been phosphorylated","sentences":[{"words":["AFT","was","ubiquitinated","when","BEF","had","been","phosphorylated"],"startOffsets":[0,4,8,22,27,31,35,40],"endOffsets":[3,7,21,26,30,34,39,54],"tags":["NNP","VBD","VBN","WRB","NNP","VBD","VBN","VBN"],"lemmas":["AFT","be","ubiquitinate","when","BEF","have","be","phosphorylate"],"entities":["B-Gene_or_gene_product","O","O","O","B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"},{"source":2,"destination":7,"relation":"advcl"},{"source":7,"destination":3,"relation":"advmod"},{"source":7,"destination":4,"relation":"nsubjpass"},{"source":7,"destination":5,"relation":"aux"},{"source":7,"destination":6,"relation":"auxpass"}],"roots":[2]},"stanford-collapsed":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"},{"source":2,"destination":7,"relation":"advcl"},{"source":7,"destination":3,"relation":"advmod"},{"source":7,"destination":4,"relation":"nsubjpass"},{"source":7,"destination":5,"relation":"aux"},{"source":7,"destination":6,"relation":"auxpass"}],"roots":[2]}}}]} """)
    val mentions = getMentionsFromDocument(doc)

    val dedup = new DeduplicationSieves()
    val precedence = new PrecedenceSieves()

    val orderedSieves =
    // track relevant mentions
      AssemblySieve(dedup.trackMentions) andThen
        // find precedence using TAM rules
        AssemblySieve(precedence.reichenbachPrecedence)

    val am: AssemblyManager = orderedSieves.apply(mentions)

    val uRep = am.distinctSimpleEvents("Ubiquitination").head
    val pRep = am.distinctSimpleEvents("Phosphorylation").head

    am.distinctPredecessorsOf(uRep).size should be(1)
    val pr = am.getPrecedenceRelationsFor(uRep).head
    pr.before.isEquivalentTo(pRep, ignoreMods = false) should be (true)
    pr.before.isEquivalentTo(pRep, ignoreMods = true) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = false) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = true) should be (true)
  }


  // Intrasential Odin rules

  val intraSent1 = "Together these data demonstrate that E2-induced SRC-3 phosphorylation is dependent on a direct interaction between SRC-3 and ERα and can occur outside of the nucleus."

  intraSent1 should "be annotated with the binding preceding the phosphorylation" in {

    val doc = jsonStringToDocument(""" {"id":"","text":"Together these data demonstrate that E2-induced SRC-3 phosphorylation is dependent on a direct interaction between SRC-3 and ERalpha and can occur outside of the nucleus.","sentences":[{"words":["Together","these","data","demonstrate","that","E2","induced","SRC-3","phosphorylation","is","dependent","on","a","direct","interaction","between","SRC-3","and","ERalpha","and","can","occur","outside","of","the","nucleus","."],"startOffsets":[0,9,15,20,32,37,40,48,54,70,73,83,86,88,95,107,115,121,125,133,137,141,147,155,158,162,169],"endOffsets":[8,14,19,31,36,39,47,53,69,72,82,85,87,94,106,114,120,124,132,136,140,146,154,157,161,169,170],"tags":["RB","DT","NNS","VBP","IN","NN","VBD","NN","NN","VBZ","JJ","IN","DT","JJ","NN","IN","NN","CC","NN","CC","MD","VB","IN","IN","DT","NN","."],"lemmas":["together","these","datum","demonstrate","that","e2","induce","src-3","phosphorylation","be","dependent","on","a","direct","interaction","between","src-3","and","eralpha","and","can","occur","outside","of","the","nucleus","."],"entities":["O","O","O","O","O","B-Simple_chemical","O","B-Gene_or_gene_product","O","O","O","O","O","O","O","O","B-Gene_or_gene_product","O","B-Gene_or_gene_product","O","O","O","O","O","O","B-Cellular_component","O"],"graphs":{"stanford-basic":{"edges":[{"source":2,"destination":1,"relation":"det"},{"source":3,"destination":0,"relation":"advmod"},{"source":3,"destination":2,"relation":"nsubj"},{"source":3,"destination":6,"relation":"ccomp"},{"source":6,"destination":19,"relation":"cc"},{"source":6,"destination":4,"relation":"mark"},{"source":6,"destination":5,"relation":"nsubj"},{"source":6,"destination":21,"relation":"conj"},{"source":6,"destination":10,"relation":"ccomp"},{"source":8,"destination":7,"relation":"nn"},{"source":10,"destination":8,"relation":"nsubj"},{"source":10,"destination":9,"relation":"cop"},{"source":10,"destination":11,"relation":"prep"},{"source":11,"destination":14,"relation":"pobj"},{"source":14,"destination":15,"relation":"prep"},{"source":14,"destination":12,"relation":"det"},{"source":14,"destination":13,"relation":"amod"},{"source":15,"destination":16,"relation":"pobj"},{"source":16,"destination":17,"relation":"cc"},{"source":16,"destination":18,"relation":"conj"},{"source":21,"destination":20,"relation":"aux"},{"source":21,"destination":22,"relation":"advmod"},{"source":21,"destination":23,"relation":"prep"},{"source":23,"destination":25,"relation":"pobj"},{"source":25,"destination":24,"relation":"det"}],"roots":[3]},"stanford-collapsed":{"edges":[{"source":2,"destination":1,"relation":"det"},{"source":3,"destination":0,"relation":"advmod"},{"source":3,"destination":2,"relation":"nsubj"},{"source":3,"destination":21,"relation":"ccomp"},{"source":3,"destination":6,"relation":"ccomp"},{"source":6,"destination":4,"relation":"mark"},{"source":6,"destination":5,"relation":"nsubj"},{"source":6,"destination":21,"relation":"conj_and"},{"source":6,"destination":10,"relation":"ccomp"},{"source":8,"destination":7,"relation":"nn"},{"source":10,"destination":8,"relation":"nsubj"},{"source":10,"destination":9,"relation":"cop"},{"source":10,"destination":14,"relation":"prep_on"},{"source":14,"destination":16,"relation":"prep_between"},{"source":14,"destination":18,"relation":"prep_between"},{"source":14,"destination":12,"relation":"det"},{"source":14,"destination":13,"relation":"amod"},{"source":16,"destination":18,"relation":"conj_and"},{"source":21,"destination":20,"relation":"aux"},{"source":21,"destination":5,"relation":"nsubj"},{"source":21,"destination":25,"relation":"prep_outside_of"},{"source":25,"destination":24,"relation":"det"}],"roots":[3]}}}]} """)
    val mentions = getMentionsFromDocument(doc)

    val dedup = new DeduplicationSieves()
    val precedence = new PrecedenceSieves()

    val orderedSieves =
    // track relevant mentions
      AssemblySieve(dedup.trackMentions) andThen
        // find precedence using intrasentential Odin rules
        AssemblySieve(precedence.intrasententialRBPrecedence)

    val am: AssemblyManager = orderedSieves.apply(mentions)

    val bRep = am.distinctSimpleEvents("Binding").head
    val pRep = am.distinctRegulations(AssemblyManager.positive).head

    am.distinctPredecessorsOf(pRep).size should be(1)
    val pr = am.getPrecedenceRelationsFor(pRep).head
    pr.before.equivalenceHash(ignoreMods = false) == bRep.equivalenceHash(ignoreMods = false) should be (true)
    pr.before.equivalenceHash(ignoreMods = true) == bRep.equivalenceHash(ignoreMods = true) should be (true)
    pr.after.equivalenceHash(ignoreMods = false) == pRep.equivalenceHash(ignoreMods = false) should be (true)
    pr.after.equivalenceHash(ignoreMods = true) == pRep.equivalenceHash(ignoreMods = true) should be (true)

  }

  // Intersentential rules

  val interSent1 = "BEF was phosphorylated. Then, AFT was ubiquitinated."

  interSent1 should "be annotated with the phosphorylation preceding the ubiquitination" in {

    val doc = jsonStringToDocument(""" {"id":"","text":"BEF was phosphorylated. Then, AFT was ubiquitinated.","sentences":[{"words":["BEF","was","phosphorylated","."],"startOffsets":[0,4,8,22],"endOffsets":[3,7,22,23],"tags":["NN","VBD","VBN","."],"lemmas":["bef","be","phosphorylate","."],"entities":["B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"}],"roots":[2]},"stanford-collapsed":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"}],"roots":[2]}}},{"words":["Then",",","AFT","was","ubiquitinated","."],"startOffsets":[24,28,30,34,38,51],"endOffsets":[28,29,33,37,51,52],"tags":["RB",",","NNP","VBD","VBN","."],"lemmas":["then",",","AFT","be","ubiquitinate","."],"entities":["O","O","B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":4,"destination":0,"relation":"advmod"},{"source":4,"destination":2,"relation":"nsubjpass"},{"source":4,"destination":3,"relation":"auxpass"}],"roots":[4]},"stanford-collapsed":{"edges":[{"source":4,"destination":0,"relation":"advmod"},{"source":4,"destination":2,"relation":"nsubjpass"},{"source":4,"destination":3,"relation":"auxpass"}],"roots":[4]}}}]} """)
    val mentions = getMentionsFromDocument(doc)

    val dedup = new DeduplicationSieves()
    val precedence = new PrecedenceSieves()

    val orderedSieves =
    // track relevant mentions
      AssemblySieve(dedup.trackMentions) andThen
        // find precedence using intersentential Odin rules
        AssemblySieve(precedence.intersententialRBPrecedence)

    val am: AssemblyManager = orderedSieves.apply(mentions)

    val uRep = am.distinctSimpleEvents("Ubiquitination").head
    val pRep = am.distinctSimpleEvents("Phosphorylation").head

    am.distinctPredecessorsOf(uRep).size should be(1)
    val pr = am.getPrecedenceRelationsFor(uRep).head
    pr.before.isEquivalentTo(pRep, ignoreMods = false) should be (true)
    pr.before.isEquivalentTo(pRep, ignoreMods = true) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = true) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = false) should be (true)
  }

  val interSent2 = "BEF was phosphorylated. Subsequently AFT was ubiquitinated."

  interSent2 should "be annotated with the phosphorylation preceding the ubiquitination" in {

    val doc = jsonStringToDocument(""" {"id":"","text":"BEF was phosphorylated. Subsequently AFT was ubiquitinated.","sentences":[{"words":["BEF","was","phosphorylated","."],"startOffsets":[0,4,8,22],"endOffsets":[3,7,22,23],"tags":["NN","VBD","VBN","."],"lemmas":["bef","be","phosphorylate","."],"entities":["B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"}],"roots":[2]},"stanford-collapsed":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"}],"roots":[2]}}},{"words":["Subsequently","AFT","was","ubiquitinated","."],"startOffsets":[24,37,41,45,58],"endOffsets":[36,40,44,58,59],"tags":["NNP","NNP","VBD","VBN","."],"lemmas":["Subsequently","AFT","be","ubiquitinate","."],"entities":["O","B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":1,"destination":0,"relation":"nn"},{"source":3,"destination":1,"relation":"nsubjpass"},{"source":3,"destination":2,"relation":"auxpass"}],"roots":[3]},"stanford-collapsed":{"edges":[{"source":1,"destination":0,"relation":"nn"},{"source":3,"destination":1,"relation":"nsubjpass"},{"source":3,"destination":2,"relation":"auxpass"}],"roots":[3]}}}]} """)
    val mentions = getMentionsFromDocument(doc)

    val dedup = new DeduplicationSieves()
    val precedence = new PrecedenceSieves()

    val orderedSieves =
    // track relevant mentions
      AssemblySieve(dedup.trackMentions) andThen
        // find precedence using intersentential Odin rules
        AssemblySieve(precedence.intersententialRBPrecedence)

    val am: AssemblyManager = orderedSieves.apply(mentions)

    val uRep = am.distinctSimpleEvents("Ubiquitination").head
    val pRep = am.distinctSimpleEvents("Phosphorylation").head

    am.distinctPredecessorsOf(uRep).size should be(1)
    val pr = am.getPrecedenceRelationsFor(uRep).head
    pr.before.isEquivalentTo(pRep, ignoreMods = false) should be (true)
    pr.before.isEquivalentTo(pRep, ignoreMods = true) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = false) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = true) should be (true)
  }

  val interSent3 = "AFT was ubiquitinated. Prior to this, BEF was phosphorylated."

  interSent3 should "be annotated with the phosphorylation preceding the ubiquitination" in {

    val doc = jsonStringToDocument(""" {"id":"","text":"AFT was ubiquitinated. Prior to this, BEF was phosphorylated.","sentences":[{"words":["AFT","was","ubiquitinated","."],"startOffsets":[0,4,8,21],"endOffsets":[3,7,21,22],"tags":["NNP","VBD","VBN","."],"lemmas":["AFT","be","ubiquitinate","."],"entities":["B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"}],"roots":[2]},"stanford-collapsed":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"}],"roots":[2]}}},{"words":["Prior","to","this",",","BEF","was","phosphorylated","."],"startOffsets":[23,29,32,36,38,42,46,60],"endOffsets":[28,31,36,37,41,45,60,61],"tags":["RB","TO","DT",",","NN","VBD","VBN","."],"lemmas":["prior","to","this",",","bef","be","phosphorylate","."],"entities":["O","O","O","O","B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":1,"destination":0,"relation":"advmod"},{"source":1,"destination":2,"relation":"pobj"},{"source":6,"destination":1,"relation":"prep"},{"source":6,"destination":4,"relation":"nsubjpass"},{"source":6,"destination":5,"relation":"auxpass"}],"roots":[6]},"stanford-collapsed":{"edges":[{"source":6,"destination":2,"relation":"prep_prior_to"},{"source":6,"destination":4,"relation":"nsubjpass"},{"source":6,"destination":5,"relation":"auxpass"}],"roots":[6]}}}]} """)
    val mentions = getMentionsFromDocument(doc)

    val dedup = new DeduplicationSieves()
    val precedence = new PrecedenceSieves()

    val orderedSieves =
    // track relevant mentions
      AssemblySieve(dedup.trackMentions) andThen
        // find precedence using intersentential Odin rules
        AssemblySieve(precedence.intersententialRBPrecedence)

    val am: AssemblyManager = orderedSieves.apply(mentions)

    val uRep = am.distinctSimpleEvents("Ubiquitination").head
    val pRep = am.distinctSimpleEvents("Phosphorylation").head

    am.distinctPredecessorsOf(uRep).size should be(1)
    val pr = am.getPrecedenceRelationsFor(uRep).head
    pr.before.isEquivalentTo(pRep, ignoreMods = false) should be (true)
    pr.before.isEquivalentTo(pRep, ignoreMods = true) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = false) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = true) should be (true)
  }

  val interSent4 = "AFT was ubiquitinated. Previously, BEF was phosphorylated."

  interSent4 should "be annotated with the phosphorylation preceding the ubiquitination" in {

    val doc = jsonStringToDocument(""" {"id":"","text":"AFT was ubiquitinated. Previously, BEF was phosphorylated.","sentences":[{"words":["AFT","was","ubiquitinated","."],"startOffsets":[0,4,8,21],"endOffsets":[3,7,21,22],"tags":["NNP","VBD","VBN","."],"lemmas":["AFT","be","ubiquitinate","."],"entities":["B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"}],"roots":[2]},"stanford-collapsed":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"}],"roots":[2]}}},{"words":["Previously",",","BEF","was","phosphorylated","."],"startOffsets":[23,33,35,39,43,57],"endOffsets":[33,34,38,42,57,58],"tags":["RB",",","NN","VBD","VBN","."],"lemmas":["previously",",","bef","be","phosphorylate","."],"entities":["O","O","B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":4,"destination":0,"relation":"advmod"},{"source":4,"destination":2,"relation":"nsubjpass"},{"source":4,"destination":3,"relation":"auxpass"}],"roots":[4]},"stanford-collapsed":{"edges":[{"source":4,"destination":0,"relation":"advmod"},{"source":4,"destination":2,"relation":"nsubjpass"},{"source":4,"destination":3,"relation":"auxpass"}],"roots":[4]}}}]} """)
    val mentions = getMentionsFromDocument(doc)

    val dedup = new DeduplicationSieves()
    val precedence = new PrecedenceSieves()

    val orderedSieves =
    // track relevant mentions
      AssemblySieve(dedup.trackMentions) andThen
        // find precedence using intersentential Odin rules
        AssemblySieve(precedence.intersententialRBPrecedence)

    val am: AssemblyManager = orderedSieves.apply(mentions)

    val uRep = am.distinctSimpleEvents("Ubiquitination").head
    val pRep = am.distinctSimpleEvents("Phosphorylation").head

    am.distinctPredecessorsOf(uRep).size should be(1)
    val pr = am.getPrecedenceRelationsFor(uRep).head
    pr.before.isEquivalentTo(pRep, ignoreMods = false) should be (true)
    pr.before.isEquivalentTo(pRep, ignoreMods = true) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = false) should be (true)
    pr.after.isEquivalentTo(uRep, ignoreMods = true) should be (true)
  }


  // Play it safe by ignoring cues not at the beginning of the sentence.
  val interSent5 = "AFT was ubiquitinated. There is intervening material and, previously, BEF was phosphorylated."

  interSent5 should "have no precedence relations" in {

    val doc = jsonStringToDocument(""" {"id":"","text":"AFT was ubiquitinated. There is intervening material and, previously, BEF was phosphorylated.","sentences":[{"words":["AFT","was","ubiquitinated","."],"startOffsets":[0,4,8,21],"endOffsets":[3,7,21,22],"tags":["NNP","VBD","VBN","."],"lemmas":["AFT","be","ubiquitinate","."],"entities":["B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"}],"roots":[2]},"stanford-collapsed":{"edges":[{"source":2,"destination":0,"relation":"nsubjpass"},{"source":2,"destination":1,"relation":"auxpass"}],"roots":[2]}}},{"words":["There","is","intervening","material","and",",","previously",",","BEF","was","phosphorylated","."],"startOffsets":[23,29,32,44,53,56,58,68,70,74,78,92],"endOffsets":[28,31,43,52,56,57,68,69,73,77,92,93],"tags":["EX","VBZ","VBG","NN","CC",",","RB",",","NN","VBD","VBN","."],"lemmas":["there","be","intervene","material","and",",","previously",",","bef","be","phosphorylate","."],"entities":["O","O","O","O","O","O","O","O","B-Gene_or_gene_product","O","O","O"],"graphs":{"stanford-basic":{"edges":[{"source":2,"destination":0,"relation":"expl"},{"source":2,"destination":1,"relation":"aux"},{"source":2,"destination":3,"relation":"dobj"},{"source":2,"destination":4,"relation":"cc"},{"source":2,"destination":10,"relation":"conj"},{"source":10,"destination":6,"relation":"advmod"},{"source":10,"destination":8,"relation":"nsubjpass"},{"source":10,"destination":9,"relation":"auxpass"}],"roots":[2]},"stanford-collapsed":{"edges":[{"source":2,"destination":0,"relation":"expl"},{"source":2,"destination":1,"relation":"aux"},{"source":2,"destination":3,"relation":"dobj"},{"source":2,"destination":10,"relation":"conj_and"},{"source":10,"destination":6,"relation":"advmod"},{"source":10,"destination":8,"relation":"nsubjpass"},{"source":10,"destination":9,"relation":"auxpass"}],"roots":[2]}}}]} """)
    val mentions = getMentionsFromDocument(doc)

    val dedup = new DeduplicationSieves()
    val precedence = new PrecedenceSieves()

    val orderedSieves =
    // track relevant mentions
      AssemblySieve(dedup.trackMentions) andThen
        // find precedence using intersentential Odin rules
        AssemblySieve(precedence.intersententialRBPrecedence)

    val am: AssemblyManager = orderedSieves.apply(mentions)

    val uRep = am.distinctSimpleEvents("Ubiquitination").head
    val pRep = am.distinctSimpleEvents("Phosphorylation").head

    am.distinctPredecessorsOf(uRep) should have size (0)
  }

}
