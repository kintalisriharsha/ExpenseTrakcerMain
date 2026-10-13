package com.example.expensetracker.frontend.sms

import android.util.Log
import com.example.expensetracker.services.entity.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsParser {

    private const val TAG = "SmsParser"

    // Primary currency + amount pattern (e.g. "Rs. 50.00", "USD 10", "$500")
    private val amountRegex = Regex(
        """(?:Rs\.?|INR|USD|\$)\s?([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Amounts preceded by these words are limits/balances/dues, not the transaction itself
    private val ignoredAmountContextRegex = Regex(
        """(avl|avail\w*|bal\w*|limit|lmt|outstanding|total\s*due|min\w*\s*due)[^\d]{0,15}$""",
        RegexOption.IGNORE_CASE
    )

    // Fallback amount regex anchored to keywords when no currency symbol is present
    private val amountNearKeywordRegex = Regex(
        """(?:debited|credited|spent|paid|withdrawn|purchase|payment|charged|received|deposited|refund)\D{0,15}?([\d,]+(?:\.\d{2})?)""" +
                """|([\d,]+(?:\.\d{2})?)\D{0,15}?(?:debited|credited|spent|paid|withdrawn|purchase|payment|charged|received|deposited|refund)""",
        RegexOption.IGNORE_CASE
    )

    private val nonAmountDigitSpanRegex = Regex(
        """(?:a/?c(?:count)?\.?\s*(?:no\.?)?[:.]?\s*[Xx*]{2,}\d{2,})""" +
                """|(?:card\s*(?:no\.?|number)?\s*(?:ending)?\s*(?:in|with)?\s*[Xx*]{2,}\d{2,})""" +
                """|(?:(?:ref(?:erence)?|txn|transaction|utr|rrn)\s*(?:no\.?|id)?[:.]?\s*\d{4,})""" +
                """|(?:ending\s+(?:in|with)?\s*\d{2,})""" +
                """|(?:[Xx*]{2,}\d{2,})""",
        RegexOption.IGNORE_CASE
    )

    private const val MAX_PLAUSIBLE_AMOUNT = 9_999_999.0

    // Generic bank alert signature for unlisted sender headers
    private val genericBankAlertRegex = Regex(
        """a/?c\s*(?:no)?[.:]?\s*[Xx*]{2,}\d|avl\s*bal|available\s*bal|acct\s*(?:no)?[.:]?\s*[Xx*]{2,}|card\s+(?:ending|no)\s*[Xx*]{0,}\d""",
        RegexOption.IGNORE_CASE
    )

    // Card bill payments, dues and statement alerts are NOT expenses
    private val nonTransactionRegex = Regex(
        """payment\s+(?:of\s+)?(?:rs\.?\s?[\d,.]+\s+)?(?:has\s+been\s+)?received""" +
                """|received\s+(?:towards|against)\s+(?:your\s+)?(?:\w+\s+)?card""" +
                """|thank\s+you\s+for\s+(?:the\s+)?payment""" +
                """|(?:payment|bill|minimum|min\.?|total)\s+(?:amount\s+)?(?:is\s+)?due""" +
                """|statement\s+(?:for|generated|dated|is\s+ready)""",
        RegexOption.IGNORE_CASE
    )

    // Detects spends made with a credit card
    private val creditCardRegex = Regex(
        """credit\s*card|\bcr\.?\s*card\b|\bcredit\s*crd\b|\bcc\b|sbi\s*card|SBICRD""",
        RegexOption.IGNORE_CASE
    )

    // Clear debit words
    private val strongDebitKeywords = listOf(
        "debited", "spent", "paid", "withdrawn", "purchase", "charged"
    )

    // Looser words; only count as debit when the SMS does not look like money coming in
    private val weakDebitKeywords = listOf("debit", "payment")

    private val incomingMoneyRegex = Regex(
        """\bcredited\b|\breceived\b|\bdeposited\b|\brefund""",
        RegexOption.IGNORE_CASE
    )

    // Ordered payee/merchant extraction regexes (most specific first)
    private val titlePatterns = listOf(
        Regex("""[;.]?\s*([A-Za-z0-9&.\-_' ]{2,30}?)\s+credited\b""", RegexOption.IGNORE_CASE),
        Regex("""\bat\s+([A-Za-z0-9&.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\bto\s+(?:VPA\s+)?([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\bfrom\s+([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""Info:\s*(?:UPI|POS|IMPS|NEFT)?/?[\d]*?/?([A-Za-z0-9&.\-_' ]{2,30})(?:\.|,|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\btrf\s+to\s+([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\bcredited\s+(?:to\s+your\s+a/?c\s+)?(?:by|from)\s+([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\btowards\s+([A-Za-z0-9&.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
    )

    // Order matters: first matching category wins
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
            "indigo", "vistara", "airindia", "redbus", "yatra", "cumta"
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

    // Icon shown for each category
    private val categoryIcons: Map<String, String> = mapOf(
        "Food & Dining"     to "🍔",
        "Groceries"         to "🛒",
        "Shopping"          to "🛍️",
        "Travel"            to "✈️",
        "Fuel"              to "⛽",
        "Bills & Utilities" to "💡",
        "Entertainment"     to "🎬",
        "Healthcare"        to "🏥",
        "Rent"              to "🏠",
        "Insurance"         to "🛡️",
        "Loan / EMI"        to "🏦",
        "ATM Withdrawal"    to "💵",
        "Transfer"          to "🔁",
        "Other"             to "💸",
    )

    private fun iconForCategory(category: String): String =
        categoryIcons[category] ?: "💸"


    private val bankPatterns: List<Pair<Regex, String>> = listOf(
        Regex("ICICI", RegexOption.IGNORE_CASE)                         to "ICICI Bank",
        Regex("HDFC", RegexOption.IGNORE_CASE)                          to "HDFC Bank",
        Regex("""\bSBI\b|SBIINB|SBIPSG|SBICRD""", RegexOption.IGNORE_CASE) to "State Bank of India",
        Regex("AXIS", RegexOption.IGNORE_CASE)                          to "Axis Bank",
        Regex("KOTAK", RegexOption.IGNORE_CASE)                         to "Kotak Bank",
        Regex("""\bPNB\b|PNBSMS""", RegexOption.IGNORE_CASE)           to "Punjab National Bank",
        Regex("""BANK OF INDIA|\bBOI\b|BOIIND""", RegexOption.IGNORE_CASE)  to "Bank of India",
        Regex("""CANARA|\bCANBNK\b""", RegexOption.IGNORE_CASE)        to "Canara Bank",
        Regex("PAYTM", RegexOption.IGNORE_CASE)                         to "Paytm",
        Regex("""YES\s?BANK|YESBNK""", RegexOption.IGNORE_CASE)        to "Yes Bank",
        Regex("""INDIAN\s?BANK|INDBNK""", RegexOption.IGNORE_CASE)     to "Indian Bank",
        Regex("""UNION\s?BANK|UBIN|UNIONB""", RegexOption.IGNORE_CASE) to "Union Bank of India",
        Regex("""BANK OF BARODA|\bBOB\b|BOBIBN""", RegexOption.IGNORE_CASE) to "Bank of Baroda",
        Regex("""IDBI""", RegexOption.IGNORE_CASE)                     to "IDBI Bank",
        Regex("""IDFC""", RegexOption.IGNORE_CASE)                     to "IDFC First Bank",
        Regex("""INDUSIND|INDUS""", RegexOption.IGNORE_CASE)           to "IndusInd Bank",
        Regex("""FEDERAL\s?BANK|FEDBNK""", RegexOption.IGNORE_CASE)    to "Federal Bank",
        Regex("""PHONEPE""", RegexOption.IGNORE_CASE)                  to "PhonePe",
        Regex("""GPAY|GOOGLEPAY""", RegexOption.IGNORE_CASE)           to "Google Pay",
        Regex("""AMAZONPAY""", RegexOption.IGNORE_CASE)                to "Amazon Pay",
    )

    private fun matchedBankName(text: String): String? {
        for ((pattern, name) in bankPatterns) {
            if (pattern.containsMatchIn(text)) return name
        }
        return null
    }

    private fun cleanRawSenderFallback(sender: String): String {
        val cleaned = sender
            .replace(Regex("""^[A-Za-z]{2}-"""), "")
            .replace(Regex("""-[A-Za-z]$"""), "")
            .trim()
        return if (cleaned.isNotBlank()) cleaned else "Bank Alert"
    }

    /** First currency amount that is not an available limit / balance / due figure. */
    private fun findTransactionAmount(body: String): String? {
        for (m in amountRegex.findAll(body)) {
            val before = body.substring(maxOf(0, m.range.first - 25), m.range.first)
            if (ignoredAmountContextRegex.containsMatchIn(before)) continue
            return m.groupValues[1].replace(",", "")
        }
        return null
    }

    fun parse(sender: String, body: String, smsTimestampMillis: Long = 0L): ExpenseEntity? {
        val normalizedSender = sender.trim()

        // 1. BANK DETECTION
        val bankFromSender = matchedBankName(normalizedSender)
        val bankFromBody = matchedBankName(body)
        val matchesGenericAlert = genericBankAlertRegex.containsMatchIn(body)
        val isBank = bankFromSender != null || bankFromBody != null || matchesGenericAlert

        if (!isBank) {
            Log.d(TAG, "Rejected: Not recognized as financial SMS")
            return null
        }

        // Card bill payments / dues / statements are not expenses
        if (nonTransactionRegex.containsMatchIn(body)) {
            Log.d(TAG, "Rejected: bill payment / due / statement alert")
            return null
        }

        // 2. AMOUNT DETECTION & FALLBACK
        var amountSource = "primary"
        var amountText = findTransactionAmount(body)

        if (amountText == null) {
            val sanitizedBody = body.replace(nonAmountDigitSpanRegex, " ")
            val fallback = amountNearKeywordRegex.find(sanitizedBody)
            amountText = (fallback?.groupValues?.get(1)?.takeIf { it.isNotEmpty() }
                ?: fallback?.groupValues?.get(2))
                ?.replace(",", "")
            amountSource = "fallback"
        }

        if (amountText == null) {
            Log.d(TAG, "Rejected: No valid amount found in SMS")
            return null
        }

        val amount = amountText.toDoubleOrNull()
        if (amount == null) {
            Log.d(TAG, "Rejected: Failed to parse amount as Double")
            return null
        }

        if (amount > MAX_PLAUSIBLE_AMOUNT) {
            Log.d(TAG, "Rejected: Amount $amount exceeds plausible transaction ceiling (source=$amountSource)")
            return null
        }

        // 3. DEBIT ONLY - all credits are ignored
        val lowerBody = body.lowercase()
        val hasStrongDebit = strongDebitKeywords.any { lowerBody.contains(it) }
        val hasWeakDebit = weakDebitKeywords.any { lowerBody.contains(it) }
        val isDebit = hasStrongDebit || (hasWeakDebit && !incomingMoneyRegex.containsMatchIn(body))

        if (!isDebit) {
            Log.d(TAG, "Rejected: Not a debit transaction")
            return null
        }

        // 4. PAYMENT MODE
        val isCreditCard = creditCardRegex.containsMatchIn(body) ||
                creditCardRegex.containsMatchIn(normalizedSender)
        val paymentMode = if (isCreditCard) "Credit Card" else "Bank"

        // 5. TITLE / MERCHANT EXTRACTION WITH FALLBACKS
        val extractedName = extractTitleFromBody(body)
        val bankName = bankFromSender ?: bankFromBody
        val titleFallback = bankName ?: cleanRawSenderFallback(normalizedSender)
        val title = extractedName ?: titleFallback

        // 6. CATEGORY
        val category = classifyCategory(title, body)

        // 7. TIMESTAMP
        val now = if (smsTimestampMillis > 0L) Date(smsTimestampMillis) else Date()

        Log.d(TAG, "Parsed OK (amountSource=$amountSource): title='$title', category='$category', amount=$amount, mode=$paymentMode")

        return ExpenseEntity(
            category      = category,
            amount        = amount,
            notes         = "Auto-parsed from SMS: $body",
            date          = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(now),
            time          = SimpleDateFormat("hh:mm a",     Locale.ENGLISH).format(now),
            contactName   = title,
            contactNumber = sender,
            icon          = iconForCategory(category),
            title         = title,
            tab           = true,
            paymentMode   = paymentMode,
        )
    }

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

    /** Always returns a non-empty category (debits only). */
    private fun classifyCategory(title: String, body: String): String {
        val haystack = "$title $body".lowercase()
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { haystack.contains(it) }) return category
        }

        val lowerBody = body.lowercase()
        if (listOf("upi", "imps", "neft", "rtgs").any { lowerBody.contains(it) }) return "Transfer"

        return "Other"
    }
}