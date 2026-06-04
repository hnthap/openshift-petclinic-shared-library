// vars/runPipeline.groovy
def call() {
    def label = "slave-${UUID.randomUUID().toString()}"

    podTemplate(
        label:          label,
        cloud:          'openshift4',
        serviceAccount: env.OCP_SERVICE_ACCOUNT,
        namespace:      env.OCP_NAMESPACE,
        containers: [
            containerTemplate(
                name:  'jnlp',
                image: "${env.REGISTRY}/jenkins/inbound-agent:latest-jdk21",
                args:  '${computer.jnlpmac} ${computer.name}'
            ),
            containerTemplate(
                name:       'maven',
                image:      "${env.REGISTRY}/jenkins/maven:3.9.12-eclipse-temurin-21-noble",
                command:    'cat',
                ttyEnabled: true
            ),
            containerTemplate(
                name:       'buildah',
                image:      "${env.REGISTRY}/jenkins/buildah:v1.38-stable",
                command:    'cat',
                ttyEnabled: true,
                privileged: true
            )
        ],
        volumes: []
    ) {
        node(label) {
            try {
                stage('Checkout SCM') {
                    checkout scm: scm, poll: true, changelog: true
                }

                // Extract metadata from pom.xml
                env.tagVersion = sh(
    script: '''awk -v RS='<' -v FS='>' '
NR == 1 || /^!/ || /^\\?/ { next }
/^\\// { depth--; next }
{
    tag = $1
    sub(/[ \\t\\n\\r].*/, "", tag)
    depth++

    if (tag == "version" && depth == 2) {
        gsub(/^[ \\t\\n\\r]+|[ \\t\\n\\r]+$/, "", $2)
        print $2
        exit
    }
}' pom.xml''',
    returnStdout: true
).trim()

                echo "Service : ${env.APP_NAME}"
                echo "Version : ${env.tagVersion}"
                echo "Branch  : ${env.BRANCH_NAME}"

                // Always run: compile, test, sonar
                compileApp()
                runUnitTests()
                scanSonar()

                // Only on main or uat/* branches: build, push, deploy, health check
                if (env.BRANCH_NAME == 'main' || env.BRANCH_NAME.startsWith('uat/')) {
                    packageApp()
                    buildImage()
                    pushImage()
                    env.SHOULD_DEPLOY = 'true'
                }
            } catch (e) {
                error "Pipeline aborted due to: ${e}"
            }

        }
    }
    
    if (env.SHOULD_DEPLOY == 'true') {
        // Deployment workaround:
        // Build/test/image stages run in the OpenShift Jenkins podTemplate containers.
        // However, the available internal container images do not include the OpenShift CLI `oc`.
        // Because deployApp() and checkHealth() require `oc`, they are run on a regular Jenkins
        // agent for now. This is intentionally non-containerized and should be replaced later
        // by a dedicated oc-capable container image in the podTemplate.
        node {
            deployApp()
            checkHealth()
        }
    }
}
