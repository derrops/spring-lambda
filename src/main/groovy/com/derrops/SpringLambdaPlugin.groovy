package com.derrops


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.tasks.compile.CompileJavaBuildOperationType
import org.gradle.api.tasks.bundling.Zip
import org.gradle.launcher.daemon.protocol.Build

class SpringLambdaPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        SpringLambdaPluginExtension extension = project.getExtensions().create("springLambda", SpringLambdaPluginExtension.class)







        //def compileJava = project.tasks.named("compileJava")
        def buildFunctionArchive = project.tasks.register("buildFunctionArchive", Zip.class) { buildFunctionArchive ->
            

            def compileJava = project.tasks.findByName("compileJava")
            if (compileJava){
                buildFunctionArchive.from(compileJava)
                buildFunctionArchive.dependsOn(compileJava)
            }

            def compileGroovy = project.tasks.findByName("compileGroovy")
            if (compileGroovy){
                buildFunctionArchive.from(compileGroovy)
                buildFunctionArchive.dependsOn(compileGroovy)
            }

            def processResources = project.tasks.findByName("processResources")
            if (processResources){
                buildFunctionArchive.from(processResources)
                buildFunctionArchive.dependsOn(processResources)
            }


            buildFunctionArchive.archiveClassifier = extension.functionClassifier

        }

        def buildLayerArchive = project.tasks.register("buildLayerArchive", Zip.class) { buildLayerArchive ->

            buildLayerArchive.into('java/lib') {
                buildLayerArchive.from(project.configurations.compileClasspath)
                buildLayerArchive.exclude ('tomcat-embed-*')
                buildLayerArchive.exclude ('org.springframework.boot:spring-boot-starter-tomcat-*')
            }

            buildLayerArchive.archiveClassifier = extension.layerClassifier

        }




//        Zip buildFunctionArchive = project.tasks.create("buildFunctionArchive", Zip.class) {
//
//
//            println("plugin.configure")
//            println project.tasks.forEach{println it}
//
//            doLast {
//
//                println("plugin.doLast")
//                println project.tasks.forEach{println it}
//
//                def tasks = project.getTasksByName("compileJava", true)
//                def compileJava = tasks.stream().findFirst().get()
//                archiveClassifier = "yolo"
//
//                println (compileJava)JavaCompile_Decorated
//                println(compileJava.getClass())
//                println ("-------")
//
//            }
//            doLast {
//                println("Finished making spring-lambda.\n\n\n")
//            }
//        }

//        Zip buildFunctionArchive = project.tasks.create("buildFunctionArchive", Zip.class) {
//            from(compileGroovy)
//            from(compileJava)
//            from(processResources)
//            include("*")
//            doLast {
//                println("good bye please work")
//            }
//        }
//
//        compileGroovy.forEach{ buildFunctionArchive.from(it) }
//        compileJava.forEach{ buildFunctionArchive.from(it) }
//        processResources.forEach{ buildFunctionArchive.from(it) }
//        buildFunctionArchive.archiveClassifier.set("function")
//        buildFunctionArchive.doLast {
//            println("HELLO FROM spring-Lambda")
//        }


        // buildLayer

    }

}