# PetClinic OpenShift Pipeline

A Jenkins shared library that builds, tests, and deploys the [Spring PetClinic](https://github.com/spring-projects/spring-petclinic) application to OpenShift. The pipeline runs in ephemeral Kubernetes pods and handles everything from compilation through production deployment.

---

## Repository layout

```
openshift-petclinic-shared-library/
├── Jenkinsfile                        # Lives in the PetClinic app repo
└── vars/
    ├── runPipeline.groovy             # Pod template + stage orchestration
    ├── compileApp.groovy              # mvn clean compile
    ├── runUnitTests.groovy            # mvn test
    ├── scanSonar.groovy               # mvn sonar:sonar
    ├── packageApp.groovy              # mvn package -DskipTests
    ├── buildImage.groovy              # buildah bud
    ├── pushImage.groovy               # buildah push
    ├── deployApp.groovy               # oc rollout to OpenShift
    └── checkHealth.groovy             # curl /actuator/health
```

---

## How it works

Each pipeline run spins up a short-lived Jenkins agent pod in OpenShift with three containers:

| Container | Image | Purpose |
|-----------|-------|---------|
| `jnlp` | `jenkins/inbound-agent:latest-jdk21` | Jenkins agent |
| `maven` | `jenkins/maven:3.9.12-eclipse-temurin-21-noble` | Compile, test, package, SonarQube scan |
| `buildah` | `jenkins/buildah:v1.38-stable` | Build and push container image |

All three images are pulled from the internal registry (`REGISTRY`). After the image is pushed, deployment runs on a regular Jenkins agent node (not in the pod) because the current pod images do not include the OpenShift CLI `oc`. This is a known limitation — see [Deployment node](#deployment-node) below.

---

## Pipeline stages

### All branches

1. **Checkout SCM** — checks out the app repo and extracts the version from `pom.xml`
2. **Compile** — `mvn clean compile`
3. **Unit Test** — `mvn test`
4. **SonarQube Scan** — `mvn sonar:sonar` using the token from `SONARQUBE_TOKEN_ID`

### `main` and `uat/*` branches only

5. **Package** — `mvn package -DskipTests`
6. **Build Image** — `buildah bud`, tagged `<DOCKER_REGISTRY>/<APP_NAME>:<version>`
7. **Push Image** — `buildah push` to `DOCKER_REGISTRY`
8. **Deploy Application** — creates or updates the OpenShift Deployment, Service, and Route; waits for rollout (180 s timeout)
9. **Health Check** — polls `http://<route>/actuator/health` after a 15 s warm-up

### Branch matrix

| Branch    | Compile | Test | Sonar | Package | Build & Push | Deploy | Health Check |
|-----------|:-------:|:----:|:-----:|:-------:|:------------:|:------:|:------------:|
| `main`    | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `uat/*`   | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| any other | ✅ | ✅ | ✅ | — | — | — | — |

---

## Configuration

All configuration lives in the `environment` block of the `Jenkinsfile`. Nothing is hardcoded in the shared library.

| Variable | Description |
|----------|-------------|
| `APP_NAME` | Application name; used as the image name and OCP resource prefix |
| `REGISTRY` | Internal registry for pulling build-tool images (`host:port`) |
| `DOCKER_REGISTRY` | Internal registry for pushing/pulling the built application image |
| `SONARQUBE_URL` | SonarQube server URL |
| `OCP_NAMESPACE` | OpenShift namespace where build pods run and the app is deployed |
| `OCP_SERVICE_ACCOUNT` | ServiceAccount used by the Jenkins agent pod |
| `NEXUS_CREDENTIALS_ID` | Jenkins credential ID (username/password) for the image registry |
| `SONARQUBE_TOKEN_ID` | Jenkins credential ID (secret text) for the SonarQube token |

### Prerequisites

The following must exist in `OCP_NAMESPACE` before the first pipeline run:

- The ServiceAccount named `OCP_SERVICE_ACCOUNT` with sufficient RBAC to create Deployments, Services, Routes, and Secrets
- A PersistentVolumeClaim for the Maven local repository cache (if used)
- The Jenkins credentials (`NEXUS_CREDENTIALS_ID`, `SONARQUBE_TOKEN_ID`) stored in Jenkins

---

## Deployment resources

The deploy stage idempotently creates or updates these OpenShift resources under the name `<APP_NAME>-<env>`:

| Resource | Details |
|----------|---------|
| `Secret` (docker-registry) | Image pull secret, recreated each run via `--dry-run=client | apply` |
| `Deployment` | Created if absent; image updated via `oc set image` if it already exists |
| `Service` | Exposes container port 8080; created once, not modified on re-runs |
| `Route` | HTTP route; created once, not modified on re-runs |

Rollout is waited on with a 180-second timeout.

---

## Environment ports

| Environment | App port | Health endpoint |
|-------------|:--------:|:---------------:|
| `prod` (`main`) | 8080 (container) | `/actuator/health` via OCP route |
| `uat` (`uat/*`) | 8080 (container) | `/actuator/health` via OCP route |

---

## Deployment node

The **Deploy** and **Health Check** stages run on a plain Jenkins agent node instead of inside the OpenShift pod. This is intentional: the available internal container images do not include `oc`, so these stages fall back to whatever agent has `oc` installed. This is a known limitation and should be resolved by adding a dedicated `oc`-capable container to the pod template.

---

## Usage

1. Add this library to Jenkins under **Manage Jenkins &rarr; Configure System &rarr; Global Pipeline Libraries** with the name `my-shared-library`.
2. Copy the `Jenkinsfile` into the root of the PetClinic app repository.
3. Update the `environment` block with values for your environment.
4. Create a multibranch pipeline job pointing at the app repository.
