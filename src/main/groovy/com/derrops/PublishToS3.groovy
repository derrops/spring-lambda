package com.derrops


import org.gradle.api.*
import org.gradle.api.tasks.*

class PublishToS3 extends DefaultTask {

    private File file
    private String bucket
    private File outputDir


    @InputFiles
    File getFile() {
        return this.file
    }


    @Input
    String getBucket() {
        return this.bucket
    }


    void setOutputDir(File outputDir) {
        if (outputDir.exists() && !outputDir.isDirectory()) {
            throw new IllegalArgumentException("Output directory must be a directory.")
        }
        this.outputDir = outputDir
    }


    @OutputDirectory
    File getOutputDir() {
        if (outputDir == null) {
            outputDir = new File(project.buildDir, "checksum_" + file.name)
            return outputDir
        } else {
            return outputDir
        }
    }


    @TaskAction
    void publish() {

        if (!getOutputDir().exists()) {
            if (!getOutputDir().mkdirs()) {
                throw new IOException("Could not create directory:" + getOutputDir())
            }
        }

        String namespace = System.properties['user.name']
        String checksum = S3Utils.getFileChecksum(file)
        String version = checksum

        // upload file
        def fileUploadKey = S3Utils.pathToFile(namespace, project.name, file, file.name, version)
        S3Utils.uploadButDoNotReplace(bucket, fileUploadKey.toString(), file)

        // produce sha file
        File shaFile = new File(outputDir, S3Utils.shaFileName(file))
        shaFile.text = checksum

        // upload sha file
        def shaUploadKey = S3Utils.pathToFile(namespace, project.name, shaFile, file.name, version)
        S3Utils.uploadButDoNotReplace(bucket, shaUploadKey.toString(), shaFile)
    }

    void setFile(File file) {
        this.file = file
    }

    void setBucket(String bucket) {
        this.bucket = bucket
    }
}