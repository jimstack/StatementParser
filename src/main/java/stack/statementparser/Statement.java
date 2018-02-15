/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.text.ParseException;
import java.util.Date;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import static stack.statementparser.DateFormats.MONTH_DAY_FORMAT;
import static stack.statementparser.DateFormats.MONTH_DAY_FULL_YEAR_FORMAT;

/**
 *
 * @author jimst
 */
public class Statement
{
    private String fileName;
    
    private String accountNumber;
    private Date startDate;
    private Date endDate;
    
    private final ReadOnlyDoubleWrapper endingBalance;
    private final ReadOnlyDoubleWrapper beginningBalance;
    
    private final ReadOnlyListWrapper<Transaction> allTransactions;
    
    public Statement(String fileName)
    {
        this.fileName = fileName;
        
        endingBalance = new ReadOnlyDoubleWrapper(this, "endingBalance", 0);
        beginningBalance = new ReadOnlyDoubleWrapper(this, "beginningBalance", 0);
        
        ObservableList<Transaction> backingList = FXCollections.observableArrayList();
        allTransactions = new ReadOnlyListWrapper<>(this, 
                                                    "allTransactions", 
                                                    backingList);
    }
    
    public void setEndingBalance(double value)
    {
        endingBalance.set(value);
    }
    
    public double getEndingBalance()
    {
        return endingBalance.doubleValue();
    }
    
    public void setBeginningBalance(double value)
    {
        beginningBalance.set(value);
    }
    
    public double getBeginningBalance()
    {
        return beginningBalance.doubleValue();
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
        return (null == startDate ? "UNKNOWN" : MONTH_DAY_FULL_YEAR_FORMAT.format(startDate));
    }

    public void setStartDate(String startDate) throws ParseException
    {
        this.startDate = MONTH_DAY_FULL_YEAR_FORMAT.parse(startDate);
    }

    public String getEndDate()
    {
        return (null == endDate ? "UNKNOWN" : MONTH_DAY_FULL_YEAR_FORMAT.format(endDate));
    }

    public void setEndDate(String endDate) throws ParseException
    {
        this.endDate = MONTH_DAY_FULL_YEAR_FORMAT.parse(endDate);
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
        
        return MONTH_DAY_FULL_YEAR_FORMAT.format(date);
    }

    public String getFileName()
    {
        return fileName;
    }
    
    public void addTransaction(Transaction transaction)
    {
        allTransactions.add(transaction);
    }

    public final ReadOnlyListProperty<Transaction> allTransactionsProperty()
    {
        return allTransactions.getReadOnlyProperty();
    }    
}
