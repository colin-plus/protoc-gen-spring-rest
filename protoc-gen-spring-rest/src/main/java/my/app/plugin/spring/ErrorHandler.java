package my.app.plugin.spring;

public class ErrorHandler {
  private static GrpcSprintErrorHandler errorHandler = GrpcSprintErrorHandler.DEFAULT;

  public static void setErrorHandler(GrpcSprintErrorHandler errorHandler) {
    ErrorHandler.errorHandler = errorHandler;
  }

  public static String handle(Throwable t) {
    return errorHandler.handle(t);
  }

  public interface GrpcSprintErrorHandler {
    String handle(Throwable t);

    GrpcSprintErrorHandler DEFAULT = t -> "{\"code\":\"error\",\"message\":\"服务器开小差了，稍后再试吧~\"}";
  }
}
