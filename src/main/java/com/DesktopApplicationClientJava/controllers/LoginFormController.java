package com.DesktopApplicationClientJava.controllers;

import com.DesktopApplicationClientJava.controllers.utils.Controller;
import com.DesktopApplicationClientJava.entities.Role;
import com.DesktopApplicationClientJava.entities.User;
import com.DesktopApplicationClientJava.validation.EmailValidator;
import com.DesktopApplicationClientJava.validation.FirstNameValidator;
import com.DesktopApplicationClientJava.validation.LastNameValidator;
import com.DesktopApplicationClientJava.validation.PasswordValidator;
import com.DesktopApplicationClientJava.validation.ThirdNameValidator;
import com.DesktopApplicationClientJava.validation.utils.ErrorMessages;
import com.dlsc.formsfx.model.structure.Field;
import com.dlsc.formsfx.model.structure.Form;
import com.dlsc.formsfx.model.structure.PasswordField;
import com.dlsc.formsfx.model.structure.Section;
import com.dlsc.formsfx.model.structure.StringField;
import com.dlsc.formsfx.model.validators.Validator;
import com.dlsc.formsfx.view.renderer.FormRenderer;
import java.util.List;
import java.util.UUID;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public class LoginFormController extends Controller {
  @FXML private VBox formContainer;
  @FXML private Button submitButton;

  private StringField firstName;
  private StringField lastName;
  private StringField thirdName;
  private StringField email;
  private PasswordField password;
  private Form loginForm;

  @FXML
  private void initialize() {
    firstName =
        Field.ofStringType("")
            .label(FirstNameValidator.FIELD_NAME)
            .placeholder("Антон")
            .required(ErrorMessages.Required)
            .validate(toArray(FirstNameValidator.validators()));
    lastName =
        Field.ofStringType("")
            .label(LastNameValidator.FIELD_NAME)
            .placeholder("Антонов")
            .required(ErrorMessages.Required)
            .validate(toArray(LastNameValidator.validators()));
    thirdName =
        Field.ofStringType("")
            .label(ThirdNameValidator.FIELD_NAME)
            .placeholder("Антонович")
            .required(ErrorMessages.Required)
            .validate(toArray(ThirdNameValidator.validators()));
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

    loginForm = Form.of(Section.of(firstName, lastName, thirdName, email, password));
    formContainer.getChildren().add(new FormRenderer(loginForm));
  }

  @FXML
  private void onSubmit(ActionEvent event) {
    boolean valid =
        firstName.validate()
            & lastName.validate()
            & thirdName.validate()
            & email.validate()
            & password.validate();
    if (!valid) {
      return;
    }

    User user = authenticate(firstName.getValue(), lastName.getValue(), email.getValue());
    if (user == null) {
      return;
    }

    session.setCurrentUser(user);
    navigator.goTo("/fxml/Search.fxml");
  }

  private User authenticate(String firstName, String lastName, String email) {
    return new User(UUID.randomUUID(), email, Role.USER, firstName, lastName, true);
  }

  @SuppressWarnings("unchecked")
  private static Validator<String>[] toArray(List<Validator<String>> validators) {
    return validators.toArray(new Validator[0]);
  }
}
