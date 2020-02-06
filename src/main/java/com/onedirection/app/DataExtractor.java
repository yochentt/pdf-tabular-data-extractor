package com.onedirection.app;

import com.onedirection.app.table.TableExtractor;
import com.onedirection.app.table.entity.Table;
import com.onedirection.app.table.entity.TableCell;
import com.onedirection.app.table.entity.TableRow;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataExtractor {

    public static void main(String[] args) throws IOException {

        try (PDDocument document = PDDocument.load(new File("build/resources/main/sample_grab.pdf"))) {

            final TableExtractor extractor = new TableExtractor(document);

            final Rectangle tableRect = new Rectangle(10, 160, 950, 65);
            final Table table = extractor.extract(0, tableRect);
            final List<Map<String, Object>> tableList = formatTableToList(table);
            System.out.println(tableList);

            final Rectangle metadataRect = new Rectangle(10, 160, 950, 65);
            final Table metadataTable = extractor.extract(0, metadataRect);
            final Map<String, Object> metadataMap = formatMetadataToMap(metadataTable);
            System.out.println(metadataMap);

            Map<String, Object> outputMap = new HashMap<>();
            outputMap.putAll(metadataMap);
            outputMap.put("table", tableList);
            System.out.println("Text in the area:\n");
            System.out.println(table.toHtml());
        }
    }

    private static Map<String, Object> formatMetadataToMap(Table table) {
        Map<String, Object> metadataMap = new HashMap<>();
        final List<TableRow> rows = table.getRows();
        for (TableRow row : table.getRows()) {
            final List<TableCell> cells = row.getCells();
            metadataMap.put(cells.get(0).getContent(), cells.get(1).getContent());
        }
        return metadataMap;
    }

    private static List<Map<String, Object>> formatTableToList(Table table) {
        List<Map<String, Object>> tableList = new ArrayList<>();
        final List<TableRow> rows = table.getRows();
        List<TableCell> headerCell = rows.get(0).getCells();
        for (int i = 1; i < rows.size(); i++) {
            final List<TableCell> cells = rows.get(i).getCells();
            final Map<String, Object> rowMap = new HashMap<>();
            for (int j = 0; j < cells.size(); j++) {
                rowMap.put(headerCell.get(j).getContent(), cells.get(j).getContent());
            }
            tableList.add(rowMap);
        }
        return tableList;
    }
}
