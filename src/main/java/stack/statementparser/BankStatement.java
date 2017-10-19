/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import static stack.statementparser.DateFormats.MONTH_DAY_YEAR_FORMAT;
import static stack.statementparser.DateFormats.MONTH_DAY_FORMAT;

/**
 *
 * @author jimst
 */
public class BankStatement
{
    private String fileName;
    
    private String accountNumber;
    private Date startDate;
    private Date endDate;
    
    private final ReadOnlyListWrapper<Transaction> allTransactions;
    private final ObservableList<Transaction> unknownTransactions;
    private final ObservableList<Transaction> depositTransactions;
    private final ObservableList<Transaction> atmWithdrawalTransactions;
    private final ObservableList<Transaction> transferInTransactions;
    private final ObservableList<Transaction> transferOutTransactions;
    private final ObservableList<Transaction> checkTransactions;
    private final ObservableList<Transaction> onlinePaymentTransactions;
    
    public BankStatement()
    {
        this(null);
    }
    
    public BankStatement(String fileName)
    {
        this.fileName = fileName;
        
        ObservableList<Transaction> backingList = FXCollections.observableArrayList();
        allTransactions = new ReadOnlyListWrapper<>(this, 
                                                    "allTransactions", 
                                                    backingList);
        unknownTransactions = new FilteredList<>(allTransactions,
                                                 t -> t.getTransactionType() == TransactionType.UNKNOWN);
        depositTransactions = new FilteredList<>(allTransactions,
                                                 t -> t.getTransactionType() == TransactionType.DEPOSIT);
        atmWithdrawalTransactions = new FilteredList<>(allTransactions,
                                                       t -> t.getTransactionType() == TransactionType.ATM_WITHDRAWAL);
        transferInTransactions = new FilteredList<>(allTransactions,
                                                    t -> t.getTransactionType() == TransactionType.TRANSFER_IN);
        transferOutTransactions = new FilteredList<>(allTransactions,
                                                     t -> t.getTransactionType() == TransactionType.TRANSFER_OUT);
        checkTransactions = new FilteredList<>(allTransactions,
                                               t -> t.getTransactionType() == TransactionType.CHECK);
        onlinePaymentTransactions = new FilteredList<>(allTransactions,
                                                       t -> t.getTransactionType() == TransactionType.ONLINE_PAYMENT);
    }

    public String getAccountNumber()
    {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber)
    {
        this.accountNumber = accountNumber;
    }

    public String getStartDate()
    {
        return MONTH_DAY_YEAR_FORMAT.format(startDate);
    }

    public void setStartDate(String startDate) throws ParseException
    {
        this.startDate = MONTH_DAY_YEAR_FORMAT.parse(startDate);
    }

    public String getEndDate()
    {
        return MONTH_DAY_YEAR_FORMAT.format(endDate);
    }

    public void setEndDate(String endDate) throws ParseException
    {
        this.endDate = MONTH_DAY_YEAR_FORMAT.parse(endDate);
    }
    
    public String getFullDate(String monthAndDay) throws ParseException
    {
        if (null == startDate || null == endDate)
        {
            return monthAndDay;
        }
        
        Date date = MONTH_DAY_FORMAT.parse(monthAndDay);
        if (date.getMonth() < startDate.getMonth())
        {
            // Year rollover
            date.setYear(endDate.getYear());
        }
        else
        {
            date.setYear(startDate.getYear());
        }
        
        return MONTH_DAY_YEAR_FORMAT.format(date);
    }

    public String getFileName()
    {
        return fileName;
    }
    
    public void addTransaction(Transaction transaction)
    {
        allTransactions.add(transaction);
    }

    public ReadOnlyListProperty<Transaction> getAllTransactions()
    {
        return allTransactions.getReadOnlyProperty();
    }

    public ObservableList<Transaction> getUnknownTransactions()
    {
        return unknownTransactions;
    }

    public ObservableList<Transaction> getDepositTransactions()
    {
        return depositTransactions;
    }

    public ObservableList<Transaction> getAtmWithdrawalTransactions()
    {
        return atmWithdrawalTransactions;
    }

    public ObservableList<Transaction> getTransferInTransactions()
    {
        return transferInTransactions;
    }

    public ObservableList<Transaction> getTransferOutTransactions()
    {
        return transferOutTransactions;
    }

    public ObservableList<Transaction> getCheckTransactions()
    {
        return checkTransactions;
    }

    public ObservableList<Transaction> getOnlinePaymentTransactions()
    {
        return onlinePaymentTransactions;
    }
}
