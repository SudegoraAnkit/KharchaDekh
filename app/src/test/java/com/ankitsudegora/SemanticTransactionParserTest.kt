package com.ankitsudegora

import com.ankitsudegora.util.SemanticTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SemanticTransactionParserTest {

    @Test
    fun parse_genuineDebit_returnsCorrectDetails() {
        val text = "Rs. 200 debited from a/c ...1234 to Swiggy. Ref: 314561829371"
        val parsed = SemanticTransactionParser.parse("HDFC Bank", text)
        
        assertNotNull(parsed)
        parsed?.let {
            assertEquals(200.0, it.amount, 0.0)
            assertEquals("DEBIT", it.type)
            assertEquals("Swiggy", it.merchant)
            assertEquals("314561829371", it.refNumber)
        }
    }

    @Test
    fun parse_genuineCredit_returnsCorrectDetails() {
        val text = "Rs. 1,000.50 credited to a/c ...1234 from John. Ref: 987654321012"
        val parsed = SemanticTransactionParser.parse("Alert", text)
        
        assertNotNull(parsed)
        parsed?.let {
            assertEquals(1000.50, it.amount, 0.0)
            assertEquals("CREDIT", it.type)
            assertEquals("John", it.merchant)
            assertEquals("987654321012", it.refNumber)
        }
    }

    @Test
    fun parse_promotionalPreApprovedSpam_returnsNull() {
        val text = "Congrats! You are pre-approved for a personal loan up to Rs. 5,00,000. Apply now!"
        val parsed = SemanticTransactionParser.parse("Promo", text)
        assertNull(parsed)
    }

    @Test
    fun parse_promotionalCashbackOffer_returnsNull() {
        val texts = listOf(
            "Get cashback of Rs 100 on your next order",
            "Win cash coupon coupon30 for discount",
            "Use voucher for extra Rs. 200 discount",
            "Scratch card offer: win cash rewards up to 1000",
            "Claim your offer promo on Amazon today"
        )
        for (t in texts) {
            assertNull(SemanticTransactionParser.parse("SMS", t))
        }
    }

    @Test
    fun parse_otpNotification_returnsNull() {
        val text = "Rs. 500 transaction otp code is 562134. Valid for 10 minutes. Do not share."
        val parsed = SemanticTransactionParser.parse("Bank Security", text)
        assertNull(parsed)
    }

    @Test
    fun parse_failedOrDeclinedTransaction_returnsNull() {
        val failedText = "Transaction of Rs. 150 failed at Swiggy. Insufficient balance."
        val declinedText = "Txn Rs. 200 declined at PhonePe. Contact bank."
        
        assertNull(SemanticTransactionParser.parse("HDFC Bank", failedText))
        assertNull(SemanticTransactionParser.parse("PhonePe", declinedText))
    }

    @Test
    fun parse_billStatementAlert_returnsNull() {
        val text = "Your HDFC Credit Card bill is generated. Rs 4500 is due on 25-Jun. Minimum amount due is Rs 200."
        val parsed = SemanticTransactionParser.parse("HDFC Card", text)
        assertNull(parsed)
    }

    @Test
    fun parse_consecutiveSameAmountNonUtr_extractsCorrectValues() {
        // Checking parsing of consecutive notifications
        val text1 = "Spent Rs. 10 at TeaShop"
        val text2 = "Spent Rs. 10 at TeaShop"
        
        val parsed1 = SemanticTransactionParser.parse("GooglePay", text1)
        val parsed2 = SemanticTransactionParser.parse("GooglePay", text2)
        
        assertNotNull(parsed1)
        assertEquals(10.0, parsed1!!.amount, 0.0)
        assertEquals("DEBIT", parsed1.type)
        assertEquals("TeaShop", parsed1.merchant)
        
        assertNotNull(parsed2)
        assertEquals(10.0, parsed2!!.amount, 0.0)
        assertEquals("DEBIT", parsed2.type)
        assertEquals("TeaShop", parsed2.merchant)
    }
}