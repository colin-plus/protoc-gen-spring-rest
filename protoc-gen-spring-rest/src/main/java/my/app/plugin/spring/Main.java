package my.app.plugin.spring;

import com.google.api.AnnotationsProto;
import com.google.protobuf.Descriptors;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

public class Main {
  public static void main(String[] args)
      throws IOException, Descriptors.DescriptorValidationException {
    BufferedInputStream inputStream = new BufferedInputStream(System.in);

    ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();
    extensionRegistry.add(AnnotationsProto.http);

    CodeGenerator codeGenerator = new CodeGenerator();
    CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(inputStream, extensionRegistry);
    CodeGeneratorResponse response = codeGenerator.generate(request);

    BufferedOutputStream outputStream = new BufferedOutputStream(System.out);
    response.writeTo(outputStream);
    outputStream.flush();
  }
}
