package architecturefixtures.notification.internal;

public class VolatileListener {
  @org.springframework.scheduling.annotation.Async
  @org.springframework.context.event.EventListener
  public void on(String event) {}
}
