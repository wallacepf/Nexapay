# Harness Build & Deploy Pipeline

Builds the banking API container and deploys it to Google Cloud Run using a
**native Harness CD deployment** (`deploymentType: GoogleCloudRun`).

```
Build (CI, Harness Cloud, amd64)          Deploy (CD, GoogleCloudRun)
  Run: ./mvnw test                          DownloadManifest
  BuildAndPushGAR -> Artifact Registry      GoogleCloudRunPrepareRollback
                                            GoogleCloudRunDeploy   (services replace)
                                            GoogleCloudRunTrafficShift (100%)
                                          rollback: GoogleCloudRunRollback
```

## Files

| File | Harness entity |
|---|---|
| `nexapay_build_deploy.yaml` | Pipeline (Build + Deploy stages) |
| `service_banking_api.yaml` | Service (`nexapay_banking_api`) |
| `environment_dev.yaml` | Environment (`dev`) |
| `infrastructure_cloud_run_dev.yaml` | Infrastructure (`cloud_run_dev`) |
| `manifests/cloud-run-service.yaml` | Cloud Run (Knative) service manifest |

The native deploy stage references the Service, Environment, and Infrastructure
by identifier, so all four entity files must exist in the same Harness project.
Import them together via **Git Experience**, or recreate them in the UI.

## Pipeline-as-code, not applied automatically

These YAMLs are source artifacts. They do nothing until you connect them to a
Harness project. Nothing in this repo talks to your Harness account on its own.

## What you must provide in Harness

| Resource | Used for | Referenced as |
|---|---|---|
| Code connector (GitHub/GitLab) | cloning this repo | `codebase.connectorRef` (runtime input) |
| GCP connector (build) | pushing to Artifact Registry | `BuildAndPushGAR.connectorRef` (runtime input) |
| GCP connector (deploy) | Cloud Run deploy + GAR pull | infra + service `connectorRef` (runtime input) |
| Cloud Run manifest in File Store | `gcloud run services replace` input | `manifests[].store` (Harness File Store) |

The deploy GCP connector's service account needs `roles/run.admin` and
`roles/iam.serviceAccountUser`, plus pull access to the Artifact Registry repo.
Unlike the earlier shell-based deploy, the native stage authenticates through
the **GCP connector**, so no `gcp_sa_key` secret is required.

Upload `manifests/cloud-run-service.yaml` to the Harness File Store as
`/cloud-run-service.yaml`, or change the manifest `store.type` in
`service_banking_api.yaml` from `Harness` to `Github`/`Git` to read it from this
repo via the code connector.

Change `orgIdentifier` (`default`) and `projectIdentifier` (`nexapay`) in every
file to match where you import — these are structural, not runtime inputs.

## How build and deploy connect

The Build stage's `BuildAndPushGAR` tags the image with `<+pipeline.sequenceId>`.
The Deploy stage passes that same value as the service's GoogleArtifactRegistry
artifact **version**, so the deploy uses exactly the image the build just pushed.

## Variables

| Variable | Default | Notes |
|---|---|---|
| `gcpProject` | *(runtime input)* | GCP project ID (build/GAR) |
| `garHost` | `us-central1-docker.pkg.dev` | Regional AR host |
| `garRepo` | `nexapay` | Artifact Registry repo |
| `imageName` | `banking-api` | image name in the repo |
| `dockerfile` | `backup/Dockerfile` | see below |

Cloud Run **region** and **service name** now live in
`infrastructure_cloud_run_dev.yaml` and `manifests/cloud-run-service.yaml`, not
in pipeline variables. GCP project appears in both the pipeline (for the GAR
build) and the infrastructure entity (for deploy) — keep the two in sync.

## Which Dockerfile it builds

The repo's **root `Dockerfile` is an intentional workshop failure** (it pulls a
nonexistent Artifact Registry base image). The working build lives at
`backup/Dockerfile`, which is why `dockerfile` defaults to it. Set it to
`Dockerfile` to make the build stage fail on purpose.

## Design notes

- **Native GoogleCloudRun deployment**, giving you Harness rollout, traffic
  shifting, and automatic rollback (the stage has a `StageRollback` failure
  strategy). This typically requires a **Harness delegate** with GCP reach,
  unlike the previous delegate-free shell deploy.
- **Build targets amd64.** Cloud Run runs amd64; a developer laptop is often
  arm64. Building here avoids the "exec format error" an arm64 image hits on
  Cloud Run.
- **Tests run in the pipeline**, because the Dockerfile builds with
  `-DskipTests` for speed.

## Not validated against the Harness schema

These files are confirmed well-formed YAML and follow the documented Google
Cloud Run deployment structure (a direct parallel of Harness's Google Cloud
Functions type). They have **not** been validated against the live Harness
schema or run, because this environment has no Harness API access.

The field most likely to need adjustment on import is the manifest **`type`**
in `service_banking_api.yaml` (`GoogleCloudRunService`), which is inferred from
the Cloud Functions analog. The `GoogleCloudRunDeploy` step `spec` is left empty
`{}` (defaults); the Harness UI may populate optional fields. Verify both when
you import.
