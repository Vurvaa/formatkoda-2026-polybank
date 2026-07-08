package ru.formatkoda.polybank.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record AccountOperationRequestDto(
		@NotBlank @Length(min = 20, max = 20) String accountNumber,
		@NotBlank
		@Pattern(
				regexp = "^(0|[1-9]\\d{0,15})(\\.\\d{1,2})?$",
				message = "amount must be a decimal string with no more than 2 fraction digits"
		)
		String amount
) {
}
