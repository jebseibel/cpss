package com.seibel.cpss.loader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for parsing CSV files from classpath.
 */
@Slf4j
public class CsvParser {

    /**
     * Parses a CSV file from classpath and returns list of maps (column -> value).
     * First row is treated as header.
     *
     * @param resourcePath Path to CSV file in classpath (e.g., "db/data/10-food-nuts.csv")
     * @return List of maps, where each map represents a row with column names as keys
     */
    public static List<Map<String, String>> parse(String resourcePath) throws IOException {
        List<Map<String, String>> records = new ArrayList<>();
        ClassPathResource resource = new ClassPathResource(resourcePath);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                log.warn("Empty CSV file: {}", resourcePath);
                return records;
            }

            String[] headers = parseCsvLine(headerLine);
            log.debug("CSV headers for {}: {}", resourcePath, String.join(", ", headers));

            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                String[] values = parseCsvLine(line);
                if (values.length != headers.length) {
                    log.warn("Line {} in {} has {} values but expected {}. Skipping line.",
                            lineNumber, resourcePath, values.length, headers.length);
                    continue;
                }

                Map<String, String> record = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    record.put(headers[i], values[i]);
                }
                records.add(record);
            }

            log.info("Parsed {} records from {}", records.size(), resourcePath);
        }

        return records;
    }

    /**
     * Parses a single CSV line, handling quoted values.
     * Simple implementation that handles basic CSV with commas and double quotes.
     */
    private static String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // Handle escaped quotes ("")
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentValue.append('"');
                    i++; // Skip next quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
            } else {
                currentValue.append(c);
            }
        }

        // Add last value
        values.add(currentValue.toString().trim());

        return values.toArray(new String[0]);
    }
}
