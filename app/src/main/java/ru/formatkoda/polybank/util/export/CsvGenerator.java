package ru.formatkoda.polybank.util.export;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import ru.formatkoda.polybank.domain.transaction.TransactionWithAccountNumbersView;
import ru.formatkoda.polybank.exception.ExportException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
public class CsvGenerator {

	private CsvGenerator() {
	}

	private static final String[] TRANSACTIONS_HEADER = {"id", "from", "to", "amount", "type", "status", "created_at"};

	public static ByteArrayInputStream toCsv(List<TransactionWithAccountNumbersView> transactions) {
		CSVFormat format = CSVFormat.DEFAULT
				.builder()
				.setHeader(TRANSACTIONS_HEADER)
				.setSkipHeaderRecord(false)
				.get();

		try (ByteArrayOutputStream out = new ByteArrayOutputStream();
			 CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(out), format)) {
			for (TransactionWithAccountNumbersView transaction : transactions)
				csvPrinter.printRecord(toRow(transaction));
			csvPrinter.flush();
			return new ByteArrayInputStream(out.toByteArray());
		} catch (IOException e) {
			log.error("error generating csv");
			throw new ExportException("failed to save data to csv: " + e.getMessage());
		}
	}

	private static List<String> toRow(TransactionWithAccountNumbersView transaction) {
		return Arrays.asList(
				String.valueOf(transaction.id()),
				transaction.fromAccountNumber() == null ? "" : transaction.fromAccountNumber().value(),
				transaction.toAccountNumber() == null ? "" : transaction.toAccountNumber().value(),
				transaction.amount().toPlainString(),
				transaction.type().name(),
				transaction.status().name(),
				transaction.createdAt().format(DateTimeFormatter.ISO_DATE_TIME)
		);
	}
}
