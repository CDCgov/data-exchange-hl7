package gov.cdc.dex.hl7.model

import gov.cdc.hl7.RedactInfo

 class RedactorInfo(path: String?, rule: String?, lineNumber: Int) : RedactInfo(path, rule, lineNumber) {
    override fun canEqual(that: Any?): Boolean {
        TODO("Not yet implemented")
    }

    override fun productElement(n: Int): Any {
        TODO("Not yet implemented")
    }

    override fun productArity(): Int {
        TODO("Not yet implemented")
    }
}