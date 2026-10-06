package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.services.ServiceException;
import com.DesktopApplicationClientJava.validation.EmailValidator;
import com.DesktopApplicationClientJava.validation.PasswordValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.PasswordField;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.model.validators.Validator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class LoginFormController extends Controller {
  @FXML private VBox formContainer;
  @FXML private Button submitButton;

  private StringField email;
  private PasswordField password;
  private Form loginForm;

  @FXML
  private void initialize() {
    email =
        Field.ofStringType("")
            .label(EmailValidator.FIELD_NAME)
            .placeholder("youremail@example.com")
            .required(ErrorMessages.Required)
            .validate(toArray(EmailValidator.validators()));
    password =
        Field.ofPasswordType("")
            .label(PasswordValidator.FIELD_NAME)
            .required(ErrorMessages.Required)
            .validate(toArray(PasswordValidator.validators()));

    loginForm = Form.of(Section.of(email, password));
    formContainer.getChildren().add(new FormRenderer(loginForm));
  }

  @FXML
  private void onSubmit(ActionEvent event) {
    boolean valid = email.validate() & password.validate();
    if (!valid) {
      return;
    }

    try {
      User user = services.getAuthService().login(email.getValue(), password.getValue());
      if (user == null) {
        return;
      }
      navigator.goTo("/fxml/Search.fxml");
    } catch (ServiceException e) {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Ошибка входа");
      alert.setHeaderText(null);
      alert.setContentText(e.getMessage());
      alert.showAndWait();
    }
  }

  @SuppressWarnings("unchecked")
  private static Validator<String>[] toArray(List<Validator<String>> validators) {
    return validators.toArray(new Validator[0]);
  }
}
