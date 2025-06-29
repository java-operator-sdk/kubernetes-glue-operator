package io.javaoperatorsdk.operator.glue.customresource.glue.condition;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ReadyConditionSpec.class, name = "ReadyCondition"),
    @JsonSubTypes.Type(value = JavaScriptConditionSpec.class, name = "JSCondition"),
    @JsonSubTypes.Type(value = QuteConditionSpec.class, name = "QuteCondition")
})
public class ConditionSpec {


}
