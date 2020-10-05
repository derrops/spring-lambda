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


        def deployLayer = project.tasks.register("deployLayer", PublishToS3.class) { deployLayer ->
            deployLayer.dependsOn(buildLayerArchive)

            deployLayer.bucket = extension.bucket

            // TODO - should rather try and use the output of the buildLayerArchiveTask
            deployLayer.file = new File(project.buildDir.path + "/distributions/" + project.name + "-" + project.version + "-" + extension.layerClassifier + ".zip")

        }

    }

}