# Samples

This is a selected list of `Glues` and `GlueOperators`, all of them are tested either by an
integration test of end-to-end test.

See main samples [here](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/sample).

See ALL `Glue` integration test resources [here](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/glue).

See ALL `GlueOperator` integration test resources [here](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/glueoperator),

## Cherry-picked samples

Each sample below is backed by an integration or end-to-end test and highlights a distinct
feature. Click through to the resource on `main`.

### `Glue`

A `Glue` manages a set of related resources directly (no parent custom resource).

- [SimpleGlue](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/glue/SimpleGlue.yaml) —
  the minimal example: a single `ConfigMap` child resource.
- [CrossReferenceResource](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/glue/CrossReferenceResource.yaml) —
  one child references a value from another (`{configMap1.data.key}`) and orders creation with
  `dependsOn`.
- [ResourceTemplate](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/glue/ResourceTemplate.yaml) —
  defines children with `resourceTemplate` (a templated YAML string) instead of an inline
  `resource`.
- [TwoResourcesAndCondition](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/glue/TwoResourcesAndCondition.yaml) —
  a JavaScript reconcile precondition (`JSCondition`) gates whether the second resource is
  created, based on the data of the first.
- [RelatedResourceSimpleWithCondition](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/glue/RelatedResourceSimpleWithCondition.yaml) —
  reads an existing (related) `Secret` not managed by the `Glue`, feeds its data into a child
  `ConfigMap`, and uses it in a condition.
- [CopySecretToConfigMap](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/glue/CopySecretToConfigMap.yaml) —
  copies a `Secret` from another namespace into a `ConfigMap`, iterating its entries with a Qute
  `{#for}` loop and `decodeBase64`.
- [ClusterScopedChild](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/glue/ClusterScopedChild.yaml) —
  manages a cluster-scoped child resource (a `Namespace`) via `clusterScoped: true`.

### `GlueOperator`

A `GlueOperator` turns a parent custom resource into a reusable template: a `Glue` is
instantiated for every instance of the `parent`.

- [SimpleGlueOperator](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/glueoperator/SimpleGlueOperator.yaml) —
  a parent CR drives a single templated `ConfigMap` (`{parent.metadata.name}`,
  `{parent.spec.value}`).
- [BulkOperator](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/glueoperator/BulkOperator.yaml) —
  generates a variable number of children from `parent.spec.replicas` using `bulk: true`.

### End-to-end samples

Larger, runnable scenarios under
[`src/test/resources/sample`](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/sample):

- [WebPage operator](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/sample/webpage/webpage.operator.yaml) —
  a `WebPage` CR is expanded into a `ConfigMap`, `Deployment`, `Service`, and a conditionally
  created `Ingress` (only when `parent.spec.exposed == true`).
- [Secret copy operator](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/sample/secretcopy/secret-copy.operator.yaml) —
  a cluster-scoped `Namespace` parent copies a shared `Secret` from `default` into each
  namespace.
- [Mutating webhook deployment](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/sample/mutation/mutation.glue.yaml) —
  deploys a full pod mutating webhook stack (`Deployment` + `Service` +
  `MutatingWebhookConfiguration`) using `dependsOn` and a `ReadyCondition`.
