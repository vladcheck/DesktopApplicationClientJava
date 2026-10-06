package com.DesktopApplicationClientJava.services.mock;

/**
 * Contract for in-memory demo services (see MOCK_DATA in Program).
 *
 * <p>UI may check for this interface to enable demo conveniences that must never trigger against
 * the real backend. Override {@link #doIfInitialPage()} when a mock needs to prepare demo state on
 * page init.
 */
public interface MockService {

  /** Demo hook, no-op by default. */
  default void doIfInitialPage() {}
}
