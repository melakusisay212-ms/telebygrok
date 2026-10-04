package com.teleexpense.counter.domain.parser

import com.teleexpense.counter.domain.model.Category
import com.teleexpense.counter.domain.model.Classification
import com.teleexpense.counter.domain.model.Direction

object CategoryClassifier {

    data class Result(
        val classification: Classification,
        val category: Category,
        val direction: Direction,
        val confidence: Float
    )

    fun classify(sender: String, body: String): Result {
        val lower = body.lowercase()
        if (AmountExtractor.isFreeOrBonus(body)) {
            return Result(Classification.IGNORE, Category.FREE_PACKAGE, Direction.INCOMING, 0.95f)
        }
        if (isP2P(lower)) {
            return Result(Classification.IGNORE, Category.PERSON_TO_PERSON_TRANSFER, Direction.OUTGOING, 0.95f)
        }
        if (isIncomingAirtime(lower)) {
            return Result(Classification.IGNORE, Category.AIRTIME, Direction.INCOMING, 0.95f)
        }
        if (isOutgoingAirtime(lower)) {
            return Result(Classification.COUNT, Category.AIRTIME, Direction.OUTGOING, 0.92f)
        }
        if (isPrepaidRecharge(lower)) {
            return Result(Classification.COUNT, Category.AIRTIME_RECHARGE, Direction.OUTGOING, 0.93f)
        }
        if (isPaidPackage(lower)) {
            val cat = when {
                lower.contains("sms") && (lower.contains("gb") || lower.contains("mb") || lower.contains("data")) ->
                    Category.MIXED_PACKAGE
                lower.contains("sms package") || (lower.contains("sms") && !lower.contains("gb") && !lower.contains("mb")) ->
                    Category.SMS_PACKAGE
                lower.contains("voice") || lower.contains("min") -> Category.VOICE_PACKAGE
                else -> Category.DATA_PACKAGE
            }
            return Result(Classification.COUNT, cat, Direction.OUTGOING, 0.94f)
        }
        if (isActivation(lower)) {
            return Result(Classification.REFERENCE, Category.PACKAGE_ACTIVATION, Direction.OUTGOING, 0.85f)
        }
        if (isPackageNoPrice(lower)) {
            return Result(Classification.REFERENCE, Category.DATA_PACKAGE, Direction.OUTGOING, 0.80f)
        }
        if (isRechargeConfirm(lower)) {
            return Result(Classification.REFERENCE, Category.AIRTIME_RECHARGE, Direction.OUTGOING, 0.85f)
        }
        return Result(Classification.IGNORE, Category.OTHER, Direction.UNKNOWN, 0.30f)
    }

    private fun isP2P(l: String) =
        (l.contains("transferred etb") || l.contains("transferred birr") || l.contains("you have transferred")) &&
            (l.contains("to ") || l.contains("(251")) && !l.contains("airtime") && !l.contains("package")

    private fun isIncomingAirtime(l: String) =
        l.contains("received airtime") || l.contains("you have received airtime top-up") ||
            (l.contains("received") && l.contains("airtime") && l.contains("from"))

    private fun isOutgoingAirtime(l: String) =
        l.contains("sent airtime top-up") || l.contains("successfully sent airtime") ||
            (l.contains("sent") && l.contains("airtime") && l.contains("to"))

    private fun isPrepaidRecharge(l: String) =
        l.contains("prepaid account has been recharged") ||
            (l.contains("recharged successfully") && l.contains("recharged balance"))

    private fun isPaidPackage(l: String) =
        (l.contains("you have paid") || l.contains("paid etb") || l.contains("paid birr")) &&
            (l.contains("package") || l.contains("for package"))

    private fun isActivation(l: String) =
        (l.contains("new service offer") || l.contains("is added to your service") ||
            l.contains("service offer is effective") || l.contains("as per your request")) &&
            (l.contains("package") || l.contains("service offer"))

    private fun isPackageNoPrice(l: String) =
        l.contains("successfully sent") && l.contains("package") &&
            !l.contains("paid") && !l.contains("etb") && !l.contains("birr")

    private fun isRechargeConfirm(l: String) =
        l.contains("you have recharged") && l.contains("your balance is")
}
