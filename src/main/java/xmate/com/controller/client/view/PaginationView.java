package xmate.com.controller.client.view;

import java.util.List;

public record PaginationView(
        int currentPage,
        int pageSize,
        int totalPages,
        long totalElements,
        boolean hasPrevious,
        boolean hasNext,
        int previousPage,
        int nextPage,
        List<Integer> pages
) {
}
