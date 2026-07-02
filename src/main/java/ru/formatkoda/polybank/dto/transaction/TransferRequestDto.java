package ru.formatkoda.polybank.dto.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

public record TransferRequestDto(
		@NotBlank @Length(min = 20, max = 20) String fromAccountNumber,
		@NotBlank @Length(min = 20, max = 20) String toAccountNumber,
		@NotNull @DecimalMin(value = "0.01") @Digits(integer = 18, fraction = 2) BigDecimal amount
) {
}
