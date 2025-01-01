# Rational and Comparison to Similar Solutions

There are other solutions comparable to *kubernetes-glue-operator* that work (or limited to work) on top
of Kubernetes resources in a language-independent way and try to simplify the operator
development but make some compromises. *kubernetes-glue-operator* has some nice properties:

1. **input is only a custom resource** (`Glue` or `GlueOperator`) that makes it very easy to set up, maintain,
   and start with. You don't have to build the project, all you have to manage is `yaml` files (CRD, inputs, deployment)
   to create an operator.
   
2. **Supports ordered resource reconciliation** which is very helpful when you have a kind of a "workflow" in your controller
   when have to wait until another resource is fully reconciled, maybe use its output (status) as an input for another resource.
   In practice, we see surprisingly a lot of such use cases, and especially could be important when managing external resource
   (more precisely Kubernetes objects that represents external resources)
 

You can reach the point with all such tools when it is not enough to cover your use case, in that case, there is no other
way just implement the controller using a classic operator framework,
like [Java Operator SDK](https://github.com/operator-framework/java-operator-sdk) or [Operator SDK](https://sdk.operatorframework.io/).

However, when you just decide that you want to have a nice Kubernetes native way to manage your application/resources using Operators, such tools make 
it very easy to start with. Or if you building a platform with simple abstractions over existing resources which are just compositions of other resources
this is probably the most elegant way to go.

As mentioned before there are solutions that are comparable to ours, and all of them have advantages and disadvantages:

- [metacontroller](https://github.com/metacontroller/metacontroller) - it a very interesting solution that allows
  to implement controller is any language programming language, just like in the case of *kubernetes-glue-operator* takes a custom resource as an input, which
  describes the Kubernetes resources that we are interested in - or watched/managed for a custom resource.
  However, it does not describe the desired state that is up to you to implement in the form of a web service endpoint,
  where all the inputs are received and the output is a list of desired resources. 
    - **pros**: you can implement a controller in any programming language,
       that does not necessarily have a nice operator framework      
    - **cons**: it is still limited to Kubernetes resources, every reconciliation requires a web service call
      this is problematic in cases when a huge number of resources are watched since the payload might
      be huge. Also, the endpoint still needs to be built and deployed.
  
  In summary *metacontroller* is a bit more generic solution this moment,
  but with additional complexity to manage, and much harder to start with.
  The main practical difference is in supporting ["bulk resources"](https://github.com/java-operator-sdk/kubernetes-glue-operator/issues/75)
  we will also support it in future versions.

- [Helm Operators](https://sdk.operatorframework.io/docs/building-operators/helm/tutorial/) - are a very efficient
  way to convert a helm chart into a controller. It also makes it very easy to start and use.
  However, the controller still needs to be build (the helm chart is not just an input configuration),
  does not handle related resources, and does not support ordering. In this terms is a bit more limited
  than *kubernetes-glue-operator*.

- [Crossplane Composition](https://docs.crossplane.io/latest/concepts/compositions/) TODO

- [Kro](https://github.com/awslabs/kro) TODO
