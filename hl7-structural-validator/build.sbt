name := "cdc.xlr.hl7.structurevalidator"
version := "0.3.1"

organization     := "gov.cdc"

scalaVersion := "2.12.10"

libraryDependencies += "com.typesafe" % "config" % "1.4.1"

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-databind" % "2.10.5.1"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.12.0"


assemblyMergeStrategy := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x                             => MergeStrategy.first
} // .assemblyMergeStrategy
