package ru.formatkoda.polybank.util.pagination;

public record PageRequest(int page, int size) {
	public PageRequest {
		if (page < 0)
			throw new IllegalArgumentException("page must not be negative");

		if (size < 1 || size > 100)
			throw new IllegalArgumentException("size must be between 1 and 100");
	}

	public int offset() {
		return page * size;
	}
}
