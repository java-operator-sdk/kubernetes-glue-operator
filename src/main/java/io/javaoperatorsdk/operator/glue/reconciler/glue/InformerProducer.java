package io.javaoperatorsdk.operator.glue.reconciler.glue;


import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.config.informer.InformerEventSourceConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

import jakarta.inject.Singleton;


/** For mocking purpose only. Too complex to mock InformerEventSource creation. */
@Singleton
public class InformerProducer {

  public InformerEventSource<GenericKubernetesResource, Glue> createInformer(
      InformerEventSourceConfiguration<GenericKubernetesResource> configuration,
      Context<Glue> context) {
    return new InformerEventSource<>(configuration,
        context.eventSourceRetriever().eventSourceContextForDynamicRegistration());
  }

}
