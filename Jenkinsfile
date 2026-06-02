// Jenkinsfile  (lives in the PetClinic app repo)

@Library('my-shared-library') _

pipeline {
    agent none   // agent is defined inside runPipeline via podTemplate

    environment {
        // Core values
        APP_NAME             = "spring-petclinic-thaphn1"

        // Target host
        APP_TARGET_IP        = '10.89.25.146'

        // Internal image registry
        REGISTRY             = '10.89.25.146:9006'
        DOCKER_REGISTRY      = '10.89.25.146:8082'

        // OpenShift — pod scheduling
        OCP_NAMESPACE        = 'training'
        OCP_SERVICE_ACCOUNT  = 'thaphn1-jenkins'
    }

    stages {
        stage('Run Pipeline') {
            steps {
                runPipeline()
            }
        }
    }
}
