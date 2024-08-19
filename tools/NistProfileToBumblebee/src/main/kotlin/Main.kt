package cdc.gov
class Main
    fun main(args: Array<String>) {
        // arg 0: path to profile.xml
        // arg 1: output path
        // arg 2: output profile name (optional)
        if (args.size < 2) {
            println(
                "Invalid number of arguments. \n" +
                        "Usage:  java -jar <jar> <full path to Profile.xml> " +
                        "<output folder path> <optional: output profile name>"
            )
        } else {
            val profilePath = args[0]
            val outputPath = args[1]
            val outputName = try {
                args[2]
            } catch (e: Exception) {
                null
            }
            val transformer = IgamtToBumblebeeTransformer()
            transformer.transformProfile(profilePath, outputPath, outputName)
        }

    }

