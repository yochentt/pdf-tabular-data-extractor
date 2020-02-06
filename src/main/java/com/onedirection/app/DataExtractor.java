package com.onedirection.app;

import com.onedirection.app.table.TableExtractor;
import com.onedirection.app.table.entity.Table;
import org.apache.pdfbox.pdmodel.PDDocument;

import java.awt.*;
import java.io.*;
import java.util.Arrays;
import java.util.Properties;

public class DataExtractor {

    private final static FilenameFilter FILTER = (dir, name) -> name.endsWith(".pdf");
    private final static String DEFAULT_INPUT_DIRECTORY_PATH = "build/resources/main/";
    private final static String DEFAULT_OUTPUT_DIRECTORY_PATH = "build/resources/output/";

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
            final Rectangle rect = getRectangle(configFile);
            final Table table = extractor.extract(0, rect);
            System.out.println("Text in the area:\n");
            System.out.println(table.toHtml());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Rectangle getRectangle(File configFile) {
        final Properties prop = new Properties();
        try (InputStream input = new FileInputStream(configFile.toString())) {
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Rectangle(Integer.parseInt(prop.getProperty("table.left-corner.x")),
                Integer.parseInt(prop.getProperty("table.left-corner.y")),
                Integer.parseInt(prop.getProperty("table.width")),
                Integer.parseInt(prop.getProperty("table.height")));
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
}
