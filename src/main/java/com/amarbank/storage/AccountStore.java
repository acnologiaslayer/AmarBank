package com.amarbank.storage;

import java.util.List;

import com.amarbank.Account;
import com.amarbank.exception.DataStoreException;

/**
 * Permanent-storage strategy for the bank's accounts.
 *
 * <p>The assignment requires a CSV file ({@link CsvAccountStore}), which is the
 * default. A SQLite-backed implementation ({@link SqliteAccountStore}) is
 * provided as an optional improvement and is fully interchangeable thanks to
 * this interface.</p>
 */
public interface AccountStore {

    /** Reads every persisted account into memory. */
    List<Account> loadAll() throws DataStoreException;

    /** Writes the full set of active accounts to permanent storage. */
    void saveAll(List<Account> accounts) throws DataStoreException;

    /** Human-readable description of where data is stored (for the status bar). */
    String describe();
}
