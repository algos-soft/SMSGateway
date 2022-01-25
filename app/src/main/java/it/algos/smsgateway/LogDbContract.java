package it.algos.smsgateway;

import android.provider.BaseColumns;

public class LogDbContract {

    // To prevent someone from accidentally instantiating the contract class,
    // make the constructor private.
    private LogDbContract() {}

    /* Inner class that defines the table contents */
    public static class LogEntry implements BaseColumns {
        public static final String TABLE_NAME = "log_items";
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_LEVEL = "level";
        public static final String COLUMN_MSG = "text";
        public static final String COLUMN_EXCEPTION = "exception";
    }

}
