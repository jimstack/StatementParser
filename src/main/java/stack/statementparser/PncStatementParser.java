/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import static stack.statementparser.DateFormats.MONTH_DAY_FORMAT;
import static stack.statementparser.DateFormats.MONTH_DAY_FULL_YEAR_FORMAT;

/**
 *
 * @author jimst
 */
public class PncStatementParser
{
    private static final Pattern ALL_WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    
    public static Set<String> accountNumbers = new HashSet<>();
    
    private static void addAccountNumber(String accountNumber, String line)
    {
        if (!accountNumbers.contains(accountNumber))
        {
//            System.out.println(accountNumber + " seen: " + line);
            accountNumbers.add(accountNumber);
        }
    }

    public static BankStatement parse(String fileName)
    {
        System.out.println("Parsing: " + fileName);
        BankStatement statement = new BankStatement(fileName);
        RawSection header = new RawSection("Header"),
                   balanceSummary = new RawSection("Balance Summary"),
                   deposits = new RawSection("Desposits"),
                   checks = new RawSection("Checks"),
                   withdrawals = new RawSection("Withdrawals and Purchases"),
                   online = new RawSection("Online Payments and Transfers"),
                   otherDeductions = new RawSection("Other Deductions"),
                   dailyBalanceDetail = new RawSection("Daily Balance Summary");

        RawSection currentSection = header;
        try (PDDocument document = PDDocument.load(new File(fileName)))
        {
            if (!document.isEncrypted())
            {            
                PDFTextStripper tStripper = new PDFTextStripper();

                String pdfFileInText = tStripper.getText(document);
                //System.out.println("Text:" + st);

                // split by whitespace
                String lines[] = pdfFileInText.split("\\r?\\n");
                for (String line : lines)
                {
                    if (StringUtils.containsIgnoreCase(line, "transfer"))
                    {
                        String words[] = line.split(" ");
                        if (words.length > 0)
                        {
                            addAccountNumber(words[words.length-1], line);
                        }
                    }
                    String words[] = line.split(" ");
                    for (String word : words)
                    {
                        if (word.length() > 15)
                        {
                            addAccountNumber(word, line);
                        }
                    }
//                    System.out.println(line);
                    
                    if (StringUtils.containsIgnoreCase(line, "deposits and other additions"))
                    {
                        currentSection = deposits;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.equalsIgnoreCase(line, "balance summary"))
                    {
                        currentSection = balanceSummary;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.containsIgnoreCase(line, "checks and substitute checks"))
                    {
                        currentSection = checks;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.containsIgnoreCase(line, "withdrawals and purchases"))
                    {
                        currentSection = withdrawals;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.containsIgnoreCase(line, "online and electronic banking deductions"))
                    {
                        currentSection = online;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.containsIgnoreCase(line, "other deductions"))
                    {
                        currentSection = otherDeductions;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.containsIgnoreCase(line, "daily balance detail"))
                    {
                        currentSection = dailyBalanceDetail;
//                        System.out.println("Loading " + currentSection.getName());
                    }

                    currentSection.getLines().add(line);
//                    System.out.println(line);
                }
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(PncStatementParser.class.getName()).log(Level.SEVERE, fileName + " -> " + ex.getMessage());
        }
                
        parseHeader(statement, header);
        parseBalanceSummary(statement, balanceSummary);
        parseDeposits(statement, deposits);
//                parseChecks(statement, checks);
        parseWithdrawalsAndPurchases(statement, withdrawals);
        parseOnlinePaymentsAndTransfers(statement, online);
        parseOtherDeductions(statement, otherDeductions);

//        System.out.println("\n***************************************************************");
//        System.out.println("***************************************************************\n");
        return statement;
    }
    
    private static void parseHeader(BankStatement statement, RawSection section)
    {
//        System.out.println("Parsing " + section.getName());
        
        boolean accountNumberFound = false,
                timePeriodFound = false;
        
        for (String line : section.getLines())
        {
            if (!accountNumberFound && StringUtils.contains(line, "account number"))
            {
                String[] parts = line.split(": ");
                statement.setAccountNumber(parts[1]);
                accountNumberFound = true;
//                System.out.println("Account number: " + statement.getAccountNumber());
            }
            else if (!timePeriodFound && StringUtils.containsIgnoreCase(line, "For the period"))
            {
                String[] parts = ALL_WHITESPACE_PATTERN.split(line);
                List<String> dates = new ArrayList<>();
                for (int i=0; i<parts.length; ++i)
                {
                    try
                    {
                        MONTH_DAY_FULL_YEAR_FORMAT.parse(parts[i]);
                        dates.add(parts[i]);
                    }
                    catch (ParseException ex)
                    {
                        // Not a date
                    }
                }
                
//                if (dates.size() != 2)
//                {
//                    throw(new UnsupportedOperationException("Incorrect number of dates found: " + line));
//                }
                try 
                {
                    statement.setStartDate(dates.get(0));
                }
                catch (ParseException ex)
                {
                    Logger.getLogger(PncStatementParser.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                try
                {
                    statement.setEndDate(dates.get(1));
                }
                catch (ParseException ex)
                {
                    Logger.getLogger(PncStatementParser.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                timePeriodFound = true;
            }                
        }
    }
    
    private static void parseDeposits(BankStatement statement, RawSection section)
    {
//        System.out.println("Parsing " + section.getName());
        List<Transaction> transactions = findTransactions(statement, section.getLines());
        for (Transaction transaction : transactions)
        {
            if (StringUtils.containsIgnoreCase(transaction.getDescription(), "transfer from"))
            {
                transaction = new Transfer(transaction);
                transaction.setTransactionType(TransactionType.TRANSFER_IN);
                
                String[] parts = ALL_WHITESPACE_PATTERN.split(transaction.getDescription());
                ((Transfer) transaction).setOtherAccount(parts[parts.length-1]);
            }
            else
            {
                transaction.setTransactionType(TransactionType.DEPOSIT);
            }
            statement.addTransaction(transaction);
        }
    }
    
    private static void parseBalanceSummary(BankStatement statement, RawSection section)
    {
//        System.out.println("Parsing " + section.getName());
        
        for (String line : section.getLines())
        {
            try
            {
                NUMBER_FORMAT.parse(line);
            }
            catch (ParseException ex)
            {
                // Not the line we want
                continue;
            }
            
            String[] parts = ALL_WHITESPACE_PATTERN.split(line);
            try
            {
                statement.setBeginningBalance(NUMBER_FORMAT.parse(parts[0]).doubleValue());
                statement.setEndingBalance(NUMBER_FORMAT.parse(parts[3]).doubleValue());
                
                return;
            }
            catch (ParseException ex)
            {
                // Not the line we want
            }
        }
    }
    
    private static void parseChecks(BankStatement statement, RawSection section)
    {
        System.out.println("Parsing " + section.getName());
        for (String line : section.getLines())
        {
            String[] parts = ALL_WHITESPACE_PATTERN.split(line);
            for (int i=0; i<parts.length; ++i)
            {
                try
                {                    
                    int checkNumber = Integer.parseInt(parts[i]);
                    ++i;
                    
                    String amount = parts[i];
                    ++i;
                    
                    String date = parts[i];
                    ++i;
                    
                    String referenceNumber = parts[i];
                    
                    String description = checkNumber + " " + amount + " " + date + " " + referenceNumber;
                    System.out.println("    Check: " + description);
                    
                    statement.addTransaction(new Check(checkNumber, referenceNumber, date, amount, description));
                }
                catch (NumberFormatException | IndexOutOfBoundsException e)
                {
                    // Not the start of a check entryy
                }
            }
            System.out.println("    " + line);
        }
    }
    
    private static void parseWithdrawalsAndPurchases(BankStatement statement, RawSection section)
    {
//        System.out.println("Parsing " + section.getName());
        List<Transaction> transactions = findTransactions(statement, section.getLines());
        Transaction previousTransaction;
        for (Transaction transaction : transactions)
        {
            if (StringUtils.containsIgnoreCase(transaction.getDescription(), "debit card") ||
                StringUtils.containsIgnoreCase(transaction.getDescription(), "check card") ||
                StringUtils.containsIgnoreCase(transaction.getDescription(), "POS Purchase"))
            {
                transaction.setTransactionType(TransactionType.DEBIT_CARD_PURCHASE);
            }
            else if (StringUtils.containsIgnoreCase(transaction.getDescription(), "ATM withdrawal"))
            {
                transaction.setTransactionType(TransactionType.ATM_WITHDRAWAL);
            }
            else if (StringUtils.containsIgnoreCase(transaction.getDescription(), " fee ") ||
                     StringUtils.containsIgnoreCase(transaction.getDescription(), " service charge "))
            {
                transaction.setTransactionType(TransactionType.FEE);
            }
            else
            {
                System.out.println("Unknown withdrawal type: " + transaction.getDescription());
                transaction.setTransactionType(TransactionType.UNKNOWN_WITHDRAWAL);
            }
            statement.addTransaction(transaction);
            
            previousTransaction = transaction;
        }
    }
    
    private static void parseOnlinePaymentsAndTransfers(BankStatement statement, RawSection section)
    {
//        System.out.println("Parsing " + section.getName());
        List<Transaction> transactions = findTransactions(statement, section.getLines());
        for (Transaction transaction : transactions)
        {
            if (StringUtils.containsIgnoreCase(transaction.getDescription(), "web pmt"))
            {
                transaction.setTransactionType(TransactionType.ONLINE_PAYMENT);
            }
            else if (StringUtils.containsIgnoreCase(transaction.getDescription(), "direct payment"))
            {
                transaction.setTransactionType(TransactionType.DIRECT_PAYMENT);
            }
            else if (StringUtils.containsIgnoreCase(transaction.getDescription(), "transfer to"))
            {                
                transaction = new Transfer(transaction);
                transaction.setTransactionType(TransactionType.TRANSFER_OUT);
                
                String[] parts = ALL_WHITESPACE_PATTERN.split(transaction.getDescription());
                ((Transfer) transaction).setOtherAccount(parts[parts.length-1]);
            }
            else if (StringUtils.containsIgnoreCase(transaction.getDescription(), " protection fee") ||
                     StringUtils.containsIgnoreCase(transaction.getDescription(), " service charge "))
            {
                transaction.setTransactionType(TransactionType.FEE);
            }
            else
            {
                System.out.println("Unknown online type: " + transaction.getDescription());
                transaction.setTransactionType(TransactionType.UNKNOWN_ONLINE_TRANSACTION);
            }
            statement.addTransaction(transaction);
        }
    }
    
    private static void parseOtherDeductions(BankStatement statement, RawSection section)
    {
//        System.out.println("Parsing " + section.getName());
        List<Transaction> transactions = findTransactions(statement, section.getLines());
        Transaction previousTransaction;
        for (Transaction transaction : transactions)
        {
            if (StringUtils.containsIgnoreCase(transaction.getDescription(), "withdrawal"))
            {
                transaction.setTransactionType(TransactionType.TELLER_WITHDRAWAL);
            }
            else if (StringUtils.containsIgnoreCase(transaction.getDescription(), "service charge") ||
                     StringUtils.containsIgnoreCase(transaction.getDescription(), " check printing fee ") ||
                     StringUtils.containsIgnoreCase(transaction.getDescription(), "od protection fee"))
            {
                transaction.setTransactionType(TransactionType.FEE);
            }
            else if (StringUtils.containsIgnoreCase(transaction.getDescription(), "transfer to"))
            {
                transaction = new Transfer(transaction);
                transaction.setTransactionType(TransactionType.TRANSFER_OUT);
                
                String[] parts = ALL_WHITESPACE_PATTERN.split(transaction.getDescription());
                ((Transfer) transaction).setOtherAccount(parts[parts.length-1]);
            }
            else
            {
                System.out.println("Unknown other deduction type: " + transaction.getDescription());
                transaction.setTransactionType(TransactionType.UNKNOWN_ONLINE_TRANSACTION);
            }
            statement.addTransaction(transaction);
            
            previousTransaction = transaction;
        }
    }
    
    private static List<Transaction> findTransactions(BankStatement statement, List<String> lines)
    {
        List<RawTransaction> rawTransactions = new ArrayList<>();
        RawTransaction currentTransaction = null;
        
        for (String line : lines)
        {
            String[] parts = line.split("\\s+");
            try
            {
                MONTH_DAY_FORMAT.parse(parts[0]);
                currentTransaction = new RawTransaction(parts[0]);
                rawTransactions.add(currentTransaction);
            }
            catch (ParseException ex)
            {
                // Not a date. Append to current transaction.
            }
            
            if (isHeaderOrFooter(line))
            {
                // Footer
                currentTransaction = null;
            }
            
            if (null != currentTransaction)
            {
                currentTransaction.getLines().add(line);
            }
        }
        
        List<Transaction> transactions = new ArrayList<>();
        for (RawTransaction rawTransaction : rawTransactions)
        {
            String line = "";
            for (String part : rawTransaction.getLines())
            {
                line += " ";
                line += part;
            }
            
            line = condenseWhitespace(line);
            //System.out.println(line);
            
            String[] parts = ALL_WHITESPACE_PATTERN.split(line);
            
            String description = "";
            for (int i=2; i<parts.length; ++i)
            {
                description += parts[i];
                description += " ";
            }
            
            if (parts.length < 2)
            {
                continue;
            }
            
            String date = "UNKNOWN";
            try
            {
                date = statement.getFullDate(parts[0]);
            }
            catch (ParseException ex)
            {
                Logger.getLogger(PncStatementParser.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            transactions.add(new Transaction(date, parts[1], description));
        }
        
        return transactions;
    }    
    
    public static String condenseWhitespace(String string)
    {         
        Matcher matcher = ALL_WHITESPACE_PATTERN.matcher(string);
        String value = matcher.replaceAll(" ");
        
        return value.trim();
    }
    
    private static boolean isHeaderOrFooter(String line)
    {
        if (StringUtils.containsIgnoreCase(line, "continued"))
        {
            return true;
        }
        
        if (StringUtils.containsIgnoreCase(line, "page") &&
            StringUtils.containsIgnoreCase(line, " of "))
        {
            return true;
        }
        
        return false;
    }
}
