// vars/deployApp.groovy
def call() {
    stage('Deploy Application') {
        def isMain      = env.BRANCH_NAME == 'main'
        def envName     = isMain ? 'prod' : 'uat'

        def appResource = "${env.APP_NAME}-${envName}"
        def image       = "${env.DOCKER_REGISTRY}/${env.APP_NAME}:${env.tagVersion}"
        def pullSecret  = "${appResource}-nexus-pull-secret"

        withCredentials([usernamePassword(
            credentialsId: env.NEXUS_CREDENTIALS_ID,
            passwordVariable: 'DOCKER_PASS',
            usernameVariable: 'DOCKER_USER'
        )])
        {
            withEnv([
                "NAMESPACE=${env.OCP_NAMESPACE}",
                "APP_RESOURCE=${appResource}",
                "IMAGE=${image}",
                "DOCKER_REGISTRY=${env.DOCKER_REGISTRY}",
                "PULL_SECRET=${pullSecret}",
                "CONTAINER_NAME=${env.APP_NAME}"
            ])
            {
                sh '''
                    set -eu

                    echo "Deploying image: $IMAGE"
                    echo "Using namespace: $NAMESPACE"
                    echo "Checking OpenShift identity (Who am I?):"
                    oc whoami

                    echo "Checking oc client..."
                    oc version --client

                    echo "Verifying namespace access..."
                    oc get project "$NAMESPACE" >/dev/null

                    echo "Creating/updating namespace-scoped image pull secret: $PULL_SECRET"
                    oc -n "$NAMESPACE" create secret docker-registry "$PULL_SECRET" \
                        --docker-server="$DOCKER_REGISTRY" \
                        --docker-username="$DOCKER_USER" \
                        --docker-password="$DOCKER_PASS" \
                        --dry-run=client \
                        -o yaml | oc -n "$NAMESPACE" apply -f -

                    echo "Creating/updating deployment: $APP_RESOURCE"

                    if oc -n "$NAMESPACE" get deployment "$APP_RESOURCE" >/dev/null 2>&1; then
                        echo "Deployment exists. Updating image..."
                        oc -n "$NAMESPACE" set image \
                            deployment/"$APP_RESOURCE" \
                            "$CONTAINER_NAME=$IMAGE"
                    else
                        echo "Deployment does not exist. Creating deployment..."
                        oc -n "$NAMESPACE" create deployment "$APP_RESOURCE" \
                            --image="$IMAGE"
                    fi

                    echo "Attaching image pull secret only to this deployment..."
                    oc -n "$NAMESPACE" patch deployment "$APP_RESOURCE" \
                        --type='merge' \
                        -p '{"spec":{"template":{"spec":{"imagePullSecrets":[{"name":"'"$PULL_SECRET"'"}]}}}}'

                    echo "Ensuring container port metadata is set to 8080..."
                    oc -n "$NAMESPACE" patch deployment "$APP_RESOURCE" \
                        --type='json' \
                        -p '[{"op":"add","path":"/spec/template/spec/containers/0/ports","value":[{"containerPort":8080,"protocol":"TCP"}]}]' \
                        >/dev/null 2>&1 || true

                    echo "Ensuring service exists..."
                    if ! oc -n "$NAMESPACE" get service "$APP_RESOURCE" >/dev/null 2>&1; then
                        oc -n "$NAMESPACE" expose deployment "$APP_RESOURCE" \
                            --name="$APP_RESOURCE" \
                            --port=8080 \
                            --target-port=8080
                    fi

                    echo "Ensuring route exists..."
                    if ! oc -n "$NAMESPACE" get route "$APP_RESOURCE" >/dev/null 2>&1; then
                        oc -n "$NAMESPACE" expose service "$APP_RESOURCE" \
                            --name="$APP_RESOURCE"
                    fi

                    echo "Waiting for rollout..."
                    oc -n "$NAMESPACE" rollout status deployment/"$APP_RESOURCE" --timeout=180s

                    echo "Deployment completed."

                    ROUTE_HOST="$(oc -n "$NAMESPACE" get route "$APP_RESOURCE" -o jsonpath='{.spec.host}' 2>/dev/null || true)"

                    if [ -n "$ROUTE_HOST" ]; then
                        echo "Application route:"
                        echo "http://$ROUTE_HOST"
                    else
                        echo "Route not found or route host unavailable."
                    fi
                '''
            }
        }
    }
}
