/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jimst
 */
public class TransferHistory
{
    private final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);
        
    private String primaryAccount;
    private String secondaryAccount;
    
    private final List<Transfer> transfers;
    
    private double netTransfers;

    public TransferHistory(String primaryAccount, String secondaryAccount)
    {
        this.primaryAccount = primaryAccount;
        this.secondaryAccount = secondaryAccount;
        
        transfers = new ArrayList<>();
        netTransfers = 0.0;
    }

    public String getPrimaryAccount()
    {
        return primaryAccount;
    }

    public String getSecondaryAccount()
    {
        return secondaryAccount;
    }

    public List<Transfer> getTransfers()
    {
        return transfers;
    }
    
    public void addTransfer(Transfer transfer)
    {
        transfers.add(transfer);
        double amount;
        try
        {
            amount = NUMBER_FORMAT.parse(transfer.getAmount()).doubleValue();
            
        }
        catch (ParseException ex)
        {
            throw(new IllegalStateException("Error parsing value: " + transfer.getAmount(), ex));
        }
        
        if (TransactionType.TRANSFER_OUT == transfer.getTransactionType())
        {
            amount *= -1.0;
            transfer.setAmount(String.valueOf(amount));
        }
        
        netTransfers += amount;
    }
    
    public double getNetTransfers()
    {
        return netTransfers;
    }
}
