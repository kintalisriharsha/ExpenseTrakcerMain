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
        "Groceries" to listOf(
            "grocery", "groceries", "supermarket", "kirana", "zepto",
            "blinkit", "instamart", "bigbasket", "dmart"
        ),
        "Shopping" to listOf(
            "amazon", "flipkart", "myntra", "ajio", "meesho", "nykaa",
            "reliance", "shop"
        ),
        "Travel" to listOf(
            "uber", "ola", "rapido", "irctc", "makemytrip", "goibibo",
            "indigo", "vistara", "airindia", "redbus", "yatra"
        ),
        "Fuel" to listOf(
            "petrol", "diesel", "fuel", "hpcl", "iocl", "bpcl", "indianoil"
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
        "Rent" to listOf(
            "rent", "nobroker", "housing.com"
        ),
        "Insurance" to listOf(
            "insurance", "policybazaar", "premium due", " lic "
        ),
        "Loan / EMI" to listOf(
            "emi", "loan", "instalment", "installment"
        ),
        "ATM Withdrawal" to listOf(
            "atm"
        ),
    )

    // Maps a raw DLT sender header (e.g. "VM-ICICIB-S", "AD-HDFCBK") to a clean,
    // readable bank name — this is what shows up instead of the raw sender code
    // when no merchant name could be extracted from the message body.
    private val bankDisplayNames: List<Pair<Regex, String>> = listOf(
        Regex("ICICI", RegexOption.IGNORE_CASE)              to "ICICI Bank",
        Regex("HDFC", RegexOption.IGNORE_CASE)                to "HDFC Bank",
        Regex("""\bSBI\b|SBIINB|SBIPSG""", RegexOption.IGNORE_CASE) to "State Bank of India",
        Regex("AXIS", RegexOption.IGNORE_CASE)                to "Axis Bank",
        Regex("KOTAK", RegexOption.IGNORE_CASE)               to "Kotak Bank",
        Regex("""\bPNB\b""", RegexOption.IGNORE_CASE)         to "Punjab National Bank",
        Regex("""BANK OF INDIA|\bBOI\b""", RegexOption.IGNORE_CASE) to "Bank of India",
        Regex("""CANARA|\bCANBNK\b""", RegexOption.IGNORE_CASE)     to "Canara Bank",
        Regex("PAYTM", RegexOption.IGNORE_CASE)               to "Paytm",
        Regex("""YES\s?BANK|YESBNK""", RegexOption.IGNORE_CASE)     to "Yes Bank",
        Regex("""INDIAN\s?BANK|INDBNK""", RegexOption.IGNORE_CASE)  to "Indian Bank",
    )

    /** Cleans a raw sender header into a readable bank name as a last-resort fallback
     *  (used only when no merchant name could be extracted from the SMS body). */
    private fun cleanBankName(sender: String): String {
        for ((pattern, name) in bankDisplayNames) {
            if (pattern.containsMatchIn(sender)) return name
        }
        // Strip a generic DLT header shape like "VM-" / "-S" if we don't recognize the bank
        return sender
            .replace(Regex("""^[A-Za-z]{2}-"""), "")
            .replace(Regex("""-[A-Za-z]$"""), "")
    }

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
        // extractTitle() falls back to the raw sender when no merchant pattern
        // matched — in that case show a clean bank name instead of the raw
        // DLT sender code (e.g. "ICICI Bank" instead of "VM-ICICIB-S").
        val contactDisplayName = if (title != sender) title else cleanBankName(normalizedSender)
        Log.d(TAG, "Parsed OK: title=$title category=$category amount=$amount contact=$contactDisplayName")

        return ExpenseEntity(
            category      = category,
            amount        = amount,
            notes         = "Auto-parsed from SMS: $body",
            date          = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(now),
            time          = SimpleDateFormat("hh:mm a",     Locale.ENGLISH).format(now),
            contactName   = contactDisplayName,
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
        val lowerBody = body.lowercase()

        if (!isDebit) {
            return when {
                lowerBody.contains("salary")   -> "Salary"
                lowerBody.contains("refund")   -> "Refund"
                lowerBody.contains("cashback") -> "Cashback"
                else                           -> "Income"
            }
        }

        val haystack = "$title $body".lowercase()
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { haystack.contains(it) }) {
                return category
            }
        }

        // No merchant keyword matched. If the extracted "title" looks like a plain
        // person's name (not an all-caps bank/DLT code) and the message is a
        // UPI/IMPS/NEFT/RTGS payment, it's almost certainly a peer-to-peer
        // transfer rather than a real "Other" expense — label it accordingly.
        val looksLikePersonName = Regex("""^[A-Za-z]+(\s[A-Za-z]+){0,2}$""").matches(title) &&
                title != title.uppercase()
        val isTransferRail = listOf("upi", "imps", "neft", "rtgs").any { lowerBody.contains(it) }
        if (looksLikePersonName && isTransferRail) {
            return "Transfer"
        }

        return "Other"
    }
}