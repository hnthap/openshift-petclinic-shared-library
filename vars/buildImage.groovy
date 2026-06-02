// vars/buildImage.groovy
def call() {
    container('buildah') {
        stage('Build Image') {
            sh """
                buildah bud \
                    --tls-verify=false \
                    --no-cache \
                    --tag ${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.tagVersion} \
                    .
            """
        }
    }
}
