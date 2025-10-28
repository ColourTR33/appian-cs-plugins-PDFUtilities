package com.appiancs.plugins.pdfutilities.dto;

import java.util.List;

import com.appiancorp.suiteapi.content.ContentService;

/**
 * A Data Transfer Object (DTO) that holds all the parameters needed
 * for an HTML to PDF conversion operation.
 */
public class ConversionRequest {

  // Service Dependencies
  public ContentService cs;

  // -------------------
  // INPUT PARAMETERS
  // -------------------
  /** The source HTML document to be converted. */
  public Long sourceDocument;

  /** The desired name for the new PDF document. */
  public String targetDocumentName;

  /** The optional description for the new PDF document. */
  public String targetDocumentDesc;

  /** The folder where the new PDF document will be saved. */
  public Long targetFolder;

  /** The width of the target PDF in millimeters. */
  public Integer targetDocumentWidth;

  /** The height of the target PDF in millimeters. */
  public Integer targetDocumentHeight;

  /** The top margin of the target PDF in millimeters. */
  public Integer targetDocumentTopMargin;

  /** The bottom margin of the target PDF in millimeters. */
  public Integer targetDocumentBottomMargin;

  /** The left margin of the target PDF in millimeters. */
  public Integer targetDocumentLeftMargin;

  /** The right margin of the target PDF in millimeters. */
  public Integer targetDocumentRightMargin;

  /** If true, forces all fonts to a standard sans-serif font. */
  public boolean simplifyFonts;

  /** A list of Appian document IDs for custom fonts to be embedded. */
  public List<Long> fontDocuments;

  /** If true, adds a CSS rule to wrap long text. */
  public boolean wrapText;

  // --- PAGE NUMBER PARAMETERS ---
  /** If true, page numbers will be added to the PDF. */
  public boolean addPageNumbers;

  /** The font size for the page numbers. */
  public Integer pageNumberFontSize;

  /** The text format for the page numbers. Use {current} and {total}. */
  public String pageFormatText;

  /** Horizontal offset (from left) for page number placement in mm. */
  public Integer pageNumberOffsetX;

  /** Vertical offset (from bottom) for page number placement in mm. */
  public Integer pageNumberOffsetY;

  /** If true, applies CSS to help wide tables wrap within the page. */
  public boolean handleWideTables;

  /** If true, applies CSS to help wide footers wrap within the page. */
  public boolean handleWideFooters;

  // Output Field
  public Long newDocumentCreated;
}