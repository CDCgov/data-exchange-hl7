package msc.edu

import io.ktor.server.application.*
import io.ktor.server.netty.*
import msc.edu.plugins.configureRouting
import msc.edu.plugins.eventHubListenerModule
//import org.koin.core.context.startKoin

fun main(args: Array<String>):Unit =
    EngineMain.main(args)

fun Application.module() {
    eventHubListenerModule()
    configureRouting()
}

//var app = startKoin {
//    modules()
//}
