package com.onedirection.app;

import com.onedirection.app.table.TableExtractor;
import com.onedirection.app.table.entity.Table;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class DataExtractor {

    public static void main(String[] args) throws IOException {

        try (PDDocument document = PDDocument.load(new File("build/resources/main/sample_grab.pdf"))) {

            final TableExtractor extractor = new TableExtractor(document);
            final Rectangle rect = new Rectangle(10, 160, 950, 65);
            final Table table = extractor.extract(0, rect);

            System.out.println("Text in the area:\n");
            System.out.println(table.toString());
        }
    }
}
