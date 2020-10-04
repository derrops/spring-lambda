package com.derrops
import com.amazonaws.AmazonServiceException
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.S3Object

import java.security.MessageDigest

class S3Utils {

    private static final ALGORITHM = "SHA-1"
    private static final MessageDigest digest = MessageDigest.getInstance(ALGORITHM)

    static String keyForFile(String namespace, String projectName, String artifactName, File file) {
        String version = getFileChecksum(file)
        basePath(namespace, projectName, artifactName, version) + "/" + file.name
    }

    static String keyForFile(String projectName, File file) {
        basePath(projectName, file) + "/" + file.name
    }

    static String basePath(String projectName, File file) {
        String version = getFileChecksum(file)
        String namespace = System.properties['user.name']
        return "${namespace}/${projectName}/${file.name}/${version}"
    }

    static String basePath(String namespace, String projectName, String artifactName, File file) {
        String version = getFileChecksum(file)
        return "${namespace}/${projectName}/${artifactName}/${version}"
    }

    static String basePath(String namespace, String projectName, String artifactName, String version) {
        return "${namespace}/${projectName}/${artifactName}/${version}"
    }

    static String pathToFile(String namespace, String projectName, File file, String artifactName, String version) {
        def basePath = basePath(namespace, projectName, artifactName, version)
        def fileUploadKey = "${basePath}/${file.name}"
        return fileUploadKey
    }

    static String shaFileName(File file) {
        return "${file.name}.${ALGORITHM.replace("-", "").toLowerCase()}"
    }

    static boolean keyExistsInBucket(String bucket, String key) {
        def s3client = AmazonS3ClientBuilder
                .standard().build()
        try {
            s3client.getObject(bucket, key)
            return true
        } catch (AmazonServiceException e) {
            String errorCode = e.getErrorCode()
            if (errorCode != "NoSuchKey") {
                throw e
            }
            return false
        }
    }

    static void uploadButDoNotReplace(String bucket, String key, File fileToUpload) {
        def s3client = AmazonS3ClientBuilder
                .standard().build()

        if (keyExistsInBucket(bucket, key)) {
            println "SKIPPING S3 UPLOAD: file ${fileToUpload.name} already exists at: ${key}"
        } else {
            println "UPLOADING TO S3: file ${fileToUpload.name} to ${key}"
            s3client.putObject(bucket, key.toString(), fileToUpload)
        }
    }


    static String getFileChecksum(File file) throws IOException
    {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file)

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024]
        int bytesCount = 0

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount)
        }

        //close the stream We don't need it now.
        fis.close()

        //Get the hash's bytes
        byte[] bytes = digest.digest()

        //This bytes[] has bytes in decimal format
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder()
        for(int i=0; i< bytes.length; i++)
        {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1))
        }

        //return complete hash
        return sb.toString()
    }


}
