package com.onedirection.app.table.entity;

import java.util.ArrayList;
import java.util.List;

public class Table {

    //--------------------------------------------------------------------------
    //  Members
    private final int pageIdx;
    private final List<TableRow> rows = new ArrayList<>();
    private final int columnsCount;

    //--------------------------------------------------------------------------
    //  Initialization and releasation
    public Table(int idx, int columnsCount) {
        this.pageIdx = idx;
        this.columnsCount = columnsCount;
    }

    //--------------------------------------------------------------------------
    //  Getter N Setter    
    public int getPageIdx() {
        return pageIdx;
    }

    public List<TableRow> getRows() {
        return rows;
    }
}
