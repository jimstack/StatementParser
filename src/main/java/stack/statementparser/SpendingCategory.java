/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author jimst
 */
public class SpendingCategory
{
    private String name;
    
    private List<TransactionGroup> transactionGroups;
    
    public SpendingCategory(String name)
    {
        this.name = name;
        transactionGroups = new ArrayList<>();
    }
    
    public String getName()
    {
        return name;
    }
    
    public boolean handle(Transaction transaction)
    {
        for (TransactionGroup group : transactionGroups)
        {
            if (group.handle(transaction))
            {
                return true;
            }
        }
        
        return false;
    }
    
    public void addTransactionGroup(TransactionGroup group)
    {
        transactionGroups.add(group);
    }
    
    public List<TransactionGroup> getTransactionGroups()
    {
        return transactionGroups;
    }
}
