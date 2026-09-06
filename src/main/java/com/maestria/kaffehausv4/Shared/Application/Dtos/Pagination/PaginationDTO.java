package com.maestria.kaffehausv4.Shared.Application.Dtos.Pagination;

public class PaginationDTO {

    private final Integer skip;
    private final Integer limit;
    private final String search;

    public PaginationDTO(Integer skip, Integer limit, String search) {
        this.skip = (skip != null) ? skip : 0;
        this.limit = (limit != null) ? limit : 10;
        this.search = (search != null) ? search : "";
    }

    public Integer getSkip() {
        return skip;
    }

    public Integer getLimit() {
        return limit;
    }

    public String getSearch() {
        return search;
    }
}
