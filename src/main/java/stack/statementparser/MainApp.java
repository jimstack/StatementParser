package stack.statementparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import static javafx.application.Platform.exit;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class MainApp extends Application
{
    
    private List<SpendingCategory> spendingCategories;

    @Override
    public void start(Stage stage) throws Exception
    {
        Logger.getLogger("org.apache.pdfbox.pdmodel.font.PDSimpleFont").setLevel(Level.SEVERE);
        
        spendingCategories = new ArrayList<>();
        createSpendingCategories();
        
        String directoryName = "D:\\Users\\JimStack\\Documents\\Divorce\\JuliaBankAccounts\\10-1716-2828";
        String allTransactionsFileName = directoryName + "\\allTransactions.xls",
               transfersFileName = directoryName + "\\transfers.xls",
               transactionsByTypeFileName = directoryName + "\\transactionsByType.xls",
               transactionsByCategoryFileName = directoryName + "\\transactionsBySpendingCategory.xls";
        
        File directory = new File(directoryName);
        String[] files = directory.list();
        
        List<BankStatement> checkingAccountStatements = new ArrayList<>();
        for (int i=0; i<files.length; ++i)
        {
            String fileName = files[i];
            if (StringUtils.endsWithIgnoreCase(fileName, "pdf"))
            {
                checkingAccountStatements.add(PncStatementParser.parse(directory.getPath() + "\\" + fileName));
            }
        }
        
        List<Transaction> allTransactions = new ArrayList<>(),
                          unhandledTransactions = new ArrayList<>();
        for (BankStatement statement : checkingAccountStatements)
        {
            System.out.println(statement.getFileName() + " " + statement.getStartDate() + " to " + statement.getEndDate());
            allTransactions.addAll(statement.getAllTransactions());
            for (Transaction transaction : statement.getAllTransactions())
            {
                if (TransactionType.purchases.contains(transaction.getTransactionType()))
                {
                    getTransactionListForType(transaction.getTransactionType()).add(transaction);

                    boolean transactionHandled = false;
                    for (SpendingCategory spendingCategory : spendingCategories)
                    {
                        if (spendingCategory.handle(transaction))
                        {
                            transactionHandled = true;
                        }
                    }
                    if (!transactionHandled)
                    {
                        unhandledTransactions.add(transaction);
                    }
                }
                
                if (TransactionType.transfers.contains(transaction.getTransactionType()))
                {
                    Transfer transfer = (Transfer) transaction;
                    TransferHistory history = getTransferHistory(statement.getAccountNumber(), transfer.getOtherAccount());
                    history.addTransfer(transfer);
                }
            }
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
        Long key = key = Long.parseLong(trimmedAccount);
        
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
        
        // Julia's Personal Spending
        spendingCategory = new SpendingCategory("Julia Personal Spending");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("J. Stepehens");
            transactionGroup.addDelimiter("j stephens");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Adam Cole Sa;on");
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

            transactionGroup = new TransactionGroup("J Crew");
            transactionGroup.addDelimiter("j crew");
            transactionGroup.addDelimiter("j. crew");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Talbots");
            transactionGroup.addDelimiter("talbots");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Lilly Pulitzer");
            transactionGroup.addDelimiter("lilly pulitzer");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Zulily");
            transactionGroup.addDelimiter("zulily");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Coach");
            transactionGroup.addDelimiter("coach");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Petunia Pickle Bottom");
            transactionGroup.addDelimiter("petunia");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("LL Bean");
            transactionGroup.addDelimiter("ll bean");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Clothes Mentor");
            transactionGroup.addDelimiter("clothes mentor");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("White House Black Market");
            transactionGroup.addDelimiter("white house");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Car Wash");
            transactionGroup.addDelimiter("team blue");
            transactionGroup.addDelimiter("redline");
            transactionGroup.addDelimiter("auto wash");
            transactionGroup.addDelimiter("car wash");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Rapid Transit");
            transactionGroup.addDelimiter("rapid transit");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Work Clothes");
            transactionGroup.addDelimiter("stylistwear");
            transactionGroup.addDelimiter("aramark");
            transactionGroup.addDelimiter("crocs");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("WP Fee");
            transactionGroup.addDelimiter("wp-fee");
            transactionGroup.addDelimiter("wpchrg");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Easy Saver");
            transactionGroup.addDelimiter("easy saver");
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
        }
        
        // Crafting expenses
        spendingCategory = new SpendingCategory("Crafting");
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
        spendingCategory = new SpendingCategory("Gardening");
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
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Trash");
            transactionGroup.addDelimiter("veolia");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Newspaper");
            transactionGroup.addDelimiter("centre daily");
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
            transactionGroup = new TransactionGroup("Mt Nittany Vet");
            transactionGroup.addDelimiter("mt nittany vet");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("CP Vets");
            transactionGroup.addDelimiter("cp vets");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("RDC Veterinary");
            transactionGroup.addDelimiter("rdc veterinary");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Wiscoy");
            transactionGroup.addDelimiter("wiscoy");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Petco");
            transactionGroup.addDelimiter("petco");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Braxton's");
            transactionGroup.addDelimiter("braxton");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        // End pet expenses

        // Cars
        spendingCategory = new SpendingCategory("Cars");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("BMW Loan");
            transactionGroup.addDelimiter("bmwfs");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("Joel Confer");
            transactionGroup.addDelimiter("joel confer");
            spendingCategory.addTransactionGroup(transactionGroup);

            transactionGroup = new TransactionGroup("PA Driver/Vehicle Services");
            transactionGroup.addDelimiter("pa driver");
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
            transactionGroup.addDelimiter("triangle bldr");
            transactionGroup.addDelimiter("triangle building");
            transactionGroup.addDelimiter("sherwin williams");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        
        spendingCategory = new SpendingCategory("Groceries");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Groceries");
            transactionGroup.addDelimiter("weis");
            transactionGroup.addDelimiter("wegmans");
            transactionGroup.addDelimiter(" giant ");
            transactionGroup.addDelimiter("acme");
            transactionGroup.addDelimiter("trader joe's");
            transactionGroup.addDelimiter("patchwork farm");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        
        spendingCategory = new SpendingCategory("Alcohol");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Beer/Wine");
            transactionGroup.addDelimiter("wine & spirits");
            transactionGroup.addDelimiter("nastase");
            transactionGroup.addDelimiter("wr hickey");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        
        spendingCategory = new SpendingCategory("General Shopping");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("General Shopping");
            transactionGroup.addDelimiter("wal-mart");
            transactionGroup.addDelimiter("wm supercenter");
            transactionGroup.addDelimiter("target");
            transactionGroup.addDelimiter("rite aid");
            transactionGroup.addDelimiter("cvs");
            transactionGroup.addDelimiter("best buy");
            transactionGroup.addDelimiter("bed bath & beyond");
            spendingCategory.addTransactionGroup(transactionGroup);
        }
        
        spendingCategory = new SpendingCategory("Eating Out");
        spendingCategories.add(spendingCategory);
        {
            transactionGroup = new TransactionGroup("Eating Out");
            transactionGroup.addDelimiter("pizza mia");
            transactionGroup.addDelimiter("burger king");
            transactionGroup.addDelimiter("chick-fil-a");
            transactionGroup.addDelimiter("domino's");
            transactionGroup.addDelimiter("subway");
            transactionGroup.addDelimiter("dunkin");
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
