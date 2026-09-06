package com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination;

public class PaginatedResponse<T> {

    private final T data;
    private final int skip;
    private final int limit;
    private final long total;
    private final boolean hasMore;

    public PaginatedResponse(T data, int skip, int limit, long total, boolean hasMore) {
        this.data = data;
        this.skip = skip;
        this.limit = limit;
        this.total = total;
        this.hasMore = hasMore;
    }

    public T getData() {
        return data;
    }

    public int getSkip() {
        return skip;
    }

    public int getLimit() {
        return limit;
    }

    public long getTotal() {
        return total;
    }

    public boolean isHasMore() {
        return hasMore;
    }
}
