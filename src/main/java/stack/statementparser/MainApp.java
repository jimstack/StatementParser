package stack.statementparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import static javafx.application.Platform.exit;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import static stack.statementparser.DateFormats.MONTH_DAY_FULL_YEAR_FORMAT;

public class MainApp extends Application
{
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    
    private List<SpendingCategory> spendingCategories;

    @Override
    public void start(Stage stage) throws Exception
    {
        Logger.getLogger("org.apache.pdfbox.pdmodel.font.PDSimpleFont").setLevel(Level.SEVERE);
        
        spendingCategories = new ArrayList<>();
        createSpendingCategories();
        
//        String directoryName = "D:\\Users\\JimStack\\Documents\\Divorce\\JuliaBankAccounts\\86-1054-4677";
//        String directoryName = "D:\\Users\\JimStack\\Documents\\Statements\\CreditCardStatements\\Amazon Visa";
//        String directoryName = "D:\\Users\\JimStack\\Documents\\Statements\\CreditCardStatements\\Disney Visa";
//        String directoryName = "D:\\Users\\JimStack\\Documents\\Statements\\CreditCardStatements\\AAA Visa";
//        String directoryName = "D:\\Users\\JimStack\\Documents\\Statements\\CreditCardStatements\\US Airways Visa";
        String directoryName = "D:\\Users\\JimStack\\Documents\\Statements\\Bank\\Reserve";
        String allTransactionsFileName = directoryName + "\\allTransactions.xls",
               transfersFileName = directoryName + "\\transfers.xls",
               transactionsByTypeFileName = directoryName + "\\transactionsByType.xls",
               transactionsByCategoryFileName = directoryName + "\\transactionsBySpendingCategory.xls";
        
        File directory = new File(directoryName);
        String[] files = directory.list();
        
        List<BankStatement> checkingAccountStatements = new ArrayList<>();
        List<CreditCardStatement> creditCardStatements = new ArrayList<>();
        for (int i=0; i<files.length; ++i)
        {
            String fileName = files[i];
            if (StringUtils.endsWithIgnoreCase(fileName, "pdf"))
            {
                checkingAccountStatements.add(PncStatementParser.parse(directory.getPath() + "\\" + fileName));
//                creditCardStatements.add(ChaseStatementParser.parse(directory.getPath() + "\\" + fileName));
//                creditCardStatements.add(BankOfAmericaStatementParser.parse(directory.getPath() + "\\" + fileName));
            }
        }
        
        writeSummaryData(creditCardStatements, directoryName + "\\StatementsSummary.xls");
        writeBankStatementSummaryData(checkingAccountStatements, directoryName + "\\StatementsSummary.xls");
        
//        for (String value : PncStatementParser.accountNumbers)
//        {
//            System.out.println(value);
//        }
        
        List<Transaction> allTransactions = new ArrayList<>(),
                          refundTransactions = new ArrayList<>(),
                          unhandledTransactions = new ArrayList<>();
        for (Statement statement : checkingAccountStatements)
        {
            System.out.println(statement.getFileName() + " " + statement.getStartDate() + " to " + statement.getEndDate());
            allTransactions.addAll(statement.allTransactionsProperty());
            for (Transaction transaction : statement.allTransactionsProperty())
            {
                if (TransactionType.purchases.contains(transaction.getTransactionType()))
                {
                    getTransactionListForType(transaction.getTransactionType()).add(transaction);

                    boolean transactionHandled = false;
                    for (SpendingCategory spendingCategory : spendingCategories)
                    {
                        if (!transactionHandled && spendingCategory.handle(transaction))
                        {
                            transactionHandled = true;
                        }
                    }
                    if (!transactionHandled)
                    {
                        unhandledTransactions.add(transaction);
                    }
                }
                else if (transaction.getTransactionType() == TransactionType.REFUND)
                {
                    refundTransactions.add(transaction);
                }
                
                if (TransactionType.transfers.contains(transaction.getTransactionType()))
                {
                    Transfer transfer = (Transfer) transaction;
                    TransferHistory history = getTransferHistory(statement.getAccountNumber(), transfer.getOtherAccount());
                    history.addTransfer(transfer);
                }
            }
        }
        
        Map<String, List<Transaction>> spendingCategoryToTransactionsMap = new HashMap<>();
        for (SpendingCategory category : spendingCategories)
        {
            List<Transaction> transactions = new ArrayList<>();
            for (TransactionGroup group : category.getTransactionGroups())
            {
                transactions.addAll(group.getTransactions());
            }
            spendingCategoryToTransactionsMap.put(category.getName(), transactions);
        }
        
        HSSFWorkbook workbook;
        
        // All Transactions
        {
            workbook = new HSSFWorkbook(); 
            WorkbookUtils.addSheet("All Transactions", allTransactions, workbook);

            try
            {
                FileOutputStream outputStream = new FileOutputStream(allTransactionsFileName);
                workbook.write(outputStream);
                workbook.close();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        // Transfers
        {
            workbook = new HSSFWorkbook(); 
            for (TransferHistory history : transferHistories.values())
            {
                WorkbookUtils.addSheet(history.getSecondaryAccount(), history.getTransfers(), workbook);
            }

            try
            {
                FileOutputStream outputStream = new FileOutputStream(transfersFileName);
                workbook.write(outputStream);
                workbook.close();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        // Transactions By Type
        {
            workbook = new HSSFWorkbook();
            for (Map.Entry<TransactionType, List<Transaction>> entry : transactionsMappedByType.entrySet())
            {
                WorkbookUtils.addSheet(entry.getKey().toString(), entry.getValue(), workbook);
            }

            try
            {
                FileOutputStream outputStream = new FileOutputStream(transactionsByTypeFileName);
                workbook.write(outputStream);
                workbook.close();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        
        // Transactions By Category
        {            
            workbook = new HSSFWorkbook();
            
            CellStyle numberCellStyle = workbook.createCellStyle();
            numberCellStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
        
            CellStyle underlinedCellStyle = workbook.createCellStyle();
            underlinedCellStyle.setBorderBottom(BorderStyle.THIN);
            
            CellStyle underlinedNumberCellStyle = workbook.createCellStyle();
            underlinedNumberCellStyle.cloneStyleFrom(numberCellStyle);
            underlinedNumberCellStyle.setBorderBottom(BorderStyle.THIN);
            
            Sheet summarySheet = workbook.createSheet("Summary");
            
            int rowNumber = 0;
            Row headerRow = summarySheet.createRow(rowNumber++);
            summarySheet.createFreezePane(0, 1);
            
            int columnNumber = 0; 
            headerRow.createCell(columnNumber++).setCellValue("Spending Category");
            headerRow.createCell(columnNumber++).setCellValue("Total Amount");

            Iterator<Cell> cellIterator = headerRow.cellIterator();
            while (cellIterator.hasNext())
            {
                cellIterator.next().setCellStyle(underlinedCellStyle);
            }
            
            List<String> categories = new ArrayList<>(spendingCategoryToTransactionsMap.keySet());
            Collections.sort(categories);
            
            Row row = null;
            for (String category : categories)
            {
                List<Transaction> categoryTansactions = spendingCategoryToTransactionsMap.get(category);
                
                double categoryTotal = 0.0;
                for (Transaction transaction : categoryTansactions)
                {
                    categoryTotal += NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue();
                }
                
                if (categoryTotal > 0)
                {
                    columnNumber = 0; 
                    row = summarySheet.createRow(rowNumber++);
                    row.createCell(columnNumber++).setCellValue(category);
                    formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(categoryTotal);       
                }
            }

            if (null != row)
            {
                cellIterator = row.cellIterator();
                while (cellIterator.hasNext())
                {
                    Cell cell = cellIterator.next();
                    if (cell.getCellTypeEnum() == CellType.NUMERIC)
                    {
                        cell.setCellStyle(underlinedNumberCellStyle);
                    }
                    else
                    {
                        cell.setCellStyle(underlinedCellStyle);
                    }
                }
            }
            
            columnNumber = 0; 
            row = summarySheet.createRow(rowNumber++);
            row.createCell(columnNumber++).setCellValue("Sum:");
            Cell cell = row.createCell(columnNumber++, CellType.FORMULA);
            cell.setCellFormula("SUM(B2:B" + (rowNumber-1) + ")");
            cell.setCellStyle(numberCellStyle);
            
            if (!refundTransactions.isEmpty())
            {
                double refundTotal = 0.0;
                for (Transaction transaction : refundTransactions)
                {
                    refundTotal += NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue();
                }
                
                if (refundTotal != 0)
                {
                    columnNumber = 0; 
                    row = summarySheet.createRow(rowNumber++); // Leave a blank row
                    row = summarySheet.createRow(rowNumber++);
                    row.createCell(columnNumber++).setCellValue("Refunds");
                    formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(refundTotal);       
                }
            }
        
            for (int i=0; i<columnNumber; ++i)
            {
                summarySheet.autoSizeColumn(i);
            }
            
            
            if (!refundTransactions.isEmpty())
            {
                WorkbookUtils.addSheet("Refunds", refundTransactions, workbook);
            }
            
            Collections.sort(spendingCategories, new Comparator<SpendingCategory>()
            {
                @Override
                public int compare(SpendingCategory o1, SpendingCategory o2)
                {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            
            for (SpendingCategory category : spendingCategories)
            {
                List<Transaction> transactions = new ArrayList<>();
                for (TransactionGroup group : category.getTransactionGroups())
                {
                    transactions.addAll(group.getTransactions());
                }
                WorkbookUtils.addSheet(category.getName(), transactions, workbook);
            }
            WorkbookUtils.addSheet("Unhandled Transactions", unhandledTransactions, workbook);

            try
            {
                FileOutputStream outputStream = new FileOutputStream(transactionsByCategoryFileName);
                workbook.write(outputStream);
                workbook.close();
            }
            catch (FileNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
//        for (TransferHistory history : transferHistories.values())
//        {
//            System.out.println(history.getNetTransfers() + ": " + history.getSecondaryAccount());
//        }
//        
//        System.out.println(count + " statements");
//        System.out.println("Visits: " + totalVisits);
//        System.out.println("Total cost: " + totalCost);
        
//        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Scene.fxml"));
//
//        Scene scene = new Scene(root);
//        scene.getStylesheets().add("/styles/Styles.css");
//
//        stage.setTitle("JavaFX and Maven");
//        stage.setScene(scene);
//        stage.show();
        exit();
    }
    
    private Cell formatNumberCell(Cell cell, CellStyle cellStyle)
    {
        cell.setCellType(CellType.NUMERIC);
        cell.setCellStyle(cellStyle);
        
        return cell;
    }
    
    private Cell formatCell(Cell cell, CellStyle cellStyle)
    {
        cell.setCellStyle(cellStyle);
        
        return cell;
    }
    
    private void writeSummaryData(List<CreditCardStatement> statements, String fileName)
    {
        HSSFWorkbook workbook = new HSSFWorkbook();
        int rowNumber = 0;
        HSSFSheet sheet = workbook.createSheet("Summary of Statements");
        Row headerRow = sheet.createRow(rowNumber++);
        sheet.createFreezePane(0, 1);

        int columnNumber = 0; 
        headerRow.createCell(columnNumber++).setCellValue("Start Date");
        headerRow.createCell(columnNumber++).setCellValue("End Date");
        headerRow.createCell(columnNumber++).setCellValue("Previous Balance");
        headerRow.createCell(columnNumber++).setCellValue("Payments/Credits");
        headerRow.createCell(columnNumber++).setCellValue("Purchases");
        headerRow.createCell(columnNumber++).setCellValue("Cash Advances");
        headerRow.createCell(columnNumber++).setCellValue("Balance Transfers");
        headerRow.createCell(columnNumber++).setCellValue("Fees Charged");
        headerRow.createCell(columnNumber++).setCellValue("Interest Charged");
        headerRow.createCell(columnNumber++).setCellValue("New Balance");
        
        
        CellStyle numberCellStyle = workbook.createCellStyle();
        numberCellStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
        
        CellStyle dateCellStyle = workbook.createCellStyle();
        dateCellStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("MM/DD/YYYY"));

        int totalColumnCount = columnNumber;
        Row row = null;
        
        Collections.sort(statements, new Comparator<CreditCardStatement>()
        {
            @Override
            public int compare(CreditCardStatement o1, CreditCardStatement o2)
            {
                try
                {
                    Date date1 = MONTH_DAY_FULL_YEAR_FORMAT.parse(o1.getStartDate()),
                         date2 = MONTH_DAY_FULL_YEAR_FORMAT.parse(o2.getStartDate());
                    
                    return date1.compareTo(date2);
                }
                catch (ParseException e)
                {
                    
                }
                
                return 0;
            }
        });
        for (CreditCardStatement statement : statements)
        {
            row = sheet.createRow(rowNumber++);

            columnNumber = 0; 
            formatCell(row.createCell(columnNumber++), dateCellStyle).setCellValue(statement.getStartDate());
            formatCell(row.createCell(columnNumber++), dateCellStyle).setCellValue(statement.getEndDate());           
            
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getBeginningBalance());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getPaymentsAndCredits());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getPurchases());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getCashAdvances());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getBalanceTransfers());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getFeesCharged());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getInterestCharged());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getEndingBalance());
        }
        
        if (null == row)
        {
            return;
        }
        
        CellStyle underlinedCellStyle = workbook.createCellStyle();
        underlinedCellStyle.setBorderBottom(BorderStyle.THIN);

        CellStyle underlinedNumberCellStyle = workbook.createCellStyle();
        underlinedNumberCellStyle.cloneStyleFrom(numberCellStyle);
        underlinedNumberCellStyle.setBorderBottom(BorderStyle.THIN);
        
        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext())
        {
            cellIterator = row.cellIterator();
            while (cellIterator.hasNext())
            {
                Cell cell = cellIterator.next();
                if (cell.getCellTypeEnum() == CellType.NUMERIC)
                {
                    cell.setCellStyle(underlinedNumberCellStyle);
                }
                else
                {
                    cell.setCellStyle(underlinedCellStyle);
                }
            }
        }
        
//        ++columnNumber; // Leave a blank column
//        headerRow.createCell(columnNumber++).setCellValue("Sum:");
//        headerRow.createCell(columnNumber++, CellType.FORMULA).setCellFormula("SUM(B2:B" + (rowNumber+1) + ")");

        row = sheet.createRow(rowNumber++);
        for (int i=3; i<totalColumnCount-1; ++i)
        {
            String columnLetter = CellReference.convertNumToColString(i);
            String formula = "SUM(" + columnLetter + "2:" + columnLetter + (rowNumber-1) + ")";
           Cell cell =  row.createCell(i, CellType.FORMULA);
           cell.setCellFormula(formula);
           cell.setCellStyle(numberCellStyle);
        }
        
        for (int i=0; i<totalColumnCount; ++i)
        {
            sheet.autoSizeColumn(i);
        }

        try
        {
            FileOutputStream outputStream = new FileOutputStream(fileName);
            workbook.write(outputStream);
            workbook.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private void writeBankStatementSummaryData(List<BankStatement> statements, String fileName)
    {
        HSSFWorkbook workbook = new HSSFWorkbook();
        int rowNumber = 0;
        HSSFSheet sheet = workbook.createSheet("Summary of Statements");
        Row headerRow = sheet.createRow(rowNumber++);
        sheet.createFreezePane(0, 1);

        int columnNumber = 0; 
        headerRow.createCell(columnNumber++).setCellValue("Start Date");
        headerRow.createCell(columnNumber++).setCellValue("End Date");
        headerRow.createCell(columnNumber++).setCellValue("Beginning Balance");
        headerRow.createCell(columnNumber++).setCellValue("All Deposits");
        headerRow.createCell(columnNumber++).setCellValue("Jim Paycheck");
        headerRow.createCell(columnNumber++).setCellValue("Julia Paycheck");
        headerRow.createCell(columnNumber++).setCellValue("ATM Withdrawals");
        headerRow.createCell(columnNumber++).setCellValue("Online Payments");
        headerRow.createCell(columnNumber++).setCellValue("Transferred In");
        headerRow.createCell(columnNumber++).setCellValue("Transferred Out");
        headerRow.createCell(columnNumber++).setCellValue("Ending Balance");
        
        
        CellStyle numberCellStyle = workbook.createCellStyle();
        numberCellStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));
        
        CellStyle dateCellStyle = workbook.createCellStyle();
        dateCellStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("MM/DD/YYYY"));

        int totalColumnCount = columnNumber;
        Row row = null;
        
        Collections.sort(statements, new Comparator<BankStatement>()
        {
            @Override
            public int compare(BankStatement o1, BankStatement o2)
            {
                try
                {
                    Date date1 = MONTH_DAY_FULL_YEAR_FORMAT.parse(o1.getStartDate()),
                         date2 = MONTH_DAY_FULL_YEAR_FORMAT.parse(o2.getStartDate());
                    
                    return date1.compareTo(date2);
                }
                catch (ParseException e)
                {
                    
                }
                
                return 0;
            }
        });
        for (BankStatement statement : statements)
        {
            row = sheet.createRow(rowNumber++);

            columnNumber = 0; 
            formatCell(row.createCell(columnNumber++), dateCellStyle).setCellValue(statement.getStartDate());
            formatCell(row.createCell(columnNumber++), dateCellStyle).setCellValue(statement.getEndDate());           
            
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getBeginningBalance());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getAllDeposits());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getJimSalaryDeposits());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getJuliaSalaryDeposits());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getAtmWithdrawals());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getOnlinePayments());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getTransferredIn());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getTransferredOut());
            formatNumberCell(row.createCell(columnNumber++), numberCellStyle).setCellValue(statement.getEndingBalance());
        }
        
        if (null == row)
        {
            return;
        }
        
        CellStyle underlinedCellStyle = workbook.createCellStyle();
        underlinedCellStyle.setBorderBottom(BorderStyle.THIN);

        CellStyle underlinedNumberCellStyle = workbook.createCellStyle();
        underlinedNumberCellStyle.cloneStyleFrom(numberCellStyle);
        underlinedNumberCellStyle.setBorderBottom(BorderStyle.THIN);
        
        Iterator<Cell> cellIterator = row.cellIterator();
        while (cellIterator.hasNext())
        {
            cellIterator = row.cellIterator();
            while (cellIterator.hasNext())
            {
                Cell cell = cellIterator.next();
                if (cell.getCellTypeEnum() == CellType.NUMERIC)
                {
                    cell.setCellStyle(underlinedNumberCellStyle);
                }
                else
                {
                    cell.setCellStyle(underlinedCellStyle);
                }
            }
        }
        
//        ++columnNumber; // Leave a blank column
//        headerRow.createCell(columnNumber++).setCellValue("Sum:");
//        headerRow.createCell(columnNumber++, CellType.FORMULA).setCellFormula("SUM(B2:B" + (rowNumber+1) + ")");

        row = sheet.createRow(rowNumber++);
        for (int i=3; i<totalColumnCount-1; ++i)
        {
            String columnLetter = CellReference.convertNumToColString(i);
            String formula = "SUM(" + columnLetter + "2:" + columnLetter + (rowNumber-1) + ")";
           Cell cell =  row.createCell(i, CellType.FORMULA);
           cell.setCellFormula(formula);
           cell.setCellStyle(numberCellStyle);
        }
        
        for (int i=0; i<totalColumnCount; ++i)
        {
            sheet.autoSizeColumn(i);
        }

        try
        {
            FileOutputStream outputStream = new FileOutputStream(fileName);
            workbook.write(outputStream);
            workbook.close();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    private Map<TransactionType, List<Transaction>> transactionsMappedByType = new HashMap<>();
    private List<Transaction> getTransactionListForType(TransactionType type)
    {
        List<Transaction> transactions = transactionsMappedByType.get(type);        
        if (null == transactions)
        {
            transactions = new ArrayList<>();
            transactionsMappedByType.put(type, transactions);
        }
        
        return transactions;
    }
    
    private Map<Long, TransferHistory> transferHistories = new HashMap<>();
    private TransferHistory getTransferHistory(String primaryAccount, String secondaryAccount)
    {
        String trimmedAccount = secondaryAccount.replaceFirst("^0+(?!$)", "");
        Long key;
        try
        {
            key = Long.parseLong(trimmedAccount);
        }
        catch (NumberFormatException e)
        {
            int foo = 0;
            throw e;
        }
        
        TransferHistory history = transferHistories.get(key);
        if (null == history)
        {
            history = new TransferHistory(primaryAccount, secondaryAccount);
            transferHistories.put(key, history);
        }
        
        return history;
    }
    
    private void createSpendingCategories()
    {
        SpendingCategory spendingCategory;
        TransactionGroup transactionGroup;
        
        // Family
        spendingCategory = new SpendingCategory("Family Entertainment");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Parks");
            transactionGroup.addDelimiter("hershey");
            transactionGroup.addDelimiter("WDW");
            transactionGroup.addDelimiter("disney resort");
            transactionGroup.addDelimiter("world of disney");
            transactionGroup.addDelimiter("canteen vending");
            transactionGroup.addDelimiter("maestro mickey");
            transactionGroup.addDelimiter("tuskers");
            transactionGroup.addDelimiter("philadelphia zoo");
            transactionGroup.addDelimiter("USAIRWAYS 0372327535620 800-428-4322 AZ");
            transactionGroup.addDelimiter("USAIRWAYS 0372327535621 800-428-4322 AZ");
            transactionGroup.addDelimiter("USAIRWAYS 0372327535622 800-428-4322 AZ");
            transactionGroup.addDelimiter("USAIRWAYS 0372327535623 800-428-4322 AZ");
            transactionGroup.addDelimiter("USAIRWAYS 0372367761");
            spendingCategory.addTransactionGroup(transactionGroup);
            
            transactionGroup = new TransactionGroup("Color Run");
            transactionGroup.addDelimiter("color run");
            spendingCategory.addTransactionGroup(transactionGroup);
            
            transactionGroup = new TransactionGroup("Movies");
            transactionGroup.addDelimiter("uec theatre");
            spendingCategory.addTransactionGroup(transactionGroup);
            
            transactionGroup = new TransactionGroup("Travel");
            transactionGroup.addDelimiter("towneplace suites wilmgtn");
            transactionGroup.addDelimiter("grotto pizza");
            spendingCategory.addTransactionGroup(transactionGroup);            
            
            transactionGroup = new TransactionGroup("Misc");
            transactionGroup.addDelimiter("northland");
            transactionGroup.addDelimiter("c.r. parks"); // pool passes
            transactionGroup.addDelimiter("crayola experience");
            transactionGroup.addDelimiter("SHELL OIL 57526140801 NEWARK DE");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End family
        
        // Julia's Personal Spending
        spendingCategory = new SpendingCategory("Julia Personal Spending");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Hair Salon");
            transactionGroup.addDelimiter("j stephens");
            transactionGroup.addDelimiter("adam cole");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Wen");
            transactionGroup.addDelimiter("chaz");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Mario Badescu");
            transactionGroup.addDelimiter("mariobadescu");
            transactionGroup.addDelimiter("mario badescu");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("MAC");
            transactionGroup.addDelimiter(" mac ");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Clothing");
            transactionGroup.addDelimiter("ll bean");
            transactionGroup.addDelimiter("clothes mentor");
            transactionGroup.addDelimiter("white house");
            transactionGroup.addDelimiter("stylistwear");
            transactionGroup.addDelimiter("aramark");
            transactionGroup.addDelimiter("crocs");
            transactionGroup.addDelimiter("j crew");
            transactionGroup.addDelimiter("j. crew");
            transactionGroup.addDelimiter("people's nation");
            transactionGroup.addDelimiter("rapid transit");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Accessories");
            transactionGroup.addDelimiter("talbots");
            transactionGroup.addDelimiter("lilly pulitzer");
            transactionGroup.addDelimiter("zulily");
            transactionGroup.addDelimiter("coach");
            transactionGroup.addDelimiter("petunia");
            transactionGroup.addDelimiter("jules artwear");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Car Wash");
            transactionGroup.addDelimiter("team blue");
            transactionGroup.addDelimiter("redline");
            transactionGroup.addDelimiter("auto wash");
            transactionGroup.addDelimiter("car wash");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("WP Fee");
            transactionGroup.addDelimiter("wp-fee");
            transactionGroup.addDelimiter("wpchrg");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Easy Saver");
            transactionGroup.addDelimiter("easy saver");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Misc");
            transactionGroup.addDelimiter("qvc");
            transactionGroup.addDelimiter("crate & barrel");
            transactionGroup.addDelimiter("SUNGLASS HUT 60047289 LEESBURG VA");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End Julia's personal spending
        
        spendingCategory = new SpendingCategory("Uncategorized");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Kohls");
            transactionGroup.addDelimiter("kohls");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Weight Watchers");
            transactionGroup.addDelimiter("weightwatchers");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("University of Pennsylvania");
            transactionGroup.addDelimiter("university of penn");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Rubber Queen");
            transactionGroup.addDelimiter("rubberqueen");
            spendingCategory.addTransactionGroup(transactionGroup);
            
            transactionGroup = new TransactionGroup("Misc");
            transactionGroup.addDelimiter("redbox");
            transactionGroup.addDelimiter("disneyshopping");
            transactionGroup.addDelimiter("1-800-flowers");
            transactionGroup.addDelimiter("staples");
            transactionGroup.addDelimiter("a flower basket");
            transactionGroup.addDelimiter("siriusxm");
            transactionGroup.addDelimiter("md dist court");
            transactionGroup.addDelimiter("mclanahan's");
            transactionGroup.addDelimiter("gymboree");
            transactionGroup.addDelimiter("carter's");
            transactionGroup.addDelimiter("rps");
            transactionGroup.addDelimiter("disney store");
            transactionGroup.addDelimiter("stride rite");
            transactionGroup.addDelimiter("brookstone");
            transactionGroup.addDelimiter("macy's");
            transactionGroup.addDelimiter("macy*s");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        
        // Jim personal spending
        spendingCategory = new SpendingCategory("Jim Personal Spending");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Hair");
            transactionGroup.addDelimiter("holiday hair");
            spendingCategory.addTransactionGroup(transactionGroup);
            
            transactionGroup = new TransactionGroup("Massage");
            transactionGroup.addDelimiter("artemis");
            spendingCategory.addTransactionGroup(transactionGroup);
            
            transactionGroup = new TransactionGroup("Clothes");
            transactionGroup.addDelimiter("jos. a. bank");
            transactionGroup.addDelimiter("jos a bank");
            spendingCategory.addTransactionGroup(transactionGroup);
            
            transactionGroup = new TransactionGroup("Hobbies");
            transactionGroup.addDelimiter("scuba");
            transactionGroup.addDelimiter("PADI");
            transactionGroup.addDelimiter("lehigh valley dive center");
            transactionGroup.addDelimiter("las americas");
            transactionGroup.addDelimiter("urban bar & grill");
            transactionGroup.addDelimiter("adobe");
            transactionGroup.addDelimiter("internet radio");
            spendingCategory.addTransactionGroup(transactionGroup);
            
            transactionGroup = new TransactionGroup("Misc");
            transactionGroup.addDelimiter("linkedin");
            transactionGroup.addDelimiter("samsung telecom");
            transactionGroup.addDelimiter("geohot");
            transactionGroup.addDelimiter("wired magazine");
            transactionGroup.addDelimiter("USAIRWAYS 0372322266196");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End Jim Personal Spending
        
        // Jim Christmas gift for Julia
        spendingCategory = new SpendingCategory("Jim Christmas Gift to Julia");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Gift");
            transactionGroup.addDelimiter("apple online");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End Christmas gift for Julia
        
        // Amazon
        spendingCategory = new SpendingCategory("Amazon");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Amazon");
            transactionGroup.addDelimiter("amazon");
            transactionGroup.addDelimiter("amzn");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End amazon
        
        // Apple
        spendingCategory = new SpendingCategory("Tablet and Smart Phone Apps");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Apple");
            transactionGroup.addDelimiter("itunes");
            spendingCategory.addTransactionGroup(transactionGroup);
            
            transactionGroup = new TransactionGroup("Google");
            transactionGroup.addDelimiter("google");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End Apple
        
        // Work Travel
        spendingCategory = new SpendingCategory("Jim Work Travel");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Flights");
            transactionGroup.addDelimiter("USAIRWAYS 0372323230784 800-428-4322 AZ");
            transactionGroup.addDelimiter("USAIRWAYS 0372323230785 800-428-4322 AZ");
            transactionGroup.addDelimiter("USAIRWAYS 0372348017057 800-428-4322 AZ");
            transactionGroup.addDelimiter("USAIRWAYS 0372360157165");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Hotels");
            transactionGroup.addDelimiter("radisson");
            transactionGroup.addDelimiter("courtyard");
            transactionGroup.addDelimiter("springhill suites belair");
            transactionGroup.addDelimiter("springhill suites -reston");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Expenses");
            transactionGroup.addDelimiter("bel air");
            transactionGroup.addDelimiter("belcamp");
            transactionGroup.addDelimiter("aberdeen");
            transactionGroup.addDelimiter("white marsh");
            transactionGroup.addDelimiter("oxford");
            transactionGroup.addDelimiter("baltimore");
            transactionGroup.addDelimiter("augusta");
            transactionGroup.addDelimiter("atlanta");
            transactionGroup.addDelimiter("ft gordon");
            transactionGroup.addDelimiter("grove town");
            transactionGroup.addDelimiter("grovetown");
            transactionGroup.addDelimiter("orlando");
            transactionGroup.addDelimiter("hodads");
            transactionGroup.addDelimiter("livingsocial");
            transactionGroup.addDelimiter("irving's at the airpo");
            transactionGroup.addDelimiter("auntie ann's philadephia");
            transactionGroup.addDelimiter("aubonpain");
            transactionGroup.addDelimiter("auntie anne's philadelphia");
            transactionGroup.addDelimiter("coca cola hanover");
            transactionGroup.addDelimiter("mclean");
            transactionGroup.addDelimiter("monster mini golf");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End work travel
        
        // Crafting expenses
        spendingCategory = new SpendingCategory("Julia Crafting");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Stampin' Up");
            transactionGroup.addDelimiter("stampin");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("mysbf");
            transactionGroup.addDelimiter("mysbf");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Paper Source");
            transactionGroup.addDelimiter("paper source");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Marketing - A Weber");
            transactionGroup.addDelimiter("aweber");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Marketing - John Sanpietro");
            transactionGroup.addDelimiter("john sanpietro");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Catherine Pooler");
            transactionGroup.addDelimiter("catherine pooler");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Market Research - Mojoness");
            transactionGroup.addDelimiter("mojoness");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Factory Card Outlet");
            transactionGroup.addDelimiter("factory card outlet");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Michael's");
            transactionGroup.addDelimiter("michaels");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Shiping");
            transactionGroup.addDelimiter("usps");
            transactionGroup.addDelimiter("fedex");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Barclay's Card");
            transactionGroup.addDelimiter("barclays");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Citi Card");
            transactionGroup.addDelimiter("citifinancial");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End crafting expenses
        
        // Gardening expenses
        spendingCategory = new SpendingCategory("Julia Gardening");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Sammis Greenhouse");
            transactionGroup.addDelimiter("sammis greenhouse");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Johnny's Selected Seed");
            transactionGroup.addDelimiter("johnny's selected seed");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("College Gardens");
            transactionGroup.addDelimiter("college gardens");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Burpee Seeds");
            transactionGroup.addDelimiter("burpee seed");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End gardening expenses
        
        // Home expenses        
        spendingCategory = new SpendingCategory("Household Expenses");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Mortgage");
            transactionGroup.addDelimiter("chase mtge");
            transactionGroup.addDelimiter("first horizon");
            transactionGroup.addDelimiter("metlife");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Electric");
            transactionGroup.addDelimiter("allegheny pwr");
            transactionGroup.addDelimiter("wpennpwr");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Car Insurance");
            transactionGroup.addDelimiter("state farm");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Life Insurance");
            transactionGroup.addDelimiter("northwestern");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Cell Phone");
            transactionGroup.addDelimiter("at&t");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Cable/Internet");
            transactionGroup.addDelimiter("comcast");
            transactionGroup.addDelimiter("tivo");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Trash");
            transactionGroup.addDelimiter("veolia");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Newspaper");
            transactionGroup.addDelimiter("centre daily");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Medical");
            transactionGroup.addDelimiter("grays woods");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Mulch");
            transactionGroup.addDelimiter("nature's cover");
            transactionGroup.addDelimiter("natures cover");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Day Care");
            transactionGroup.addDelimiter("Christ Community");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Soccer Shots");
            transactionGroup.addDelimiter("soccer shots");
            transactionGroup.addDelimiter("soccershots");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Misc");
            transactionGroup.addDelimiter("ez pass");
            transactionGroup.addDelimiter("cleaners 4 less");
            transactionGroup.addDelimiter("aaasp mbr dues");
            transactionGroup.addDelimiter("marraras");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End home expenses
        
        // Credit Cards
        spendingCategory = new SpendingCategory("Credit Card Payments");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Chase Credit Cards");
            transactionGroup.addDelimiter("chase card");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Bank of America");
            transactionGroup.addDelimiter("bank of america");
            transactionGroup.addDelimiter("bk of amer");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End credit cards
        
        // Pet expenses
        spendingCategory = new SpendingCategory("Pet Expenses");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Vet Care");
            transactionGroup.addDelimiter("mt nittany vet");
            transactionGroup.addDelimiter("cp vets");
            transactionGroup.addDelimiter("rdc veterinary");
            transactionGroup.addDelimiter("brandys veterinary");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Pet Supplies");
            transactionGroup.addDelimiter("wiscoy");
            transactionGroup.addDelimiter("petco");
            transactionGroup.addDelimiter("braxton");
            transactionGroup.addDelimiter("thatpetplace");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End pet expenses

        // Cars
        spendingCategory = new SpendingCategory("Car-Related Expenses");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("BMW Loan");
            transactionGroup.addDelimiter("bmwfs");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Maintenance/Repairs");
            transactionGroup.addDelimiter("joel confer");
            transactionGroup.addDelimiter("autowerkes");
            transactionGroup.addDelimiter("lmr tires");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("PA Driver/Vehicle Services");
            transactionGroup.addDelimiter("pa driver");
            spendingCategory.addTransactionGroup(transactionGroup);
            
            transactionGroup = new TransactionGroup("Parts");
            transactionGroup.addDelimiter("pepboys");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End cars
        
        // Gas stations
        spendingCategory = new SpendingCategory("Gas Stations");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Exxon");
            transactionGroup.addDelimiter("exxon");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Sheetz");
            transactionGroup.addDelimiter("sheetz");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("WaWa");
            transactionGroup.addDelimiter("wawa");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Sunoco");
            transactionGroup.addDelimiter("sunoco");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Gulf");
            transactionGroup.addDelimiter("gulf");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Hess");
            transactionGroup.addDelimiter("hess");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Lyken's");
            transactionGroup.addDelimiter("lykens");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Royal Farms");
            transactionGroup.addDelimiter("royal farms");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End gas stations
        
        spendingCategory = new SpendingCategory("Home Improvement");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Home Improvement");
            transactionGroup.addDelimiter("lowes");
            transactionGroup.addDelimiter("home depot");
            transactionGroup.addDelimiter("homedepot");
            transactionGroup.addDelimiter("triangle bldr");
            transactionGroup.addDelimiter("triangle building");
            transactionGroup.addDelimiter("sherwin williams");
            transactionGroup.addDelimiter("schnecksville tv");
            spendingCategory.addTransactionGroup(transactionGroup);
            
            transactionGroup = new TransactionGroup("Moving");
            transactionGroup.addDelimiter("u-haul");
            transactionGroup.addDelimiter("322 self storage");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        
        spendingCategory = new SpendingCategory("Groceries");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Groceries");
            transactionGroup.addDelimiter("weis");
            transactionGroup.addDelimiter("wegmans");
            transactionGroup.addDelimiter("az metro");
            transactionGroup.addDelimiter(" giant ");
            transactionGroup.addDelimiter("giant 6111");
            transactionGroup.addDelimiter("giant 6072");
            transactionGroup.addDelimiter("acme");
            transactionGroup.addDelimiter("trader joe's");
            transactionGroup.addDelimiter("patchwork farm");
            transactionGroup.addDelimiter("wild for salmon");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        
        spendingCategory = new SpendingCategory("Beer and Liquor Stores");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Beer/Wine");
            transactionGroup.addDelimiter("wine & spirits");
            transactionGroup.addDelimiter("nastase");
            transactionGroup.addDelimiter("wr hickey");
            transactionGroup.addDelimiter("wine world");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        
        spendingCategory = new SpendingCategory("General Shopping");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("General Shopping");
            transactionGroup.addDelimiter("wal-mart");
            transactionGroup.addDelimiter("walmart");
            transactionGroup.addDelimiter("wm supercenter");
            transactionGroup.addDelimiter("target");
            transactionGroup.addDelimiter("kmart");
            transactionGroup.addDelimiter("rite aid");
            transactionGroup.addDelimiter("cvs");
            transactionGroup.addDelimiter("best buy");
            transactionGroup.addDelimiter("bed bath & beyond");
            transactionGroup.addDelimiter("dollar general");
            transactionGroup.addDelimiter("dollar-general");
            transactionGroup.addDelimiter("walgreens");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        
        spendingCategory = new SpendingCategory("Eating Out");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Eating Out");
            transactionGroup.addDelimiter("five guys");
            transactionGroup.addDelimiter("pizza mia");
            transactionGroup.addDelimiter("hi way pizza");
            transactionGroup.addDelimiter("burger king");
            transactionGroup.addDelimiter("chick-fil-a");
            transactionGroup.addDelimiter("domino's");
            transactionGroup.addDelimiter("subway");
            transactionGroup.addDelimiter("dunkin");
            transactionGroup.addDelimiter("champs");
            transactionGroup.addDelimiter("mcdonald's");
            transactionGroup.addDelimiter("pro sports catering");
            transactionGroup.addDelimiter("duclaw");
            transactionGroup.addDelimiter("champs");
            transactionGroup.addDelimiter("chipotle");
            transactionGroup.addDelimiter("mt nittany inn");
            transactionGroup.addDelimiter("assante");
            transactionGroup.addDelimiter("happy valley brew");
            transactionGroup.addDelimiter("dp dough");
            transactionGroup.addDelimiter("wendys");
            transactionGroup.addDelimiter("wendy's");
            transactionGroup.addDelimiter("bonfattos");
            transactionGroup.addDelimiter("the olive");
            transactionGroup.addDelimiter("bellefonte wok");
            transactionGroup.addDelimiter("roly poly");
            transactionGroup.addDelimiter("Appalachian Brewing Co");
            transactionGroup.addDelimiter("Otto's");
            transactionGroup.addDelimiter("olde new york");
            transactionGroup.addDelimiter("the naked egg");
            transactionGroup.addDelimiter("cracker barrel");
            transactionGroup.addDelimiter("quaker steak");
            transactionGroup.addDelimiter("dairy queen");
            transactionGroup.addDelimiter("dickeys");
            transactionGroup.addDelimiter("luna 2");
            transactionGroup.addDelimiter("kaarma");
            transactionGroup.addDelimiter("outback");
            transactionGroup.addDelimiter("the corner room");
            transactionGroup.addDelimiter("harris teeter");
            transactionGroup.addDelimiter("jays crabshack");
            transactionGroup.addDelimiter("red lobster");
            transactionGroup.addDelimiter("the tavern");
            transactionGroup.addDelimiter("home delivery pizza");
            transactionGroup.addDelimiter("basta pasta");
            transactionGroup.addDelimiter("jersey mike's");
            transactionGroup.addDelimiter("philly soft pretzel");
            transactionGroup.addDelimiter("west side stadium");
            transactionGroup.addDelimiter("melt down grilled cheese");
            transactionGroup.addDelimiter("villa lentini");
            transactionGroup.addDelimiter("american ale house");            
            transactionGroup.addDelimiter("wholefds");
            transactionGroup.addDelimiter("cafe bonapart");
            transactionGroup.addDelimiter("paddy's beach");
            transactionGroup.addDelimiter("cafe on the park");
            transactionGroup.addDelimiter("brazen hen");
            transactionGroup.addDelimiter("malted barley");
            transactionGroup.addDelimiter("petes famous pizza");
            transactionGroup.addDelimiter("la fiesta sandwiches");
            transactionGroup.addDelimiter("papa john's");
            transactionGroup.addDelimiter("charley's");
            transactionGroup.addDelimiter("pinkberry");            
            transactionGroup.addDelimiter("corner bakery");
            transactionGroup.addDelimiter("bayou bakery");          
            transactionGroup.addDelimiter("two amys");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        launch(args);
    }

}
