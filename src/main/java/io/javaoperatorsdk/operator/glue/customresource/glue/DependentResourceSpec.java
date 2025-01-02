package io.javaoperatorsdk.operator.glue.customresource.glue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.fabric8.crd.generator.annotation.PreserveUnknownFields;
import io.fabric8.generator.annotation.Required;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.glue.customresource.glue.condition.ConditionSpec;

public class DependentResourceSpec {

  @Required
  private String name;

  private boolean clusterScoped = Boolean.FALSE;

  @PreserveUnknownFields
  private GenericKubernetesResource resource;

  private String resourceTemplate;

  private Matcher matcher = Matcher.SSA;

  private Boolean bulk = false;

  private List<String> dependsOn = new ArrayList<>();

  @PreserveUnknownFields
  private ConditionSpec readyPostCondition;

  @PreserveUnknownFields
  private ConditionSpec condition;

  public String getName() {
    return name;
  }

  public DependentResourceSpec setName(String name) {
    this.name = name;
    return this;
  }

  public List<String> getDependsOn() {
    return dependsOn;
  }

  public DependentResourceSpec setDependsOn(List<String> dependsOn) {
    this.dependsOn = dependsOn;
    return this;
  }

  public ConditionSpec getReadyPostCondition() {
    return readyPostCondition;
  }

  public DependentResourceSpec setReadyPostCondition(ConditionSpec readyPostConditionSpec) {
    this.readyPostCondition = readyPostConditionSpec;
    return this;
  }


  public GenericKubernetesResource getResource() {
    return resource;
  }

  public DependentResourceSpec setResource(GenericKubernetesResource resource) {
    this.resource = resource;
    return this;
  }

  public ConditionSpec getCondition() {
    return condition;
  }

  public DependentResourceSpec setCondition(ConditionSpec conditionSpec) {
    this.condition = conditionSpec;
    return this;
  }

  public String getResourceTemplate() {
    return resourceTemplate;
  }

  public DependentResourceSpec setResourceTemplate(String resourceTemplate) {
    this.resourceTemplate = resourceTemplate;
    return this;
  }

  public boolean isClusterScoped() {
    return clusterScoped;
  }

  public void setClusterScoped(boolean clusterScoped) {
    this.clusterScoped = clusterScoped;
  }

  public Matcher getMatcher() {
    return matcher;
  }

  public void setMatcher(Matcher matcher) {
    this.matcher = matcher;
  }

  public Boolean getBulk() {
    return bulk;
  }

  public void setBulk(Boolean bulk) {
    this.bulk = bulk;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    DependentResourceSpec that = (DependentResourceSpec) o;
    return clusterScoped == that.clusterScoped && Objects.equals(name, that.name)
        && Objects.equals(resource, that.resource)
        && Objects.equals(resourceTemplate, that.resourceTemplate) && matcher == that.matcher
        && Objects.equals(bulk, that.bulk) && Objects.equals(dependsOn, that.dependsOn)
        && Objects.equals(readyPostCondition, that.readyPostCondition)
        && Objects.equals(condition, that.condition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, clusterScoped, resource, resourceTemplate, matcher, bulk, dependsOn,
        readyPostCondition, condition);
  }
}
