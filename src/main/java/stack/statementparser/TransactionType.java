/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package stack.statementparser;

import java.util.EnumSet;

/**
 *
 * @author jimst
 */
public enum TransactionType
{
    UNKNOWN, 
    UNKNOWN_WITHDRAWAL,
    UNKNOWN_ONLINE_TRANSACTION,
    UNKNOWN_DEDUCTION,
    DEPOSIT, 
    ATM_WITHDRAWAL, 
    CHECK, 
    CREDIT_CARD_PURCHASE,
    DEBIT_CARD_PURCHASE, 
    FEE, 
    TRANSFER_IN, 
    TRANSFER_OUT, 
    ONLINE_PAYMENT,
    DIRECT_PAYMENT,
    TELLER_WITHDRAWAL,
    REFUND;
    
    public static final EnumSet<TransactionType> purchases = EnumSet.of(CREDIT_CARD_PURCHASE, DEBIT_CARD_PURCHASE, ONLINE_PAYMENT, DIRECT_PAYMENT);
    
    public static final EnumSet<TransactionType> transfers = EnumSet.of(TRANSFER_IN, TRANSFER_OUT);
}
