package ui;

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
abstract class FxTestBase {

  protected final FxRobot robot = new FxRobot();

  @BeforeAll
  void initToolkit() throws TimeoutException {
    FxToolkit.registerPrimaryStage();
  }

  @AfterAll
  void cleanupToolkit() throws TimeoutException {
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
