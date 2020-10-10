package com.derrops


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Zip

class SpringLambdaPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        SpringLambdaPluginExtension extension = project.getExtensions().create("springLambda", SpringLambdaPluginExtension.class)


//        def buildFunctionArchive = project.tasks.register("buildFunctionArchive", Zip.class) { buildFunctionArchive ->
//
//
//            def compileJava = project.tasks.findByName("compileJava")
//            if (compileJava){
//                buildFunctionArchive.from(compileJava)
//                buildFunctionArchive.dependsOn(compileJava)
//            }
//
//            def compileGroovy = project.tasks.findByName("compileGroovy")
//            if (compileGroovy){
//                buildFunctionArchive.from(compileGroovy)
//                buildFunctionArchive.dependsOn(compileGroovy)
//            }
//
//            def processResources = project.tasks.findByName("processResources")
//            if (processResources){
//                buildFunctionArchive.from(processResources)
//                buildFunctionArchive.dependsOn(processResources)
//            }
//
//            buildFunctionArchive.archiveClassifier = extension.functionClassifier
//
//        }

        def buildFunctionArchive = project.tasks.register("buildFunctionArchive", Zip.class) { buildFunctionArchive ->

            def classes = project.tasks.findByName("classes")
            if (classes) {
                buildFunctionArchive.dependsOn(classes)
                def sourceSets = project.getConvention().getPlugin(JavaPluginConvention.class).getSourceSets()
                buildFunctionArchive.from(sourceSets.findByName("main").output)
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


        def publishLayerArchiveToS3 = project.tasks.register("publishLayerArchiveToS3", PublishToS3.class) { deployLayer ->

            def buildLayerArchiveTask = project.tasks.findByName("buildLayerArchive")
            deployLayer.dependsOn(buildLayerArchiveTask)

            deployLayer.bucket = extension.bucket
            deployLayer.file = buildLayerArchiveTask.outputs.files.singleFile

        }


        def publishFunctionArchiveToS3 = project.tasks.register("publishFunctionArchiveToS3", PublishToS3.class) { deployFunction ->

            def buildFunctionArchiveTask = project.tasks.findByName("buildFunctionArchive")
            deployFunction.dependsOn(buildFunctionArchiveTask)

            deployFunction.bucket = extension.bucket
            deployFunction.file = buildFunctionArchiveTask.outputs.files.singleFile

        }

        def publishLambdaLayerVersion = project.tasks.register("publishLambdaLayerVersion", PublishLambdaTask.class) { publishLambdaLayerVersion ->

            def publishLayerArchiveToS3Task = project.tasks.findByName("publishLayerArchiveToS3")
            def buildLayerArchiveTask = project.tasks.findByName("buildLayerArchive")

            publishLambdaLayerVersion.dependsOn(publishLayerArchiveToS3Task)
            publishLambdaLayerVersion.dependsOn(buildLayerArchiveTask)

            publishLambdaLayerVersion.bucket = extension.bucket
            publishLambdaLayerVersion.file = buildLayerArchiveTask.outputs.files.singleFile

        }

        def publishLambdaVersionTask = project.tasks.register("publishLambdaVersionTask", PublishNewLambdaVersionTask.class) { publishLambdaVersionTask ->

            def publishFunctionArchiveToS3Task = project.tasks.findByName("publishFunctionArchiveToS3")
            def publishLambdaLayerVersionTask = project.tasks.findByName("publishLambdaLayerVersion")
            def buildFunctionArchiveTask = project.tasks.findByName("buildFunctionArchive")

            publishLambdaVersionTask.dependsOn(publishFunctionArchiveToS3Task)
            publishLambdaVersionTask.dependsOn(publishLambdaLayerVersionTask)

            println ("layerInfo = " + publishLambdaLayerVersionTask.outputs.files.singleFile)

            publishLambdaVersionTask.layerInfo = publishLambdaLayerVersionTask.outputs.files.singleFile
            publishLambdaVersionTask.bucket = extension.bucket
            publishLambdaVersionTask.code = buildFunctionArchiveTask.outputs.files.singleFile
            publishLambdaVersionTask.lambdaName = extension.lambda
        }


    }

}