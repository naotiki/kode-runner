package model

sealed class ContainerRuntime(val imageName:String){
    data object Kotlin:ContainerRuntime(imageName = "")
}
