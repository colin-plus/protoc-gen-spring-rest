package my.app.plugin.spring;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.common.base.CaseFormat;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.DescriptorProtos.*;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import lombok.Builder;
import lombok.Data;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CodeGenerator {
  public CodeGeneratorResponse generate(CodeGeneratorRequest request)
      throws DescriptorValidationException {

    List<FileDescriptor> fileDescriptorList =
        Lists.newArrayList(
            MethodOptions.getDescriptor().getFile(),
            AnnotationsProto.getDescriptor(),
            HttpRule.getDescriptor().getFile());

    CodeGeneratorResponse.Builder response = CodeGeneratorResponse.newBuilder();

    Map<String, Descriptor> lookupMap = new HashMap<>(100);

    for (FileDescriptorProto fileDescriptorProto : request.getProtoFileList()) {
      fileDescriptorList.add(
          FileDescriptor.buildFrom(
              fileDescriptorProto, fileDescriptorList.toArray(new FileDescriptor[] {})));

      String fileDescriptorProtoPackage = fileDescriptorProto.getPackage();
      for (DescriptorProto descriptorProto : fileDescriptorProto.getMessageTypeList()) {
        String prefix = ".";

        if (!Strings.isNullOrEmpty(fileDescriptorProtoPackage)) {
          prefix += fileDescriptorProtoPackage + ".";
        }

        String descriptorProtoName = prefix + descriptorProto.getName();
        Descriptor descriptor =
            FileDescriptor.buildFrom(
                    fileDescriptorProto, fileDescriptorList.toArray(new FileDescriptor[] {}))
                .findMessageTypeByName(descriptorProto.getName());
        lookupMap.put(descriptorProtoName, descriptor);
      }

      List<ServiceMethod> serviceMethodList = new ArrayList<>();
      for (ServiceDescriptor serviceDescriptor :
          FileDescriptor.buildFrom(
                  fileDescriptorProto, fileDescriptorList.toArray(new FileDescriptor[] {}))
              .getServices()) {
        ServiceDescriptorProto serviceDescriptorProto = serviceDescriptor.toProto();
        for (MethodDescriptorProto methodDescriptorProto : serviceDescriptorProto.getMethodList()) {
          if (!methodDescriptorProto.getOptions().hasExtension(AnnotationsProto.http)) {
            continue;
          }
          if (methodDescriptorProto.getClientStreaming()
              || methodDescriptorProto.getServerStreaming()) {
            continue;
          }
          serviceMethodList.add(
              ServiceMethod.builder()
                  .serviceDescriptor(serviceDescriptor)
                  .methodDescriptorProto(methodDescriptorProto)
                  .build());
        }
      }

      if (!serviceMethodList.isEmpty()) {
        generate(fileDescriptorProto, response, lookupMap, serviceMethodList);
      }
    }

    return response.build();
  }

  private void generate(
      FileDescriptorProto fileDescriptorProto,
      CodeGeneratorResponse.Builder response,
      Map<String, Descriptor> lookupMap,
      List<ServiceMethod> serviceMethodList) {
    Controller controller = buildController(fileDescriptorProto, serviceMethodList, lookupMap);

    if (controller != null) {
      MustacheFactory mustacheFactory = new DefaultMustacheFactory();
      Mustache mustache = mustacheFactory.compile("spring-rest.tpl");
      StringWriter writer = new StringWriter();
      mustache.execute(writer, controller);

      response.addFile(
          CodeGeneratorResponse.File.newBuilder()
              .setContent(writer.toString())
              .setName(controller.getFileName())
              .build());
    }
  }

  private Controller buildController(
      FileDescriptorProto fileDescriptorProto,
      List<ServiceMethod> serviceMethodList,
      Map<String, Descriptor> lookupMap) {
    ServiceDescriptorProto serviceDescriptorProto =
        serviceMethodList.get(0).getServiceDescriptor().toProto();

    String protoName = fileDescriptorProto.getName();
    String packageName = javaPackage(fileDescriptorProto);
    String className = controllerClassName(serviceDescriptorProto);
    String grpcStub = grpcStubClass(fileDescriptorProto, serviceDescriptorProto);
    String fileName = packageName.replace('.', '/') + "/" + className + ".java";

    List<ControllerMethod> list = new ArrayList<>();
    for (ServiceMethod serviceMethod : serviceMethodList) {
      Descriptor input = lookupMap.get(serviceMethod.getMethodDescriptorProto().getInputType());
      Descriptor output = lookupMap.get(serviceMethod.getMethodDescriptorProto().getOutputType());
      List<ControllerMethod> controllerMethodList =
          buildControllerMethod(serviceMethod, input, output);
      list.addAll(controllerMethodList);
    }

    if (list.isEmpty()) {
      return null;
    }

    return Controller.builder()
        .protoName(protoName)
        .fileName(fileName)
        .javaPackage(javaPackage(fileDescriptorProto))
        .className(className)
        .grpcStub(grpcStub)
        .methodList(list)
        .needImportRequestBody(list.stream().anyMatch(ControllerMethod::isHasBody))
        .build();
  }

  private List<ControllerMethod> buildControllerMethod(
      ServiceMethod serviceMethod, Descriptor input, Descriptor output) {
    MethodDescriptorProto methodDescriptorProto = serviceMethod.getMethodDescriptorProto();
    HttpRule httpRule = methodDescriptorProto.getOptions().getExtension(AnnotationsProto.http);
    List<HttpRule> httpRuleList =
        ImmutableList.<HttpRule>builder()
            .add(httpRule)
            .addAll(httpRule.getAdditionalBindingsList())
            .build();

    List<ControllerMethod> controllerMethodList = new ArrayList<>();

    int methodIndex = 0;
    for (HttpRule rule : httpRuleList) {
      String httpUrl = httpUrl(rule);
      if (httpUrl == null || httpUrl.isEmpty()) {
        continue;
      }

      String javaMethod = serviceMethod.getMethodDescriptorProto().getName();
      String httpMethod = rule.getPatternCase().toString();
      boolean hasBody = !Strings.isNullOrEmpty(rule.getBody());
      String requestType = className(input);
      String responseType = className(output);

      ControllerMethod controllerMethod =
          ControllerMethod.builder()
              .name(javaMethod)
              .index(methodIndex++)
              .method(httpMethod)
              .url(httpUrl)
              .hasBody(hasBody)
              .requestType(requestType)
              .responseType(responseType)
              .build();

      controllerMethodList.add(controllerMethod);
    }

    return controllerMethodList;
  }

  private String httpUrl(HttpRule rule) {
    switch (rule.getPatternCase()) {
      case GET:
        return rule.getGet();
      case PUT:
        return rule.getPut();
      case POST:
        return rule.getPost();
      case DELETE:
        return rule.getDelete();
      case PATCH:
        return rule.getPatch();
      case CUSTOM:
        return rule.getCustom().getPath();
      default:
        return null;
    }
  }

  public static String javaPackage(FileDescriptorProto fileDescriptorProto) {
    return fileDescriptorProto.getOptions().getJavaPackage();
  }

  public static String grpcStubClass(
      FileDescriptorProto fileDescriptorProto, ServiceDescriptorProto serviceDescriptorProto) {
    String javaPackage = javaPackage(fileDescriptorProto);
    String serviceName = serviceDescriptorProto.getName();
    return javaPackage + "." + serviceName + "Grpc." + serviceName + "Stub";
  }

  public static String controllerClassName(ServiceDescriptorProto serviceDescriptorProto) {
    return serviceDescriptorProto.getName() + "RestController";
  }

  public static String className(Descriptor descriptor) {
    String javaPackage = descriptor.getFile().getOptions().getJavaPackage();
    String outerClassName = descriptor.getFile().getOptions().getJavaOuterClassname();
    boolean multipleFiles = descriptor.getFile().getOptions().getJavaMultipleFiles();

    StringBuilder builder = new StringBuilder(javaPackage);
    builder.append(".");

    if (multipleFiles) {
      builder.append(descriptor.getName());
    } else {
      String baseClassName;
      if (!outerClassName.isEmpty()) {
        baseClassName = outerClassName;
      } else {
        String baseName = descriptor.getFile().getName();
        baseName = baseName.substring(baseName.lastIndexOf('/') + 1);
        baseName = baseName.replace(".proto", "");
        baseClassName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, baseName);
      }
      builder.append(baseClassName).append(".").append(descriptor.getName());
    }

    return builder.toString();
  }

  @Data
  @Builder
  private static class ServiceMethod {
    private ServiceDescriptor serviceDescriptor;
    private MethodDescriptorProto methodDescriptorProto;
  }

  @Data
  @Builder
  private static class Controller {
    private String protoName;
    private String fileName;
    private String javaPackage;
    private String className;
    private String grpcStub;
    private List<ControllerMethod> methodList;
    private boolean needImportRequestBody;
  }

  @Data
  @Builder
  private static class ControllerMethod {
    private String name;
    private String method;
    private String url;
    private String requestType;
    private String responseType;
    private boolean hasBody;
    private int index;
  }
}
