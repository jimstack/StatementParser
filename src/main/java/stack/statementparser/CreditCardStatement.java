/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import javafx.beans.property.ReadOnlyDoubleWrapper;

/**
 *
 * @author jimst
 */
public class CreditCardStatement extends Statement
{
    private final ReadOnlyDoubleWrapper paymentsAndCredits;
    private final ReadOnlyDoubleWrapper purchases;
    private final ReadOnlyDoubleWrapper cashAdvances;
    private final ReadOnlyDoubleWrapper balanceTransfers;
    private final ReadOnlyDoubleWrapper feesCharged;
    private final ReadOnlyDoubleWrapper interestCharged;
    
    public CreditCardStatement(String fileName)
    {
        super(fileName);
        
        paymentsAndCredits = new ReadOnlyDoubleWrapper(this, "paymentsAndCredits", 0);
        purchases = new ReadOnlyDoubleWrapper(this, "purchases", 0);
        cashAdvances = new ReadOnlyDoubleWrapper(this, "cashAdvances", 0);
        balanceTransfers = new ReadOnlyDoubleWrapper(this, "balanceTransfers", 0);
        feesCharged = new ReadOnlyDoubleWrapper(this, "feesCharged", 0);
        interestCharged = new ReadOnlyDoubleWrapper(this, "interestCharged", 0);
    }    
    
    public void setPaymentsAndCredits(double value)
    {
        paymentsAndCredits.set(value);
    }
    
    public double getPaymentsAndCredits()
    {
        return paymentsAndCredits.doubleValue();
    }
    
    public void setPurchases(double value)
    {
        purchases.set(value);
    }
    
    public double getPurchases()
    {
        return purchases.doubleValue();
    }
    
    public void setCashAdvances(double value)
    {
        cashAdvances.set(value);
    }
    
    public double getCashAdvances()
    {
        return cashAdvances.doubleValue();
    }
    
    public void setBalanceTransfers(double value)
    {
        balanceTransfers.set(value);
    }
    
    public double getBalanceTransfers()
    {
        return balanceTransfers.doubleValue();
    }
    
    public void setFeesCharged(double value)
    {
        feesCharged.set(value);
    }
    
    public double getFeesCharged()
    {
        return feesCharged.doubleValue();
    }
    
    public void setInterestCharged(double value)
    {
        interestCharged.set(value);
    }
    
    public double getInterestCharged()
    {
        return interestCharged.doubleValue();
    }
}
