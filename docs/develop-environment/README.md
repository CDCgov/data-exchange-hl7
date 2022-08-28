# Local Dev Environment Set-up

## Java 11

- Install Java 11, if not available already, such as from: [https://docs.microsoft.com/en-us/java/openjdk/download](https://docs.microsoft.com/en-us/java/openjdk/download)
  
```bash 
$ java -version
openjdk version "11.0.15"

$ javac -version
javac 11.0.15
```
## Kotlin

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
