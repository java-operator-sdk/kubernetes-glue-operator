# Kubernetes Glue Operator Helm chart

Installs the [Kubernetes Glue Operator](https://github.com/java-operator-sdk/kubernetes-glue-operator):
its `Glue` and `GlueOperator` CRDs, RBAC, and the operator `Deployment`.

## Install from the Helm repository

```bash
helm repo add glue-operator https://java-operator-sdk.github.io/kubernetes-glue-operator
helm repo update
helm install glue-operator glue-operator/kubernetes-glue-operator \
  --namespace glue-operator --create-namespace
```

## Install from source

```bash
helm install glue-operator ./helm/kubernetes-glue-operator \
  --namespace glue-operator --create-namespace
```

## CRDs

The `Glue` and `GlueOperator` CRDs live in the chart's `crds/` directory. Helm installs
them on first install but, by design, does **not** upgrade or delete them. On an upgrade
that changes the CRD schema, apply the new CRDs manually:

```bash
kubectl apply -f helm/kubernetes-glue-operator/crds/
```

## Configuration

| Key | Default | Description |
| --- | --- | --- |
| `replicaCount` | `1` | Operator replicas. |
| `image.repository` | `ghcr.io/java-operator-sdk/kubernetes-glue-operator` | Operator image. |
| `image.tag` | `""` | Image tag; defaults to the chart `appVersion`. |
| `image.pullPolicy` | `IfNotPresent` | Image pull policy. |
| `imagePullSecrets` | `[]` | Image pull secrets. |
| `watchNamespaces` | `[]` | Namespaces to watch; empty means all namespaces. |
| `serviceAccount.create` | `true` | Create a ServiceAccount. |
| `serviceAccount.name` | `""` | Override the ServiceAccount name. |
| `serviceAccount.annotations` | `{}` | ServiceAccount annotations. |
| `rbac.create` | `true` | Create ClusterRoles and bindings. |
| `rbac.allAccess` | `true` | Grant cluster-wide access to all resources. Disable for least privilege. |
| `rbac.extraRules` | `[]` | Extra rules added to the operator's ClusterRole (use with `allAccess: false`). |
| `rbac.bindView` | `true` | Bind the ServiceAccount to the built-in `view` ClusterRole. |
| `service.type` | `ClusterIP` | Service type. |
| `service.port` | `80` | Service port. |
| `resources` | `{}` | Container resource requests/limits. |
| `livenessProbe` / `readinessProbe` / `startupProbe` | Quarkus health endpoints | Probe config; set to `{}` to disable. |
| `extraEnv` | `[]` | Extra environment variables for the container. |
| `podAnnotations`, `podLabels`, `nodeSelector`, `tolerations`, `affinity`, `podSecurityContext`, `securityContext` | `{}` / `[]` | Standard pod/container scheduling and security knobs. |

### Why `rbac.allAccess`?

A `Glue` can manage **arbitrary** resource types, so the operator is granted cluster-wide
access by default. For a least-privilege setup, scope it to only the resource types your
`Glue`s manage:

```yaml
rbac:
  allAccess: false
  extraRules:
    - apiGroups: [""]
      resources: ["configmaps", "secrets", "services"]
      verbs: ["*"]
    - apiGroups: ["apps"]
      resources: ["deployments"]
      verbs: ["*"]
```
