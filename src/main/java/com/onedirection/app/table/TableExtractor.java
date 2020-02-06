package com.onedirection.app.table;

import com.onedirection.app.table.entity.Table;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TableExtractor {

    public List<Table> extract() {

    }

    private static class TextPositionExtractor extends PDFTextStripperByArea {
        private final List<TextPosition> textPositions = new ArrayList<>();

        private TextPositionExtractor(PDDocument document) throws IOException {
            super();
            super.setSortByPosition(true);
            super.document = document;
        }

        public void stripPage(int pageId) throws IOException {
            this.setStartPage(pageId + 1);
            this.setEndPage(pageId + 1);
            PDPage firstPage = document.getPage(pageId);
            super.extractRegions(firstPage);
            try (Writer writer = new OutputStreamWriter(new ByteArrayOutputStream())) {
                writeText(document, writer);
            }
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            this.textPositions.addAll(textPositions);
        }

        /**
         * and order by textPosition.getY() ASC
         * @return
         * @throws IOException
         */
        private List<TextPosition> extract(int pageId, Rectangle rect) throws IOException {

            super.addRegion("test", rect);

            this.stripPage(pageId);
            //sort
            Collections.sort(textPositions, new Comparator<TextPosition>() {
                @Override
                public int compare(TextPosition o1, TextPosition o2) {
                    int retVal = 0;
                    if (o1.getY() < o2.getY()) {
                        retVal = -1;
                    } else if (o1.getY() > o2.getY()) {
                        retVal = 1;
                    }
                    return retVal;

                }
            });
            return this.textPositions;
        }
    }

}
