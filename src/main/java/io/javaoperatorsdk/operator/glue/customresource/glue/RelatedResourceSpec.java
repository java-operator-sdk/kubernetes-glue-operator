package io.javaoperatorsdk.operator.glue.customresource.glue;

import java.util.List;
import java.util.Objects;

import io.fabric8.generator.annotation.Required;

public class RelatedResourceSpec {

  // name for referencing the resource from templates and conditions (not name from object metadata)
  @Required
  private String name;
  private String namespace;

  @Required
  private String apiVersion;
  @Required
  private String kind;
  private boolean clusterScoped = Boolean.FALSE;
  private List<String> resourceNames;


  public String getApiVersion() {
    return apiVersion;
  }

  public RelatedResourceSpec setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
    return this;
  }

  public String getKind() {
    return kind;
  }

  public RelatedResourceSpec setKind(String kind) {
    this.kind = kind;
    return this;
  }

  public String getName() {
    return name;
  }

  public RelatedResourceSpec setName(String name) {
    this.name = name;
    return this;
  }

  public List<String> getResourceNames() {
    return resourceNames;
  }

  public RelatedResourceSpec setResourceNames(List<String> resourceNames) {
    this.resourceNames = resourceNames;
    return this;
  }

  public String getNamespace() {
    return namespace;
  }

  public RelatedResourceSpec setNamespace(String namespace) {
    this.namespace = namespace;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    RelatedResourceSpec that = (RelatedResourceSpec) o;
    return clusterScoped == that.clusterScoped && Objects.equals(name, that.name)
        && Objects.equals(apiVersion, that.apiVersion) && Objects.equals(kind, that.kind)
        && Objects.equals(namespace, that.namespace)
        && Objects.equals(resourceNames, that.resourceNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, apiVersion, kind, clusterScoped, namespace, resourceNames);
  }

  public boolean isClusterScoped() {
    return clusterScoped;
  }

  public void setClusterScoped(boolean clusterScoped) {
    this.clusterScoped = clusterScoped;
  }
}
