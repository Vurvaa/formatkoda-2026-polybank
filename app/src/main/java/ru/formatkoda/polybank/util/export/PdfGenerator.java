package ru.formatkoda.polybank.util.export;

import lombok.extern.slf4j.Slf4j;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.exception.ExportException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
public class PdfGenerator {

	private static final String[] TRANSACTIONS_HEADER = {"ID", "From", "To", "Amount", "Type", "Status", "Created at"};

	private static final String APPROVED_TEXT = """
			Заверено советом директоров компании Polybank
			
			Денисов-Элерс Э.Ф.
			Николаев А.Д.
			Парфенов Я.М.
			Вырва Е.В.
			""";
	private static final String STAMP_PATH = "/export/stamp.png";
	private static final String FONT_PATH = "/export/font.ttf";

	private PdfGenerator() {
	}

	public static ByteArrayInputStream toPdf(List<TransactionWithAccountNumbersView> transactions) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Document document = new Document(PageSize.A4.rotate());

			PdfWriter.getInstance(document, out);
			document.open();

			addTitle(document);
			addTransactionsTable(document, transactions);
			addSignature(document);

			document.close();

			return new ByteArrayInputStream(out.toByteArray());
		} catch (RuntimeException e) {
			log.error("error generating pdf", e);
			throw new ExportException("failed to save data to pdf: " + e.getMessage());
		} catch (Exception e) {
			log.error("error closing pdf output stream", e);
			throw new ExportException("failed to save data to pdf: " + e.getMessage());
		}
	}

	private static void addTitle(Document document) throws DocumentException {
		Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
		Paragraph title = new Paragraph("Account transactions report", titleFont);

		title.setAlignment(Element.ALIGN_CENTER);
		title.setSpacingAfter(20);

		document.add(title);
	}

	private static void addTransactionsTable(
			Document document,
			List<TransactionWithAccountNumbersView> transactions
	) throws DocumentException {
		PdfPTable table = new PdfPTable(TRANSACTIONS_HEADER.length);

		table.setWidthPercentage(100);
		table.setWidths(new float[]{1.2f, 2.4f, 2.4f, 1.8f, 1.6f, 1.6f, 2.8f});
		table.setHeaderRows(1);

		addHeaders(table);

		for (TransactionWithAccountNumbersView transaction : transactions)
			addTransactionRow(table, transaction);

		document.add(table);
	}

	private static void addHeaders(PdfPTable table) {
		Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

		for (String header : TRANSACTIONS_HEADER) {
			PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));

			cell.setHorizontalAlignment(Element.ALIGN_CENTER);
			cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
			cell.setPadding(6);

			table.addCell(cell);
		}
	}

	private static void addTransactionRow(PdfPTable table, TransactionWithAccountNumbersView transaction) {
		Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);

		addCell(table, String.valueOf(transaction.id()), cellFont);
		addCell(table, transaction.fromAccountNumber() == null
				? "-" : transaction.fromAccountNumber().value(), cellFont);
		addCell(table, transaction.toAccountNumber() == null ? "-" : transaction.toAccountNumber().value(), cellFont);
		addCell(table, transaction.amount().toPlainString(), cellFont);
		addCell(table, transaction.type().name(), cellFont);
		addCell(table, transaction.status().name(), cellFont);
		addCell(table, transaction.createdAt().format(DateTimeFormatter.ISO_DATE_TIME), cellFont);
	}

	private static void addCell(PdfPTable table, String value, Font font) {
		PdfPCell cell = new PdfPCell(new Phrase(value, font));

		cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		cell.setPadding(5);

		table.addCell(cell);
	}

	private static void addSignature(Document document) throws DocumentException, IOException {
		Image stamp = loadStamp();

		PdfPTable signatureTable = new PdfPTable(3);

		signatureTable.setWidthPercentage(100);
		signatureTable.setWidths(new float[]{3f, 4f, 1.5f});
		signatureTable.setSpacingBefore(25);

		PdfPCell spacerCell = new PdfPCell();
		spacerCell.setBorder(Rectangle.NO_BORDER);

		PdfPCell textCell = new PdfPCell(new Phrase(APPROVED_TEXT +
				"\n" + OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE),
				createRegularFont()));

		textCell.setBorder(Rectangle.NO_BORDER);
		textCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
		textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		textCell.setPaddingBottom(5);
		textCell.setPaddingRight(10);

		PdfPCell imageCell = new PdfPCell();

		imageCell.addElement(stamp);
		imageCell.setBorder(Rectangle.NO_BORDER);
		imageCell.setBackgroundColor(Color.WHITE);
		imageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
		imageCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
		imageCell.setPadding(0);

		signatureTable.addCell(spacerCell);
		signatureTable.addCell(textCell);
		signatureTable.addCell(imageCell);

		document.add(signatureTable);
	}

	private static Image loadStamp() throws IOException {
		try (InputStream input = PdfGenerator.class.getResourceAsStream(STAMP_PATH)) {
			if (input == null)
				throw new ExportException("stamp image not found");

			BufferedImage source = ImageIO.read(input);

			if (source == null)
				throw new ExportException("failed to read stamp image");

			BufferedImage rgbImage = new BufferedImage(
					source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);

			Graphics2D graphics = rgbImage.createGraphics();

			try {
				graphics.setColor(Color.WHITE);
				graphics.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
				graphics.drawImage(source, 0, 0, null);
			} finally {
				graphics.dispose();
			}

			ByteArrayOutputStream output = new ByteArrayOutputStream();
			ImageIO.write(rgbImage, "png", output);

			return Image.getInstance(output.toByteArray());
		}
	}

	private static Font createRegularFont() {
		try (InputStream input = PdfGenerator.class.getResourceAsStream(FONT_PATH)) {
			if (input == null)
				throw new ExportException("font not found");

			BaseFont baseFont = BaseFont.createFont(
					"font.ttf",
					BaseFont.IDENTITY_H,
					BaseFont.EMBEDDED,
					true,
					input.readAllBytes(),
					null
			);

			return new Font(baseFont, 10);
		} catch (IOException | DocumentException e) {
			throw new ExportException("failed to load pdf font: " + e.getMessage());
		}
	}
}
