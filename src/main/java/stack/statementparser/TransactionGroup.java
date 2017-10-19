/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author jimst
 */
public class TransactionGroup
{
    private String name;
    
    private List<Transaction> transactions;
    
    private List<String> delimiters;
    
    public TransactionGroup(String description)
    {
        this.name = name;
        
        transactions = new ArrayList<>();
        delimiters = new ArrayList<>();
    }
    
    public String getName()
    {
        return name;
    }
    
    public void addDelimiter(String value)
    {
        delimiters.add(value);
    }
    
    public boolean handle(Transaction transaction)
    {
        for (String delimiter : delimiters)
        {
            if (StringUtils.containsIgnoreCase(transaction.getDescription(), delimiter))
            {
                addTransaction(transaction);
                return true;
            }
        }
        
        return false;
    }
    
    public List<Transaction> getTransactions()
    {
        return transactions;
    }
    
    private void addTransaction(Transaction transaction)
    {
        transactions.add(transaction);
    }
}
