package com.amarbank;

import java.nio.file.Path;
import javax.swing.SwingUtilities;

import com.amarbank.exception.DataStoreException;
import com.amarbank.gui.BankAppGUI;

public class Main {
    public static void main(String[] args) {
        try {
            BankManagement bankManagement = new BankManagement(Path.of("data"));
            BankOperations bank = new BankOperations(bankManagement);
            SwingUtilities.invokeLater(() -> new BankAppGUI(bank).setVisible(true));
        } catch (DataStoreException e) {
            System.err.println("Could not start the bank: " + e.getMessage());
        }
    }
}