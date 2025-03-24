package io.javaoperatorsdk.operator.glue.reconciler.glue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.glue.ControllerConfig;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.EventSourceRetriever;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class InformerRegisterTest {


  public static final String LABEL_SELECTOR = "myapp=true";

  @Test
  @SuppressWarnings("unchecked")
  void registersInformerWithLabelSelectorIfConfigured() {
    var gvk = GroupVersionKind.gvkFor(ConfigMap.class);
    var labelSelectors = Map.of(InformerRegister.toSimpleString(gvk),
        LABEL_SELECTOR);
    var config = Mockito.mock(ControllerConfig.class);
    when(config.resourceLabelSelector()).thenReturn(labelSelectors);
    var informerProducer = mock(InformerProducer.class);
    var register = new InformerRegister(informerProducer, config);
    var mockContext = mock(Context.class);
    var mockEventSourceRetriever = mock(EventSourceRetriever.class);
    when(mockContext.eventSourceRetriever()).thenReturn(mockEventSourceRetriever);

    register.registerInformer(mockContext, gvk, testGlue());

    verify(informerProducer).createInformer(argThat(c -> {
      assertThat(c.getInformerConfig().getLabelSelector()).isEqualTo(LABEL_SELECTOR);
      return true;
    }), any());
  }


  @Test
  void gvkToSimpleString() {
    assertThat(InformerRegister.toSimpleString(new GroupVersionKind("apps", "v1", "Deployment")))
        .isEqualTo("apps/v1#Deployment");
    assertThat(InformerRegister.toSimpleString(new GroupVersionKind("v1", "ConfigMap")))
        .isEqualTo("v1#ConfigMap");
  }

  Glue testGlue() {
    Glue glue = new Glue();
    glue.setMetadata(new ObjectMetaBuilder()
        .withName("test1")
        .build());
    return glue;
  }

}
