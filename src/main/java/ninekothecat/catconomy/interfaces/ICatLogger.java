package ninekothecat.catconomy.interfaces;

import ninekothecat.catplugincore.money.enums.TransactionResult;
import ninekothecat.catplugincore.money.interfaces.ITransaction;

/**
 * The interface Cat logger.
 */
public interface ICatLogger {
    /**
     * logs a successful transaction.
     *
     * @param transaction the transaction
     */
    void success(ITransaction transaction);

    /**
     * logs a failed transaction
     *
     * @param transaction the transaction
     * @param result      the result
     */
    void fail(ITransaction transaction, TransactionResult result);

    /**
     * logs an error transaction
     *
     * @param transaction the transaction
     * @param result      the result
     * @param exception   the exception
     */
    void error(ITransaction transaction,TransactionResult result, Exception exception);
}
