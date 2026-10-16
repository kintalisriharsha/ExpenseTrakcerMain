package com.example.expensetracker.frontend.sms

import android.util.Log
import com.example.expensetracker.services.entity.ExpenseEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsParser {

    private const val TAG = "SmsParser"

    // Currency + amount pattern (e.g. "Rs. 50.00", "INR 10", "₹500")
    private val amountRegex = Regex(
        """(?:Rs\.?|INR|₹)\s?([\d,]+(?:\.\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    private const val MAX_PLAUSIBLE_AMOUNT = 9_999_999.0

    // Generic bank alert signature (masked account / card / balance wording)
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

    // Promo / reminder / OTP / link messages are never transactions
    private val promoRegex = Regex(
        """\botp\b|\boffer\b|cashback|\bwin\b|click|https?:|www\.|bit\.ly""" +
                """|recharge\s+(?:now|with)|pay\s+now|pay\s+(?:your|bill)|reminder|expir|valid\s+(?:till|until)""" +
                """|\bapply\b|download|unlimited|congratulations|\bkyc\b|\bplan\b""",
        RegexOption.IGNORE_CASE
    )

    // A real debit sentence: "debited by Rs 500", "Rs 500 debited", "spent Rs 500" ...
    private val debitSentenceRegex = Regex(
        """(?:debited|withdrawn|spent|paid|sent|charged)\s*(?:by|for|of|with|from|:)?\s*(?:rs\.?|inr|₹)\s?[\d,]+(?:\.\d{1,2})?""" +
                """|(?:rs\.?|inr|₹)\s?[\d,]+(?:\.\d{1,2})?\s*(?:has\s+been\s+|is\s+|was\s+)?(?:debited|withdrawn|spent|paid|sent|charged)""",
        RegexOption.IGNORE_CASE
    )

    // Proof it's tied to an actual account / card / transaction
    private val accountProofRegex = Regex(
        """a/?c\.?\s*(?:no\.?)?[:.]?\s*[Xx*]+\d{2,}|acct?\s*[Xx*]+\d{2,}""" +
                """|card\s*(?:no\.?)?\s*(?:ending|[Xx*]+)\s*\d{2,}""" +
                """|\b(?:upi|imps|neft|rtgs|utr|rrn|ref(?:erence)?|txn)\b""",
        RegexOption.IGNORE_CASE
    )

    // Detects spends made with a credit card
    private val creditCardRegex = Regex(
        """credit\s*card|\bcr\.?\s*card\b|\bcredit\s*crd\b|\bcc\b|sbi\s*card|SBICRD""",
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

    /** Amount taken only from the matched debit sentence (never a balance / limit). */
    private fun findDebitAmount(body: String): String? {
        val sentence = debitSentenceRegex.find(body)?.value ?: return null
        return amountRegex.find(sentence)?.groupValues?.get(1)?.replace(",", "")
    }

    fun parse(sender: String, body: String, smsTimestampMillis: Long = 0L): ExpenseEntity? {
        val normalizedSender = sender.trim()

        // 1. BANK DETECTION - sender header (or a masked account number) must say "bank".
        //    A bank name merely appearing in the body is NOT enough.
        val bankFromSender = matchedBankName(normalizedSender)
        val bankFromBody = matchedBankName(body)
        val matchesGenericAlert = genericBankAlertRegex.containsMatchIn(body)

        if (bankFromSender == null && !matchesGenericAlert) {
            Log.d(TAG, "Rejected: sender is not a bank and no account pattern in body")
            return null
        }

        // 2. REJECT promos, OTPs, reminders, links, bill dues, statements
        if (promoRegex.containsMatchIn(body) || nonTransactionRegex.containsMatchIn(body)) {
            Log.d(TAG, "Rejected: promo / reminder / due / statement")
            return null
        }

        // 3. MUST be a real debit sentence with an amount - no fallback guessing
        if (!debitSentenceRegex.containsMatchIn(body)) {
            Log.d(TAG, "Rejected: no real debit sentence")
            return null
        }

        // 4. MUST reference an account / card / UPI / ref number
        if (!accountProofRegex.containsMatchIn(body)) {
            Log.d(TAG, "Rejected: no account or transaction reference")
            return null
        }

        // 5. AMOUNT
        val amount = findDebitAmount(body)?.toDoubleOrNull()
        if (amount == null || amount <= 0.0 || amount > MAX_PLAUSIBLE_AMOUNT) {
            Log.d(TAG, "Rejected: invalid amount")
            return null
        }

        // 6. PAYMENT MODE
        val isCreditCard = creditCardRegex.containsMatchIn(body) ||
                creditCardRegex.containsMatchIn(normalizedSender)
        val paymentMode = if (isCreditCard) "Credit Card" else "Bank"

        // 7. TITLE / MERCHANT EXTRACTION WITH FALLBACKS
        val extractedName = extractTitleFromBody(body)
        val bankName = bankFromSender ?: bankFromBody
        val titleFallback = bankName ?: cleanRawSenderFallback(normalizedSender)
        val title = extractedName ?: titleFallback

        // 8. CATEGORY
        val category = classifyCategory(title, body)

        // 9. TIMESTAMP
        val now = if (smsTimestampMillis > 0L) Date(smsTimestampMillis) else Date()

        Log.d(TAG, "Parsed OK: title='$title', category='$category', amount=$amount, mode=$paymentMode")

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
            // Skip junk titles like "15-Oct-31 For Rs 500"
            if (!match.isNullOrBlank() &&
                !match.any { it.isDigit() } &&
                !match.contains("rs", ignoreCase = true)
            ) {
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