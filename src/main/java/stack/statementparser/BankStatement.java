/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jimst
 */
public class BankStatement extends Statement
{
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
    
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
        super(fileName);
        
        unknownTransactions = new FilteredList<>(allTransactionsProperty(),
                                                 t -> t.getTransactionType() == TransactionType.UNKNOWN);
        depositTransactions = new FilteredList<>(allTransactionsProperty(),
                                                 t -> t.getTransactionType() == TransactionType.DEPOSIT);
        atmWithdrawalTransactions = new FilteredList<>(allTransactionsProperty(),
                                                       t -> t.getTransactionType() == TransactionType.ATM_WITHDRAWAL);
        transferInTransactions = new FilteredList<>(allTransactionsProperty(),
                                                    t -> t.getTransactionType() == TransactionType.TRANSFER_IN);
        transferOutTransactions = new FilteredList<>(allTransactionsProperty(),
                                                     t -> t.getTransactionType() == TransactionType.TRANSFER_OUT);
        checkTransactions = new FilteredList<>(allTransactionsProperty(),
                                               t -> t.getTransactionType() == TransactionType.CHECK);
        onlinePaymentTransactions = new FilteredList<>(allTransactionsProperty(),
                                                       t -> t.getTransactionType() == TransactionType.ONLINE_PAYMENT);
    }
    
    public double getAllDeposits()
    {
        double sum = 0.0;
        for (Transaction transaction : depositTransactions)
        {
            double amount = 0.0;
            try
            {
                amount = NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue();
            }
            catch (ParseException ex)
            {
                Logger.getLogger(BankStatement.class.getName()).log(Level.SEVERE, null, ex);
            }
            sum += amount;
        }
        
        return sum;
    }
    
    public double getJuliaSalaryDeposits()
    {
        double sum = 0.0;
        for (Transaction transaction : depositTransactions)
        {
            if (StringUtils.containsIgnoreCase(transaction.getDescription(), "direct dep rdc vet"))
            {
                double amount = 0.0;
                try
                {
                    amount = NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue();
                }
                catch (ParseException ex)
                {
                    Logger.getLogger(BankStatement.class.getName()).log(Level.SEVERE, null, ex);
                }
                sum += amount;
            }
        }
        
        return sum;
    }
    
    public double getJimSalaryDeposits()
    {
        double sum = 0.0;
        for (Transaction transaction : depositTransactions)
        {
            if (StringUtils.containsIgnoreCase(transaction.getDescription(), "payroll remcom") ||
                StringUtils.containsIgnoreCase(transaction.getDescription(), "direct dep commnet"))
            {
                double amount = 0.0;
                try
                {
                    amount = NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue();
                }
                catch (ParseException ex)
                {
                    Logger.getLogger(BankStatement.class.getName()).log(Level.SEVERE, null, ex);
                }
                sum += amount;
            }
        }
        
        return sum;
    }
    
    public double getAtmWithdrawals()
    {
        double sum = 0.0;
        for (Transaction transaction : atmWithdrawalTransactions)
        {
            double amount = 0.0;
            try
            {
                amount = NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue();
            }
            catch (ParseException ex)
            {
                Logger.getLogger(BankStatement.class.getName()).log(Level.SEVERE, null, ex);
            }
            sum += amount;
        }
        
        return sum;
    }
    
    public double getOnlinePayments()
    {
        double sum = 0.0;
        for (Transaction transaction : onlinePaymentTransactions)
        {
            double amount = 0.0;
            try
            {
                amount = NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue();
            }
            catch (ParseException ex)
            {
                Logger.getLogger(BankStatement.class.getName()).log(Level.SEVERE, null, ex);
            }
            sum += amount;
        }
        
        return sum;
    }    
    
    public double getTransferredIn()
    {
        double sum = 0.0;
        for (Transaction transaction : transferInTransactions)
        {
            double amount = 0.0;
            try
            {
                amount = NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue();
            }
            catch (ParseException ex)
            {
                Logger.getLogger(BankStatement.class.getName()).log(Level.SEVERE, null, ex);
            }
            sum += amount;
        }
        
        return sum;
    }
    
    public double getTransferredOut()
    {
        double sum = 0.0;
        for (Transaction transaction : transferOutTransactions)
        {
            double amount = 0.0;
            try
            {
                amount = NUMBER_FORMAT.parse(transaction.getAmount()).doubleValue();
            }
            catch (ParseException ex)
            {
                Logger.getLogger(BankStatement.class.getName()).log(Level.SEVERE, null, ex);
            }
            sum += amount;
        }
        
        return sum;
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
