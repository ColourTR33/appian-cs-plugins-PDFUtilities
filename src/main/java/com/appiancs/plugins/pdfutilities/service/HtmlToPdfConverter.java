package com.appiancs.plugins.pdfutilities.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Element;
import org.w3c.dom.Document;

import com.appiancorp.suiteapi.content.ContentConstants;
import com.appiancs.plugins.pdfutilities.dto.ConversionRequest;
import com.appiancs.plugins.pdfutilities.dto.ConversionResult;
import com.appiancs.plugins.pdfutilities.util.ParameterValidation;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.util.XRLog;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class HtmlToPdfConverter {

  private static final Logger LOG = Logger.getLogger(HtmlToPdfConverter.class);
  private final ConversionRequest request;
  private final Map<String, Boolean> uriReachableCache = new HashMap<>();

  public HtmlToPdfConverter(ConversionRequest request) {
    this.request = request;
  }

  // =================================================================================
  // == 1. PUBLIC EXECUTION METHOD
  // =================================================================================

  /**
   * Orchestrates the entire HTML to PDF conversion process.
   */
  public ConversionResult executeConversion() {
    File tempPdfFile = null;
    ExecutorService executor = null;

    try {
      // Step 1: Prepare the HTML DOM (parsing, cleaning, styling)
      tempPdfFile = File.createTempFile("temp_pdf_" + request.sourceDocument, ".pdf");

      if (LOG.isEnabledFor(org.apache.log4j.Level.INFO)) {
        LOG.info("Temporary PDF file created: " + tempPdfFile.getAbsolutePath());
      }

      org.jsoup.nodes.Document preparedHtml = prepareHtmlDom();

      // Step 2: Load custom fonts (only if needed)
      List<FontData> fontDataList = null;
      if (!request.simplifyFonts) {
        fontDataList = loadFontData();
      }

      // Step 3: Generate the PDF from the prepared HTML

      executor = Executors.newSingleThreadExecutor();

      final File finalTempFile = tempPdfFile;
      final org.jsoup.nodes.Document finalHtml = preparedHtml;
      final List<FontData> finalFonts = fontDataList;

      Future<?> future = executor.submit(() -> {
        try {
          generatePdf(finalHtml, finalFonts, finalTempFile); // Assuming this method exists
          return null;
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      });

      try {
        // Use the timeout provided in request, defaulting to 60 seconds if null
        long limit = (request.timeout != null && request.timeout > 0) ? request.timeout : 120000L;

        future.get(limit, TimeUnit.MILLISECONDS);

      } catch (TimeoutException e) {
        // CRITICAL: The PDF engine hung. Kill the task immediately.
        future.cancel(true);
        String msg = "TIMEOUT ERROR: PDF Generation timed out after " + request.timeout +
          "ms. The HTML structure may be too complex (nested tables, infinite loops).";
        LOG.error(msg);
        return new ConversionResult(msg);
      } catch (ExecutionException e) {
        String msg = "RENDERING ERROR: " + e.getCause().getMessage();
        LOG.error(msg, e);
        return new ConversionResult(msg);
      } catch (InterruptedException e) {
        String msg = "INTERRUPT ERROR: The conversion process was interrupted.";
        LOG.error(msg, e);
        Thread.currentThread().interrupt(); // Restore the interrupt status
        return new ConversionResult(msg);
      }

      // Step 4: Post-process the PDF (e.g., add page numbers)
      addPageNumbers(tempPdfFile);

      // Step 5: Upload the final PDF to Appian
      uploadPdfToAppian(tempPdfFile);

      // Return the newly created document ID
      return new ConversionResult(request.newDocumentCreated);

    } catch (Exception e) {
      LOG.error("Unexpected Error", e);
      return new ConversionResult("System Error: " + e.getMessage());
    } finally {
      if (executor != null) {
        executor.shutdownNow();
      }
      FileUtils.deleteQuietly(tempPdfFile);
      if (LOG.isEnabledFor(org.apache.log4j.Level.INFO)) {
        LOG.info("Temporary PDF file cleanup executed.");
      }
    }
  }

  // =================================================================================
  // == 2. CORE LOGIC METHODS
  // =================================================================================

  /**
   * Downloads, parses, and prepares the HTML for conversion.
   * This includes handling external resources and applying global styles.
   */
  private org.jsoup.nodes.Document prepareHtmlDom() throws Exception {
    com.appiancorp.suiteapi.knowledge.Document sourceDoc = request.cs.download(request.sourceDocument, ContentConstants.VERSION_CURRENT,
      false)[0];
    File htmlFile = sourceDoc.accessAsReadOnlyFile();
    org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(htmlFile, "UTF-8");

    // Handle unreachable external resources (images, stylesheets)
    handleUnreachableResources(jsoupDoc);

    // Update Appian image document IDs to local file paths
    ParameterValidation.setImageFilePath(jsoupDoc, request.cs);

    // Apply global styles based on smart service inputs
    applyGlobalStyles(jsoupDoc);
    if (LOG.isEnabledFor(org.apache.log4j.Level.INFO)) {
      LOG.info("HTML DOM prepared successfully.");
    }
    return jsoupDoc;
  }

  /**
   * Generates a PDF from a Jsoup document and returns a temporary File object.
   */
  private void generatePdf(org.jsoup.nodes.Document jsoupDoc, List<FontData> fontDataList, File tempPdfFile) throws Exception {
    if (jsoupDoc == null) {
      throw new IllegalArgumentException("The input HTML document cannot be null.");
    }
    if (tempPdfFile == null) {
      throw new IllegalArgumentException("The output PDF file cannot be null.");
    }

    XRLog.listRegisteredLoggers().forEach(logger -> XRLog.setLevel(logger, Level.WARNING));

    try (OutputStream outputStream = Files.newOutputStream(tempPdfFile.toPath())) {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      builder.useDefaultPageSize(request.targetDocumentWidth, request.targetDocumentHeight, PdfRendererBuilder.PageSizeUnits.MM);

      if (fontDataList != null && !fontDataList.isEmpty()) {
        for (FontData font : fontDataList) {
          try {
            String fontFamilyName = font.getName().replaceAll("\\..*$", "");
            builder.useFont(font.getFile(), fontFamilyName);
          } catch (Exception e) {
            if (LOG.isEnabledFor(org.apache.log4j.Level.WARN)) {
              LOG.warn("Could not load font '" + font.getName() + "'.", e);
            }
          }
        }
      }

      Document w3cDoc = new W3CDom().fromJsoup(jsoupDoc);
      builder.withW3cDocument(w3cDoc, ".");
      builder.toStream(outputStream);
      builder.run();
    }
    if (LOG.isEnabledFor(org.apache.log4j.Level.INFO)) {
      LOG.info("Temporary PDF file generated successfully.");
    }
  }

  private void addPageNumbers(File pdfFile) {
    // Only run if the feature is enabled in the request.
    if (!request.addPageNumbers) {
      if (LOG.isEnabledFor(org.apache.log4j.Level.INFO)) {
        LOG.info("Skipping page number addition as it is not enabled.");
      }
      return;
    }

    if (pdfFile == null || !pdfFile.exists()) {
      LOG.error("Cannot add page numbers: PDF file is null or does not exist");
      return;
    }

    // Load the existing PDF document within a try-with-resources block.
    try (PDDocument doc = PDDocument.load(pdfFile)) {
      int totalPages = doc.getNumberOfPages();

      for (int i = 0; i < totalPages; i++) {
        PDPage page = doc.getPage(i);
        try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
          // Prepare the page number text (e.g., "Page 1 of 10")
          String text = request.pageFormatText
            .replace("{current}", String.valueOf(i + 1))
            .replace("{total}", String.valueOf(totalPages));

          contentStream.beginText();
          contentStream.setFont(PDType1Font.HELVETICA, request.pageNumberFontSize);

          // Convert offsets from mm to PDF points (1mm ≈ 2.83 points)
          float offsetXInPoints = request.pageNumberOffsetX * 2.83f;
          float offsetYInPoints = request.pageNumberOffsetY * 2.83f;

          // Position the text based on the offsets.
          contentStream.newLineAtOffset(offsetXInPoints, offsetYInPoints);
          contentStream.showText(text);
          contentStream.endText();
        }
      }
      // Save the modified document back over the original temporary file.
      doc.save(pdfFile);
      if (LOG.isEnabledFor(org.apache.log4j.Level.INFO)) {
        LOG.info("Successfully added page numbers to the PDF.");
      }
    } catch (IOException e) {
      // Log the error but don't fail the entire smart service.
      // The PDF will still be created, just without page numbers.
      if (LOG.isEnabledFor(org.apache.log4j.Level.ERROR)) {
        LOG.error("Could not add page numbers to PDF due to an error. The process will continue without them.", e);
      }
    }
  }

  /**
   * Uploads the generated PDF file to the Appian Content Service.
   */
  private void uploadPdfToAppian(File pdfFile) throws Exception {
    com.appiancorp.suiteapi.knowledge.Document outputPdfDocument = new com.appiancorp.suiteapi.knowledge.Document();
    outputPdfDocument.setName(request.targetDocumentName.trim());
    if (request.targetDocumentDesc != null) {
      outputPdfDocument.setDescription(request.targetDocumentDesc.trim());
    }
    outputPdfDocument.setExtension("pdf");
    outputPdfDocument.setParent(request.targetFolder);

    request.newDocumentCreated = request.cs.create(outputPdfDocument, ContentConstants.UNIQUE_NONE);
    com.appiancorp.suiteapi.knowledge.Document finalDoc = request.cs.download(request.newDocumentCreated, ContentConstants.VERSION_CURRENT,
      false)[0];

    try (OutputStream outputStream = finalDoc.getOutputStream()) {
      Files.copy(pdfFile.toPath(), outputStream);
    }
    if (LOG.isEnabledFor(org.apache.log4j.Level.INFO)) {
      LOG.info("PDF successfully uploaded to Appian as document ID: " + request.newDocumentCreated);
    }
  }

  // =================================================================================
  // == 3. HELPER METHODS (Supporting Logic)
  // =================================================================================

  /**
   * Downloads custom font documents from Appian.
   */
  private List<FontData> loadFontData() throws Exception {
    // TODO: Implement font downloading logic here.
    if (LOG.isEnabledFor(org.apache.log4j.Level.INFO)) {
      LOG.info("Skipping custom font loading (not yet implemented).");
    }
    return new ArrayList<>();
  }

  /**
   * Iterates through external resources and removes unreachable ones.
   */
  private void handleUnreachableResources(org.jsoup.nodes.Document jsoupDoc) {
    for (Element img : jsoupDoc.select("img[src]")) {
      String src = img.attr("abs:src");
      if (isExternalUrl(src) && isUriUnreachable(src)) {
        if (LOG.isEnabledFor(org.apache.log4j.Level.WARN)) {
          LOG.warn("Removing unreachable image source: " + src);
        }
        img.removeAttr("src");
      }
    }
    for (Element link : jsoupDoc.select("link[href][rel=stylesheet]")) {
      String href = link.attr("abs:href");
      if (isExternalUrl(href) && isUriUnreachable(href)) {
        if (LOG.isEnabledFor(org.apache.log4j.Level.WARN)) {
          LOG.warn("Removing unreachable stylesheet link: " + href);
        }
        link.remove();
      }
    }
  }

  /**
   * Injects global CSS styles into the HTML head based on request parameters.
   */
  private void applyGlobalStyles(org.jsoup.nodes.Document jsoupDoc) {
    Element head = jsoupDoc.head();
    if (head.select("style").isEmpty()) {
      head.prepend("<style></style>");
    }

    StringBuilder styles = new StringBuilder();

    if (request.simplifyFonts) {
      styles.append("* { font-family: Arial, sans-serif !important; ");
    } else {
      // When simplifyFonts is false, we rely on the fonts loaded in generatePdf
      // and the existing CSS in the document. We can add a fallback here if desired.
      styles.append("* { ");
    }

    if (request.wrapText) {
      styles.append("word-wrap: break-word; ");
    }
    styles.append("}");

    if (request.handleWideTables) {
      styles.append("@page {\n" +
        "  margin: 24px 10px;\n" +
        "  -fs-max-overflow-pages: 10; /* 0 by default */\n" +
        "  -fs-overflow-pages-direction: ltr; /* Also available is rtl */\n" +
        "  \n" +
        "  @top-left {\n" +
        "    /* Note the use of the -fs-if-cut-off function below. */\n" +
        "    content: \"Page \" counter(page) -fs-if-cut-off(\" (continued)\") \" of \" counter(pages);\n" +
        "  }\n" +
        "}");
    }

    if (request.handleWideFooters) {
      styles.append(
        "[id*='footer'], [class*='footer'] { " +
          "width: 100% !important; " + /* Force the container to fit the page */
          "word-wrap: break-word; " + /* Force long text inside to wrap */
          "} ");
    }

    Element styleTag = head.selectFirst("style");

    if (styleTag != null) {
      styleTag.prepend(styles.toString());
    }
  }

  /**
   * Checks if a URL is external.
   */
  private boolean isExternalUrl(String url) {
    return url != null && (url.startsWith("http://") || url.startsWith("https://"));
  }

  /**
   * Checks if a URL is unreachable, using a cache to avoid redundant checks.
   */
  private boolean isUriUnreachable(String urlString) {
    if (uriReachableCache.containsKey(urlString)) {
      return !uriReachableCache.get(urlString); // Return inverse of reachability
    }
    boolean isReachable = false;
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) new URL(urlString).openConnection();
      connection.setRequestMethod("HEAD");
      connection.setConnectTimeout(3000);
      connection.setReadTimeout(3000);
      isReachable = (connection.getResponseCode() == HttpURLConnection.HTTP_OK);
    } catch (Exception e) {
      if (LOG.isEnabledFor(org.apache.log4j.Level.WARN)) {
        LOG.warn("Resource at URL is not reachable: " + urlString);
      }
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }
    uriReachableCache.put(urlString, isReachable);
    return !isReachable;
  }

  // =================================================================================
  // == 4. PRIVATE NESTED CLASSES
  // =================================================================================

  private static class FontData {
    private final String name;
    private final File file;

    public FontData(String name, File file) {
      this.name = name;
      this.file = file;
    }

    public String getName() {
      return name;
    }

    public File getFile() {
      return file;
    }
  }
}