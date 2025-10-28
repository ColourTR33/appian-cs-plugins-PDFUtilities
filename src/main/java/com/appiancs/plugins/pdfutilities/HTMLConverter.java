package com.appiancs.plugins.pdfutilities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
import com.appiancorp.suiteapi.process.framework.AppianSmartService;
import com.appiancorp.suiteapi.process.framework.Input;
import com.appiancorp.suiteapi.process.framework.Order;
import com.appiancorp.suiteapi.process.framework.Required;
import com.appiancorp.suiteapi.process.framework.Unattended;
import com.appiancorp.suiteapi.process.palette.AutomationSmartServicesDocumentGeneration;
import com.appiancs.plugins.pdfutilities.dto.ConversionRequest;
import com.appiancs.plugins.pdfutilities.service.HtmlToPdfConverter;
import com.appiancs.plugins.pdfutilities.util.ParameterValidation;

@AutomationSmartServicesDocumentGeneration
@Unattended
@Order({ "SourceDocument", "TargetDocumentName", "TargetDocumentDesc", "TargetFolder", "TargetDocumentWidth", "TargetDocumentHeight",
  "TargetDocumentTopMargin", "TargetDocumentBottomMargin", "TargetDocumentLeftMargin", "TargetDocumentRightMargin", "SimplifyFonts",
  "WrapText", "HandleWideTables", "HandleWideFooters" })
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class HTMLConverter extends AppianSmartService {
  private static final Logger LOG = Logger.getLogger(HTMLConverter.class);

  // Inputs
  private Long sourceDocument;
  private String targetDocumentName;
  private String targetDocumentDesc;
  private Long targetFolderPicker;
  private Integer targetDocumentWidth;
  private Integer targetDocumentHeight;
  private Integer targetDocumentTopMargin;
  private Integer targetDocumentBottomMargin;
  private Integer targetDocumentLeftMargin;
  private Integer targetDocumentRightMargin;
  private boolean simplifyFonts;
  private boolean wrapText;
  private boolean handleWideTables;
  private boolean handleWideFooters;

  // Outputs
  private Long targetDocumentCreated;
  private boolean errorOccurred;
  private String errorMessage;

  // Internal
  private ContentService cs;
  // private static final Float ONE_MM_IN_PX = 3.7795275591F;
  private static final String DEFAULT_WIDTH = "210";
  private static final String DEFAULT_HEIGHT = "297";
  private static final String DEFAULT_TOP_MARGIN = "35";
  private static final String DEFAULT_BOTTOM_MARGIN = "35";
  private static final String DEFAULT_LEFT_MARGIN = "35";
  private static final String DEFAULT_RIGHT_MARGIN = "35";
  private static final String DEFAULT_SIMPLIFY_FONTS = "true";
  private static final String DEFAULT_WRAP_TEXT = "true";
  private static final String DEFAULT_WIDE_FOOTER = "true";
  private static final String DEFAULT_WIDE_TABLE = "true";
  private static final String NAME_TARGET_WIDTH = "TargetDocumentWidth";
  private static final String NAME_TARGET_HEIGHT = "TargetDocumentHeight";
  private static final String NAME_TARGET_FOLDER_PICKER = "TargetFolderPicker";
  private static final String NAME_SOURCE_DOCUMENT = "SourceDocument";
  private static final String NAME_TARGET_DOCUMENT_NAME = "TargetDocumentName";
  private static final String NAME_TARGET_DOCUMENT_DESC = "TargetDocumentDesc";
  private static final String NAME_TARGET_DOCUMENT_TOP_MARGIN = "TargetDocumentTopMargin";
  private static final String NAME_TARGET_DOCUMENT_BOTTOM_MARGIN = "TargetDocumentBottomMargin";
  private static final String NAME_TARGET_DOCUMENT_LEFT_MARGIN = "TargetDocumentLeftMargin";
  private static final String NAME_TARGET_DOCUMENT_RIGHT_MARGIN = "TargetDocumentRightMargin";

  public HTMLConverter(ContentService cs) {
    super();
    this.cs = cs;
  }

  @Override
  public void run() throws SmartServiceException {
    Document validSourceDoc;
    try {
      ParameterValidation.checkNotNullOrBlank(targetDocumentName, "TargetDocumentName");
      ParameterValidation.checkIsPositive(targetFolderPicker, "TargetFolder");
      ParameterValidation.checkInRange(targetDocumentWidth, 1, 850, "TargetDocumentWidth");
      ParameterValidation.checkInRange(targetDocumentHeight, 1, 850, "TargetDocumentHeight");
      ParameterValidation.checkInRange(targetDocumentTopMargin, 1, 150, "TargetDocumentTopMargin");
      ParameterValidation.checkInRange(targetDocumentBottomMargin, 1, 150, "TargetDocumentBottomMargin");
      ParameterValidation.checkInRange(targetDocumentLeftMargin, 1, 150, "TargetDocumentLeftMargin");
      ParameterValidation.checkInRange(targetDocumentRightMargin, 1, 150, "TargetDocumentRightMargin");

      validSourceDoc = ParameterValidation.validateAndGetDocument(sourceDocument, cs, "html", "SourceDocument");

      ConversionRequest request = new ConversionRequest();

      request.cs = this.cs;
      request.sourceDocument = this.sourceDocument;
      request.targetDocumentBottomMargin = this.targetDocumentBottomMargin;
      request.targetDocumentTopMargin = this.targetDocumentTopMargin;
      request.targetDocumentLeftMargin = this.targetDocumentLeftMargin;
      request.targetDocumentRightMargin = this.targetDocumentRightMargin;
      request.targetDocumentDesc = this.targetDocumentDesc;
      request.targetDocumentWidth = this.targetDocumentWidth;
      request.targetDocumentHeight = this.targetDocumentHeight;
      request.targetFolder = this.targetFolderPicker;
      request.targetDocumentName = this.targetDocumentName;
      request.simplifyFonts = this.simplifyFonts;
      request.wrapText = this.wrapText;
      request.handleWideFooters = this.handleWideFooters;
      request.handleWideTables = this.handleWideTables;

      if (LOG.isEnabledFor(Level.INFO)) {
        LOG.info("All parameters and content validated. Starting conversion for document: " + validSourceDoc.getExternalFilename());
      }
      HtmlToPdfConverter converter = new HtmlToPdfConverter(request);
      converter.executeConversion();

    } catch (IllegalArgumentException e) {
      // Any validation failure from our utility class is caught here.
      if (LOG.isEnabledFor(Level.ERROR)) {
        LOG.error("Validation failed for HTMLConverter: " + e.getMessage());
      }
      this.errorOccurred = true;
      this.errorMessage = e.getMessage(); // Use the clear, user-friendly message.
    } catch (Exception e) {
      // Catch any other unexpected errors during processing.
      if (LOG.isEnabledFor(Level.ERROR)) {
        LOG.error("An unexpected error occurred in HTMLConverter.", e);
      }
      this.errorOccurred = true;
      this.errorMessage = "An unexpected error occurred: " + e.getMessage();
    }
  }

  @Input(required = Required.ALWAYS)
  @DocumentDataType
  @Name(NAME_SOURCE_DOCUMENT)
  public void setSourceDocument(Long source) {
    this.sourceDocument = source;
  }

  @Input(required = Required.ALWAYS)
  @Name(NAME_TARGET_DOCUMENT_NAME)
  public void setTargetDocumentName(String name) {
    this.targetDocumentName = name;
  }

  @Input(required = Required.OPTIONAL)
  @Name(NAME_TARGET_DOCUMENT_DESC)
  public void setTargetDocumentDesc(String desc) {
    this.targetDocumentDesc = desc;
  }

  @Input(required = Required.ALWAYS)
  @FolderDataType
  @Name(NAME_TARGET_FOLDER_PICKER)
  public void setTargetFolderPicker(Long folder) {
    this.targetFolderPicker = folder;
  }

  @Input(required = Required.ALWAYS, defaultValue = DEFAULT_WIDTH)
  @Name(NAME_TARGET_WIDTH)
  public void setTargetDocumentWidth(Integer width) {
    this.targetDocumentWidth = width;
  }

  @Input(required = Required.ALWAYS, defaultValue = DEFAULT_HEIGHT)
  @Name(NAME_TARGET_HEIGHT)
  public void setTargetDocumentHeight(Integer height) {
    this.targetDocumentHeight = height;
  }

  @Input(required = Required.ALWAYS, defaultValue = DEFAULT_TOP_MARGIN)
  @Name(NAME_TARGET_DOCUMENT_TOP_MARGIN)
  public void setTargetDocumentTopMargin(Integer margin) {
    this.targetDocumentTopMargin = margin;
  }

  @Input(required = Required.ALWAYS, defaultValue = DEFAULT_BOTTOM_MARGIN)
  @Name(NAME_TARGET_DOCUMENT_BOTTOM_MARGIN)
  public void setTargetDocumentBottomMargin(Integer margin) {
    this.targetDocumentBottomMargin = margin;
  }

  @Input(required = Required.ALWAYS, defaultValue = DEFAULT_LEFT_MARGIN)
  @Name(NAME_TARGET_DOCUMENT_LEFT_MARGIN)
  public void setTargetDocumentLeftMargin(Integer margin) {
    this.targetDocumentLeftMargin = margin;
  }

  @Input(required = Required.ALWAYS, defaultValue = DEFAULT_RIGHT_MARGIN)
  @Name(NAME_TARGET_DOCUMENT_RIGHT_MARGIN)
  public void setTargetDocumentRightMargin(Integer margin) {
    this.targetDocumentRightMargin = margin;
  }

  @Input(required = Required.ALWAYS, defaultValue = DEFAULT_SIMPLIFY_FONTS)
  @Name("SimplifyFonts")
  public void setSimplifyFonts(Boolean simplify) {
    this.simplifyFonts = simplify;
  }

  @Input(required = Required.ALWAYS, defaultValue = DEFAULT_WRAP_TEXT)
  @Name("WrapText")
  public void setWrapText(Boolean wrap) {
    this.wrapText = wrap;
  }

  @Input(required = Required.OPTIONAL, defaultValue = DEFAULT_WIDE_TABLE)
  @Name("WideTable")
  public void setWideTable(Boolean wideTable) {
    this.handleWideTables = wideTable;
  }

  @Input(required = Required.OPTIONAL, defaultValue = DEFAULT_WIDE_FOOTER)
  @Name("WideFooter")
  public void setWideFooter(Boolean wideFooter) {
    this.handleWideFooters = wideFooter;
  }

  @Name("NewDocumentCreated")
  @DocumentDataType
  public Long getNewDocumentCreated() {
    return targetDocumentCreated;
  }

  @Name("errorOccurred")
  public boolean getErrorOccurred() {
    return errorOccurred;
  }

  @Name("errorMessage")
  public String getErrorMessage() {
    return errorMessage;
  }
}
