# Harness Build & Deploy Pipeline

`nexapay_build_deploy.yaml` builds the banking API container and deploys it to
Google Cloud Run.

```
Build (CI, Harness Cloud, amd64)          Deploy (CI, Harness Cloud)
  Run: ./mvnw test                          Run: gcloud run deploy
  BuildAndPushGAR: image -> Artifact Registry
```

## Pipeline-as-code, not applied automatically

This YAML is a source artifact. It does nothing until you connect it to a
Harness project, either by importing it through **Git Experience**
(Pipelines → New → Import From Git) or by pasting it into a new pipeline's YAML
editor. Nothing in this repo talks to your Harness account on its own.

## What you must provide in Harness

The pipeline leans on account resources it cannot carry in Git:

| Resource | Used for | Referenced as |
|---|---|---|
| Code connector (GitHub/GitLab) | cloning this repo | `codebase.connectorRef` (runtime input) |
| GCP connector | pushing to Artifact Registry | `BuildAndPushGAR.connectorRef` (runtime input) |
| Secret `gcp_sa_key` | Cloud Run deploy auth | `<+secrets.getValue("gcp_sa_key")>` |

The `gcp_sa_key` secret is a GCP service account key JSON. The account needs
`roles/run.admin` and `roles/iam.serviceAccountUser`, plus push access to the
Artifact Registry repo.

Change `orgIdentifier` (`default`) and `projectIdentifier` (`nexapay`) at the
top of the YAML to match where you import it — these are structural and cannot
be runtime inputs.

## Variables

Defaults are set for a `us-central1` / `nexapay` layout; override at run time or
edit the YAML.

| Variable | Default | Notes |
|---|---|---|
| `gcpProject` | *(runtime input)* | GCP project ID |
| `garHost` | `us-central1-docker.pkg.dev` | Regional AR host |
| `garRepo` | `nexapay` | Artifact Registry repo |
| `imageName` | `banking-api` | image name in the repo |
| `cloudRunRegion` | `us-central1` | |
| `cloudRunService` | `nexapay-banking-api` | |
| `dockerfile` | `backup/Dockerfile` | see below |

## Which Dockerfile it builds

The repo's **root `Dockerfile` is an intentional workshop failure** (it pulls a
nonexistent Artifact Registry base image). The working build lives at
`backup/Dockerfile`, which is why `dockerfile` defaults to it.

Set `dockerfile` to `Dockerfile` to make the **build stage fail on purpose** —
a pipeline-side version of the same troubleshooting exercise.

## Design notes

- **Both stages run on Harness Cloud**, so no delegate is needed. The deploy is
  a `gcloud run deploy` in a Run step rather than a native Harness CD
  Deployment stage; the native path is more idiomatic but requires a delegate
  and Service/Environment/Infrastructure definitions. For a workshop the
  transparent shell deploy is easier to reason about.
- **Builds target amd64.** Cloud Run runs amd64; a developer laptop is often
  arm64. Building here avoids the "exec format error" that an arm64 image hits
  on Cloud Run.
- **Tests run in the pipeline**, because the Dockerfile builds with
  `-DskipTests` for speed. A broken test fails the build here.

## Not validated against the Harness schema

The YAML is confirmed well-formed and structurally correct, but it has **not**
been validated against Harness's pipeline schema or run, because this
environment has no Harness API access. Expect to resolve connector identifiers
and possibly minor step-schema details on first import.
