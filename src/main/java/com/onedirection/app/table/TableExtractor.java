package com.onedirection.app.table;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.onedirection.app.table.entity.Table;
import com.onedirection.app.table.entity.TableCell;
import com.onedirection.app.table.entity.TableRow;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.*;

public class TableExtractor {

    private final Logger logger = LoggerFactory.getLogger(TableExtractor.class);

    private PDDocument document;

    private final double wordPadding = 0.5;

    public TableExtractor(final PDDocument document) {
        this.document = document;
    }

    public Table extract(final int pageId, final Rectangle rect) {
        Multimap<Integer, Range<Integer>> pageIdNLineRangesMap = LinkedListMultimap.create();
        Multimap<Integer, TextPosition> pageIdNTextsMap = LinkedListMultimap.create();
        try {
            List<TextPosition> texts = extractTextPositions(pageId, rect);//sorted by .getY() ASC
            //extract line ranges
            List<Range<Integer>> lineRanges = getLineRanges(pageId, texts);
            //extract column ranges
            List<TextPosition> textsByLineRanges = getTextsByLineRanges(lineRanges, texts);

            pageIdNLineRangesMap.putAll(pageId, lineRanges);
            pageIdNTextsMap.putAll(pageId, textsByLineRanges);

            //Calculate columnRanges
            List<Range<Integer>> columnRanges = getColumnRanges(textsByLineRanges);
            Table table = buildTable(pageId, textsByLineRanges, lineRanges, columnRanges);
            logger.debug(String.format("Found %d row(s) and %d column(s) of a table in page %d", table.getRows().size(), columnRanges.size(), pageId));
            return table;

        } catch (IOException ex) {
            throw new RuntimeException("Parse pdf file fail", ex);
        }
    }

    //--------------------------------------------------------------------------
    //  Implement N Override
    //--------------------------------------------------------------------------
    //  Utils

    /**
     * Texts in tableContent have been ordered by .getY() ASC
     * @param pageIdx
     * @param tableContent
     * @param rowTrapRanges
     * @param columnTrapRanges
     * @return
     */
    private Table buildTable(int pageIdx, List<TextPosition> tableContent,
                             List<Range<Integer>> rowTrapRanges, List<Range<Integer>> columnTrapRanges) {
        Table retVal = new Table(pageIdx, columnTrapRanges.size());
        int idx = 0;
        int rowIdx = 0;
        List<TextPosition> rowContent = new ArrayList<>();
        while (idx < tableContent.size()) {
            TextPosition textPosition = tableContent.get(idx);
            Range<Integer> rowTrapRange = rowTrapRanges.get(rowIdx);
            Range<Integer> textRange = Range.closed((int) textPosition.getY(),
                    (int) (textPosition.getY() + textPosition.getHeight()));
            if (rowTrapRange.encloses(textRange)) {
                rowContent.add(textPosition);
                idx++;
            } else {
                TableRow row = buildRow(rowIdx, rowContent, columnTrapRanges);
                retVal.getRows().add(row);
                //next row: clear rowContent
                rowContent.clear();
                rowIdx++;
            }
        }
        //last row
        if (!rowContent.isEmpty() && rowIdx < rowTrapRanges.size()) {
            TableRow row = buildRow(rowIdx, rowContent, columnTrapRanges);
            retVal.getRows().add(row);
        }
        //return
        return retVal;
    }

    /**
     * @param rowIdx
     * @param rowContent
     * @param columnTrapRanges
     * @return
     */
    private TableRow buildRow(int rowIdx, List<TextPosition> rowContent, List<Range<Integer>> columnTrapRanges) {
        TableRow retVal = new TableRow(rowIdx);
        //Sort rowContent
        Collections.sort(rowContent, new Comparator<TextPosition>() {
            @Override
            public int compare(TextPosition o1, TextPosition o2) {
                int retVal = 0;
                if (o1.getX() < o2.getX()) {
                    retVal = -1;
                } else if (o1.getX() > o2.getX()) {
                    retVal = 1;
                }
                return retVal;
            }
        });
        int idx = 0;
        int columnIdx = 0;
        List<TextPosition> cellContent = new ArrayList<>();
        while (idx < rowContent.size()) {
            TextPosition textPosition = rowContent.get(idx);
            Range<Integer> columnTrapRange = columnTrapRanges.get(columnIdx);
            Range<Integer> textRange = Range.closed((int) textPosition.getX(),
                    (int) (textPosition.getX() + textPosition.getWidth()));
            if (columnTrapRange.encloses(textRange)) {
                cellContent.add(textPosition);
                idx++;
            } else {
                TableCell cell = buildCell(columnIdx, cellContent);
                retVal.getCells().add(cell);
                //next column: clear cell content
                cellContent.clear();
                columnIdx++;
            }
        }
        if (!cellContent.isEmpty() && columnIdx < columnTrapRanges.size()) {
            TableCell cell = buildCell(columnIdx, cellContent);
            retVal.getCells().add(cell);
        }
        //return
        return retVal;
    }

    private TableCell buildCell(int columnIdx, List<TextPosition> cellContent) {
        Collections.sort(cellContent, new Comparator<TextPosition>() {
            @Override
            public int compare(TextPosition o1, TextPosition o2) {
                int retVal = 0;
                if (o1.getX() < o2.getX()) {
                    retVal = -1;
                } else if (o1.getX() > o2.getX()) {
                    retVal = 1;
                }
                return retVal;
            }
        });
        //String cellContentString = Joiner.on("").join(cellContent.stream().map(e -> e.getCharacter()).iterator());
        StringBuilder cellContentBuilder = new StringBuilder();
        for (TextPosition textPosition : cellContent) {
            cellContentBuilder.append(textPosition.getUnicode());
        }
        String cellContentString = cellContentBuilder.toString();
        return new TableCell(columnIdx, cellContentString);
    }

    private List<TextPosition> extractTextPositions(int pageId, Rectangle rect) throws IOException {
        TextPositionExtractor extractor = new TextPositionExtractor(this.document);
        return extractor.extract(pageId, rect);
    }

    /**
     * Remove all texts in excepted lines
     *
     * TexPositions are sorted by .getY() ASC
     * @param lineRanges
     * @param textPositions
     * @return
     */
    private List<TextPosition> getTextsByLineRanges(List<Range<Integer>> lineRanges, List<TextPosition> textPositions) {
        List<TextPosition> retVal = new ArrayList<>();
        int idx = 0;
        int lineIdx = 0;
        while (idx < textPositions.size() && lineIdx < lineRanges.size()) {
            TextPosition textPosition = textPositions.get(idx);
            Range<Integer> textRange = Range.closed((int) textPosition.getY(),
                    (int) (textPosition.getY() + textPosition.getHeight()));
            Range<Integer> lineRange = lineRanges.get(lineIdx);
            if (lineRange.encloses(textRange)) {
                retVal.add(textPosition);
                idx++;
            } else if (lineRange.upperEndpoint() < textRange.lowerEndpoint()) {
                lineIdx++;
            } else {
                idx++;
            }
        }
        //return
        return retVal;
    }

    /**
     * @param texts
     * @return
     */
    private List<Range<Integer>> getColumnRanges(Collection<TextPosition> texts) {
        TrapRangeBuilder rangesBuilder = new TrapRangeBuilder();
        for (TextPosition text : texts) {
            Range<Integer> range = Range.closed((int) text.getX(), (int) (text.getX() + text.getWidth() + this.wordPadding));
            rangesBuilder.addRange(range);
        }
        return rangesBuilder.build();
    }

    private List<Range<Integer>> getLineRanges(int pageId, List<TextPosition> pageContent) {
        TrapRangeBuilder lineTrapRangeBuilder = new TrapRangeBuilder();
        for (TextPosition textPosition : pageContent) {
            Range<Integer> lineRange = Range.closed((int) textPosition.getY(),
                    (int) (textPosition.getY() + textPosition.getHeight()));
            //add to builder
            lineTrapRangeBuilder.addRange(lineRange);
        }
        List<Range<Integer>> lineTrapRanges = lineTrapRangeBuilder.build();
        return lineTrapRanges;
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
