package com.tecknobit.dotdb;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.ui.table.JBTable;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.intellij.util.ui.JBUI.Borders.empty;
import static java.awt.Font.DIALOG;
import static java.awt.Font.PLAIN;
import static java.sql.DriverManager.getConnection;

public class dotDbWindow implements ToolWindowFactory {

    public static ToolWindow toolWindow;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            Class.forName("org.sqlite.JDBC");
            if (dotDbWindow.toolWindow == null)
                toolWindow.hide();
            dotDbWindow.toolWindow = toolWindow;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class dotDbContent {

        private static final String ALL_FIELDS = "All fields";

        private final String[] states = {ALL_FIELDS, ""};

        private final JPanel contentPanel = new JPanel();

        private final String databaseFilePath;

        private JPanel tablePanel;

        private JPanel comboPanel;

        private String currentTableSelected;

        private JBScrollPane tablePane;

        private Connection connection;

        private final ArrayList<String> tables;

        public dotDbContent(String databaseFilePath) throws SQLException {
            this.databaseFilePath = databaseFilePath;
            contentPanel.setLayout(new VerticalLayout(10));
            contentPanel.setBorder(empty(10));
            tables = getTables();
            initView();
        }

        private void initView() throws SQLException {
            loadCombobox();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                while (true) {
                    if (toolWindow.isVisible()) {
                        try {
                            ArrayList<String> nTables = getTables();
                            if (!tables.equals(nTables)) {
                                tables.clear();
                                tables.addAll(nTables);
                                if (!tables.contains(currentTableSelected))
                                    currentTableSelected = tables.get(0);
                                loadCombobox();
                            }
                            Thread.sleep(500);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        }

        private ArrayList<String> getTables() throws SQLException {
            ArrayList<String> tables = new ArrayList<>();
            connection = getConnection("jdbc:sqlite:" + databaseFilePath);
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet sTables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            while (sTables.next())
                tables.add(sTables.getString("TABLE_NAME"));
            return tables;
        }

        private void loadCombobox() throws SQLException {
            if (comboPanel != null)
                contentPanel.remove(comboPanel);
            comboPanel = new JPanel();
            comboPanel.setLayout(new VerticalLayout(10));
            comboPanel.add(getHeaderTitle("Tables"));
            ComboBox<String> tablesComboBox = new ComboBox<>(tables.toArray(new String[0]));
            if (currentTableSelected == null)
                currentTableSelected = tables.get(0);
            tablesComboBox.setSelectedItem(currentTableSelected);
            tablesComboBox.addActionListener(e -> {
                try {
                    currentTableSelected = (String) tablesComboBox.getSelectedItem();
                    createTablePanel(currentTableSelected);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
            comboPanel.add(tablesComboBox);
            refreshPanel(comboPanel);
            contentPanel.add(comboPanel);
            createTablePanel(currentTableSelected);
            refreshPanel();
        }

        private void createTablePanel(String table) throws SQLException {
            if (tablePanel != null)
                contentPanel.remove(tablePanel);
            tablePanel = new JPanel();
            tablePanel.setLayout(new VerticalLayout(10));
            tablePanel.add(getHeaderTitle("Work with: " + table));
            String[] columns = getTableDetails(table).getFirst();
            final String[] fields = new String[columns.length + 1];
            fields[0] = ALL_FIELDS;
            System.arraycopy(columns, 0, fields, 1, columns.length);
            JPanel searchPanel = new JPanel();
            searchPanel.setLayout(new HorizontalLayout(10));
            ComboBox<String> fieldsComboBox = new ComboBox<>(fields);
            fieldsComboBox.addActionListener(e -> {
                states[0] = (String) fieldsComboBox.getSelectedItem();
                try {
                    createTableView(table, states[0], states[1]);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
            searchPanel.add(fieldsComboBox);
            SearchTextField searchTextField = new SearchTextField(false);
            searchTextField.addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    try {
                        states[1] = searchTextField.getText();
                        createTableView(table, states[0], states[1]);
                    } catch (Exception ex) {
                        states[1] = "";
                    } finally {
                        searchTextField.requestFocus();
                    }
                }
            });
            searchPanel.add(searchTextField);
            JButton clearBnt = new JButton("CLEAR");
            clearBnt.addActionListener(e -> {
                if (!states[0].equals(ALL_FIELDS) || !states[1].isEmpty()) {
                    searchTextField.setText("");
                    fieldsComboBox.setSelectedIndex(0);
                    states[1] = "";
                    try {
                        createTableView(table, ALL_FIELDS, "");
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
            searchPanel.add(clearBnt);
            refreshPanel(searchPanel);
            tablePanel.add(searchPanel);
            createTableView(table, ALL_FIELDS, "");
        }

        private void createTableView(String table, String field, String fieldValue) throws SQLException {
            if (tablePane != null)
                tablePanel.remove(tablePane);
            while (connection.isClosed())
                Thread.onSpinWait();
            String query = "SELECT * FROM " + table;
            Pair<String[], Integer> tableDetails = getTableDetails(table);
            String[] columns = tableDetails.getFirst();
            int columnsCount = tableDetails.getSecond();
            if (!fieldValue.isBlank()) {
                if (field.equals(ALL_FIELDS)) {
                    StringBuilder fieldsQuery = new StringBuilder();
                    for (String column : columns) {
                        if (fieldsQuery.length() > 0)
                            fieldsQuery.append("OR ");
                        fieldsQuery.append(column).append(" LIKE '%").append(fieldValue).append("%' ");
                    }
                    query = "SELECT * FROM " + table + " WHERE " + fieldsQuery;
                } else
                    query = "SELECT * FROM " + table + " WHERE " + field + " LIKE '%" + fieldValue + "%'";
            }
            JBTable tableView = new JBTable();
            tableView.setRowSelectionAllowed(false);
            tableView.setCellSelectionEnabled(true);
            ResultSet rows = connection.createStatement().executeQuery(query);
            DefaultTableModel model = new DefaultTableModel(columns, 0);
            while (rows.next()) {
                Object[] rowItems = new Object[columnsCount];
                for (int j = 1; j <= columnsCount; j++)
                    rowItems[j - 1] = rows.getObject(j);
                model.addRow(rowItems);
            }
            rows.close();
            connection.close();
            if (model.getRowCount() > 0) {
                tableView.setModel(model);
                tablePane = new JBScrollPane(tableView) {
                    @Override
                    public Dimension getPreferredSize() {
                        // TODO: 18/06/2023 TO CHECK\
                        int height = tableView.getPreferredSize().height;
                        return new Dimension(100, height + tableView.getRowHeight() + 5);
                    }
                };
                tablePanel.add(tablePane);
                refreshPanel(tablePanel);
            }
            contentPanel.add(tablePanel);
            refreshPanel();
        }

        private Pair<String[], Integer> getTableDetails(String table) throws SQLException {
            ResultSet rows = connection.createStatement().executeQuery("SELECT * FROM " + table);
            ResultSetMetaData metaData = rows.getMetaData();
            int columnsCount = metaData.getColumnCount();
            String[] columns = new String[columnsCount];
            for (int j = 1; j <= columnsCount; j++)
                columns[j - 1] = metaData.getColumnName(j);
            return new Pair<>(columns, columnsCount);
        }

        /**
         * Method to get a header title
         *
         * @param title: title of the label
         * @return header title as {@link JLabel}
         */
        private JLabel getHeaderTitle(String title) {
            JLabel lTitle = new JLabel(title);
            lTitle.setFont(getFontText(20));
            return lTitle;
        }

        /**
         * Method to get the font for a {@link JComponent}
         *
         * @param size: size of the text
         * @return font as {@link Font}
         */
        private Font getFontText(int size) {
            return getFontText(DIALOG, size);
        }

        /**
         * Method to get the font for a {@link JComponent}
         *
         * @param font: font to use
         * @param size: size of the text
         * @return font as {@link Font}
         */
        private Font getFontText(String font, int size) {
            return new Font(font, PLAIN, size);
        }

        private void refreshPanel() {
            refreshPanel(contentPanel);
        }

        private void refreshPanel(JPanel panel) {
            panel.validate();
            panel.repaint();
        }

        public JBScrollPane getContentPanel() {
            return new JBScrollPane(contentPanel);
        }

    }

}
