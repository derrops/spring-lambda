package com.derrops

import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model.InvocationType
import com.amazonaws.services.lambda.model.InvokeRequest
import com.amazonaws.services.lambda.model.InvokeResult
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.ByteBuffer
import java.nio.charset.Charset

class LambdaSmokeTestTask extends DefaultTask {

    String lambda

    File file

    File outputDir

    void setLambda(String lambda) {
        this.lambda = lambda
    }

    void setOutputDir(File outputDir) {
        this.outputDir = outputDir
    }

    void setFile(File file) {
        this.file = file
    }

    @Input
    String getLambda() {
        return lambda
    }

    @InputFiles
    File getFile() {
        return file
    }

    @OutputDirectory
    File getOutputDir() {
        if (outputDir == null) {
            outputDir = new File(project.buildDir, "smoketest")
            return outputDir
        } else {
            return outputDir
        }
    }

    @TaskAction
    void test(){

        if (!getOutputDir().exists()) {
            if (!getOutputDir().mkdirs()) {
                throw new IOException("Could not create directory:" + getOutputDir())
            }
        }

        def lambdaClient = AWSLambdaClientBuilder.defaultClient()
        String payload = getClass().getClassLoader().getResourceAsStream("dwellings.json").text


        InvokeRequest invokeRequest = new InvokeRequest()
        invokeRequest.setFunctionName(lambda)
        invokeRequest.setPayload(payload)
        invokeRequest.setInvocationType(InvocationType.RequestResponse)

        InvokeResult invokeResult = lambdaClient.invoke(invokeRequest)

        File outputFile = new File(outputDir, "smoketest.json")
        ByteBuffer byteBuffer = invokeResult.getPayload()
        Charset charset = Charset.forName("ISO-8859-1")
        String text = charset.decode(byteBuffer).toString()
        outputFile.text = new JsonBuilder(text).toPrettyString()

        def outputMap = new JsonSlurper().parseText(text)
        if (outputMap.statusCode == null || outputMap.statusCode != 200) {
            throw new RuntimeException("Status code not 200")
        }
    }

}
