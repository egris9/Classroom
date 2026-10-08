package com.Classroom_ai.Classroom.course;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Work that must only happen once the database change it belongs to has been committed. */
final class AfterCommit {

    private AfterCommit() {
    }

    /** Work that undoes a side effect made during the transaction, such as a stored file, if the transaction rolls back. */
    static void onRollback(Runnable work) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    work.run();
                }
            }
        });
    }

    static void run(Runnable work) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                work.run();
            }
        });
    }
}
