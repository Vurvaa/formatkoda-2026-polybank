package ru.formatkoda.polybank.dto.transaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

public record TopUpRequestDto(
		@NotBlank @Length(min = 20, max = 20) String accountNumber,
		@NotNull BigDecimal amount
) {
}
