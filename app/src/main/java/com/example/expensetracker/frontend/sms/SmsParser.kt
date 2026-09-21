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

    // Fallback for messages that state the amount without a currency symbol right
    // next to it (e.g. "500.00 has been debited", "debited with 500"). Anchored to
    // a transaction keyword on either side so it doesn't grab unrelated numbers
    // like account digits or reference IDs.
    private val amountNearKeywordRegex = Regex(
        """(?:debited|credited|spent|paid|withdrawn|purchase|payment|charged|received|deposited|refund)\D{0,15}?([\d,]+\.\d{2})""" +
                """|([\d,]+\.\d{2})\D{0,15}?(?:debited|credited|spent|paid|withdrawn|purchase|payment|charged|received|deposited|refund)""",
        RegexOption.IGNORE_CASE
    )

    // Generic signature of a bank/transaction alert, for senders/bodies that don't
    // match any name in bankPatterns (a hardcoded list can never cover every bank).
    // These are the phrases banks almost always include regardless of which bank
    // it is, so this catches legitimate alerts that would otherwise be silently
    // dropped just because the institution wasn't on the list.
    private val genericBankAlertRegex = Regex(
        """a/?c\s*(?:no)?[.:]?\s*[Xx*]{2,}\d|avl\s*bal|available\s*bal|acct\s*(?:no)?[.:]?\s*[Xx*]{2,}|card\s+(?:ending|no)\s*[Xx*]{0,}\d""",
        RegexOption.IGNORE_CASE
    )

    private val debitKeywords = listOf(
        "debited", "debit", "spent", "paid", "withdrawn",
        "purchase", "payment", "charged"
    )

    private val creditKeywords = listOf(
        "credited", "credit", "received", "deposited", "refund"
    )

    private val titlePatterns = listOf(
        Regex("""\bat\s+([A-Za-z0-9&.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\bto\s+(?:VPA\s+)?([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\bfrom\s+([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""Info:\s*(?:UPI|POS|IMPS|NEFT)?/?[\d]*?/?([A-Za-z0-9&.\-_' ]{2,30})(?:\.|,|\s*$)""", RegexOption.IGNORE_CASE),
        // Additional real-world formats banks actually send
        Regex("""\btrf\s+to\s+([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\bcredited\s+(?:to\s+your\s+a/?c\s+)?(?:by|from)\s+([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\btowards\s+([A-Za-z0-9&.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
    )

    private val categoryKeywords: Map<String, List<String>> = linkedMapOf(
        "Food & Dining" to listOf(
            "swiggy", "zomato", "restaurant", "cafe", "food", "dominos",
            "pizza", "starbucks", "mcdonald", "kfc", "eatsure", "smartq"
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
    ).mapValues { (_, keywords) -> keywords.map { it.lowercase() } }

    private val bankPatterns: List<Pair<Regex, String>> = listOf(
        Regex("ICICI", RegexOption.IGNORE_CASE)                       to "ICICI Bank",
        Regex("HDFC", RegexOption.IGNORE_CASE)                        to "HDFC Bank",
        Regex("""\bSBI\b|SBIINB|SBIPSG|SBICRD""", RegexOption.IGNORE_CASE) to "State Bank of India",
        Regex("AXIS", RegexOption.IGNORE_CASE)                        to "Axis Bank",
        Regex("KOTAK", RegexOption.IGNORE_CASE)                       to "Kotak Bank",
        Regex("""\bPNB\b|PNBSMS""", RegexOption.IGNORE_CASE)          to "Punjab National Bank",
        Regex("""BANK OF INDIA|\bBOI\b|BOIIND""", RegexOption.IGNORE_CASE)  to "Bank of India",
        Regex("""CANARA|\bCANBNK\b""", RegexOption.IGNORE_CASE)       to "Canara Bank",
        Regex("PAYTM", RegexOption.IGNORE_CASE)                       to "Paytm",
        Regex("""YES\s?BANK|YESBNK""", RegexOption.IGNORE_CASE)       to "Yes Bank",
        Regex("""INDIAN\s?BANK|INDBNK""", RegexOption.IGNORE_CASE)    to "Indian Bank",
        Regex("""UNION\s?BANK|UBIN|UNIONB""", RegexOption.IGNORE_CASE) to "Union Bank of India",
        Regex("""BANK OF BARODA|\bBOB\b|BOBIBN""", RegexOption.IGNORE_CASE) to "Bank of Baroda",
        Regex("""IDBI""", RegexOption.IGNORE_CASE)                    to "IDBI Bank",
        Regex("""IDFC""", RegexOption.IGNORE_CASE)                    to "IDFC First Bank",
        Regex("""INDUSIND|INDUS""", RegexOption.IGNORE_CASE)          to "IndusInd Bank",
        Regex("""FEDERAL\s?BANK|FEDBNK""", RegexOption.IGNORE_CASE)   to "Federal Bank",
        Regex("""PHONEPE""", RegexOption.IGNORE_CASE)                 to "PhonePe",
        Regex("""GPAY|GOOGLEPAY""", RegexOption.IGNORE_CASE)          to "Google Pay",
        Regex("""AMAZONPAY""", RegexOption.IGNORE_CASE)               to "Amazon Pay",
    )

    /** Returns the canonical, registered display name of the bank/payment service
     *  matched in [text] (sender header or message body), or null if none matched. */
    private fun matchedBankName(text: String): String? {
        for ((pattern, name) in bankPatterns) {
            if (pattern.containsMatchIn(text)) return name
        }
        return null
    }

    /** Last-resort cleanup of a raw DLT header (e.g. "VM-ICICIB-S") for cases where
     *  even bankPatterns didn't recognize it — strips the generic prefix/suffix
     *  letters DLT headers use, rather than showing the raw code as-is. */
    private fun cleanRawSenderFallback(sender: String): String {
        return sender
            .replace(Regex("""^[A-Za-z]{2}-"""), "")
            .replace(Regex("""-[A-Za-z]$"""), "")
    }

    fun parse(sender: String, body: String, smsTimestampMillis: Long = 0L): ExpenseEntity? {
        val normalizedSender = sender.trim()

        val bankFromSender = matchedBankName(normalizedSender)
        val bankFromBody = matchedBankName(body)
        // Named-list match is preferred (it gives us a clean display name), but an
        // unlisted bank whose message still carries the standard alert phrasing
        // (masked account number, "avl bal", etc.) is accepted too — a hardcoded
        // list of ~20 banks can never cover every institution that texts alerts.
        val matchesGenericAlert = genericBankAlertRegex.containsMatchIn(body)
        val isBank = bankFromSender != null || bankFromBody != null || matchesGenericAlert
        Log.d(TAG, "sender='$normalizedSender' isBank=$isBank (generic=$matchesGenericAlert)")
        if (!isBank) {
            Log.d(TAG, "Rejected: neither sender nor body matched any known bank/payment service or generic alert pattern")
            return null
        }

        var amountText = amountRegex.find(body)
            ?.groupValues?.get(1)
            ?.replace(",", "")
        if (amountText == null) {
            val fallback = amountNearKeywordRegex.find(body)
            amountText = (fallback?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
                ?: fallback?.groupValues?.get(2))
                ?.replace(",", "")
        }
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

        // Use the SMS's own send time so a message that Android delivers to us late
        // (first-run permission dialogs, Doze/battery throttling, delayed delivery)
        // still lands on the day/time it actually happened, instead of every
        // delayed message bunching up onto "today".
        val now = if (smsTimestampMillis > 0L) Date(smsTimestampMillis) else Date()

        // Prefer the actual payee/payer name found inside the message body.
        // Only fall back to the bank/service's clean, registered name (never the
        // raw sender/getOriginatingAddress header) when no name is found in the text.
        val nameFoundInBody = extractTitleFromBody(body)
        val fallbackBankName = bankFromSender ?: bankFromBody ?: cleanRawSenderFallback(normalizedSender)
        val title = nameFoundInBody ?: fallbackBankName

        val category = classifyCategory(title, body, isDebit)
        Log.d(TAG, "Parsed OK: title=$title category=$category amount=$amount")

        return ExpenseEntity(
            category      = category,
            amount        = amount,
            notes         = "Auto-parsed from SMS: $body",
            date          = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(now),
            time          = SimpleDateFormat("hh:mm a",     Locale.ENGLISH).format(now),
            contactName   = title,
            contactNumber = sender,
            icon          = if (isDebit) "\uD83D\uDCB8" else "\uD83D\uDCB0",
            title         = title,
            tab           = isDebit,
        )
    }

    /** Returns the name found inside the message body (merchant, payee, or payer),
     *  or null if none of the patterns matched — caller decides the fallback. */
    private fun extractTitleFromBody(body: String): String? {
        for (pattern in titlePatterns) {
            val match = pattern.find(body)?.groupValues?.get(1)?.trim()
            if (!match.isNullOrBlank()) {
                return match
                    .replace(Regex("""\s+"""), " ")
                    .split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercase) }
            }
        }
        return null
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

        // No merchant keyword matched. If the extracted title looks like a plain
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