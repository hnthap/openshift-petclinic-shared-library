// vars/buildImage.groovy
def call() {
    container('buildah') {
        withCredentials([usernamePassword(
            credentialsId: env.NEXUS_CREDENTIALS_ID,
            passwordVariable: 'DOCKER_PASS',
            usernameVariable: 'DOCKER_USER'
        )])
        {
            withEnv([
                "DOCKER_REGISTRY=${env.DOCKER_REGISTRY}",
                "APP_NAME=${env.APP_NAME}",
                "TAG_VERSION=${env.tagVersion}"
            ])
            {
                stage('Build Image') {
                    sh '''
                        set -euo pipefail

                        echo "$DOCKER_PASS" | buildah login \
                            --tls-verify=false \
                            -u "$DOCKER_USER" \
                            --password-stdin \
                            "$DOCKER_REGISTRY"

                        ls -lah target

                        find target -maxdepth 1 -type f -name "*.jar" -print\

                        buildah bud \
                            --tls-verify=false \
                            --no-cache \
                            --tag "$DOCKER_REGISTRY/$APP_NAME:$TAG_VERSION" \
                            .
                    '''
                }
            }
        }
    }
}
