package ru.formatkoda.polybank.util.export;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportFormat {
	CSV("csv"),
	PDF("pdf");

	private final String format;
}
