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
import java.util.Date;
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
import static stack.statementparser.DateFormats.MONTH_NAME_DAY_FORMAT;
import static stack.statementparser.DateFormats.MONTH_NAME_DAY_FULL_YEAR_FORMAT;

/**
 *
 * @author jimst
 */
public class BankOfAmericaStatementParser
{
    private static final Pattern ALL_WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern HEADER_ENTRY_PATTERN = Pattern.compile("[a-zA-z\\s]+");
    private static final Pattern REPEATED_PERIOD_PATTERN = Pattern.compile("(\\.)\\1+");
    private static final Pattern NON_ASCII_PATTERN = Pattern.compile("[^\\p{ASCII}]+");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.US);
    
    public static Set<String> accountNumbers = new HashSet<>();

    public static CreditCardStatement parse(String fileName)
    {
        System.out.println("Parsing: " + fileName);
        CreditCardStatement statement = new CreditCardStatement(fileName);
        RawSection header = new RawSection("Header"),
                   fees = new RawSection("Fees"),
                   paymentsAndOtherCredits = new RawSection("Payments and Other Credits"),
                   accountActivity = new RawSection("Account Activity"),
                   junk = new RawSection("Junk");

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
                    System.out.println(line);
                    
                    if (StringUtils.containsIgnoreCase(line, "account number") ||
                        StringUtils.containsIgnoreCase(line, "previous balance"))
                    {
                        currentSection = header;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.containsIgnoreCase(line, "purchases and adjustments") && !StringUtils.containsIgnoreCase(line, ".."))
                    {
                        currentSection = accountActivity;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.containsIgnoreCase(line, "payments and other credits") && !StringUtils.containsIgnoreCase(line, ".."))
                    {
                        currentSection = paymentsAndOtherCredits;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.containsIgnoreCase(line, "interest charged") && !StringUtils.containsIgnoreCase(line, ".."))
                    {
                        currentSection = junk;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.equalsIgnoreCase(line, "fees"))
                    {
                        currentSection = fees;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    
                    currentSection.getLines().add(line);
////                    System.out.println(line);
                }
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(BankOfAmericaStatementParser.class.getName()).log(Level.SEVERE, fileName + " -> " + ex.getMessage());
        }
                
        parseHeader(statement, header);
        parsePaymentsAndOtherCredits(statement, paymentsAndOtherCredits);
        parseAccountActivity(statement, accountActivity);
        
//        printSection(accountActivity);
//        printSection(header);
//        printSection(junk);
//        printSection(points);
        
        return statement;
    }
    
    private static void printSection(RawSection section)
    {
        System.out.println("    " + section.getName());
        for (String line : section.getLines())
        {
            boolean numberFound = false;
//            System.out.println("        " + line);
            String[] words = ALL_WHITESPACE_PATTERN.split(line);
            for (String word : words)
            {
                if (!numberFound)
                {
                    try
                    {
                        NUMBER_FORMAT.parse(word);
                        System.out.println("        " + line);
                        numberFound = true;
                    }
                    catch (ParseException ex)
                    {
                    }
                }
            }
        }
    }
    
    private static double parseDollarFigure(String line)
    {
        double result = 0.0;
        
        Matcher matcher = HEADER_ENTRY_PATTERN.matcher(line);
        String intermediateResult1 = matcher.replaceAll("");
        
        matcher = REPEATED_PERIOD_PATTERN.matcher(intermediateResult1);
        String intermediateResult2 = matcher.replaceAll("");
        
        matcher = NON_ASCII_PATTERN.matcher(intermediateResult2);
        String intermediateResult3 = matcher.replaceAll("");
        
        String intermediateResult4 = intermediateResult3.replace("$", "");
        String intermediateResult5 = intermediateResult4.replace("-", "");
        
        try
        {
            result = NUMBER_FORMAT.parse(intermediateResult5).doubleValue();
        }
        catch (ParseException ex)
        {
            Logger.getLogger(BankOfAmericaStatementParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
    private static void parseHeader(CreditCardStatement statement, RawSection section)
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
            else if (StringUtils.containsIgnoreCase(line, "new balance ") && StringUtils.containsIgnoreCase(line, "$"))
            {
                statement.setEndingBalance(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "previous balance") && StringUtils.containsIgnoreCase(line, ".."))
            {
                statement.setBeginningBalance(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "payments and other credits") && StringUtils.containsIgnoreCase(line, ".."))
            {
                statement.setPaymentsAndCredits(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "purchases and adju") && StringUtils.containsIgnoreCase(line, ".."))
            {
                statement.setPurchases(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "cash advances") && StringUtils.containsIgnoreCase(line, ".."))
            {
                statement.setCashAdvances(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "balance transfers") && StringUtils.containsIgnoreCase(line, ".."))
            {
                statement.setBalanceTransfers(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "fees charged") && StringUtils.containsIgnoreCase(line, ".."))
            {
                statement.setFeesCharged(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "interest charged") && StringUtils.containsIgnoreCase(line, ".."))
            {
                statement.setInterestCharged(parseDollarFigure(line));
            }
            else if (!timePeriodFound && DateFormats.isDateRangeWithNamedMonths(line))
            {
                String[] parts = line.split(" - ");
                
                if (parts.length == 2)
                {
                    try
                    {
                        Date startDate = MONTH_NAME_DAY_FORMAT.parse(parts[0]),
                             endDate = MONTH_NAME_DAY_FULL_YEAR_FORMAT.parse(parts[1]);
                        
                        int startMonth = startDate.getMonth(),
                            endMonth = endDate.getMonth();
                        if (endMonth >= startMonth)
                        {
                            startDate.setYear(endDate.getYear());
                        }
                        else
                        {
                            startDate.setYear(endDate.getYear()-1);
                        }
                        
                        statement.setStartDate(MONTH_DAY_FULL_YEAR_FORMAT.format(startDate));
                        statement.setEndDate(MONTH_DAY_FULL_YEAR_FORMAT.format(endDate));
                        
                        timePeriodFound = true;
                    }
                    catch (ParseException ex)
                    {
                        // Not a date
                    }
                }
            }                
        }
    }
    
    private static void parseAccountActivity(Statement statement, RawSection section)
    {
//        System.out.println("Parsing " + section.getName());
        List<Transaction> transactions = findTransactions(statement, section.getLines());
        for (Transaction transaction : transactions)
        {
            double amount = 0.0;
            try
            {
                amount = NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue();
            }
            catch (ParseException ex)
            {
                Logger.getLogger(BankOfAmericaStatementParser.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            if (amount < 0)
            {
                transaction.setTransactionType(TransactionType.REFUND);
            }
            else
            {
                transaction.setTransactionType(TransactionType.CREDIT_CARD_PURCHASE);
            }
            
            statement.addTransaction(transaction);
        }
    }
    
    private static void parsePaymentsAndOtherCredits(Statement statement, RawSection section)
    {
//        System.out.println("Parsing " + section.getName());
        List<Transaction> transactions = findTransactions(statement, section.getLines());
        for (Transaction transaction : transactions)
        {
            double amount = 0.0;
            try
            {
                Matcher matcher = NON_ASCII_PATTERN.matcher(transaction.getAmount());
                String result = matcher.replaceAll("");
                amount = NUMBER_FORMAT.parse(result).doubleValue();
            }
            catch (ParseException ex)
            {
                Logger.getLogger(BankOfAmericaStatementParser.class.getName()).log(Level.SEVERE, null, ex);
            }            
            
            if (!StringUtils.containsIgnoreCase(transaction.getDescription(), "payment"))
            {
                transaction.setTransactionType(TransactionType.REFUND);
                statement.addTransaction(transaction);
            }
        }
    }
    
    private static List<Transaction> findTransactions(Statement statement, List<String> lines)
    {
        List<RawTransaction> rawTransactions = new ArrayList<>();
        RawTransaction currentTransaction = null;
        
        for (String line : lines)
        {
            if (StringUtils.containsIgnoreCase(line, " payment ") || 
                StringUtils.containsIgnoreCase(line, "continued on next page") ||
                StringUtils.containsIgnoreCase(line, "$"))
            {
                currentTransaction = null;
                continue;
            }
            
            String[] parts = line.split("\\s+");
            if (parts.length == 0)
            {
                continue;
            }
                
            try
            {
                MONTH_DAY_FORMAT.parse(parts[0]);
                currentTransaction = new RawTransaction(parts[0]);
                rawTransactions.add(currentTransaction);
            }
            catch (ParseException ex)
            {
                // Not a date. Additional line of description
            }
            
            try
            {
                CURRENCY_FORMAT.parse(parts[0]);
                currentTransaction = null; // Is $ summary for transaction type.
            }
            catch (ParseException ex)
            {
                // Not a summary currency value
            }
            
            if (null != currentTransaction)
            {
                Matcher matcher = NON_ASCII_PATTERN.matcher(line);
                currentTransaction.getLines().add(matcher.replaceAll(" "));
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
            for (int i=1; i<parts.length-1; ++i)
            {
                if (DateFormats.isMonthAndDay(parts[i]))
                {
                    continue;
                }
                
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
                Logger.getLogger(BankOfAmericaStatementParser.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            transactions.add(new Transaction(date, parts[parts.length-1], description));
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
