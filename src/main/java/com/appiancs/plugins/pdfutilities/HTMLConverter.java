package com.appiancs.plugins.pdfutilities;

import com.appiancorp.suiteapi.common.Name;
import com.appiancorp.suiteapi.content.ContentService;
import com.appiancorp.suiteapi.knowledge.Document;
import com.appiancorp.suiteapi.knowledge.DocumentDataType;
import com.appiancorp.suiteapi.knowledge.FolderDataType;
import com.appiancorp.suiteapi.process.exceptions.SmartServiceException;
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
@Order({
  "SourceDocument",
  "TargetDocumentName",
  "TargetDocumentDesc",
  "TargetFolder",
  "TargetDocumentWidth",
  "TargetDocumentHeight",
  "TargetDocumentTopMargin",
  "TargetDocumentBottomMargin",
  "TargetDocumentLeftMargin",
  "TargetDocumentRightMargin",
  "SimplifyFonts",
  "WrapText",
  "HandleWideTables",
  "HandleWideFooters",
  "AddPageNumbers",
  "PageFormatText",
  "PageNumberFontSize",
  "PageNumberOffsetX",
  "PageNumberOffsetY"
})
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class HTMLConverter extends BaseSmartService {
  private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(HTMLConverter.class);
  // Inputs
  private Long sourceDocument;
  // Target Inputs
  private String targetDocumentName;
  private String targetDocumentDesc;
  private Long targetFolder;
  private Integer targetDocumentWidth;
  private Integer targetDocumentHeight;
  private Integer targetDocumentTopMargin;
  private Integer targetDocumentBottomMargin;
  private Integer targetDocumentLeftMargin;
  private Integer targetDocumentRightMargin;
  // Formatting Option Inputs
  private boolean simplifyFonts;
  private boolean wrapText;
  private boolean handleWideTables;
  private boolean handleWideFooters;
  // Page Numbering Inputs
  private Boolean addPageNumbers;
  private String pageFormatText;
  private Integer pageNumberFontSize;
  private Integer pageNumberOffsetX;
  private Integer pageNumberOffsetY;

  // Outputs
  private Long targetDocumentCreated;

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
  private static final String DEFAULT_ADD_PAGE_NUMBERS = "false";
  private static final String DEFAULT_PAGE_FORMAT_TEXT = "Page {current} of {total}";
  private static final String DEFAULT_PAGE_NUMBER_FONT_SIZE = "10";
  private static final String DEFAULT_PAGE_NUMBER_OFFSET_X = "20";
  private static final String DEFAULT_PAGE_NUMBER_OFFSET_Y = "20";

  private static final String NAME_TARGET_WIDTH = "TargetDocumentWidth";
  private static final String NAME_TARGET_HEIGHT = "TargetDocumentHeight";
  private static final String NAME_TARGET_FOLDER = "TargetFolder";
  private static final String NAME_SOURCE_DOCUMENT = "SourceDocument";
  private static final String NAME_TARGET_DOCUMENT_NAME = "TargetDocumentName";
  private static final String NAME_TARGET_DOCUMENT_DESC = "TargetDocumentDesc";
  private static final String NAME_TARGET_DOCUMENT_TOP_MARGIN = "TargetDocumentTopMargin";
  private static final String NAME_TARGET_DOCUMENT_BOTTOM_MARGIN = "TargetDocumentBottomMargin";
  private static final String NAME_TARGET_DOCUMENT_LEFT_MARGIN = "TargetDocumentLeftMargin";
  private static final String NAME_TARGET_DOCUMENT_RIGHT_MARGIN = "TargetDocumentRightMargin";
  private static final String NAME_SIMPLIFY_FONTS = "SimplifyFonts";
  private static final String NAME_WRAP_TEXT = "WrapText";
  private static final String NAME_HANDLE_WIDE_TABLES = "HandleWideTables";
  private static final String NAME_HANDLE_WIDE_FOOTERS = "HandleWideFooters";
  private static final String NAME_NEW_DOCUMENT_CREATED = "NewDocumentCreated";
  private static final String NAME_ADD_PAGE_NUMBERS = "AddPageNumbers";
  private static final String NAME_PAGE_FORMAT_TEXT = "PageFormatText";
  private static final String NAME_PAGE_NUMBER_FONT_SIZE = "PageNumberFontSize";
  private static final String NAME_PAGE_NUMBER_OFFSET_X = "PageNumberOffsetX";
  private static final String NAME_PAGE_NUMBER_OFFSET_Y = "PageNumberOffsetY";

  public HTMLConverter(ContentService cs) {
    super(cs, LOG);
  }

  @Override
  public void run() throws SmartServiceException {
    Document validSourceDoc;
    try {
      ParameterValidation.checkNotNullOrBlank(targetDocumentName, "TargetDocumentName");
      ParameterValidation.checkIsPositive(targetFolder, "TargetFolder");
      ParameterValidation.checkInRange(targetDocumentWidth, 1, 850, "TargetDocumentWidth");
      ParameterValidation.checkInRange(targetDocumentHeight, 1, 850, "TargetDocumentHeight");
      ParameterValidation.checkInRange(targetDocumentTopMargin, 1, 150, "TargetDocumentTopMargin");
      ParameterValidation.checkInRange(targetDocumentBottomMargin, 1, 150, "TargetDocumentBottomMargin");
      ParameterValidation.checkInRange(targetDocumentLeftMargin, 1, 150, "TargetDocumentLeftMargin");
      ParameterValidation.checkInRange(targetDocumentRightMargin, 1, 150, "TargetDocumentRightMargin");

      if (addPageNumbers != null && addPageNumbers) {
        ParameterValidation.checkNotNullOrBlank(pageFormatText, "PageFormatText");
        ParameterValidation.checkInRange(pageNumberFontSize, 6, 16, "PageNumberFontSize");
        ParameterValidation.checkInRange(pageNumberOffsetX, 0, 1000, "PageNumberOffsetX");
        ParameterValidation.checkInRange(pageNumberOffsetY, 0, 1000, "PageNumberOffsetY");
      }

      validSourceDoc = ParameterValidation.validateAndGetDocument(sourceDocument, cs, "html", "SourceDocument");

      ConversionRequest request = new ConversionRequest();
      // Service
      request.cs = super.cs;

      // Source & Target
      request.sourceDocument = sourceDocument;
      request.targetFolder = targetFolder;
      request.targetDocumentName = targetDocumentName;
      request.targetDocumentDesc = targetDocumentDesc;

      // Page dimensions
      request.targetDocumentBottomMargin = targetDocumentBottomMargin;
      request.targetDocumentTopMargin = targetDocumentTopMargin;
      request.targetDocumentLeftMargin = targetDocumentLeftMargin;
      request.targetDocumentRightMargin = targetDocumentRightMargin;
      request.targetDocumentWidth = targetDocumentWidth;
      request.targetDocumentHeight = targetDocumentHeight;

      // Formatting options
      request.simplifyFonts = simplifyFonts;
      request.wrapText = wrapText;
      request.handleWideFooters = handleWideFooters;
      request.handleWideTables = handleWideTables;

      // Page numbering
      request.addPageNumbers = addPageNumbers != null ? this.addPageNumbers : false;
      request.pageFormatText = pageFormatText != null ? this.pageFormatText : DEFAULT_PAGE_FORMAT_TEXT;
      request.pageNumberFontSize = pageNumberFontSize != null ? this.pageNumberFontSize : Integer.parseInt(DEFAULT_PAGE_NUMBER_FONT_SIZE);
      request.pageNumberOffsetX = pageNumberOffsetX != null ? this.pageNumberOffsetX : Integer.parseInt(DEFAULT_PAGE_NUMBER_OFFSET_X);
      request.pageNumberOffsetY = pageNumberOffsetY != null ? this.pageNumberOffsetY : Integer.parseInt(DEFAULT_PAGE_NUMBER_OFFSET_Y);

      if (LOG.isInfoEnabled()) {
        LOG.info("All parameters and content validated. Starting conversion for document: " + validSourceDoc.getExternalFilename());
      }

      HtmlToPdfConverter converter = new HtmlToPdfConverter(request);
      this.targetDocumentCreated = converter.executeConversion();

      if (this.targetDocumentCreated == null) {
        throw new IllegalStateException("PDF conversion completed but document ID was not created");
      }
    } catch (IllegalStateException e) {
      handleException(e, "Document creation failed in HTMLConverter");
    } catch (IllegalArgumentException e) {
      handleException(e, "Validation failed for HTMLConverter");
    } catch (Exception e) {
      handleException(e, "An unexpected error occurred in HTMLConverter.");
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
  @Name(NAME_TARGET_FOLDER)
  public void setTargetFolder(Long folder) {
    this.targetFolder = folder;
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
  @Name(NAME_SIMPLIFY_FONTS)
  public void setSimplifyFonts(Boolean simplify) {
    this.simplifyFonts = simplify;
  }

  @Input(required = Required.ALWAYS, defaultValue = DEFAULT_WRAP_TEXT)
  @Name(NAME_WRAP_TEXT)
  public void setWrapText(Boolean wrap) {
    this.wrapText = wrap;
  }

  @Input(required = Required.OPTIONAL, defaultValue = DEFAULT_WIDE_TABLE)
  @Name(NAME_HANDLE_WIDE_TABLES)
  public void setWideTable(Boolean wideTable) {
    this.handleWideTables = wideTable;
  }

  @Input(required = Required.OPTIONAL, defaultValue = DEFAULT_WIDE_FOOTER)
  @Name(NAME_HANDLE_WIDE_FOOTERS)
  public void setWideFooter(Boolean wideFooter) {
    this.handleWideFooters = wideFooter;
  }

  @Input(required = Required.OPTIONAL, defaultValue = DEFAULT_ADD_PAGE_NUMBERS)
  @Name(NAME_ADD_PAGE_NUMBERS)
  public void setAddPageNumbers(Boolean addPageNumbers) {
    this.addPageNumbers = addPageNumbers;
  }

  @Input(required = Required.OPTIONAL, defaultValue = DEFAULT_PAGE_FORMAT_TEXT)
  @Name(NAME_PAGE_FORMAT_TEXT)
  public void setPageFormatText(String pageFormatText) {
    this.pageFormatText = pageFormatText;
  }

  @Input(required = Required.OPTIONAL, defaultValue = DEFAULT_PAGE_NUMBER_FONT_SIZE)
  @Name(NAME_PAGE_NUMBER_FONT_SIZE)
  public void setPageNumberFontSize(Integer pageNumberFontSize) {
    this.pageNumberFontSize = pageNumberFontSize;
  }

  @Input(required = Required.OPTIONAL, defaultValue = DEFAULT_PAGE_NUMBER_OFFSET_X)
  @Name(NAME_PAGE_NUMBER_OFFSET_X)
  public void setPageNumberOffsetX(Integer pageNumberOffsetX) {
    this.pageNumberOffsetX = pageNumberOffsetX;
  }

  @Input(required = Required.OPTIONAL, defaultValue = DEFAULT_PAGE_NUMBER_OFFSET_Y)
  @Name(NAME_PAGE_NUMBER_OFFSET_Y)
  public void setPageNumberOffsetY(Integer pageNumberOffsetY) {
    this.pageNumberOffsetY = pageNumberOffsetY;
  }

  @Name(NAME_NEW_DOCUMENT_CREATED)
  @DocumentDataType
  public Long getNewDocumentCreated() {
    return targetDocumentCreated;
  }

}
