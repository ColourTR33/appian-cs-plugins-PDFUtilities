# PDF Utilities
PDF conversion utilities

## HTML to PDF Smart Service
Converts an HTML formatted document to PDF with a wide range of configuration options. This smart service is ideal for generating dynamic, print-ready documents from HTML source files within Appian.

### Data Tab
| Input | Data Type | Required | Multiple | Description |
| :--- |:---:|:---:|:---:|:--- |
| **Source Document** | Document | Yes | No | The source HTML document to be converted. Must have a `.html` or `.htm` extension. |
| **Target Document Name** | Text | Yes | No | The name for the newly created PDF document (e.g., "My Generated Report.pdf"). |
| **Target Document Desc** | Text | No | No | An optional description for the new PDF document. |
| **Target Folder** | Folder | Yes | No | The Appian folder where the new PDF document will be saved. Provides a folder picker. |
| **Target Document Width** | Integer | Yes | No | The width of the PDF page in millimeters (mm). Defaults to 210 (A4 width). |
| **Target Document Height** | Integer | Yes | No | The height of the PDF page in millimeters (mm). Defaults to 297 (A4 height). |
| **Target Document Top Margin** | Integer | Yes | No | The top margin of the PDF page in millimeters (mm). Defaults to 35. |
| **Target Document Bottom Margin**| Integer | Yes | No | The bottom margin of the PDF page in millimeters (mm). Defaults to 35. |
| **Target Document Left Margin** | Integer | Yes | No | The left margin of the PDF page in millimeters (mm). Defaults to 35. |
| **Target Document Right Margin**| Integer | Yes | No | The right margin of the PDF page in millimeters (mm). Defaults to 35. |
| **Simplify Fonts** | Boolean | Yes | No | If `true`, all fonts are overridden with `Arial, sans-serif`. If `false`, fonts from the HTML and custom fonts are used. Defaults to `true`. |
| **Wrap Text** | Boolean | Yes | No | If `true`, a global CSS rule (`word-wrap: break-word;`) is applied to prevent text from overflowing the page. Defaults to `true`. |
| **Handle Wide Tables** | Boolean | No | No | If `true`, injects special CSS to handle content wider than the page by printing it on subsequent pages. Defaults to `false`. |
| **Handle Wide Footers**| Boolean | No | No | If `true`, injects CSS to force common footer elements to fit the page width and wrap their content. Defaults to `false`. |
| **Add Page Numbers** | Boolean | Yes | No | If `true`, page numbers will be added to each page. Defaults to `false`. |
| **Page Number Font Size** | Integer | No | No | The font size for page numbers. Only used if `Add Page Numbers` is `true`. Defaults to 10. |
| **Page Format Text** | Text | No | No | The text format for page numbers. Use `{current}` for the current page and `{total}` for the total count. Defaults to "Page {current} of {total}". |
| **Page Number Offset X** | Integer | No | No | The horizontal position of the page number in millimeters (mm) from the left edge. Defaults to 100. |
| **Page Number Offset Y** | Integer | No | No | The vertical position of the page number in millimeters (mm) from the bottom edge. Defaults to 10. |
| **Custom Font Documents** | Document | No | Yes | A list of font documents (e.g., `.ttf`, `.otf`) to use in the PDF. Only used if `Simplify Fonts` is `false`. |

| Output | Data Type | Multiple | Description |
| :--- |:---:|:---:|:--- |
| **New Document Created** | Document | No | The newly generated PDF document. |
| **errorOccurred** | Boolean | No | Returns `true` if an error occurred during the conversion; otherwise, `false`. |
| **errorMessage** | Text | No | Provides a descriptive message if an error occurred. |
