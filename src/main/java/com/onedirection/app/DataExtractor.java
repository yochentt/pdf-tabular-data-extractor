package com.onedirection.app;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class DataExtractor {

    public static void main(String[] args) throws IOException {

        try (PDDocument document = PDDocument.load(new File(args[0]))) {

            PDFTextStripperByArea stripper = new PDFTextStripperByArea();
            stripper.setSortByPosition(true);
            Rectangle rect = new Rectangle(10, 280, 275, 60);
            stripper.addRegion("class1", rect);
            PDPage firstPage = document.getPage(0);
            stripper.extractRegions(firstPage);
            System.out.println("Text in the area:" + rect);
            System.out.println(stripper.getTextForRegion("class1"));
        }
    }
}
