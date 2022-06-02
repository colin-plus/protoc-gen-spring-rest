package my.app.plugin.spring;

import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RequestBuilder {
  public static <V extends Message> void build(
      Message.Builder builder, HttpServletRequest request, String body)
      throws InvalidProtocolBufferException {
    if (body != null && !Objects.equals(body.trim(), "")) {
      JsonFormat.parser().merge(body, builder);
    }

    Map<String, String> parameterMap =
        request.getParameterMap().keySet().stream()
            .collect(Collectors.toMap(Function.identity(), request::getParameter));
    builder
        .getDescriptorForType()
        .getFields()
        .forEach(
            it -> {
              if (parameterMap.containsKey(it.getName())) {
                Object value = parseObject(it, parameterMap.get(it.getName()));
                if (value != null) {
                  if (!builder.hasField(it)) {
                    builder.setField(it, value);
                  }
                }
              }
            });
  }

  private static Object parseObject(FieldDescriptor fieldDescriptor, String parameter) {
    Object value = null;
    FieldDescriptor.JavaType javaType = fieldDescriptor.getJavaType();
    switch (javaType) {
      case INT:
        value = Integer.valueOf(parameter);
        break;
      case LONG:
        value = Long.valueOf(parameter);
        break;
      case FLOAT:
        value = Float.valueOf(parameter);
        break;
      case DOUBLE:
        value = Double.valueOf(parameter);
        break;
      case BOOLEAN:
        value = Boolean.valueOf(parameter);
        break;
      case STRING:
        value = parameter;
        break;
      case ENUM:
      case MESSAGE:
      case BYTE_STRING:
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + javaType);
    }
    return value;
  }
}
