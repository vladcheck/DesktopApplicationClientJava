package com.DesktopApplicationClientJava.ui;

import java.util.concurrent.TimeoutException;
import javafx.scene.control.TextInputControl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.testfx.api.FxRobot;
import org.testfx.api.FxToolkit;

/**
 * Toolkit поднимается один раз на класс, а не перед каждым тестом, как это делает {@code
 * ApplicationExtension}. Чистое состояние между тестами даёт пересоздание сцены и моков в {@code
 * BeforeEach} наследника.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class FxTestBase {

  protected final FxRobot robot = new FxRobot();

  @BeforeAll
  void initToolkit() throws TimeoutException {
    startSharedToolkit();
  }

  @AfterAll
  void cleanupToolkit() throws TimeoutException {
    stopSharedToolkit(robot);
  }

  /**
   * Подъём toolkit для тестов, которые не могут наследовать этот класс (уже есть предок). Повторные
   * вызовы дешёвые: toolkit стартует один раз на JVM.
   */
  public static void startSharedToolkit() throws TimeoutException {
    FxToolkit.registerPrimaryStage();
  }

  /** Пара к {@link #startSharedToolkit()}. */
  public static void stopSharedToolkit(FxRobot robot) throws TimeoutException {
    FxToolkit.cleanupStages();
    FxToolkit.cleanupInput(robot);
  }

  /**
   * Синтетический ввод под headless Monocle идёт ~90 мс на клавишу и иногда теряется, а контроллеры
   * читают поля только по кнопке, поэтому текст ставится напрямую.
   */
  protected static void fillText(FxRobot robot, String query, String value) {
    robot.interact(() -> robot.lookup(query).queryAs(TextInputControl.class).setText(value));
  }
}
