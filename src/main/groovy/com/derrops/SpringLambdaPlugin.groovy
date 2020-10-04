package com.derrops

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Zip

class SpringLambdaPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        SpringLambdaPluginExtension extension = project.getExtensions().create("springLambda", SpringLambdaPluginExtension.class);


        project.tasks.withType(Zip) { task ->
            task.doFirst{

            }

        }

        project.task
//
//        project.task("hello-world").doLast {
//
//        }
//        Task dfs
//        project.add(dfs)

//        project.task("hello").doLast {
//            System.out.println("Hello, " + extension.getGreeter())
//            System.out.println("I have a message for You: " + extension.getMessage())
//        }

        project.task("hello").doLast {

            System.out.println("Hello, " + extension.getBucket())
            System.out.println("I have a message for You: " + extension.getMessage())
        }

    }

}