package my.app.plugin.spring;

public class ErrorHandler {
  private static GrpcSpringErrorHandler errorHandler = GrpcSpringErrorHandler.DEFAULT;

  public static void setErrorHandler(GrpcSpringErrorHandler errorHandler) {
    ErrorHandler.errorHandler = errorHandler;
  }

  public static String handle(Throwable t) {
    return errorHandler.handle(t);
  }

  public interface GrpcSpringErrorHandler {
    String handle(Throwable t);

    GrpcSpringErrorHandler DEFAULT = t -> "{\"code\":\"error\",\"message\":\"服务器开小差了，稍后再试吧~\"}";
  }
}
