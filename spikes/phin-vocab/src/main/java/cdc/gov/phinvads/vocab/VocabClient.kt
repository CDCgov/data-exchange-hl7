package cdc.gov.phinvads.vocab
import com.caucho.hessian.client.HessianProxyFactory

import gov.cdc.vocab.service.VocabService
import gov.cdc.vocab.service.bean.ValueSet
import gov.cdc.vocab.service.bean.ValueSetConcept
import java.net.MalformedURLException

class VocabClient {
     private var service: VocabService? = null
     private val serviceUrl = "https://phinvads.cdc.gov/vocabService/v2"

    init {
        try {
            service = HessianProxyFactory().create(VocabService::class.java, serviceUrl) as VocabService
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
    }

    fun getAllValueSets(): MutableList<ValueSet>? {
        val valuesResultSet =  service!!.allValueSets
        return valuesResultSet.valueSets
    }

    fun getValueSetConcepts(valueSet: ValueSet): MutableList<ValueSetConcept>? {
        val valueSetVersionResult = service!!.getValueSetVersionByValueSetOidAndVersionNumber(valueSet.oid, 0)
        val concepts = service!!.getValueSetConceptsByValueSetVersionId(valueSetVersionResult.valueSetVersion?.id, 1, 3500)
        return concepts.valueSetConcepts
    }

    fun getValueSetKey(valueSet: ValueSet): StringBuilder {
        val vocabKey = StringBuilder()
        vocabKey.append(valueSet.code)
      //  val valueSetVersionResult = service!!.getValueSetVersionByValueSetOidAndVersionNumber(valueSet.oid, 0)
       // val version = valueSetVersionResult.valueSetVersion ?.versionNumber
       // vocabKey.append("_").append("$version")
       // println("VocabKey :${vocabKey}")
        return vocabKey
    }
}
