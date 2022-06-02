package my.app.plugin.spring;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;
import org.springframework.web.context.request.async.DeferredResult;

public class SpringUnaryObserver<V extends Message> implements StreamObserver<V> {
  private static final String ERROR_RESPONSE =
      "{\"code\":\"error\",\"message\":\"服务器开小差了，稍后再试吧~\"}";
  private final DeferredResult<String> result;

  public SpringUnaryObserver(DeferredResult<String> result) {
    this.result = result;
  }

  @Override
  public void onNext(V value) {
    if (value != null) {
      try {
        result.setResult(JsonFormat.printer().print(value));
      } catch (Exception e) {
        onError(e);
      }
    }
  }

  @Override
  public void onError(Throwable t) {
    try {
      result.setResult(ERROR_RESPONSE);
    } catch (Exception ignore) {
    }
  }

  @Override
  public void onCompleted() {}
}
