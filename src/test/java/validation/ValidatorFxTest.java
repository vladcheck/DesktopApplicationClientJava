package validation;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;

public abstract class ValidatorFxTest {
  private static volatile boolean toolkitStarted;

  @BeforeAll
  static void startJavaFxToolkit() throws InterruptedException {
    if (toolkitStarted) {
      return;
    }
    setHeadlessDefaults();
    CountDownLatch latch = new CountDownLatch(1);
    try {
      Platform.startup(latch::countDown);
    } catch (IllegalStateException alreadyStarted) {
      latch.countDown();
    }
    latch.await(30, TimeUnit.SECONDS);
    toolkitStarted = true;
  }

  private static void setHeadlessDefaults() {
    setIfAbsent("testfx.headless", "true");
    setIfAbsent("testfx.robot", "glass");
    setIfAbsent("glass.platform", "Monocle");
    setIfAbsent("monocle.platform", "Headless");
    setIfAbsent("prism.order", "sw");
    setIfAbsent("prism.text", "t2k");
    setIfAbsent("java.awt.headless", "true");
  }

  private static void setIfAbsent(String key, String value) {
    if (System.getProperty(key) == null) {
      System.setProperty(key, value);
    }
  }
}
