package com.tecknobit.dotdb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import static java.util.Arrays.deepEquals;

/**
 * The {@code TableDetails} class is useful to manage the real-time system of the plugin and keep refreshed the database
 * shown
 *
 * @author N7ghtm4r3 - Tecknobit
 */
public class TableDetails {

    /**
     * {@code columns} of the table
     */
    private String[] columns;

    /**
     * {@code columnsCount} number of the columns of the table
     */
    private int columnsCount;

    /**
     * {@code tableContent} the content of the table
     */
    private ArrayList<Object[]> tableContent;

    /**
     * {@code tableChanged} the table refreshed after invoked {@link #hasChanged(Connection, String)} method
     */
    private TableDetails tableChanged;

    /**
     * Constructor to init a {@link TableDetails} object
     *
     * @param columns:      columns of the table
     * @param columnsCount: number of the columns of the table
     * @param tableContent: the content of the table
     */
    public TableDetails(String[] columns, int columnsCount, ArrayList<Object[]> tableContent) {
        this.columns = columns;
        this.columnsCount = columnsCount;
        this.tableContent = tableContent;
    }

    /**
     * Method to get {@link #columns} instance <br>
     * No-any params required
     *
     * @return {@link #columns} instance as array of {@link String}
     */
    public String[] getColumns() {
        return columns;
    }

    /**
     * Method to get {@link #columnsCount} instance <br>
     * No-any params required
     *
     * @return {@link #columnsCount} instance as int
     */
    public int getColumnsCount() {
        return columnsCount;
    }

    /**
     * Method to get {@link #tableContent} instance <br>
     * No-any params required
     *
     * @return {@link #tableContent} instance as {@link ArrayList} of {@link Object} array
     */
    public ArrayList<Object[]> getTableContent() {
        return getTableContent("");
    }

    /**
     * Method to get {@link #tableContent} instance
     *
     * @param query: the query to fetch the correct content
     * @return {@link #tableContent} instance as {@link ArrayList} of {@link Object} array
     * @apiNote if the query contains {@code "LIKE"} keyword will be returned the result of {@link #getTableChangedContent()} method
     */
    public ArrayList<Object[]> getTableContent(String query) {
        if (query.contains("LIKE"))
            return getTableChangedContent();
        return tableContent;
    }

    /**
     * Method to get {@link #tableContent} instance of the {@link #tableChanged} <br>
     * No-any params required
     *
     * @return {@link #tableContent} of the {@link #tableChanged} instance as {@link ArrayList} of {@link Object} array
     */
    public ArrayList<Object[]> getTableChangedContent() {
        if (tableChanged == null)
            return tableContent;
        return tableChanged.getTableContent();
    }

    /**
     * Method to get the update query
     *
     * @param tableName:   the name of the table where execute the update
     * @param column:      the index of the column where the cell value has been changed
     * @param row:         the index of the row where the cell value has been changed
     * @param updateValue: the new value to insert in the table
     * @return update query as {@link String}
     */
    public String getUpdateQuery(String tableName, int column, int row, Object updateValue) {
        StringBuilder whereConditions = new StringBuilder();
        Object[] contents = tableContent.get(row);
        for (int j = 0; j < contents.length; j++) {
            if (whereConditions.length() == 0)
                whereConditions.append("' WHERE ");
            else
                whereConditions.append(" AND ");
            whereConditions.append(columns[j]).append("='").append(contents[j]).append("'");
        }
        return "UPDATE " + tableName + " SET " + columns[column] + "='" + updateValue + whereConditions;
    }

    /**
     * Method to check if the table shown has changed
     *
     * @param connection: the connection instance to use to check if the table has changed
     * @param query:      the query to execute to check if the table has changed
     * @return whether the table has changed as boolean
     */
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
        if (changed)
            tableChanged = new TableDetails(columns, columnsCount, tableContent);
        return changed;
    }

    /**
     * Method to compare two tables
     *
     * @param firstTable:  the first table of the comparison
     * @param secondTable: the second table of the comparison
     * @return whether the table are equal or are different
     */
    private boolean compareTable(TableDetails firstTable, TableDetails secondTable) {
        boolean changed = true;
        if (deepEquals(firstTable.getColumns(), secondTable.getColumns())
                && firstTable.getColumnsCount() == secondTable.getColumnsCount()) {
            ArrayList<Object[]> tableContent = firstTable.getTableContent();
            ArrayList<Object[]> newContent = secondTable.getTableContent();
            int contentSize = tableContent.size();
            changed = contentSize != newContent.size();
            for (int j = 0; j < contentSize && !changed; j++)
                changed = !deepEquals(tableContent.get(j), newContent.get(j));
        }
        return changed;
    }

    /**
     * Method to get the table details
     *
     * @param connection: the connection instance to use to get the table details
     * @param table:      the table name to get its details
     * @return table details as {@link TableDetails}
     */
    public static TableDetails getTableDetails(Connection connection, String table) throws SQLException {
        return getTableDetails("SELECT * FROM " + table, connection);
    }

    /**
     * Method to get the table details
     *
     * @param connection: the connection instance to use to get the table details
     * @param query:      the query to execute to get the table details
     * @return table details as {@link TableDetails}
     */
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
