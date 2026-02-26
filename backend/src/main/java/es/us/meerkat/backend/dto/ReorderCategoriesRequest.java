package es.us.meerkat.backend.dto;

import java.util.List;
import jakarta.validation.constraints.NotNull;

public record ReorderCategoriesRequest(
    @NotNull(message = "La lista de IDs es requerida")
    List<Long> categoryIds
) {}
