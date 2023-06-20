package com.tecknobit.dotdb;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
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
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.intellij.util.ui.JBUI.Borders.empty;
import static com.tecknobit.dotdb.TableDetails.getTableDetails;
import static java.awt.Font.DIALOG;
import static java.awt.Font.PLAIN;
import static java.sql.DriverManager.getConnection;

public class dotDbWindow implements ToolWindowFactory {

    private static final Application APPLICATION = ApplicationManager.getApplication();

    public static volatile ToolWindow toolWindow;

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

        private TableDetails currentTableDetails;

        private String query;

        public dotDbContent(String databaseFilePath) throws SQLException {
            this.databaseFilePath = databaseFilePath;
            contentPanel.setLayout(new VerticalLayout(10));
            contentPanel.setBorder(empty(10));
            tables = getTables();
            initView();
        }

        private void initView() {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                while (true) {
                    if (toolWindow.isVisible()) {
                        try {
                            ArrayList<String> nTables = getTables();
                            if (!tables.equals(nTables)) {
                                tables.clear();
                                tables.addAll(nTables);
                                if (!tables.contains(currentTableSelected)) {
                                    currentTableSelected = tables.get(0);
                                    query = getBaseQuery();
                                }
                                currentTableDetails = null;
                            }
                            APPLICATION.invokeLater(() -> {
                                try {
                                    loadCombobox();
                                } catch (SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            });
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
            while (connection.isClosed())
                Thread.onSpinWait();
            if (currentTableDetails == null || currentTableDetails.hasChanged(connection, query)) {
                if (comboPanel != null)
                    contentPanel.remove(comboPanel);
                comboPanel = new JPanel();
                comboPanel.setLayout(new VerticalLayout(10));
                comboPanel.add(getHeaderTitle("Tables"));
                ComboBox<String> tablesComboBox = new ComboBox<>(tables.toArray(new String[0]));
                if (currentTableSelected == null) {
                    currentTableSelected = tables.get(0);
                    query = getBaseQuery();
                }
                if (currentTableDetails == null)
                    initCurrentTable();
                tablesComboBox.setSelectedItem(currentTableSelected);
                tablesComboBox.addActionListener(e -> {
                    try {
                        states[0] = ALL_FIELDS;
                        states[1] = "";
                        currentTableSelected = (String) tablesComboBox.getSelectedItem();
                        initCurrentTable();
                        createTablePanel();
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                comboPanel.add(tablesComboBox);
                refreshPanel(comboPanel);
                contentPanel.add(comboPanel);
                createTablePanel();
                refreshPanel();
            }
        }

        private void createTablePanel() throws SQLException {
            if (tablePanel != null)
                contentPanel.remove(tablePanel);
            tablePanel = new JPanel();
            tablePanel.setLayout(new VerticalLayout(10));
            tablePanel.add(getHeaderTitle("Work with: " + currentTableSelected));
            String[] columns = currentTableDetails.getColumns();
            final String[] fields = new String[columns.length + 1];
            fields[0] = ALL_FIELDS;
            System.arraycopy(columns, 0, fields, 1, columns.length);
            JPanel searchPanel = new JPanel();
            searchPanel.setLayout(new HorizontalLayout(10));
            ComboBox<String> fieldsComboBox = new ComboBox<>(fields);
            fieldsComboBox.setSelectedItem(states[0]);
            fieldsComboBox.addActionListener(e -> {
                states[0] = (String) fieldsComboBox.getSelectedItem();
                try {
                    createTableView();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
            searchPanel.add(fieldsComboBox);
            SearchTextField searchTextField = new SearchTextField(false);
            searchTextField.setText(states[1]);
            searchTextField.addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    try {
                        states[1] = searchTextField.getText();
                        createTableView();
                    } catch (Exception ex) {
                        states[1] = "";
                    } finally {
                        searchTextField.requestFocus();
                    }
                }
            });
            searchPanel.add(searchTextField);
            APPLICATION.invokeLater(() -> {
                if (!states[1].isBlank()) {
                    searchTextField.grabFocus();
                    searchTextField.requestFocusInWindow();
                }
            });
            JButton clearBnt = new JButton("CLEAR");
            clearBnt.addActionListener(e -> {
                if (!states[0].equals(ALL_FIELDS) || !states[1].isEmpty()) {
                    searchTextField.setText("");
                    fieldsComboBox.setSelectedIndex(0);
                    states[1] = "";
                    try {
                        createTableView();
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });
            searchPanel.add(clearBnt);
            refreshPanel(searchPanel);
            tablePanel.add(searchPanel);
            createTableView();
        }

        private void createTableView() throws SQLException {
            if (tablePane != null)
                tablePanel.remove(tablePane);
            while (connection.isClosed())
                Thread.onSpinWait();
            JBTable tableView = new JBTable();
            tableView.setRowSelectionAllowed(false);
            tableView.setCellSelectionEnabled(true);
            String[] columns = currentTableDetails.getColumns();
            DefaultTableModel model = new DefaultTableModel(columns, 0);
            if (!states[1].isBlank()) {
                if (states[0].equals(ALL_FIELDS)) {
                    StringBuilder fieldsQuery = new StringBuilder();
                    for (String column : columns) {
                        if (fieldsQuery.length() > 0)
                            fieldsQuery.append("OR ");
                        fieldsQuery.append(column).append(" LIKE '%").append(states[1]).append("%' ");
                    }
                    query = getBaseQuery() + " WHERE " + fieldsQuery;
                } else
                    query = getBaseQuery() + " WHERE " + states[0] + " LIKE '%" + states[1] + "%'";
            } else
                query = getBaseQuery();
            for (Object[] rowItems : currentTableDetails.getTableContent(query))
                model.addRow(rowItems);
            if (model.getRowCount() > 0) {
                tableView.setModel(model);
                tablePane = new JBScrollPane(tableView) {
                    @Override
                    public Dimension getPreferredSize() {
                        return new Dimension(100, tableView.getPreferredSize().height +
                                tableView.getRowHeight() + 5);
                    }
                };
                tablePanel.add(tablePane);
                refreshPanel(tablePanel);
            }
            contentPanel.add(tablePanel);
            refreshPanel();
        }

        private void initCurrentTable() throws SQLException {
            currentTableDetails = getTableDetails(query, connection);
        }

        private String getBaseQuery() {
            return "SELECT * FROM " + currentTableSelected;
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
