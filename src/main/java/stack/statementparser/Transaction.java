/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

/**
 *
 * @author jimst
 */
public class Transaction
{
    String date;
    String amount;
    String description;
    
    TransactionType transactionType;

    public Transaction(String date, String amount, String description)
    {
        this.date = date;
        this.amount = amount;
        this.description = description;
        
        transactionType = TransactionType.UNKNOWN;
    }

    public TransactionType getTransactionType()
    {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType)
    {
        this.transactionType = transactionType;
    }

    public String getDate()
    {
        return date;
    }

    public String getAmount()
    {
        return amount;
    }
    
    public void setAmount(String value)
    {
        amount = value;
    }

    public String getDescription()
    {
        return description;
    }
    
    @Override
    public String toString()
    {
        return date + " " + description + " " + amount;
    }
}
