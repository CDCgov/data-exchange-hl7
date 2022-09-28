# Local Dev Environment Set-up

## IntelliJ 
 - Download the Ultimate edition of IntelliJ from [JetBrains](https://www.jetbrains.com/idea/download/#section=windows) and install it locally.
 - Licenses are provided to each individual that needs one. Please contact [Jodi Mann](mailto:rgi5@cdc.gov)
 - within IntelliJ, you can download Java JDKs/SDKs, Kotlin, scala, etc.
 - Install Azure Toolkit plugin.
 
 If you prefer to install those manually, follow the next steps:
 
 ## Azure support
  - Download and install [Azure CLI](https://learn.microsoft.com/en-us/cli/azure/install-azure-cli-windows?tabs=azure-cli
  - Download and install [Azure functions core tools 4.x](https://learn.microsoft.com/en-us/azure/azure-functions/functions-run-local?tabs=v4%2Cwindows%2Ccsharp%2Cportal%2Cbash)

## Github
 - Download and install [github client](https://git-scm.com/downloads)  (optional)
 - you can use github cli as well if you choose to do so [Download here](https://cli.github.com/)
  
  
 
## Manual Installs 
### Java 11

- Install Java 11, if not available already, such as from: [https://docs.microsoft.com/en-us/java/openjdk/download](https://docs.microsoft.com/en-us/java/openjdk/download)
  
```bash 
$ java -version
openjdk version "11.0.15"

$ javac -version
javac 11.0.15
```
### Kotlin

**(Needed if not Using IntelliJ)**
- Install the compiler for Kotlin such as from the Assets section of a release: [https://github.com/JetBrains/kotlin/releases](https://github.com/JetBrains/kotlin/releases)
- Download file and unzip into a folder with write access 
- Update sytem PATH with the bin location of the Kotlin compiler, such as: ```C:/Kotlin-compiler/bin```
  
 ```bash 
$ kotlin -version
info: kotlinc-jvm 1.7.10 
```

## VS Code

- Install Visual Studio Code from [https://code.visualstudio.com/Download](https://code.visualstudio.com/Download)
- Install Extensions for:
  -  Azure Functions: [https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-azurefunctions](https://marketplace.visualstudio.com/items?itemName=ms-azuretools.vscode-azurefunctions)
  - Azure Functions Core Tools: [https://github.com/Azure/azure-functions-core-tools#installing](https://github.com/Azure/azure-functions-core-tools#installing) - skip this step if already installed by the Azure Functions extension above
  -  Extension Pack for Java: [https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) - optional for Java
  -  Kotlin language support: [https://marketplace.visualstudio.com/items?itemName=mathiasfrohlich.Kotlin](https://marketplace.visualstudio.com/items?itemName=mathiasfrohlich.Kotlin)
