# my-shared-library — PetClinic OpenShift Pipeline

## Repository layout

```
shared-library/
├── Jenkinsfile                        # Lives in the PetClinic app repo
└── vars/
    ├── runPipeline.groovy             # podTemplate + orchestration
    ├── compileApp.groovy              # mvn clean compile
    ├── runUnitTests.groovy            # mvn test
    ├── scanSonar.groovy               # mvn sonar:sonar
    ├── buildImage.groovy              # buildah bud
    ├── pushImage.groovy               # buildah push
    ├── deployApp.groovy               # SSH → podman run on target host
    └── checkHealth.groovy             # curl /actuator/health
```

---

## Configuration

All configuration lives in the `environment` block of the `Jenkinsfile`.
No values are hardcoded in the shared library.

| Variable                 | Description                                            |
|--------------------------|--------------------------------------------------------|
| `APP_TARGET_IP`          | IP of the deployment target host                       |
| `REGISTRY`               | Internal image registry to pull images to build (`host:port`) |
| `DOCKER_REGISTRY`        | Internal image registry to push/pull built images      |
| `OCP_NAMESPACE`          | OpenShift namespace where build pods are scheduled     |
| `OCP_SERVICE_ACCOUNT`    | ServiceAccount used by the build pod                   |

The Kubernetes secrets and PVC referenced above must already exist in
`OCP_NAMESPACE` before the pipeline runs. How they are created and managed
is outside the scope of this pipeline.

---

## Branch behaviour

| Branch    | Compile | Test | Sonar | Build & Push | Deploy | Health Check |
|-----------|:-------:|:----:|:-----:|:------------:|:------:|:------------:|
| `main`    | ✅       | ✅    | ✅     | ✅            | ✅      | ✅            |
| `uat/*`   | ✅       | ✅    | ✅     | ✅            | ✅      | ✅            |
| any other | ✅       | ✅    | ✅     | -            | -      | -            |

---

## Port mapping

| Environment | App port | Health check port |
|-------------|----------|-------------------|
| prod (main) | 8383     | 9383              |
| uat (uat/*) | 8384     | 9384              |
