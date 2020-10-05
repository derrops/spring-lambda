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
            }


            buildFunctionArchive.archiveClassifier.set("function")

            buildFunctionArchive.doLast {
                println "hello-from-spring-lambda"
            }

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