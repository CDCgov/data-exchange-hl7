name := "cdc.xlr.hl7.structurevalidator"
version := "0.4.0"

organization     := "gov.cdc"

scalaVersion := "2.12.16"

libraryDependencies += "com.typesafe" % "config" % "1.4.2"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.13.3"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.3"


assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x                             => MergeStrategy.first
} // .assemblyMergeStrategy
