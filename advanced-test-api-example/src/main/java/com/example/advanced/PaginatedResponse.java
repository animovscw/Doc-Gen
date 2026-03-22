package com.example.advanced;

import java.util.List;

public class PaginatedResponse<T> {
    public List<T> content;
    public Integer page;
    public Integer size;
    public Long total;
    public Integer totalPages;
    public Boolean hasNext;

    public PaginatedResponse() {}

    public PaginatedResponse(List<T> content, Integer page, Integer size, 
                           Long total, Integer totalPages, Boolean hasNext) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = totalPages;
        this.hasNext = hasNext;
    }
}

