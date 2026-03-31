package com.financetracker.app

import com.financetracker.app.service.sms.SmsParser
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmsParserTest {

    // ── HDFC ──────────────────────────────────────────────────────────────

    @Test
    fun `parse HDFC debit SMS`() {
        val body = "Dear Customer, Rs.1,234.56 has been debited from your A/c XX4321 on 15-03-2026. Info: SWIGGY"
        val result = SmsParser.parse(body, System.currentTimeMillis())
        assertThat(result).isNotNull()
        assertThat(result!!.amount).isWithin(0.01).of(1234.56)
        assertThat(result.isCredit).isFalse()
        assertThat(result.last4Digits).isEqualTo("4321")
    }

    @Test
    fun `parse HDFC credit SMS`() {
        val body = "Rs.50,000.00 credited to your A/c XX9876. Info: SALARY"
        val result = SmsParser.parse(body, System.currentTimeMillis())
        assertThat(result).isNotNull()
        assertThat(result!!.amount).isWithin(0.01).of(50000.0)
        assertThat(result.isCredit).isTrue()
    }

    // ── ICICI ─────────────────────────────────────────────────────────────

    @Test
    fun `parse ICICI debit SMS`() {
        val body = "ICICI Bank: Your a/c XX5678 has been debited with INR 500.00 on 01-Mar-26 at UBER."
        val result = SmsParser.parse(body, System.currentTimeMillis())
        assertThat(result).isNotNull()
        assertThat(result!!.amount).isWithin(0.01).of(500.0)
        assertThat(result.isCredit).isFalse()
        assertThat(result.last4Digits).isEqualTo("5678")
    }

    // ── SBI ───────────────────────────────────────────────────────────────

    @Test
    fun `parse SBI UPI debit`() {
        val body = "Your a/c XXXX1234 is debited by Rs.250 on 10-01-2026. UPI Ref No 123456789012. If not you, call 1800."
        val result = SmsParser.parse(body, System.currentTimeMillis())
        assertThat(result).isNotNull()
        assertThat(result!!.amount).isWithin(0.01).of(250.0)
        assertThat(result.isCredit).isFalse()
        assertThat(result.upiRef).isNotNull()
    }

    // ── Axis ──────────────────────────────────────────────────────────────

    @Test
    fun `parse Axis Bank SMS`() {
        val body = "Transaction Alert: Rs. 2500.00 spent on Axis Bank Debit Card XX7777 at AMAZON on 20-03-2026."
        val result = SmsParser.parse(body, System.currentTimeMillis())
        assertThat(result).isNotNull()
        assertThat(result!!.amount).isWithin(0.01).of(2500.0)
        assertThat(result.isCredit).isFalse()
        assertThat(result.last4Digits).isEqualTo("7777")
    }

    // ── Kotak ─────────────────────────────────────────────────────────────

    @Test
    fun `parse Kotak payment SMS`() {
        val body = "Kotak Bank: INR 1000.00 paid via UPI from a/c XX3333. Ref: 9876543210."
        val result = SmsParser.parse(body, System.currentTimeMillis())
        assertThat(result).isNotNull()
        assertThat(result!!.amount).isWithin(0.01).of(1000.0)
    }

    // ── Edge cases ────────────────────────────────────────────────────────

    @Test
    fun `return null for non-transaction SMS`() {
        val body = "Your OTP is 123456. Do not share."
        val result = SmsParser.parse(body, System.currentTimeMillis())
        assertThat(result).isNull()
    }

    @Test
    fun `parse amount with no decimal`() {
        val body = "Rs.5000 debited from your account."
        val result = SmsParser.parse(body, System.currentTimeMillis())
        assertThat(result).isNotNull()
        assertThat(result!!.amount).isWithin(0.01).of(5000.0)
    }

    @Test
    fun `parse amount with INR prefix`() {
        val body = "INR 300.00 debited from your account at ZOMATO."
        val result = SmsParser.parse(body, System.currentTimeMillis())
        assertThat(result).isNotNull()
        assertThat(result!!.amount).isWithin(0.01).of(300.0)
    }

    @Test
    fun `dedup hash differs for different amounts`() {
        val r1 = SmsParser.parse("Rs.100 debited at merchant ABC", 1000000L)
        val r2 = SmsParser.parse("Rs.200 debited at merchant ABC", 1000000L)
        assertThat(r1).isNotNull()
        assertThat(r2).isNotNull()
        assertThat(r1!!.dedupHash).isNotEqualTo(r2!!.dedupHash)
    }

    @Test
    fun `isBankSms identifies known senders`() {
        assertThat(SmsParser.isBankSms("HDFCBK")).isTrue()
        assertThat(SmsParser.isBankSms("ICICIB")).isTrue()
        assertThat(SmsParser.isBankSms("SBIBNK")).isTrue()
        assertThat(SmsParser.isBankSms("AXISBK")).isTrue()
        assertThat(SmsParser.isBankSms("unknown")).isFalse()
    }
}
