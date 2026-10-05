package com.DesktopApplicationClientJava.validation.utils;

import com.DesktopApplicationClientJava.utils.FieldError;
import java.util.function.Function;
import javafx.scene.Node;
import net.synedra.validatorfx.Decoration;
import net.synedra.validatorfx.ValidationMessage;

public final class FieldErrorDecoration {
  private FieldErrorDecoration() {}

  public static Function<ValidationMessage, Decoration> forError(FieldError error) {
    return message ->
        new Decoration() {
          @Override
          public void add(Node target) {
            error.show(message.getText());
          }

          @Override
          public void remove(Node target) {
            error.hide();
          }
        };
  }
}
