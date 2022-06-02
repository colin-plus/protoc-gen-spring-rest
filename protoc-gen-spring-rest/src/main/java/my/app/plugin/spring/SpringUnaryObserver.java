package my.app.plugin.spring;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.stub.StreamObserver;
import org.springframework.web.context.request.async.DeferredResult;

public class SpringUnaryObserver<V extends Message> implements StreamObserver<V> {
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
      result.setResult(ErrorHandler.handle(t));
    } catch (Exception ignore) {
    }
  }

  @Override
  public void onCompleted() {}
}
