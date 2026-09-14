package com.maestria.kaffehausv4.Shared.Infrastructure.Persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Pageable basado en desplazamiento absoluto.
 *
 * PaginationDTO expresa la paginacion como 'skip' (offset) + 'limit', mientras que
 * PageRequest.of(pagina, tamano) razona en numeros de pagina. Traducir con
 * skip / limit solo es exacto cuando skip es multiplo de limit, asi que esta clase
 * implementa Pageable devolviendo el offset tal cual y evita ese error silencioso.
 */
public class OffsetPageRequest implements Pageable {

    private final int offset;
    private final int limit;

    public OffsetPageRequest(int offset, int limit) {
        this.offset = Math.max(offset, 0);
        this.limit = limit < 1 ? 1 : limit;
    }

    @Override
    public int getPageNumber() {
        return offset / limit;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(offset + limit, limit);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetPageRequest(offset - limit, limit) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, limit);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest(pageNumber * limit, limit);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
