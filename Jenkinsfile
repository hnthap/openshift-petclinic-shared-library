// Jenkinsfile  (lives in the PetClinic app repo)

@Library('my-shared-library') _

pipeline {
    agent none   // agent is defined inside runPipeline via podTemplate

    environment {
        // Core values
        APP_NAME             = "spring-petclinic-thaphn1"

        // Internal image registry and SonarQube
        REGISTRY             = '10.89.25.146:9006'
        DOCKER_REGISTRY      = '10.89.25.146:8082'
        SONARQUBE_URL        = 'http://10.89.25.146:9000'

        // OpenShift
        OCP_NAMESPACE        = 'training'
        OCP_SERVICE_ACCOUNT  = 'thaphn1-jenkins'

        // Jenkins credentials IDs
        NEXUS_CREDENTIALS_ID = 'thaphn1-nexus-credentials'
        SONARQUBE_TOKEN_ID   = 'thaphn1-sonarqube-token'
    }

    stages {
        stage('Run Pipeline') {
            steps {
                runPipeline()
            }
        }
    }
}
