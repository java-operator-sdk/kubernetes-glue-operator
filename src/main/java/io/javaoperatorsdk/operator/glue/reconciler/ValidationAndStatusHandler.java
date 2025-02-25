package io.javaoperatorsdk.operator.glue.reconciler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.CustomResource;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.glue.GlueException;
import io.javaoperatorsdk.operator.glue.customresource.AbstractStatus;
import io.javaoperatorsdk.operator.glue.customresource.glue.DependentResourceSpec;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.customresource.glue.GlueSpec;
import io.javaoperatorsdk.operator.glue.customresource.glue.GlueStatus;
import io.javaoperatorsdk.operator.glue.customresource.glue.RelatedResourceSpec;
import io.javaoperatorsdk.operator.glue.customresource.operator.GlueOperator;
import io.javaoperatorsdk.operator.glue.customresource.operator.GlueOperatorStatus;

import jakarta.inject.Singleton;

@Singleton
public class ValidationAndStatusHandler {

  public static final int MAX_MESSAGE_SIZE = 150;

  private static final Logger log = LoggerFactory.getLogger(ValidationAndStatusHandler.class);

  public static final String NON_UNIQUE_NAMES_FOUND_PREFIX = "Non unique names found: ";

  public <T extends CustomResource<?, ? extends AbstractStatus>> ErrorStatusUpdateControl<T> updateStatusErrorMessage(
      Exception e,
      T resource) {
    log.error("Error during reconciliation of resource. Name: {} namespace: {}, Kind: {}",
        resource.getMetadata().getName(), resource.getMetadata().getNamespace(), resource.getKind(),
        e);
    if (e instanceof ValidationAndStatusHandler.NonUniqueNameException ex) {
      resource.getStatus()
          .setErrorMessage(NON_UNIQUE_NAMES_FOUND_PREFIX + String.join(",", ex.getDuplicates()));
      return ErrorStatusUpdateControl.patchStatus(resource).withNoRetry();
    } else {
      var message = e.getMessage();
      if (message == null) {
        message = e.getClass().getName();
      }
      if (message.length() > MAX_MESSAGE_SIZE) {
        message = message.substring(0, MAX_MESSAGE_SIZE) + "...";
      }
      resource.getStatus().setErrorMessage("Error: " + message);
      return ErrorStatusUpdateControl.patchStatus(resource);
    }
  }

  public UpdateControl<GlueOperator> handleStatusUpdate(GlueOperator primary) {
    if (primary.getStatus() == null) {
      primary.setStatus(new GlueOperatorStatus());
    }
    return handleGenericStatusUpdate(primary);
  }

  public UpdateControl<Glue> handleStatusUpdate(Glue primary) {
    if (primary.getStatus() == null) {
      primary.setStatus(new GlueStatus());
    }
    return handleGenericStatusUpdate(primary);
  }

  private <T extends CustomResource<?, ? extends AbstractStatus>> UpdateControl<T> handleGenericStatusUpdate(
      T primary) {
    boolean patch = false;

    if (primary.getStatus().getErrorMessage() != null) {
      patch = true;
      primary.getStatus().setErrorMessage(null);
    }
    if (!primary.getMetadata().getGeneration()
        .equals(primary.getStatus().getObservedGeneration())) {
      patch = true;
      primary.getStatus().setObservedGeneration(primary.getMetadata().getGeneration());
    }

    if (patch) {
      primary.getMetadata().setResourceVersion(null);
      return UpdateControl.patchStatus(primary);
    } else {
      return UpdateControl.noUpdate();
    }

  }

  public void checkIfValidGlueSpec(GlueSpec glueSpec) {
    checkIfBulkProvidesResourceTemplate(glueSpec);
    checkIfNamesAreUnique(glueSpec);
  }

  private void checkIfBulkProvidesResourceTemplate(GlueSpec glueSpec) {
    glueSpec.getChildResources().forEach(r -> {
      if (Boolean.TRUE.equals(r.getBulk()) && r.getResourceTemplate() == null) {
        throw new GlueException("Bulk resource requires a template to be set");
      }
    });
  }

  void checkIfNamesAreUnique(GlueSpec glueSpec) {
    Set<String> seen = new HashSet<>();
    List<String> duplicates = new ArrayList<>();

    Consumer<String> deduplicate = n -> {
      if (seen.contains(n)) {
        duplicates.add(n);
      } else {
        seen.add(n);
      }
    };
    glueSpec.getChildResources().stream().map(DependentResourceSpec::getName).forEach(deduplicate);
    glueSpec.getRelatedResources().stream().map(RelatedResourceSpec::getName).forEach(deduplicate);

    if (!duplicates.isEmpty()) {
      throw new NonUniqueNameException(duplicates);
    }
  }

  public static class NonUniqueNameException extends GlueException {

    private final List<String> duplicates;

    public NonUniqueNameException(List<String> duplicates) {
      this.duplicates = duplicates;
    }

    public List<String> getDuplicates() {
      return duplicates;
    }
  }

}
