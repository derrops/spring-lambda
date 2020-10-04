package com.derrops
import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model.PublishVersionRequest
import com.amazonaws.services.lambda.model.PublishVersionResult
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class PublishNewLambdaVersionTask extends DefaultTask {

    private File layerInfo

    private String lambdaName = project.name

    File code

    private File outputDir

    private String bucket

    @OutputDirectory
    File getOutputDir() {
        if (outputDir == null) {
            outputDir = new File(project.buildDir, "lambdaDeploy_" + project.name)
            return outputDir
        } else {
            return outputDir
        }
    }

    @TaskAction
    void publish(){

        String codeS3Key = S3Utils.keyForFile(project.name, code)

        def lambdaClient = AWSLambdaClientBuilder.defaultClient()

        /**
         *
         * UPDATE THE LAMBDA LAYER!
         *
         * arn=arn:aws:lambda:ap-southeast-2:632953687273:layer:ziel-app-layer
         * version=5
         * date=2020-05-10T09:09:53.272+0000
         */

        println("----layerInfo----")
        println(layerInfo)

        def layerInfoFile = layerInfo.listFiles()[0]

        FileReader reader = new FileReader(layerInfoFile)
        Properties properties = new Properties()
        properties.load(reader)

        String lambdaLayerArn = properties.get("arn")
        String lambdaLayerVersion = properties.get("version")
        String lambdaLayerArnVersion = "${lambdaLayerArn}:${lambdaLayerVersion}"

        println "UPDATING LAMBDA LAYER to ${lambdaLayerArnVersion}"
        UpdateFunctionConfigurationRequest updateLayersRequest = new UpdateFunctionConfigurationRequest()
        updateLayersRequest.setLayers(Arrays.asList(lambdaLayerArnVersion))
        updateLayersRequest.setFunctionName(lambdaName)
        UpdateFunctionConfigurationResult result = lambdaClient.updateFunctionConfiguration(updateLayersRequest)
        def jsonLayer = JsonOutput.toJson(result)
        File layerOutput = new File(outputDir, "layer.json")
        layerOutput.text = JsonOutput.prettyPrint(jsonLayer)


        println "UPDATING LAMBDA CODE to ${codeS3Key}"
        UpdateFunctionCodeRequest updateFunctionCodeRequest = new UpdateFunctionCodeRequest()
        updateFunctionCodeRequest.setS3Bucket(bucket)
        updateFunctionCodeRequest.setS3Key(codeS3Key)
        updateFunctionCodeRequest.setFunctionName(lambdaName)
        lambdaClient.updateFunctionCode(updateFunctionCodeRequest)
        def jsonCode = JsonOutput.toJson(result)
        File codeOutput = new File(outputDir, "code.json")
        codeOutput.text = JsonOutput.prettyPrint(jsonCode)


    }

    @InputFiles
    File getLayerInfo() {
        return layerInfo
    }

    void setLayerInfo(File layerInfo) {
        this.layerInfo = layerInfo
    }

    String getLambdaName() {
        return lambdaName
    }

    void setLambdaName(String lambdaName) {
        this.lambdaName = lambdaName
    }

    @InputFiles
    String getBucket() {
        return bucket
    }

    void setBucket(String bucket) {
        this.bucket = bucket
    }

    @InputFiles
    File getCode() {
        return code
    }

}

