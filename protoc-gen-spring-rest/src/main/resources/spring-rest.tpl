// DON'T EDIT THIS FILE!

// Generate by: https://github.com/colin-plus/protoc-gen-spring-rest
// Source file: {{protoName}}

package {{javaPackage}};

import my.app.plugin.spring.RequestBuilder;
import my.app.plugin.spring.SpringUnaryObserver;

{{#needImportRequestBody}}import org.springframework.web.bind.annotation.RequestBody;{{/needImportRequestBody}}
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController("{{javaPackage}}.{{className}}")
public class {{className}} {
    private final {{grpcStub}} stub;

    public {{className}}({{grpcStub}} stub) {
        this.stub = stub;
    }
    {{#methodList}}

    @RequestMapping(method = RequestMethod.{{method}}, value = "{{url}}")
    public DeferredResult<String> {{name}}_{{method}}_{{index}}(HttpServletRequest request, HttpServletResponse response{{#hasBody}}, @RequestBody String body{{/hasBody}}) {
        DeferredResult<String> result = new DeferredResult<>(60 * 1000L);
        SpringUnaryObserver<{{responseType}}> observer = new SpringUnaryObserver<>(result);
        try {
            {{requestType}}.Builder builder = {{requestType}}.newBuilder();
            RequestBuilder.build(builder, request, {{#hasBody}}body{{/hasBody}}{{^hasBody}}null{{/hasBody}});
            stub.{{name}}(builder.build(), observer);
        } catch(Exception e) {
            observer.onError(e);
        }
        return result;
    }
    {{/methodList}}
}
