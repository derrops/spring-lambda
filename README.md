
plugins {
  id 'groovy'
  id 'jacoco'
  id 'org.springframework.boot' version "2.2.6.RELEASE"
  id 'org.gradle.crypto.checksum' version '1.1.0'
  id 'io.spring.dependency-management' version "1.0.9.RELEASE"
}

configurations {
  developmentOnly
  runtimeClasspath {
    extendsFrom developmentOnly
  }
}


def rdsSecretDev = "dev-ziel-db-${databaseType}"

def codeCoverageExcludesReport = [
        '**/*StreamLambdaHandler*/**',
        '**/*CognitoIdentityFilter*/**',
        '**/*WebConfig*/**',
        '**/*Application*/**',
        '**/*$*_closure*'
]

def codeCoverageExcludesVerification = [
        'com.oe.tenant.config.WebConfig',
        'com.oe.tenant.filter.CognitoIdentityFilter',
        'com.oe.tenant.StreamLambdaHandler',
        'com.oe.tenant.Application',
        'com.oe.tenant.StreamLambdaHandler.*'
]



repositories {
  jcenter()
  mavenLocal()
  mavenCentral()
}

dependencies {

  implementation (localGroovy())
  implementation 'org.codehaus.groovy:groovy-all:3.0.4'
  
  implementation('org.springframework.boot:spring-boot-starter')
  implementation('org.springframework.boot:spring-boot-starter-web')
  implementation('org.springframework.boot:spring-boot-starter-validation')
  implementation('org.springframework.boot:spring-boot-starter-data-jpa')

  implementation('io.symphonia:lambda-logging:1.0.1')

  compileOnly 'javax.xml.bind:jaxb-api:2.3.0'
  compileOnly 'com.amazonaws.serverless:aws-serverless-java-container-springboot2:[1.4,)'
  implementation 'mysql:mysql-connector-java:5.1.48'

  // Development Dependencies Only
  developmentOnly("org.springframework.boot:spring-boot-devtools")

  // Test Dependencies
  testCompile('junit:junit:4.12')
  testImplementation('org.springframework.boot:spring-boot-starter-test') {
    exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
  }
  testRuntimeOnly 'com.h2database:h2'
  testCompile 'org.assertj:assertj-core:3.16.1'
  testImplementation 'junit:junit:4.12'
  testImplementation 'org.testng:testng:6.9.6'
}

jacocoTestCoverageVerification {

  violationRules {
    rule {
      element = 'CLASS'
      limit {
        counter = 'LINE'
        value = 'COVEREDRATIO'
        minimum = 1.0
      }
      excludes = codeCoverageExcludesVerification
    }
  }
}

jacocoTestReport {
  reports {
    xml.enabled false
    html.enabled true
    csv.enabled false
  }

  afterEvaluate {
    classDirectories.setFrom(files(classDirectories.files.collect {
      fileTree(dir: it,
              exclude: codeCoverageExcludesReport
      )
    }))
  }
}

test {
  finalizedBy jacocoTestReport
}

jacocoTestReport {
  dependsOn test
}

jacocoTestCoverageVerification {
  dependsOn jacocoTestReport
}



test {
  useJUnitPlatform()
}



task buildFunction(type: Zip) {

  archiveClassifier = "function"

  preserveFileTimestamps = devMode ? false : true
  reproducibleFileOrder = devMode ? true : false
  from compileGroovy
  from processResources
}

task buildLayer(type: Zip) {
  archiveClassifier = "layer"

  preserveFileTimestamps = devMode ? false : true
  reproducibleFileOrder = devMode ? true : false
  into('java/lib') {
    from(configurations.compileClasspath) {
      exclude 'tomcat-embed-*'
      exclude 'org.springframework.boot:spring-boot-starter-tomcat-*'
    }
  }
}

task publishLayer(type: PublishToS3) {
  dependsOn buildLayer
  bucket = appBucket
  file = buildLayer.outputs.files.singleFile
}

task publishFunction(type: PublishToS3) {
  dependsOn buildFunction
  bucket = appBucket
  file = buildFunction.outputs.files.singleFile
}

task publishLambdaLayerVersion(type: PublishLambdaTask) {
  dependsOn buildLayer, publishLayer
  bucket = appBucket
  file = buildLayer.outputs.files.singleFile
}

task publishNewLambdaVersion(type: PublishNewLambdaVersionTask) {
  dependsOn publishLambdaLayerVersion, publishFunction
  layerInfo = publishLambdaLayerVersion.outputs.files.singleFile
  bucket = appBucket
  code = buildFunction.outputs.files.singleFile
}

task smokeTest(type: LambdaSmokeTestTask) {
  dependsOn publishNewLambdaVersion
  lambda = project.name
  file = publishNewLambdaVersion.outputs.files.singleFile
  outputs.upToDateWhen { false }
  outputs.cacheIf { false }
}



//task bootRunDev {
//  bootRun.configure {
//    def secrets = SecretsUtils.getSecretAsMap(rdsSecretDev)
//    secrets.entrySet().forEach{ environment it.key, it.value}
//  }
//}
//
//bootRunDev.finalizedBy bootRun