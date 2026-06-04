// vars/pushImage.groovy
def call() {
    container('buildah') {
        withCredentials([usernamePassword(
            credentialsId: env.NEXUS_CREDENTIALS_ID,
            passwordVariable: 'DOCKER_PASS',
            usernameVariable: 'DOCKER_USER'
        )])
        {
            stage('Push Image') {
                sh """
                    buildah login \
                        --tls-verify=false \
                        -u '$DOCKER_USER' \
                        -p '$DOCKER_PASS' \
                        ${env.DOCKER_REGISTRY}
                    buildah push \
                        --tls-verify=false \
                        --format=docker \
                        ${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.tagVersion}
                """
            }
        }
    }
}
