package com.tecknobit.dotdb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import static java.util.Arrays.deepEquals;

public class TableDetails {

    private String[] columns;

    private int columnsCount;

    private ArrayList<Object[]> tableContent;

    private TableDetails tableChanged;

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
        return getTableContent("");
    }

    public ArrayList<Object[]> getTableContent(String query) {
        if (query.contains("LIKE"))
            return getTableChangedContent();
        return tableContent;
    }

    public ArrayList<Object[]> getTableChangedContent() {
        if (tableChanged == null)
            return tableContent;
        return tableChanged.getTableContent();
    }

    public boolean hasChanged(Connection connection, String query) throws SQLException {
        TableDetails tmpTableDetails = getTableDetails(query, connection);
        String[] columns = tmpTableDetails.getColumns();
        int columnsCount = tmpTableDetails.getColumnsCount();
        ArrayList<Object[]> tableContent = tmpTableDetails.getTableContent();
        boolean changed;
        if (tableChanged == null || !query.contains("LIKE")) {
            changed = compareTable(this, tmpTableDetails);
            this.columns = columns;
            this.columnsCount = columnsCount;
            this.tableContent = tableContent;
        } else
            changed = compareTable(tableChanged, tmpTableDetails);
        if (changed) {
            tableChanged = new TableDetails(tmpTableDetails.getColumns(), tmpTableDetails.getColumnsCount(),
                    tmpTableDetails.getTableContent());
        }
        return changed;
    }

    private boolean compareTable(TableDetails firstTable, TableDetails tmpTableDetails) {
        boolean changed = true;
        if (deepEquals(firstTable.getColumns(), tmpTableDetails.getColumns())
                && firstTable.getColumnsCount() == tmpTableDetails.getColumnsCount()) {
            ArrayList<Object[]> tableContent = firstTable.getTableContent();
            ArrayList<Object[]> newContent = tmpTableDetails.getTableContent();
            int contentSize = tableContent.size();
            int newContentSize = newContent.size();
            changed = contentSize != newContentSize;
            for (int j = 0; j < contentSize && !changed; j++)
                changed = !deepEquals(tableContent.get(j), newContent.get(j));
        }
        return changed;
    }

    public static TableDetails getTableDetails(Connection connection, String table) throws SQLException {
        return getTableDetails("SELECT * FROM " + table, connection);
    }

    public static TableDetails getTableDetails(String query, Connection connection) throws SQLException {
        ResultSet rows = connection.createStatement().executeQuery(query);
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
        rows.close();
        return new TableDetails(columns, columnsCount, content);
    }

}
