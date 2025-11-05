package com.appiancs.plugins.pdfutilities.util;

import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jsoup.nodes.Element;

import com.appiancorp.suiteapi.common.exceptions.InvalidVersionException;
import com.appiancorp.suiteapi.common.exceptions.PrivilegeException;
import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.content.exceptions.InvalidContentException;

public final class ParameterValidation {

  private static final Logger LOG = Logger.getLogger(ParameterValidation.class);

  private ParameterValidation() {
  }

  public static void checkNotNullOrBlank(String value, String paramName) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Parameter '" + paramName + "' cannot be null or blank.");
    }
  }

  public static void checkIsPositive(Long value, String paramName) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException("Parameter '" + paramName + "' must be a positive number.");
    }
  }

  public static void checkIsPositive(Integer value, String paramName) {
    if (value == null || value <= 0) {
      throw new IllegalArgumentException("Parameter '" + paramName + "' must be a positive number.");
    }
  }

  public static void checkInRange(Integer value, int min, int max, String paramName) {
    checkIsPositive(value, paramName); // First, ensure it's not null/negative
    if (value < min || value > max) {
      throw new IllegalArgumentException(paramName + " must be between " + min + " and " + max + ".");
    }
  }

  public static com.appiancorp.suiteapi.knowledge.Document validateAndGetDocument(Long docId, ContentService cs, String expectedExtension,
    String paramName) {
    checkIsPositive(docId, paramName); // Basic check first

    com.appiancorp.suiteapi.knowledge.Document doc;
    try {
      doc = cs.download(docId, ContentConstants.VERSION_CURRENT, false)[0];
    } catch (InvalidContentException | InvalidVersionException | PrivilegeException e) {
      if (LOG.isEnabledFor(Level.ERROR)) {
        LOG.error("Failed to download document ID " + docId, e);
      }
      throw new IllegalArgumentException("The " + paramName + " is invalid, could not be found, or you do not have permission to see it.",
        e);
    }

    if (doc == null) {
      throw new IllegalArgumentException("The " + paramName + " could not be found.");
    }

    String actualExtension = doc.getExtension();
    if (actualExtension == null || !actualExtension.equalsIgnoreCase(expectedExtension)) {
      throw new IllegalArgumentException(
        "The " + paramName + " must be a '" + expectedExtension + "' file, but it was a '" + actualExtension + "'.");
    }
    return doc;
  }

  public static void setImageFilePath(org.jsoup.nodes.Document jsoupDoc, ContentService cs) {
    // Select all <img> elements that have a 'src' attribute
    for (Element img : jsoupDoc.select("img[src]")) {
      String src = img.attr("src");
      try {
        // Check if the 'src' attribute is a valid number (i.e., a document ID)
        long docId = Long.parseLong(src);

        // If it is, download the document to get its temporary file path
        com.appiancorp.suiteapi.knowledge.Document imageDoc = cs.download(docId, ContentConstants.VERSION_CURRENT, false)[0];
        File tempImageFile = imageDoc.accessAsReadOnlyFile();

        // Update the 'src' attribute to the local file URI
        String localUri = tempImageFile.toURI().toString();
        img.attr("src", localUri);
        if (LOG.isEnabledFor(Level.INFO)) {
          LOG.info("Updated image path for document ID " + docId + " to: " + localUri);
        }
      } catch (NumberFormatException e) {
        // The 'src' was not a document ID (e.g., an external URL or base64 data), so we ignore it.
        if (LOG.isEnabledFor(Level.DEBUG)) {
          LOG.debug("Skipping image src, as it is not a document ID: " + src);
        }
      } catch (Exception e) {
        // Failed to download or access the document.
        if (LOG.isEnabledFor(Level.ERROR)) {
          LOG.error("Could not process image with document ID: " + src, e);
        }
        // Fallback: Remove the src so the PDF doesn't show a broken image icon.
        img.removeAttr("src");
      }
    }
  }

}
