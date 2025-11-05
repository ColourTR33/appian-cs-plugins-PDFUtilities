package com.appiancs.plugins.pdfutilities;

import java.util.Objects;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;

/**
 * An abstract base class for all smart services in this plugin.
 * It centralises common functionality, such as error handling,
 * and standard output parameters (errorOccurred, errorMessage).
 */

public abstract class BaseSmartService extends AppianSmartService {

  // These outputs are common to all smart services
  protected boolean errorOccurred;
  protected String errorMessage;

  protected final transient ContentService cs;
  private final Logger log;

  public BaseSmartService(ContentService cs, Logger log) {
    super();
    this.cs = Objects.requireNonNull(cs, "ContentService cannot be null");
    this.log = log;
  }

  /**
   * A centralised method to handle exceptions, log them,
   * and set the standard error output process variables.
   *
   * @param e
   *          The exception that was caught.
   * @param userFriendlyMessage
   *          A clear message to be returned to the process.
   * @throws SmartServiceException
   *           Throws the configured exception to stop the node.
   */

  protected void handleException(Exception e, String userFriendlyMessage) throws SmartServiceException {
    log.error(userFriendlyMessage, e);
    this.errorOccurred = true;
    this.errorMessage = userFriendlyMessage;
    throw new SmartServiceException.Builder(getClass(), e).userMessage(userFriendlyMessage).build();
  }

  @Name("errorOccurred")
  public boolean isErrorOccurred() {
    return errorOccurred;
  }

  @Name("errorMessage")
  public String getErrorMessage() {
    return errorMessage;
  }
}
