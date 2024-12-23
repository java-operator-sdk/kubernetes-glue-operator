package io.javaoperatorsdk.operator.glue.customresource.glue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GlueSpec {

  private List<DependentResourceSpec> childResources = new ArrayList<>();

  private List<RelatedResourceSpec> relatedResources = new ArrayList<>();

  public List<DependentResourceSpec> getChildResources() {
    return childResources;
  }

  public void setChildResources(List<DependentResourceSpec> childResources) {
    this.childResources = childResources;
  }

  public List<RelatedResourceSpec> getRelatedResources() {
    return relatedResources;
  }

  public GlueSpec setRelatedResources(List<RelatedResourceSpec> relatedResources) {
    this.relatedResources = relatedResources;
    return this;
  }

  @Override
  public String toString() {
    return "GlueSpec{" +
        "resources=" + childResources +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GlueSpec that = (GlueSpec) o;
    return Objects.equals(childResources, that.childResources);
  }

  @Override
  public int hashCode() {
    return Objects.hash(childResources);
  }
}
