package io.javaoperatorsdk.operator.glue.customresource.operator;

import java.util.Objects;

public class GlueMetadata {

  private String name;
  private String namespace;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GlueMetadata that = (GlueMetadata) o;
    return Objects.equals(name, that.name) && Objects.equals(namespace, that.namespace);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, namespace);
  }
}
