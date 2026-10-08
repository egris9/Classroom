package com.Classroom_ai.Classroom.course;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Work that must only happen once the database change it belongs to has been committed. */
final class AfterCommit {

    private AfterCommit() {
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
