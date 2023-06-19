package com.tecknobit.dotdb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

public class TableDetails {

    private final String[] columns;

    private final int columnsCount;

    private final ArrayList<Object[]> tableContent;

    private TableDetails lastTableChanged;

    public TableDetails(String[] columns, int columnsCount, ArrayList<Object[]> tableContent) {
        this.columns = columns;
        this.columnsCount = columnsCount;
        this.tableContent = tableContent;
    }

    public String[] getColumns() {
        return columns;
    }

    public int getColumnsCount() {
        return columnsCount;
    }

    public ArrayList<Object[]> getTableContent() {
        return tableContent;
    }

    public TableDetails getLastTableChanged() {
        if (lastTableChanged == null)
            return this;
        return lastTableChanged;
    }

    public boolean hasChanged(Connection connection, String table) throws SQLException {
        lastTableChanged = getTableDetails(connection, table);
        if (Arrays.deepEquals(columns, lastTableChanged.getColumns()) && columnsCount == lastTableChanged.getColumnsCount()) {
            ArrayList<Object[]> newContent = lastTableChanged.getTableContent();
            int contentSize = tableContent.size();
            int newContentSize = newContent.size();
            if (contentSize != newContentSize)
                return true;
            boolean changed = false;
            for (int j = 0; j < contentSize && !changed; j++)
                changed = !Arrays.deepEquals(tableContent.get(j), newContent.get(j));
            return changed;
        }
        return true;
    }

    public static TableDetails getTableDetails(Connection connection, String table) throws SQLException {
        ResultSet rows = connection.createStatement().executeQuery("SELECT * FROM " + table);
        ResultSetMetaData metaData = rows.getMetaData();
        int columnsCount = metaData.getColumnCount();
        String[] columns = new String[columnsCount];
        ArrayList<Object[]> content = new ArrayList<>();
        for (int j = 1; j <= columnsCount; j++)
            columns[j - 1] = metaData.getColumnName(j);
        while (rows.next()) {
            Object[] rowItems = new Object[columnsCount];
            for (int j = 1; j <= columnsCount; j++)
                rowItems[j - 1] = rows.getObject(j);
            content.add(rowItems);
        }
        return new TableDetails(columns, columnsCount, content);
    }

}
