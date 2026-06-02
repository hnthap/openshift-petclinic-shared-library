// vars/deployApp.groovy
def call() {
    container('buildah') {
        stage('Deploy Application') {
            def isMain  = env.BRANCH_NAME == 'main'
            def port    = isMain ? '8083' : '8084'
            def envName = isMain ? 'prod' : 'uat'
            def image   = "${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.tagVersion}"

            sh """
                # Ensure the SSH key has correct permissions (mounted from secret as read-only)
                # Use a writable copy so ssh is happy
                cp /root/.ssh/ssh-privatekey /tmp/deploy_key
                chmod 600 /tmp/deploy_key

                ssh -i /tmp/deploy_key \
                    -o StrictHostKeyChecking=no \
                    -o BatchMode=yes \
                    deploy@${env.APP_TARGET_IP} << 'ENDSSH'
                        buildah pull --tls-verify=false ${image}
                        buildah rm petclinic-${envName} 2>/dev/null || true
                        podman rm -f petclinic-${envName} 2>/dev/null || true
                        podman run -d \
                            -p ${port}:8080 \
                            --name petclinic-${envName} \
                            --restart unless-stopped \
                            ${image}
ENDSSH
                rm -f /tmp/deploy_key
            """
        }
    }
}
