name := "cdc.xlr.hl7.structurevalidator"
version := "0.4.3"

organization     := "gov.cdc"

scalaVersion := "2.12.16"


libraryDependencies += "com.typesafe" % "config" % "1.4.2"
libraryDependencies += "xom" % "xom" % "1.3.7"


libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.3"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.3"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.12" % Test

assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x                             => MergeStrategy.first
} // .assemblyMergeStrategy
