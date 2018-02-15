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
import static stack.statementparser.DateFormats.MONTH_DAY_TWO_DIGIT_YEAR_FORMAT;

/**
 *
 * @author jimst
 */
public class ChaseStatementParser
{
    private static final Pattern ALL_WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern MONEY_PATTERN = Pattern.compile("\\$");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    
    public static Set<String> accountNumbers = new HashSet<>();

    public static CreditCardStatement parse(String fileName)
    {
        System.out.println("Parsing: " + fileName);
        CreditCardStatement statement = new CreditCardStatement(fileName);
        RawSection header = new RawSection("Header"),
                   points = new RawSection("Points"),
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
//                    System.out.println(line);
                    
                    if (StringUtils.containsIgnoreCase(line, "account number") ||
                        StringUtils.containsIgnoreCase(line, "previous balance"))
                    {
                        currentSection = header;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.containsIgnoreCase(line, "previous points balance"))
                    {
                        currentSection = points;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.containsIgnoreCase(line, "purchase interest") ||
                             StringUtils.containsIgnoreCase(line, "year-to-date") ||
                             StringUtils.containsIgnoreCase(line, "amount rewards"))
                    {
                        currentSection = junk;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    else if (StringUtils.containsIgnoreCase(line, "account activity") ||
                             StringUtils.containsIgnoreCase(line, "merchant name"))
                    {
                        currentSection = accountActivity;
//                        System.out.println("Loading " + currentSection.getName());
                    }
                    
                    currentSection.getLines().add(line);
////                    System.out.println(line);
                }
            }
        }
        catch (IOException ex)
        {
            Logger.getLogger(ChaseStatementParser.class.getName()).log(Level.SEVERE, fileName + " -> " + ex.getMessage());
        }
                
        parseHeader(statement, header);
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
        
        String[] parts = MONEY_PATTERN.split(line);
        String amount = parts[1].replace(",", "");
        result = Double.parseDouble(amount);
        
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
            else if (StringUtils.containsIgnoreCase(line, "previous balance ") && StringUtils.containsIgnoreCase(line, "$"))
            {
                statement.setBeginningBalance(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "payment, credits ") && StringUtils.containsIgnoreCase(line, "$"))
            {
                statement.setPaymentsAndCredits(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "purchases ") && StringUtils.containsIgnoreCase(line, "$"))
            {
                statement.setPurchases(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "cash advances ") && StringUtils.containsIgnoreCase(line, "$"))
            {
                statement.setCashAdvances(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "balance transfers ") && StringUtils.containsIgnoreCase(line, "$"))
            {
                statement.setBalanceTransfers(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "fees charged ") && StringUtils.containsIgnoreCase(line, "$"))
            {
                statement.setFeesCharged(parseDollarFigure(line));
            }
            else if (StringUtils.containsIgnoreCase(line, "interest charged ") && StringUtils.containsIgnoreCase(line, "$"))
            {
                statement.setInterestCharged(parseDollarFigure(line));
            }
            else if (!timePeriodFound && StringUtils.containsIgnoreCase(line, "opening/closing date"))
            {
                String[] parts = ALL_WHITESPACE_PATTERN.split(line);
                List<String> dates = new ArrayList<>();
                for (int i=0; i<parts.length; ++i)
                {
                    try
                    {
                        Date date = MONTH_DAY_TWO_DIGIT_YEAR_FORMAT.parse(parts[i]);
                        dates.add(MONTH_DAY_FULL_YEAR_FORMAT.format(date));
                    }
                    catch (ParseException ex)
                    {
                        // Not a date
                    }
                }

                try 
                {
                    statement.setStartDate(dates.get(0));
                }
                catch (ParseException ex)
                {
                    Logger.getLogger(ChaseStatementParser.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                try
                {
                    statement.setEndDate(dates.get(1));
                }
                catch (ParseException ex)
                {
                    Logger.getLogger(ChaseStatementParser.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                timePeriodFound = true;
//                System.out.println("Period: " + statement.getStartDate() + " to " + statement.getEndDate());
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
                Logger.getLogger(ChaseStatementParser.class.getName()).log(Level.SEVERE, null, ex);
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
    
    private static List<Transaction> findTransactions(Statement statement, List<String> lines)
    {
        List<RawTransaction> rawTransactions = new ArrayList<>();
        RawTransaction currentTransaction = null;
        
        for (String line : lines)
        {
            if (StringUtils.containsIgnoreCase(line, "payment thank you"))
            {
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
                // Not a date. Ignore.
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
            for (int i=1; i<parts.length-1; ++i)
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
                Logger.getLogger(ChaseStatementParser.class.getName()).log(Level.SEVERE, null, ex);
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
