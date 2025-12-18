package com.appiancs.plugins.pdfutilities;

import java.util.Objects;

import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancs.plugins.pdfutilities.dto.ConversionResult;

public abstract class BaseSmartService extends AppianSmartService {

  protected boolean errorOccurred = false;
  protected String errorMessage = "";

  protected final transient ContentService cs;
  private final Logger log;

  public BaseSmartService(ContentService cs, Logger log) {
    super();
    this.cs = Objects.requireNonNull(cs, "ContentService cannot be null");
    this.log = log;
  }

  /**
   * Processes a standard ConversionResult object.
   * If the result indicates failure, it logs the error and sets the output flags.
   */
  protected void handleResult(ConversionResult result) {
    if (result == null) {
      handleError(new IllegalStateException("Result object was null."), "Service returned no result.");
      return;
    }

    if (result.isSuccess()) {
      this.errorOccurred = false;
      this.errorMessage = null;
    } else {
      log.warn("Smart Service Failure: " + result.getErrorMessage());
      this.errorOccurred = true;
      this.errorMessage = result.getErrorMessage();
    }
  }

  protected void handleError(Exception e, String contextMessage) {
    String finalMsg = contextMessage + ": " + e.getMessage();
    log.error(finalMsg, e);
    this.errorOccurred = true;
    this.errorMessage = finalMsg;
  }

  @Name("ErrorOccurred")
  public boolean isErrorOccurred() {
    return errorOccurred;
  }

  @Name("ErrorMessage")
  public String getErrorMessage() {
    return errorMessage;
  }
}