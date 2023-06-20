package com.tecknobit.dotdb;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
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

/**
 * The {@code dotDbWindow} class is useful to manage the {@code dotDb}'s view where the user can edit and work on own
 * databases
 *
 * @author N7ghtm4r3 - Tecknobit
 * @see ToolWindowFactory
 **/
public class dotDbWindow implements ToolWindowFactory {

    /**
     * {@code APPLICATION} instance to manage the IDE Thread model and the graphic components
     */
    private static final Application APPLICATION = ApplicationManager.getApplication();

    /**
     * {@code toolWindow} window where set the content
     */
    public static volatile ToolWindow toolWindow;

    /**
     * Method to create the tool window to insert the plugin UI
     *
     * @param project:    the current project
     * @param toolWindow: toolwindow where set the content
     */
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

    /**
     * The {@code dotDbContent} class is useful to manage the plugin's UI content and manage the database
     *
     * @author N7ghtm4r3 - Tecknobit
     */
    public static class dotDbContent {

        /**
         * {@code ALL_FIELDS} option for the fields {@link ComboBox}
         */
        private static final String ALL_FIELDS = "All fields";

        /**
         * {@code states} to manage the search filter:
         * <ul>
         *     <li>
         *         states[0] -> field to filter the search
         *     </li>
         *     <li>
         *         states[1] -> value of the query to search
         *     </li>
         * </ul>
         */
        private final String[] states = {ALL_FIELDS, ""};

        /**
         * {@code contentPanel} main panel to contain the UI
         */
        private final JPanel contentPanel = new JPanel();

        /**
         * {@code databaseFilePath} the database file path of the database to show
         */
        private final String databaseFilePath;

        /**
         * {@code tablePanel} panel to contain the table view of the database
         */
        private JPanel tablePanel;

        /**
         * {@code comboPanel} panel to contain the view of the tables of the database
         */
        private JPanel comboPanel;

        /**
         * {@code currentTableSelected} the current table name selected
         */
        private String currentTableSelected;

        /**
         * {@code tablePane} panel to make scrollable the {@link #tablePanel}
         */
        private JBScrollPane tablePane;

        /**
         * {@code connection} instance to manage the connection with the database chosen
         */
        private Connection connection;

        /**
         * {@code tables} list of tables of the database to show
         */
        private final ArrayList<String> tables;

        /**
         * {@code currentTableDetails} the current table details in base of {@link #currentTableSelected}
         */
        private TableDetails currentTableDetails;

        /**
         * {@code query} the query to execute to refresh the UI with the refreshed database
         */
        private String query;

        /**
         * Constructor to init a {@link dotDbContent} object
         *
         * @param databaseFilePath: the database file path of the database to show
         */
        public dotDbContent(String databaseFilePath) throws SQLException {
            this.databaseFilePath = databaseFilePath;
            contentPanel.setLayout(new VerticalLayout(10));
            contentPanel.setBorder(empty(10));
            tables = getTables();
            initView();
        }

        /**
         * Method to init and keep the UI updated almost in real-time (there is a refresh time of 0.5 seconds each lap) <br>
         * No-any params required
         */
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

        /**
         * Method to get the tables of the database to show <br>
         * No-any params required
         *
         * @return tables of the database as {@link ArrayList} as {@link String}
         */
        private ArrayList<String> getTables() throws SQLException {
            ArrayList<String> tables = new ArrayList<>();
            connection = getConnection("jdbc:sqlite:" + databaseFilePath);
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet sTables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            while (sTables.next())
                tables.add(sTables.getString("TABLE_NAME"));
            return tables;
        }

        /**
         * Method to load the combobox with the {@link #tables} of the database to show <br>
         * No-any params required
         */
        private void loadCombobox() throws SQLException {
            while (connection.isClosed())
                Thread.onSpinWait();
            if (currentTableDetails == null || currentTableDetails.hasChanged(connection, query)) {
                if (comboPanel != null)
                    contentPanel.remove(comboPanel);
                comboPanel = new JPanel();
                comboPanel.setLayout(new VerticalLayout(10));
                comboPanel.add(getHeaderTitle("Tables"));
                if (currentTableSelected == null) {
                    currentTableSelected = tables.get(0);
                    query = getBaseQuery();
                }
                if (currentTableDetails == null)
                    initCurrentTable();
                ComboBox<String> tablesComboBox = new ComboBox<>(tables.toArray(new String[0]));
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

        /**
         * Method to create the table panel and the search panel to filter the records of the database to show <br>
         * No-any params required
         */
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

        /**
         * Method to create the table view where place the database to show <br>
         * No-any params required
         *
         * @apiNote you can edit the cells value to make an UPDATE on the database
         */
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
            model.addTableModelListener(e -> {
                int column = e.getColumn();
                int row = e.getFirstRow();
                if (column > -1) {
                    try {
                        while (connection.isClosed())
                            Thread.onSpinWait();
                        connection.prepareStatement(currentTableDetails.getUpdateQuery(currentTableSelected, column, row,
                                model.getValueAt(row, column))).executeUpdate();
                    } catch (SQLException ex) {
                        Messages.showErrorDialog(ex.getLocalizedMessage(), "Error During the Update");
                    }
                }
            });
            query = getBaseQuery();
            if (!states[1].isBlank()) {
                if (states[0].equals(ALL_FIELDS)) {
                    StringBuilder fieldsQuery = new StringBuilder();
                    for (String column : columns) {
                        if (fieldsQuery.length() > 0)
                            fieldsQuery.append("OR ");
                        fieldsQuery.append(column).append(" LIKE '%").append(states[1]).append("%' ");
                    }
                    query += " WHERE " + fieldsQuery;
                } else
                    query += " WHERE " + states[0] + " LIKE '%" + states[1] + "%'";
            }
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

        /**
         * Method to init the {@link #currentTableDetails} to correctly perform the plugin's workflow <br>
         * No-any params required
         */
        private void initCurrentTable() throws SQLException {
            currentTableDetails = getTableDetails(query, connection);
        }

        /**
         * Method to get the base query <br>
         * No-any params required
         *
         * @return base query as {@link String}
         */
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
            lTitle.setFont(new Font(DIALOG, PLAIN, 20));
            return lTitle;
        }

        /**
         * Method to refresh the UI of the {@link #contentPanel} <br>
         * No-any params required
         */
        private void refreshPanel() {
            refreshPanel(contentPanel);
        }

        /**
         * Method to refresh the UI of a panel
         *
         * @param panel: the panel to refresh
         */
        private void refreshPanel(JPanel panel) {
            panel.validate();
            panel.repaint();
        }

        /**
         * Method to get {@link #contentPanel} instance <br>
         * No-any params required
         *
         * @return {@link #contentPanel} instance as {@link JBScrollPane} to make it scrollable
         */
        public JBScrollPane getContentPanel() {
            return new JBScrollPane(contentPanel);
        }

    }

}
