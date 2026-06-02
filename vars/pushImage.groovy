// vars/pushImage.groovy
// Auth is handled by the nexus-docker-config secret mounted at /root/.docker/
def call() {
    container('buildah') {
        stage('Push Image') {
            sh """
                buildah push \
                    --tls-verify=false \
                    ${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.tagVersion}
            """
        }
    }
}
