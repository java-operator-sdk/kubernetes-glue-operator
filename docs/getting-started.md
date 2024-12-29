# Getting Started

## Start Minikube/Kind

Start a local Kubernetes cluster using for example [Minikube](https://minikube.sigs.k8s.io/docs/start/) or [Kind](https://kind.sigs.k8s.io/docs/user/quick-start/#installation).

## Deploy the Operator

Apply the CustomResourceDefinitions:

```bash
kubectl apply -f https://github.com/java-operator-sdk/kubernetes-glue-operator/releases/latest/download/glues.io.javaoperatorsdk.operator.glue-v1.yml -f https://github.com/java-operator-sdk/kubernetes-glue-operator/releases/latest/download/glueoperators.io.javaoperatorsdk.operator.glue-v1.yml
```

Deploy the `kubernetes-glue-operator`:

```bash
kubectl apply -f https://github.com/java-operator-sdk/kubernetes-glue-operator/releases/latest/download/kubernetes.yml
```

Note that this deployment gives the controller access to all the resources on the cluster, this is not what you might want in production.

## Try out the [WebPage Sample](https://github.com/java-operator-sdk/kubernetes-glue-operator/tree/main/src/test/resources/sample/webpage)

First apply the CustomResourceDefinition for `WebPage` :

```bash
kubectl apply -f https://raw.githubusercontent.com/java-operator-sdk/kubernetes-glue-operator/main/src/test/resources/sample/webpage/webpage.crd.yaml
```

Apply the `GlueOperator` for `WebPage`:

```bash
kubectl apply -f https://raw.githubusercontent.com/java-operator-sdk/kubernetes-glue-operator/main/src/test/resources/sample/webpage/webpage.operator.yaml
```

Create a new static `WebPage`:

```bash
kubectl apply -f https://raw.githubusercontent.com/java-operator-sdk/kubernetes-glue-operator/main/src/test/resources/sample/webpage/webpage.sample.yaml
```

All done! Check the resources created, if using minikube you can open the served WebPage with `minikube service webpage1`.

