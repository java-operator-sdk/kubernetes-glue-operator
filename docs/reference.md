# Reference Documentation

The implementation generalizes and extends [`DependentResource`](https://javaoperatorsdk.io/docs/dependent-resources) 
and [`Workflow`](https://javaoperatorsdk.io/docs/workflows) features 
of [Java Operator SDK](https://github.com/operator-framework/java-operator-sdk) and more.
Although it is limited only to Kubernetes resources it makes it very easy to use in language-independent 
(DependentResources in JOSDK are also covering external resources) way. 

## [Glue resource](https://github.com/csviri/kubernetes-glue-operator/releases/latest/download/glues.glue-v1.yml)

`Glue` is the heart of the operator. Note that `GlueOperator` controller just creates a new `Glue` with a related resource, 
for each parent custom resource. `Glue` defines `childResources` (sometimes referred to as managed resources) and `related resources`:

### Child resources

#### Attributes

The `childResources` section is a list of resources to be reconciled (created, updated, deleted by controller). 
It has several attributes:

- **`name`** - is a mandatory unique (unique also regarding related resources) attribute.
  The resource is referenced by this name from other places, typically other resource templates and `JSCondition`.
  If it is used in a `JSCondition` the `name` must be a valid JavaScript variable name.
- **`clusterScoped`** - a flag to indicate if the resource is cluster scoped. Default value is `false`. 
  It is mandatory to set this for cluster scoped resources.
- **`resource`** - is the desired state of the resource applied by default using Server Side Apply. The resource is templated using
  [qute templating engine](https://quarkus.io/guides/qute-reference), other resources can be referenced from the templates, see below.  
  If the resource is namespace scoped and the namespace attribute is not specified in `.metadata` automatically the namespace of `Glue` 
  is used.
- **`dependsOn`** - is a list of names of other child resources (not related resources). The resource is not reconciled until all the resources
   which it depends on are not reconciled and ready (if there is a `readyPostCondition` present). 
   Note that during the cleanup phase (when a `Glue` is deleted) resources are cleaned up in reverse order.
- **`condition`** - a condition to specify if the resource should be there or not, thus even if the condition is evaluated to be `true`
   and the resource is created, if one of the following reconciliations the condition is evaluated to `false` the resource is deleted.
   (Same as `reconcilePrecondition` in Java Operator SDK)
- **`readyPostCondition`** - condition to check if the resource is considered to be ready. If a resource is ready all the resources, which depend on it
   can proceed in reconciliation.

#### Built-in conditions

At the moment there are two types of built-in conditions provided:

- **`ReadyCondition`** - check if a resource is up and running. Use it only as a `readyPostCondition`. See sample usage [here](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/test/resources/sample/mutation/mutation.glue.yaml#L24-L25).
- **`JSCondition`** - a generic condition, that allows writing conditions in JavaScript. As input, all the resources are available which
  are either child or related. The script should return a boolean value.
  See accessing the related resource in [WebPage sample](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/test/resources/sample/webpage/webpage.operator.yaml#L62-L64),
  and cross-referencing resources [here](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/test/resources/glue/TwoResourcesAndCondition.yaml#L23-L28).

### Related resources

Related resources are resources that are not reconciled (not created, updated, or deleted) during reconciliation, but serve as an input for it.
See sample usage within `Glue` [here](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/test/resources/glue/RelatedResourceSimpleWithCondition.yaml)
The following attributes can be defined for a related resource:

- **`name`** - same as for child resource, unique identifier, used to reference the resource.
- **`clusterScoped`** - if the related resource is cluster scoped. Default is `false`.
- **`apiVersion`** - Kubernetes resource API Version of the resource
- **`kind`** - Kubernetes kind property of the resource
- **`resourceNames`** - list of string of the resource names within the same namespace as `Glue`.  

### Referencing other resources

Both in `JSCondition` and resource templates other resources can be referenced by the name. 

If there are more `resourceNames` specified for a related resource, the resource is referenced in a form
`[related resource name]#[resource name]`. See sample [here](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/test/resources/glue/MultiNameRelatedResource.yaml).

When a resource `B` references another resource `A`, resource `A` will be guaranteed to be in the cache - especially for initial reconciliation when the resource is created -
only if `B` depends on `A` on it. This is natural, in other words, after reconciliation up-to-date version of the resource is guaranteed to be in the cache after reconciliation.
See sample resource cross-referencing [here](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/test/resources/glue/CrossReferenceResource.yaml).

The metadata of `Glue` can be referenced under `glueMetadata`, see sample [here](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/test/resources/glue/TemplateForConcurrency.yaml#L12-L12)

In addition to that in `GlueOperator` the **`parent`** attribute can be used to reference the parent resource on which behalf the resources are created. See sample [here](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/test/resources/glueoperator/Templating.yaml).

### Reconciliation notes

The reconciliation is triggered either on a change of the `Glue` or any child or related resources. 

On every reconciliation, each child resource is reconciled, and if a resource is updated, it is added to a cache, so it is available for templating
for a resource that depends on it.

The `DependentResource` implementation of JOSDK makes all kinds of optimizations on the reconciliation which are utilized (or will be also here). 

## [GlueOperator resource](https://github.com/csviri/kubernetes-glue-operator/releases/latest/download/glueoperators.glue-v1.yml)

The specs of `GlueOperator` are almost identical to `Glue`, it just adds some additional attributes: 
 
 - **`parent`** - specifies the resources handled by the operator. Targets are usually custom resources but not necessarily,
     it also works with built-in Kubernetes resources. With the following sub-attributes:
   - **`apiVersion`** and **`kind`** - of the target custom resources.
   - **`labelSelector`** - optional label selector for the target resources.
   - **`clusterScoped`** - optional boolean value, if the parent resource is cluster scoped. Default is `false`.
 - **`glueMetadata`** - optionally, you can customize the `Glue` resource created for each parent resource. 
    This is especially important when the parent is a cluster scoped resource - in that case it is mandatory to set. 
    Using this you can specify the **`name`** and **`namespace`** of the created `Glue`.
    See usage on the sample [secret-copy-operator](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/test/resources/sample/secretcopy/secret-copy.operator.yaml#L10-L12). 

See minimal `GlueOperator` [here](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/test/resources/glueoperator/SimpleGlueOperator.yaml).

## Deployment

Implementation is using [Quarkus Operator SDK (QOSDK)](https://github.com/quarkiverse/quarkus-operator-sdk), 
the default [configuration options](https://docs.quarkiverse.io/quarkus-operator-sdk/dev/includes/quarkus-operator-sdk.html) 
defined by QOSDK can be overridden using environment variables.

With every release, there are Kubernetes resources provided to make an initial deployment very simple.
See `kubernetes.yml` in [release assets](https://github.com/csviri/kubernetes-glue-operator/releases).
While we will provide more options, users are encouraged to enhance/adjust this for their purposes.

Since the project is a meta-controller, it needs to have access rights to all the resources it manages. 
When creating specialized roles for a deployment, roles should contain the union of required access rights
for all the child resources, specifically: `["list", "watch", "create", "patch", "delete"]`
and `["list", "watch"]` for related resources.

Cluster and various (single or multiple) namespace-scoped deployments are supported.

### Passing configuration values

To pass configuration values use [environment variables](https://kubernetes.io/docs/tasks/inject-data-application/define-environment-variable-container/) for the `Deployment`.

Use the format that is defined in [quarkus](https://quarkus.io/guides/config-reference#environment-variables`).

### Sharding with label selectors

The operator can be deployed to only target certain `Glue` or `GlueOperator` resources based on [label selectors](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/).
You can use simply the [configuration](https://docs.quarkiverse.io/quarkus-operator-sdk/dev/includes/quarkus-operator-sdk.html#quarkus-operator-sdk_quarkus-operator-sdk-controllers-controllers-selector)
from Quarkus Operator SDK to set the label selector for the reconciler.

The configuration for `Glue` looks like:

`quarkus.operator-sdk.controllers.glue.selector=mylabel=myvalue` 

for `GlueOperator`:

`quarkus.operator-sdk.controllers.glue-operator.selector=mylabel=myvalue`

This will work with any label selector for `GlueOperator` and with simple label selectors for `Glue`,
thus in `key=value` or just `key` form. 


With `Glue` there is a caveat. `GlueOperator` works in a way that it creates a `Glue` resource for every 
custom resource tracked, so if there is a label selector defined for `Glue` it needs to add this label
to the `Glue` resource when it is created. Since it is not trivial to parse label selectors, in more 
complex forms of label selectors (other the ones mentioned above), the labels to add to the `Glue` resources
by a `GlueOperator` needs to be specified explicitly using 
[`glue.operator.glue-operator-managed-glue-label`](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/main/java/io/csviri/operator/glue/ControllerConfig.java#L10-L10) 
config key (which is a type of map). Therefore, for a label selector that specified two values for a glue:

`quarkus.operator-sdk.controllers.glue.selector=mylabel1=value1,mylabel2=value2`

you need to add the following configuration params:

`glue.operator.glue-operator-managed-glue-label.mylabel1=value1`

`glue.operator.glue-operator-managed-glue-label.mylabel2=value2`

This will ensure that the labels are added correctly to the `Glue`. See the related 
[integration test](https://github.com/csviri/kubernetes-glue-operator/blob/main/src/test/java/io/csviri/operator/glue/GlueOperatorComplexLabelSelectorTest.java#L23-L23).

### Label selectors managed resources

For efficiency reasons, there is always one informer registered for a single resource type, even if there are more `Glue`-s or `GlueOperators` 
handled by a deployment that contains resources for the same type. For example, if there are multiple `Glues` managing `ConfigMaps` there will always be
just one informer for a `ConfigMap`.Therefore, label selectors can be configured only per resource type, not per `Glue` or `GlueOperator`.

To configure a label selector for a resource use `glue.operator.resource-label-selector` (Map), which is followed by the identifier of the resource type in the key,
and the value is the label selector itself. The resource type is in the form: `[group/]version#kind`.
For example to define a label selector for ConfigMaps set:

`glue.operator.resource-label-selector.v1#ConfigMap=mylabel=samplevalue`

or for Deployment:

`glue.operator.resource-label-selector.apps/v1#Deployment=mylabel=samplevalue`


## Implementation details and performance

Informers are used optimally, in terms of that, for every resource type only one informer is registered in the background. Event there are more `Glue` or `GlueOperator`
resources containing the same resource type. 

The templating and some of the Javascript condition is probably the most time-consuming and resource-intensive part which will 
be continuously improved in the follow-up releases. 

## Samples

1. [WebPage](https://github.com/csviri/kubernetes-glue-operator/tree/main/src/test/resources/sample/webpage) `GlueOperator`, serves a static website from the cluster.
   To achieve this, it creates three resources a `Deployment` running Nginx, a `ConfigMap` that contains the HTML file an mounted to nginx, a `Service` and an optional `Ingress`
   to expose the static web page.
3. [Muatation Hook Deployment](https://github.com/csviri/kubernetes-glue-operator/tree/main/src/test/resources/sample/mutation), described on the project home page.
4. [Additional `Glue` samples](https://github.com/csviri/kubernetes-glue-operator/tree/main/src/test/resources/glue), note that these are used for integration testing.
5. [Additional `GlueOperator` samples](https://github.com/csviri/kubernetes-glue-operator/tree/main/src/test/resources/glueoperator), also used for integration testing.

## Related documents

- [Dependent Resources documentation in Java Operator SDK](https://javaoperatorsdk.io/docs/dependent-resources)
- [Workflows documentation in Java Operator SDK](https://javaoperatorsdk.io/docs/workflows)
