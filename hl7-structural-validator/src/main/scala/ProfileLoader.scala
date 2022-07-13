package cdc.xlr.structurevalidator

import java.io.{BufferedInputStream}

trait ProfileLoader {

    def profile(): BufferedInputStream
    def constraints(): BufferedInputStream
    def valueSets(): BufferedInputStream

} // .ProfileLoader