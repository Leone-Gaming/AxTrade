package com.artillexstudios.axtrade.trade.logging.database;

import java.sql.Connection;

public interface DatabaseImpl {

    void init();
    void close();

    Connection connection();

}
