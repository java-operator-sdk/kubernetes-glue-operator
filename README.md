# Kubernetes Glue Operator

Kubernetes Glue Operator is a powerful Kubernetes **meta operator** that allows you to create other **operators in a declarative** way by **simply
applying a custom resource**. (**Not** related to AWS Glue)

It provides facilities to compose Kubernetes resources and describes how the resource
should be reconciled. Supports conditional resources in runtime, and ordering of resource reconciliation.
In other words, it also allows you to write **workflows** over resources in a **GitOps** friendly way. 

The project is implemented as a thin layer on top of battle-tested [Workflow feature](https://javaoperatorsdk.io/docs/workflows/) of Java Operator SDK.

## Documentation

[Getting Started](/docs/getting-started.md)

[Reference Documentation](/docs/reference.md)

[Rational and Comparison with Similar Solutions](/docs/comparison.md)

[Sample Index](/docs/sample-index.md)

## Contact Us

Either in the discussion section here on GitHub or at [Kubernetes Slack Operator Channel](https://kubernetes.slack.com/archives/CAW0GV7A5). While
in "object" form only placeholder substitutions are possible, in string template you can use all the 
features of qute.

## Quick Introduction

### The `GlueOperator` Resource

The project introduces two Kubernetes custom resources `Glue` and `GlueOperator`.
You can use `GlueOperator` to define your own operator.
Let's take a look at an example, where we define an operator for WebPage custom resource, that represents a static website served from the Cluster. (You can see the
[full example here](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/sample/webpage))

```yaml

apiVersion: "glueoperator.sample/v1"
kind: WebPage
metadata:
  name: hellows
spec:
  exposed: false  # should be an ingress created or not
  html: |  # the target html
    <html>
      <head>
        <title>Hello Operator World</title>
      </head>
      <body>
        Hello World! 
      </body>
    </html>
```

To create an operator (or more precisely the controller part) with `kubernetes-glue-operator` we have first apply
the [CRD for WebPage](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/sample/webpage/webpage.crd.yml).
To define how the `WebPage` should be reconciled, thus what resources should be created for
a `WebPage`, we prepare a `GlueOperator`:

```yaml
apiVersion: io.javaoperatorsdk.operator.glue/v1beta1
kind: GlueOperator
metadata:
  name: webpage-operator
spec:
  parent:
    apiVersion: glueoperator.sample/v1  # watches all the custom resource of type WebPage
    kind: WebPage
    statusTemplate: | # update the status of the custom resource at the end of reconciliation
      observedGeneration: {parent.metadata.generation}
  childResources:
    - name: htmlconfigmap
      resource:
        apiVersion: v1
        kind: ConfigMap
        metadata:
          name: "{parent.metadata.name}"  # the parent resource (target webpage instance) can be referenced as "parent"
        data:
          index.html: "{parent.spec.html}" # adding the html from spec to a config map
    - name: deployment
      resource:
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: "{parent.metadata.name}"
        spec: # details omitted
          spec:
            containers:
              - name: nginx
                image: nginx:1.17.0
                volumeMounts:
                  - name: html-volume
                    mountPath: /usr/share/nginx/html
            volumes:
              - name: html-volume
                configMap:
                  name: "{parent.metadata.name}" # mounting the html using the config map to nginx server
    - name: service
      resource:
        apiVersion: v1
        kind: Service
        metadata:
          name: "{parent.metadata.name}"
        spec: # Omitted details
    - name: ingress
      condition:
        type: JSCondition
        script: | # creating just ingress only if the exposed is true (this can be changed in runtime)
          parent.spec.exposed == "true";
      resource:
        apiVersion: networking.k8s.io/v1
        kind: Ingress
        metadata:
          name: "{parent.metadata.name}"
      # Omitted Details
```

There are multiple aspects to see here. The four related resources will be templated
and applied to the cluster if such a resource is created. The reconciliation will be triggered if anything changes in the custom or child resources. 

Note also the `condition` part for `Ingress` resource contains multiple types of conditions, `JSCondition` is
used in this example, which allows writing conditions in Javascript. The `Ingress` will be created if the `.spec.exposed` property
is true. If the property is changed to `false` after, the resource is deleted.

### The `Glue` Resource

`Glue` is very similar to `GlueOperator`, with identical properties, except it does not have a parent. Thus, it does not define a controller, just a set of resources to reconcile. 
Why is this useful? Note the **`dependsOn`** and **`readyPostCondition`** features, this allows you to write workflows on resources in a GitOps friendly way. Thus to make sure
that resources are reconciled in a certain order after some conditions are met. To understand this better, see use cases like [this](https://github.com/kubernetes/kubernetes/issues/106802) in 
Kubernetes, are typically meant to be solved by `Glue`.

Let's look at another example, that will show the mentioned features (available both for `Glue` and `GlueOperator`). Again, Kubernetes does not require ordering regarding how
resources are applied, however, there are certain cases when this is needed also for Kubernetes, but especially useful when Kubernetes controllers manage external resources.

The following example shows how to deploy a [dynamic admission controller](https://kubernetes.io/docs/reference/access-authn-authz/extensible-admission-controllers/) that mutates 
all the `Pods`, adding annotation on them. Note that this is a tricky situation since the endpoint for the `MutatingWebhookConfiguration` is also a `Pod`, thus 'Pods' should be 
first up and running before the configuration is applied, otherwise, the mutation webhook will block the changes on the pods, which would render the cluster unable to manage `Pods'.
(Irrelevant details are omitted, see the full version [here](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/resources/sample/mutation/mutation.glue.yaml), 
see the full E2E test [here](https://github.com/java-operator-sdk/kubernetes-glue-operator/blob/main/src/test/java/io/java-operator-sdk/operator/glue/sample/mutation/MutationWebhookDeploymentE2E.java))

```yaml
apiVersion: io.javaoperatorsdk.operator.glue/v1beta1
kind: Glue
metadata:
  name: mutation-webhook-deployment
spec:
  childResources:
    - name: service
      resource:
        apiVersion: v1
        kind: Service
        metadata:
          name: pod-mutating-hook
        spec:
          # spec omitted       
    - name: deployment  # webhook web-service endpoint
      # ready postconditions define when a Deployment is considered "ready",
      # thus up and running.
      readyPostCondition:
        type: ReadyCondition  
      resource:
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: pod-mutating-hook
        spec:
          replicas: 2
          template:            
            spec:
              containers:              
                  image: ghcr.io/javaoperatorsdk/sample-pod-mutating-webhook:0.1.0                  
                  name: pod-mutating-hook
                  ports:
                    - containerPort: 443
                      name: https
                      protocol: TCP
                        
    - name: mutation_hook_config
      clusterScoped: true
      # dependsOn relation means, that the resource will be reconciled only if all
      # the listed resources are already reconciled and ready (if ready post-condition is present).
      # This resource will be applied after the service and deployment are applied,
      # and the deployment is ready, thus all the pods are started up and ready.
      dependsOn:
        - deployment
        - service
      resource:
        apiVersion: admissionregistration.k8s.io/v1
        kind: MutatingWebhookConfiguration
        metadata:          
          name: pod-mutating-webhook
        webhooks:
          - admissionReviewVersions:
              - v1
            clientConfig:
              service:
                name: pod-mutating-hook
                namespace: default
                path: /mutate
            failurePolicy: Fail
            name: sample.mutating.webhook
            rules:
              - apiGroups:
                  - ""
                apiVersions:
                  - v1
                operations:
                  - UPDATE
                  - CREATE
                resources:
                  - pods                         
```

The `dependsOn` relation is a useful concept in certain situations, that might be familiar from other infrustructure-as-a-code tools, `kubernetes-glue-operator` adopts it to Kubernetes operators.

