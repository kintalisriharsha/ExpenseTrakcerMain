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

    // Real transaction amounts are essentially never this large; guards against a stray
    // long digit sequence (phone number, OTP, etc.) that slips past sanitization.
    private const val MAX_PLAUSIBLE_AMOUNT = 9_999_999.0

    // Generic bank alert signature for unlisted sender headers
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

    // Ordered payee/merchant extraction regexes (most specific first)
    private val titlePatterns = listOf(
        // ICICI & general bank format: "...debited for Rs X; <Name> credited."
        Regex("""[;.]?\s*([A-Za-z0-9&.\-_' ]{2,30}?)\s+credited\b""", RegexOption.IGNORE_CASE),
        Regex("""\bat\s+([A-Za-z0-9&.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\bto\s+(?:VPA\s+)?([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""\bfrom\s+([A-Za-z0-9@.\-_' ]{2,30}?)(?:\s+on\b|\s+ref\b|[.,]|\s*$)""", RegexOption.IGNORE_CASE),
        Regex("""Info:\s*(?:UPI|POS|IMPS|NEFT)?/?[\d]*?/?([A-Za-z0-9&.\-_' ]{2,30})(?:\.|,|\s*$)""", RegexOption.IGNORE_CASE),
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

    fun parse(sender: String, body: String, smsTimestampMillis: Long = 0L): ExpenseEntity? {
        val normalizedSender = sender.trim()

        // 1. BANK DETECTION & FALLBACK
        val bankFromSender = matchedBankName(normalizedSender)
        val bankFromBody = matchedBankName(body)
        val matchesGenericAlert = genericBankAlertRegex.containsMatchIn(body)
        val isBank = bankFromSender != null || bankFromBody != null || matchesGenericAlert

        if (!isBank) {
            Log.d(TAG, "Rejected: Not recognized as financial SMS")
            return null
        }

        // 2. AMOUNT DETECTION & FALLBACK
        var amountSource = "primary"
        var amountText = amountRegex.find(body)
            ?.groupValues?.get(1)
            ?.replace(",", "")

        if (amountText == null) {
            // Strip account/card/reference numbers so the keyword-proximity fallback
            // can't mistake them for the transaction amount (e.g. "debited from
            // A/c XX8823451" used to yield amount = 8823451).
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

        // Sanity guard: reject implausibly large "amounts" that are almost certainly
        // a mis-captured account/reference/phone number that slipped through.
        if (amount > MAX_PLAUSIBLE_AMOUNT) {
            Log.d(TAG, "Rejected: Amount $amount exceeds plausible transaction ceiling (source=$amountSource)")
            return null
        }

        // 3. DEBIT/CREDIT DETECTION
        val lowerBody = body.lowercase()
        val isDebit = debitKeywords.any { lowerBody.contains(it) }
        val isCredit = creditKeywords.any { lowerBody.contains(it) }

        if (!isDebit && !isCredit) {
            Log.d(TAG, "Rejected: No debit or credit keywords matched")
            return null
        }

        // 4. TITLE / MERCHANT EXTRACTION WITH FALLBACKS
        // Order: Extracted Name -> Bank Name -> Cleaned Sender Header -> "Unknown Merchant"
        val extractedName = extractTitleFromBody(body)
        val bankName = bankFromSender ?: bankFromBody
        val titleFallback = bankName ?: cleanRawSenderFallback(normalizedSender)
        val title = extractedName ?: titleFallback

        // 5. CATEGORY CLASSIFICATION WITH GUARANTEED FALLBACK
        val category = classifyCategory(title, body, isDebit)

        // 6. TIMESTAMP FALLBACK
        val now = if (smsTimestampMillis > 0L) Date(smsTimestampMillis) else Date()

        Log.d(TAG, "Parsed OK (amountSource=$amountSource): title='$title', category='$category', amount=$amount")

        return ExpenseEntity(
            category      = category,
            amount        = amount,
            notes         = "Auto-parsed from SMS: $body",
            date          = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(now),
            time          = SimpleDateFormat("hh:mm a",     Locale.ENGLISH).format(now),
            contactName   = title,
            contactNumber = sender,
            icon          = if (isDebit) "\uD83D\uDCB8" else "\uD83D\uDCB0", // 💸 or 💰
            title         = title,
            tab           = isDebit,
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

    /**
     * Categorizes the SMS into a specific bucket.
     * GUARANTEED: Always returns a valid category string; never returns null or empty.
     */
    private fun classifyCategory(title: String, body: String, isDebit: Boolean): String {
        val lowerBody = body.lowercase()

        // Income / Credit categorization
        if (!isDebit) {
            return when {
                lowerBody.contains("salary")   -> "Salary"
                lowerBody.contains("refund")   -> "Refund"
                lowerBody.contains("cashback") -> "Cashback"
                else                           -> "Income" // Default fallback for incoming money
            }
        }

        // Expense / Debit categorization by merchant keywords
        val haystack = "$title $body".lowercase()
        for ((category, keywords) in categoryKeywords) {
            if (keywords.any { haystack.contains(it) }) {
                return category
            }
        }

        // Fallback 1: Bank transfer payment rails (UPI, IMPS, NEFT, RTGS)
        val isTransferRail = listOf("upi", "imps", "neft", "rtgs").any { lowerBody.contains(it) }
        if (isTransferRail) {
            return "Transfer"
        }

        // Fallback 2: General unknown debit expense
        return "Other"
    }
}