package com.githa.core.common;

/**
 * Page request parameters for pagination.
 * Replaces Spring's Pageable interface for Quarkus.
 */
public class PageRequest {
    private int page = 0;
    private int size = 20;
    private String sort;

    public PageRequest() {
    }

    public PageRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public PageRequest(int page, int size, String sort) {
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public int getOffset() {
        return page * size;
    }
}
