package com.amarbank.storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import com.amarbank.Account;
import com.amarbank.exception.DataStoreException;

/**
 * The assignment's required permanent storage: a plain CSV file.
 *
 * <p>Values are escaped/parsed using RFC-4180-style quoting so that holder
 * names containing commas or quotes survive a save/load round-trip. Writes go
 * to a temporary file first and are then atomically moved into place, so a
 * crash mid-write can never corrupt the existing database.</p>
 */
public class CsvAccountStore implements AccountStore {

    private final Path csvFile;

    public CsvAccountStore(Path csvFile) throws DataStoreException {
        this.csvFile = csvFile;
        try {
            Path parent = csvFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new DataStoreException("Could not create data directory for " + csvFile, e);
        }
    }

    @Override
    public List<Account> loadAll() throws DataStoreException {
        List<Account> accounts = new ArrayList<>();
        if (!Files.exists(csvFile)) {
            return accounts; // first run: no file yet
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataStoreException("Could not read CSV file " + csvFile, e);
        }
        boolean headerSkipped = false;
        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }
            if (!headerSkipped) {
                headerSkipped = true; // first non-blank line is the header row
                continue;
            }
            accounts.add(AccountCodec.fromRow(parseCsvLine(line)));
        }
        return accounts;
    }

    @Override
    public void saveAll(List<Account> accounts) throws DataStoreException {
        StringBuilder sb = new StringBuilder();
        sb.append(joinCsv(AccountCodec.COLUMNS)).append(System.lineSeparator());
        for (Account account : accounts) {
            sb.append(joinCsv(AccountCodec.toRow(account))).append(System.lineSeparator());
        }
        Path tmp = csvFile.resolveSibling(csvFile.getFileName() + ".tmp");
        try {
            Files.writeString(tmp, sb.toString(), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, csvFile,
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                // Some filesystems do not support atomic moves; fall back.
                Files.move(tmp, csvFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new DataStoreException("Could not write CSV file " + csvFile, e);
        }
    }

    @Override
    public String describe() {
        return "CSV file: " + csvFile.toAbsolutePath();
    }

    // ---------- CSV helpers ----------

    /** Joins fields into one CSV line, quoting where needed. */
    private static String joinCsv(String[] fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(fields[i]));
        }
        return sb.toString();
    }

    /** Quotes a field if it contains a comma, quote or newline. */
    private static String escape(String field) {
        String value = field == null ? "" : field;
        boolean mustQuote = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!mustQuote) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    /** Parses one CSV line into fields, honouring RFC-4180 double-quote escaping. */
    private static String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"'); // escaped quote
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
