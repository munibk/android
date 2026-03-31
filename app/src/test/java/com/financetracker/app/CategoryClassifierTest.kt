package com.financetracker.app

import com.financetracker.app.service.classifier.CategoryClassifier
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CategoryClassifierTest {

    private lateinit var classifier: CategoryClassifier

    @Before
    fun setup() {
        classifier = CategoryClassifier()
    }

    // ── Food ──────────────────────────────────────────────────────────────

    @Test fun `classify Swiggy as Food`()  = runTest { assertThat(classifier.classify("", "Swiggy")).isEqualTo("Food") }
    @Test fun `classify Zomato as Food`()  = runTest { assertThat(classifier.classify("", "Zomato")).isEqualTo("Food") }
    @Test fun `classify Dominos as Food`() = runTest { assertThat(classifier.classify("", "Dominos")).isEqualTo("Food") }
    @Test fun `classify KFC as Food`()     = runTest { assertThat(classifier.classify("", "KFC")).isEqualTo("Food") }

    // ── Transport ─────────────────────────────────────────────────────────

    @Test fun `classify Uber as Transport`()  = runTest { assertThat(classifier.classify("", "Uber")).isEqualTo("Transport") }
    @Test fun `classify Ola as Transport`()   = runTest { assertThat(classifier.classify("", "Ola")).isEqualTo("Transport") }
    @Test fun `classify IRCTC as Transport`() = runTest { assertThat(classifier.classify("", "IRCTC")).isEqualTo("Transport") }
    @Test fun `classify IndiGo as Transport`() = runTest { assertThat(classifier.classify("", "IndiGo")).isEqualTo("Transport") }

    // ── Shopping ──────────────────────────────────────────────────────────

    @Test fun `classify Amazon as Shopping`()  = runTest { assertThat(classifier.classify("", "Amazon")).isEqualTo("Shopping") }
    @Test fun `classify Flipkart as Shopping`() = runTest { assertThat(classifier.classify("", "Flipkart")).isEqualTo("Shopping") }
    @Test fun `classify Myntra as Shopping`()   = runTest { assertThat(classifier.classify("", "Myntra")).isEqualTo("Shopping") }
    @Test fun `classify Nykaa as Shopping`()    = runTest { assertThat(classifier.classify("", "Nykaa")).isEqualTo("Shopping") }

    // ── Bills ─────────────────────────────────────────────────────────────

    @Test fun `classify Airtel as Bills`()    = runTest { assertThat(classifier.classify("", "Airtel")).isEqualTo("Bills") }
    @Test fun `classify Jio as Bills`()       = runTest { assertThat(classifier.classify("", "Jio")).isEqualTo("Bills") }
    @Test fun `classify Vodafone as Bills`()  = runTest { assertThat(classifier.classify("", "Vodafone")).isEqualTo("Bills") }
    @Test fun `classify BESCOM as Bills`()    = runTest { assertThat(classifier.classify("", "BESCOM")).isEqualTo("Bills") }

    // ── Entertainment ────────────────────────────────────────────────────

    @Test fun `classify Netflix as Entertainment`()    = runTest { assertThat(classifier.classify("", "Netflix")).isEqualTo("Entertainment") }
    @Test fun `classify Hotstar as Entertainment`()    = runTest { assertThat(classifier.classify("", "Hotstar")).isEqualTo("Entertainment") }
    @Test fun `classify BookMyShow as Entertainment`() = runTest { assertThat(classifier.classify("", "BookMyShow")).isEqualTo("Entertainment") }
    @Test fun `classify Spotify as Entertainment`()    = runTest { assertThat(classifier.classify("", "Spotify")).isEqualTo("Entertainment") }

    // ── Health ───────────────────────────────────────────────────────────

    @Test fun `classify PharmEasy as Health`() = runTest { assertThat(classifier.classify("", "PharmEasy")).isEqualTo("Health") }
    @Test fun `classify Apollo as Health`()    = runTest { assertThat(classifier.classify("", "Apollo")).isEqualTo("Health") }
    @Test fun `classify 1mg as Health`()       = runTest { assertThat(classifier.classify("", "1mg")).isEqualTo("Health") }

    // ── Salary ───────────────────────────────────────────────────────────

    @Test fun `classify Salary as Salary`() = runTest { assertThat(classifier.classify("", "Salary")).isEqualTo("Salary") }
    @Test fun `classify Payroll as Salary`() = runTest { assertThat(classifier.classify("", "Payroll")).isEqualTo("Salary") }

    // ── Savings ──────────────────────────────────────────────────────────

    @Test fun `classify Zerodha as Savings`() = runTest { assertThat(classifier.classify("", "Zerodha")).isEqualTo("Savings") }
    @Test fun `classify Groww as Savings`()   = runTest { assertThat(classifier.classify("", "Groww")).isEqualTo("Savings") }

    // ── Fallback ─────────────────────────────────────────────────────────

    @Test fun `unknown merchant defaults to Other`() = runTest { assertThat(classifier.classify("", "XYZRandomShop")).isEqualTo("Other") }

    // ── User overrides ────────────────────────────────────────────────────

    @Test
    fun `user override takes priority over keyword match`() = runTest {
        classifier.saveOverride("Swiggy", "Entertainment") // user changed it
        assertThat(classifier.classify("", "Swiggy")).isEqualTo("Entertainment")
    }

    @Test
    fun `user override is case insensitive`() = runTest {
        classifier.saveOverride("swiggy", "Bills")
        assertThat(classifier.classify("", "SWIGGY")).isEqualTo("Bills")
    }

    // ── Raw text fallback ─────────────────────────────────────────────────

    @Test
    fun `classify via raw text when merchant is Unknown`() = runTest {
        val raw = "Your a/c was debited for Netflix subscription."
        assertThat(classifier.classify(raw, "Unknown")).isEqualTo("Entertainment")
    }
}
