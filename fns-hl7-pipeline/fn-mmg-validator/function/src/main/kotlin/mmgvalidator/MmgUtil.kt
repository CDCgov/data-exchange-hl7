package com.example

import com.google.gson.*

class MmgUtil () {

  fun getGenV2(): MMG {

    val genV2MmgName = "Generic_MMG_V2.0.json"

        // TODO: get from Redis
        val mmgGenV2Json = this::class.java.getResource("/" + genV2MmgName).readText()

        return Gson().fromJson(mmgGenV2Json, MMG::class.java)

  } // .getGenV2

  fun getMmg(mmgName: String): MMG {

        val mmgJson = this::class.java.getResource("/" + mmgName + ".json" ).readText()
        
        return Gson().fromJson(mmgJson, MMG::class.java)

  } // .getMmg

} // .MmgUtil