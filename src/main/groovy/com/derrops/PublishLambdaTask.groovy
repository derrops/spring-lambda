package com.derrops


import com.amazonaws.services.lambda.AWSLambdaClientBuilder
import com.amazonaws.services.lambda.model.LayerVersionContentInput
import com.amazonaws.services.lambda.model.PublishLayerVersionRequest
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GetObjectRequest
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.text.DateFormat
import java.text.SimpleDateFormat


/**
 * Publishes a new LambdaLayer
 */
class PublishLambdaTask extends DefaultTask {

    @Input
    private String runtime = "java11"

    @Input
    private String layerName = project.name + "-layer"
    private String bucket
    private File file
    private File outputDir

    @InputFiles
    File getFile() {
        return this.file
    }

    @OutputDirectory
    File getOutputDir() {
        if (outputDir == null) {
            outputDir = new File(project.buildDir, "lambdalayer_" + file.name)
            return outputDir
        } else {
            return outputDir
        }
    }

    String nowAsISO() {
        TimeZone tz = TimeZone.getTimeZone("UTC")
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'") // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz)
        return df.format(new Date())
    }

    @TaskAction
    void publish() {

        if (!getOutputDir().exists()) {
            if (!getOutputDir().mkdirs()) {
                throw new IOException("Could not create directory:" + getOutputDir())
            }
        }

        String layerInfoFileName = "layer.info"

        String namespace = System.properties['user.name']
        String artifactName = file.name
        File propsFile = new File(outputDir, layerInfoFileName)

        println "getting Lambda info key"
        String lambdaInfoKey = S3Utils.basePath(namespace, project.name, artifactName, file) + "/${layerInfoFileName}"

        // check if this step needs to be done
        if (S3Utils.keyExistsInBucket(bucket, lambdaInfoKey)) {
            println "SKIPPING LAYER CREATION: file ${layerInfoFileName} at : ${lambdaInfoKey}"

            def s3client = AmazonS3ClientBuilder
                    .standard().build()

            println("DOWNLOADING file: s3://${bucket}/${lambdaInfoKey}")
            s3client.getObject(
                    new GetObjectRequest(bucket, lambdaInfoKey),
                    propsFile
            )

            return
        }

        def lambdaClient = AWSLambdaClientBuilder.defaultClient()
        def request = new PublishLayerVersionRequest()

        def layerVersionContentInput = new LayerVersionContentInput()
        layerVersionContentInput.setS3Bucket(bucket)




        String s3Key = S3Utils.keyForFile(namespace, project.name, artifactName, file)
        println "Creating Lambda Layer with key: ${s3Key}"
        layerVersionContentInput.setS3Key(s3Key)

        // if dirty specify that
        request.setDescription(nowAsISO() + " - Local Deploy")
        request.setCompatibleRuntimes(Arrays.asList(runtime))
        request.setLayerName(layerName)
        request.setContent(layerVersionContentInput)


        // PROPERTIES FILE FOR LAMBDA

        def result = lambdaClient.publishLayerVersion(request)

        String arn = result.getLayerArn()
        Long version = result.getVersion()
        String date = result.getCreatedDate()

        println "Successfully created new Layer: ${arn}"

        propsFile.text = "arn=${arn}\n" +
                "version=${version}\n" +
                "date=${date}"

        // upload to s3 for reference if this needs to be done again in the future
        S3Utils.uploadButDoNotReplace(bucket, lambdaInfoKey, propsFile)
    }

    String getRuntime() {
        return runtime
    }

    void setRuntime(String runtime) {
        this.runtime = runtime
    }

    String getLayerName() {
        return layerName
    }

    @InputFiles
    String getBucket() {
        return bucket
    }

    void setBucket(String bucket) {
        this.bucket = bucket
    }

    void setFile(File file) {
        this.file = file
    }

    void setOutputDir(File outputDir) {
        this.outputDir = outputDir
    }
}
