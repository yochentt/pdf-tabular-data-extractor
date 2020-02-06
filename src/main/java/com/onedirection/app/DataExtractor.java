package com.onedirection.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onedirection.app.table.TableExtractor;
import com.onedirection.app.table.entity.Table;
import com.onedirection.app.table.entity.TableCell;
import com.onedirection.app.table.entity.TableRow;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.Arrays;
import java.util.Properties;

public class DataExtractor {

    private static final FilenameFilter FILTER = (dir, name) -> name.endsWith(".pdf");
    private static final String DEFAULT_INPUT_DIRECTORY_PATH = "build/resources/main/";
    private static final String DEFAULT_OUTPUT_DIRECTORY_PATH = "build/resources/output/";
    private static final String TABLE_KEY = "Table";
    private static final String METADATA_KEY = "Metadata";

    public static void main(String[] args) {
        final String inDirectoryPath = getIn(args);
        final String outDirectoryPath = getOut(args);

        final File pdfDirectory = new File(inDirectoryPath);
        final File[] pdfFiles = pdfDirectory.listFiles(FILTER);
        Arrays.asList(pdfFiles).stream().forEach(pdfFile -> processPdfFile(pdfFile, outDirectoryPath));
    }

    private static void processPdfFile(File pdfSource, String outDirectoryPath) {
        final File configFile = getConfigFile(pdfSource.getParentFile());

        try (PDDocument document = PDDocument.load(pdfSource)) {
            final TableExtractor extractor = new TableExtractor(document);
            final List<Map<String, Object>> tableList = new ArrayList<>();
            final Map<String, Object> metadataMap = new HashMap<>();

            getRectangles(configFile).entrySet().stream().forEach(stringListEntry -> {
                    String type = stringListEntry.getKey();
                    List<Rectangle> rectangles = stringListEntry.getValue();

                    switch (type)  {
                        case TABLE_KEY:
                            rectangles.stream().forEach(rectangle -> {
                                final Table table = extractor.extract(0, rectangle);
                                tableList.addAll(formatTableToList(table));
                            });
                            break;
                        case METADATA_KEY:
                            rectangles.stream().forEach(rectangle -> {
                                final Table table = extractor.extract(0, rectangle);

                                metadataMap.putAll(formatMetadataToMap(table));
                            });
                            break;
                    }
            });

            final Map<String, Object> outputMap = new HashMap<>();
            outputMap.putAll(metadataMap);
            outputMap.put("table", tableList);

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(outputMap);
            System.out.println(json);

            File directory = new File(outDirectoryPath);
            if (!directory.exists()) {
                directory.mkdir();
            }

            objectMapper.writeValue(new File(String.format("%s/%s.json", outDirectoryPath, pdfSource.getName())), outputMap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, java.util.List<Rectangle>> getRectangles(File configFile) {
        final Properties prop = new Properties();
        try (InputStream input = new FileInputStream(configFile.toString())) {
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, List<Rectangle>> rectangleMap = new HashMap<>();
        java.util.List<Rectangle> rectangleTableList = new ArrayList<>();
        // Get list of rectangle
        if (prop.getProperty("table.left-corner.x") != null) {
            rectangleTableList.add(new Rectangle(Integer.parseInt(prop.getProperty("table.left-corner.x")),
                    Integer.parseInt(prop.getProperty("table.left-corner.y")),
                    Integer.parseInt(prop.getProperty("table.width")),
                    Integer.parseInt(prop.getProperty("table.height"))
            ));
        }
        rectangleMap.put(TABLE_KEY, rectangleTableList);

        java.util.List<Rectangle> rectangleMetadataList = new ArrayList<>();
        int i = 1;
        while (prop.getProperty("metadata." + i + ".left-corner.x") != null) {
            // property #i has value p
            rectangleMetadataList.add(new Rectangle(
                    Integer.parseInt(prop.getProperty("metadata." + i + ".left-corner.x")),
                    Integer.parseInt(prop.getProperty("metadata." + i + ".left-corner.y")),
                    Integer.parseInt(prop.getProperty("metadata." + i + ".width")),
                    Integer.parseInt(prop.getProperty("metadata." + i + ".height"))
            ));
            rectangleMap.put(METADATA_KEY, rectangleMetadataList);
            i++;
        }

        return rectangleMap;
    }

    private static File getConfigFile(File directory) {
        if (!directory.isDirectory()) {
            throw new RuntimeException("It is not a directory: " + directory.toString());
        }
        return new File(directory.getPath() + "/extractor.properties");
    }

    private static String getOut(String[] args) {
        String retVal = getArg(args, "out", DEFAULT_OUTPUT_DIRECTORY_PATH);
        if (retVal == null) {
            throw new RuntimeException("Missing output location");
        }
        return retVal;
    }

    private static String getIn(String[] args) {
        String retVal = getArg(args, "in", DEFAULT_INPUT_DIRECTORY_PATH);
        if (retVal == null) {
            throw new RuntimeException("Missing input file");
        }
        return retVal;
    }

    private static String getArg(String[] args, String name, String defaultValue) {
        int argIdx = -1;
        for (int idx = 0; idx < args.length; idx++) {
            if (("-" + name).equals(args[idx])) {
                argIdx = idx;
                break;
            }
        }
        if (argIdx == -1) {
            System.out.println("Argument " + name + " is using default value: " + defaultValue);
            return defaultValue;
        } else if (argIdx < args.length - 1) {
            return args[argIdx + 1].trim();
        } else {
            throw new RuntimeException("Missing argument value. Argument name: " + name);
        }
    }

    private static Map<String, Object> formatMetadataToMap(Table table) {
        Map<String, Object> metadataMap = new HashMap<>();
        for (TableRow row : table.getRows()) {
            final List<TableCell> cells = row.getCells();
            if (cells.size() < 2) {
                continue;
            }
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
