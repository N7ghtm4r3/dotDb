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
import com.intellij.ui.content.Content;
import com.intellij.ui.table.JBTable;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemListener;
import java.sql.*;
import java.util.ArrayList;

import static com.intellij.ui.content.ContentFactory.SERVICE.getInstance;
import static com.intellij.util.ui.JBUI.Borders.empty;
import static java.awt.Font.DIALOG;
import static java.awt.Font.PLAIN;
import static java.sql.DriverManager.getConnection;

public class dotDbWindow implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        try {
            Class.forName("org.sqlite.JDBC");
            dotDbContent content = new dotDbContent();
            Content iContent = getInstance().createContent(content.getContentPanel(), "", false);
            toolWindow.getContentManager().addContent(iContent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class dotDbContent {

        private static final String ALL_FIELDS = "All fields";

        private final JPanel contentPanel = new JPanel();

        private final Connection connection;

        private final ArrayList<String> tables;

        private JPanel tablePanel;

        private JBScrollPane tablePane;

        public dotDbContent() throws SQLException {
            contentPanel.setLayout(new VerticalLayout(10));
            contentPanel.setBorder(empty(10));
            // TODO: 18/06/2023 TO CHANGE WITH DYNAMIC PATH
            connection = getConnection("jdbc:sqlite:C:\\Users\\utente\\Desktop\\Tecknobit\\Dependencies\\Plugins\\Demo\\JavaDockyDemo\\gstorage.db");
            tables = new ArrayList<>();
            getTables();
            loadCombobox();
        }

        private void getTables() throws SQLException {
            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet sTables = metaData.getTables(null, null, "%", new String[]{"TABLE"});
            while (sTables.next())
                tables.add(sTables.getString("TABLE_NAME"));
        }

        private void loadCombobox() {
            JPanel comboPanel = new JPanel();
            comboPanel.setLayout(new VerticalLayout(10));
            comboPanel.add(getHeaderTitle("Tables"));
            comboPanel.add(createComboBox(tables.toArray(new String[0]), e -> {
                try {
                    createTablePanel(e.getItem().toString());
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    refreshContentPanel();
                }
            }));
            contentPanel.add(comboPanel);
            refreshContentPanel();
        }

        private void createTablePanel(String table) throws SQLException {
            final String[] states = {ALL_FIELDS, ""};
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
            ComboBox<String> fieldComboBox = createComboBox(fields, e -> {
                states[0] = e.getItem().toString();
                try {
                    if (!states[1].isBlank())
                        createTableView(table, states[0], states[1]);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            });
            searchPanel.add(fieldComboBox);
            SearchTextField searchTextField = new SearchTextField(false);
            searchTextField.addDocumentListener(new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    try {
                        states[1] = e.getDocument().getText(0, e.getLength());
                        if (states[1].isBlank())
                            states[0] = ALL_FIELDS;
                        createTableView(table, states[0], states[1]);
                    } catch (Exception ignored) {
                    } finally {
                        if (!states[1].endsWith("\n"))
                            searchTextField.requestFocus();
                    }
                }
            });
            searchPanel.add(searchTextField);
            JButton clearBnt = new JButton("CLEAR");
            clearBnt.addActionListener(e -> {
                searchTextField.setText("");
                fieldComboBox.setSelectedIndex(0);
                try {
                    createTableView(table, ALL_FIELDS, "");
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                } finally {
                    refreshContentPanel();
                }
            });
            searchPanel.add(clearBnt);
            tablePanel.add(searchPanel);
            createTableView(table, ALL_FIELDS, "");
        }

        private void createTableView(String table, String field, String fieldValue) throws SQLException {
            if (tablePane != null)
                tablePanel.remove(tablePane);
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
                        fieldsQuery.append(column).append("='").append(fieldValue).append("' ");
                    }
                    query = "SELECT * FROM " + table + " WHERE " + fieldsQuery;
                } else
                    query = "SELECT * FROM " + table + " WHERE " + field + "='" + fieldValue + "'";
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
            }
            contentPanel.add(tablePanel);
            refreshContentPanel();
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

        private ComboBox<String> createComboBox(String[] options, ItemListener itemListener) {
            ComboBox<String> comboBox = new ComboBox<>(options);
            comboBox.addItemListener(itemListener);
            return comboBox;
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

        private void refreshContentPanel() {
            contentPanel.requestFocus();
            contentPanel.invalidate();
        }

        public JBScrollPane getContentPanel() {
            return new JBScrollPane(contentPanel);
        }

    }

}
