package architecturefixtures.notification.internal;

public class MetaVolatileListener {
  @org.springframework.scheduling.annotation.Async
  @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
  @java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
  public @interface Background {}

  @Background
  @org.springframework.context.event.EventListener
  public void on(String event) {}
}
