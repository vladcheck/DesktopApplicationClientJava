package ui.integration;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.DesktopApplicationClientJava.Api.ApiClient;
import com.DesktopApplicationClientJava.services.AuthService;
import com.DesktopApplicationClientJava.services.ResourceService;
import com.DesktopApplicationClientJava.services.impl.AuthServiceImpl;
import com.DesktopApplicationClientJava.services.impl.ResourceServiceImpl;
import com.DesktopApplicationClientJava.session.Session;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

@Tag("integration")
public abstract class IntegrationTestCase {
  static int timeoutMs = 1000;

  protected static Dotenv dotenv;
  protected static String adminEmail;
  protected static String adminPassword;

  protected AuthService authService;
  protected ResourceService resourceService;

  @BeforeAll
  static void loadEnv() {
    Path dotenvPath = Path.of(System.getProperty("dotenv.path", ".env")).toAbsolutePath();
    dotenv =
        Dotenv.configure()
            .directory(dotenvPath.getParent().toString())
            .filename(dotenvPath.getFileName().toString())
            .ignoreIfMissing()
            .load();

    adminEmail = var("IT_ADMIN_EMAIL", "admin@example.com");
    adminPassword = var("IT_ADMIN_PASSWORD", null);
  }

  @BeforeEach
  void setUpServices() {
    assumeTrue(isBackendUp(), "Backend is not running at localhost:8080, skipping");
    Session session = new Session();
    ApiClient apiClient = new ApiClient(session);
    authService = new AuthServiceImpl(apiClient, session);
    resourceService = new ResourceServiceImpl(apiClient);
  }

  protected static String var(String key, String fallback) {
    return dotenv.get(key, fallback);
  }

  protected static boolean isBackendUp() {
    try (Socket socket = new Socket()) {
      socket.connect(
          new InetSocketAddress("localhost", Integer.parseInt(var("port", "8080"))), timeoutMs);
      return true;
    } catch (IOException e) {
      return false;
    }
  }
}
