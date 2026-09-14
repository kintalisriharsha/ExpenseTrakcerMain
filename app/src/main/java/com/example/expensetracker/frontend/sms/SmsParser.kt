package com.example.expensetracker.frontend.sms

import android.util.Log
import com.example.expensetracker.services.entity.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsParser {

    private const val TAG = "SmsParser"

    private val amountRegex = Regex(
        """(?:Rs\.?|INR|USD|\$)\s?([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "withdrawn",
        "purchase", "payment", "charged"
    )

    private val creditKeywords = listOf(
        "credited", "credit", "received", "deposited", "refund"
    )

    // Matched against BOTH the sender ID and the message body — catches bank
    // names spelled out in plain text, regardless of the DLT sender header format
    private val bankKeywords = listOf(
        "HDFC", "ICICI", "SBI", "AXIS",
        "KOTAK", "PNB", "BANK OF INDIA", "CANARA",
        "PAYTM", "YES BANK", "INDIAN BANK"
    )

    private val titlePatterns = listOf(
        Regex("""\bat\s+([A-Za-z0-9&.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\bto\s+(?:VPA\s+)?([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\bfrom\s+([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""Info:\s*(?:UPI|POS|IMPS|NEFT)?/?[\d]*?/?([A-Za-z0-9&.\-_' ]{2,30})(?:\.|,|\s*$)""", RegexOption.IGNORE_CASE),
    )

    private val categoryKeywords: LinkedHashMap<String, List<String>> = linkedMapOf(
        "Food & Dining" to listOf(
            "swiggy", "zomato", "restaurant", "cafe", "food", "dominos",
            "pizza", "starbucks", "mcdonald", "kfc", "eatsure"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa",
            "reliance", "dmart", "bigbasket", "shop"
        ),
        "Travel" to listOf(
            "uber", "ola", "rapido", "irctc", "makemytrip", "goibibo",
            "indigo", "vistara", "airindia", "redbus", "yatra"
        ),
        "Bills & Utilities" to listOf(
            "electricity", "airtel", "jio", "vodafone", "vi ", "recharge",
            "broadband", "gas", "water bill", "dth", "tata power"
        ),
        "Entertainment" to listOf(
            "netflix", "spotify", "primevideo", "hotstar", "bookmyshow",
            "sonyliv", "youtube premium"
        ),
        "Healthcare" to listOf(
            "pharmacy", "apollo", "hospital", "clinic", "medplus",
            "practo", "1mg", "netmeds"
        ),
        "ATM Withdrawal" to listOf(
            "atm"
        ),
    )

    fun parse(sender: String, body: String): ExpenseEntity? {
        val normalizedSender = sender.trim()

        val isBank = bankKeywords.any { keyword ->
            normalizedSender.contains(keyword, ignoreCase = true) ||
                    body.contains(keyword, ignoreCase = true)
        }
        Log.d(TAG, "sender='$normalizedSender' isBank=$isBank")
        if (!isBank) {
            Log.d(TAG, "Rejected: neither sender nor body matched any known bank")
            return null
        }

        val amountText = amountRegex.find(body)
            ?.groupValues?.get(1)
            ?.replace(",", "")
        Log.d(TAG, "amountText=$amountText")
        if (amountText == null) {
            Log.d(TAG, "Rejected: no amount pattern matched in body")
            return null
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null) {
            Log.d(TAG, "Rejected: amountText '$amountText' did not parse as Double")
            return null
        }

        val lowerBody = body.lowercase()
        val isDebit  = debitKeywords.any  { lowerBody.contains(it) }
        val isCredit = creditKeywords.any { lowerBody.contains(it) }
        Log.d(TAG, "isDebit=$isDebit isCredit=$isCredit")

        if (!isDebit && !isCredit) {
            Log.d(TAG, "Rejected: no debit or credit keyword found")
            return null
        }

        val now = Date()
        val title = extractTitle(body, sender)
        val category = classifyCategory(title, body, isDebit)
        Log.d(TAG, "Parsed OK: title=$title category=$category amount=$amount")

        return ExpenseEntity(
            category      = category,
            amount        = amount,
            notes         = "Auto-parsed from SMS: $body",
            date          = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(now),
            time          = SimpleDateFormat("hh:mm a",     Locale.getDefault()).format(now),
            contactName   = sender,
            contactNumber = sender,
            icon          = if (isDebit) "\uD83D\uDCB8" else "\uD83D\uDCB0",
            title         = title,
            tab           = isDebit,
        )
    }

    private fun extractTitle(body: String, sender: String): String {
        for (pattern in titlePatterns) {
            val match = pattern.find(body)?.groupValues?.get(1)?.trim()
            if (!match.isNullOrBlank()) {
                return match
                    .replace(Regex("""\s+"""), " ")
                    .split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
            }
        }
        return sender
    }

    private fun classifyCategory(title: String, body: String, isDebit: Boolean): String {
        if (!isDebit) return "Income"

        val haystack = "$title $body".lowercase()
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { haystack.contains(it) }) {
                return category
            }
        }
        return "Other"
    }
}