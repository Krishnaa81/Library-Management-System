import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.time.LocalDate;
import java.util.Vector;

/**
 * Library Management System (Single file)
 * Tech: Java 17+, JDBC (MySQL), Swing UI
 *
 * Tables:
 *  books(id INT PK AI, title VARCHAR(255), author VARCHAR(255), copies INT)
 *  members(id INT PK AI, name VARCHAR(255), email VARCHAR(255))
 *  loans(id INT PK AI, book_id INT FK, member_id INT FK, loan_date DATE, return_date DATE NULL)
 *
 * NOTE: Update DB_* constants to match your MySQL setup.
 */
public class LibraryApp extends JFrame {

    // ====== DB CONFIG - CHANGE THESE ======
    private static final String DB_URL = "jdbc:mysql://localhost:3306/library_db?serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Krishna@000"; // <-- change

    // ====== UI Components ======
    private JTable booksTable, membersTable, loansTable;
    private DefaultTableModel booksModel, membersModel, loansModel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Optional: Use system look & feel
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}

            // Ensure driver present (for older setups)
            try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException e) {
                JOptionPane.showMessageDialog(null,
                        "MySQL JDBC driver not found.\nAdd mysql-connector-j to classpath.",
                        "Driver Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            new LibraryApp().setVisible(true);
        });
    }

    public LibraryApp() {
        super("Library Management System — JDBC + Swing");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 650);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Books", buildBooksPanel());
        tabs.addTab("Members", buildMembersPanel());
        tabs.addTab("Loans", buildLoansPanel());

        add(tabs, BorderLayout.CENTER);

        // Load initial data
        refreshBooks();
        refreshMembers();
        refreshLoans();
    }

    // ====================== BOOKS ======================

    private JPanel buildBooksPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        booksModel = new DefaultTableModel(new String[]{"ID", "Title", "Author", "Copies"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        booksTable = new JTable(booksModel);
        booksTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField titleField = new JTextField(20);
        JTextField authorField = new JTextField(20);
        JSpinner copiesSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 1000, 1));

        gc.gridx = 0; gc.gridy = 0; form.add(new JLabel("Title"), gc);
        gc.gridx = 1; form.add(titleField, gc);
        gc.gridx = 0; gc.gridy = 1; form.add(new JLabel("Author"), gc);
        gc.gridx = 1; form.add(authorField, gc);
        gc.gridx = 0; gc.gridy = 2; form.add(new JLabel("Copies"), gc);
        gc.gridx = 1; form.add(copiesSpinner, gc);

        JButton addBtn = new JButton("Add Book");
        addBtn.addActionListener(e -> {
            String t = titleField.getText().trim();
            String a = authorField.getText().trim();
            int c = (int) copiesSpinner.getValue();
            if (t.isEmpty() || a.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Title and Author required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String sql = "INSERT INTO books(title, author, copies) VALUES (?, ?, ?)";
            try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, t);
                ps.setString(2, a);
                ps.setInt(3, c);
                ps.executeUpdate();
                titleField.setText(""); authorField.setText(""); copiesSpinner.setValue(1);
                refreshBooks();
            } catch (SQLException ex) {
                showError(ex);
            }
        });

        JButton delBtn = new JButton("Delete Selected");
        delBtn.addActionListener(e -> {
            int row = booksTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a book to delete.");
                return;
            }
            int id = (int) booksModel.getValueAt(row, 0);
            int ok = JOptionPane.showConfirmDialog(this,
                    "Delete book ID " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                String sql = "DELETE FROM books WHERE id=?";
                try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    refreshBooks();
                } catch (SQLException ex) {
                    showError(ex);
                }
            }
        });

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshBooks());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.add(addBtn);
        actions.add(delBtn);
        actions.add(refreshBtn);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(form, BorderLayout.WEST);
        top.add(actions, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(booksTable), BorderLayout.CENTER);
        return root;
    }

    private void refreshBooks() {
        loadToTable("SELECT id, title, author, copies FROM books ORDER BY id", booksModel);
    }

    // ====================== MEMBERS ======================

    private JPanel buildMembersPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        membersModel = new DefaultTableModel(new String[]{"ID", "Name", "Email"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        membersTable = new JTable(membersModel);
        membersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(20);
        JTextField emailField = new JTextField(20);

        gc.gridx = 0; gc.gridy = 0; form.add(new JLabel("Name"), gc);
        gc.gridx = 1; form.add(nameField, gc);
        gc.gridx = 0; gc.gridy = 1; form.add(new JLabel("Email"), gc);
        gc.gridx = 1; form.add(emailField, gc);

        JButton addBtn = new JButton("Add Member");
        addBtn.addActionListener(e -> {
            String n = nameField.getText().trim();
            String m = emailField.getText().trim();
            if (n.isEmpty() || m.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name and Email required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String sql = "INSERT INTO members(name, email) VALUES (?, ?)";
            try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, n);
                ps.setString(2, m);
                ps.executeUpdate();
                nameField.setText(""); emailField.setText("");
                refreshMembers();
            } catch (SQLException ex) {
                showError(ex);
            }
        });

        JButton delBtn = new JButton("Delete Selected");
        delBtn.addActionListener(e -> {
            int row = membersTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a member to delete.");
                return;
            }
            int id = (int) membersModel.getValueAt(row, 0);
            int ok = JOptionPane.showConfirmDialog(this,
                    "Delete member ID " + id + "?\n(Note: will fail if member has active loans.)",
                    "Confirm", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                String sql = "DELETE FROM members WHERE id=?";
                try (Connection con = getCon(); PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    refreshMembers();
                } catch (SQLException ex) {
                    showError(ex);
                }
            }
        });

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshMembers());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.add(addBtn);
        actions.add(delBtn);
        actions.add(refreshBtn);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(form, BorderLayout.WEST);
        top.add(actions, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(membersTable), BorderLayout.CENTER);
        return root;
    }

    private void refreshMembers() {
        loadToTable("SELECT id, name, email FROM members ORDER BY id", membersModel);
    }

    // ====================== LOANS ======================

    private JPanel buildLoansPanel() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        loansModel = new DefaultTableModel(new String[]{
                "LoanID", "BookID", "Title", "MemberID", "Name", "LoanDate", "ReturnDate"
        }, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        loansTable = new JTable(loansModel);

        // Borrow controls
        JPanel borrowPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        JSpinner bookIdSpin = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));
        JSpinner memberIdSpin = new JSpinner(new SpinnerNumberModel(1, 1, 1_000_000, 1));

        gc.gridx = 0; gc.gridy = 0; borrowPanel.add(new JLabel("Book ID"), gc);
        gc.gridx = 1; borrowPanel.add(bookIdSpin, gc);
        gc.gridx = 0; gc.gridy = 1; borrowPanel.add(new JLabel("Member ID"), gc);
        gc.gridx = 1; borrowPanel.add(memberIdSpin, gc);

        JButton issueBtn = new JButton("Issue (Borrow)");
        issueBtn.addActionListener((ActionEvent e) -> {
            int bookId = (int) bookIdSpin.getValue();
            int memberId = (int) memberIdSpin.getValue();
            issueBook(bookId, memberId);
        });

        JButton returnBtn = new JButton("Return Selected Loan");
        returnBtn.addActionListener(e -> {
            int row = loansTable.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Select a loan to return (row with ReturnDate empty).");
                return;
            }
            Object loanIdObj = loansModel.getValueAt(row, 0);
            Object returnDateObj = loansModel.getValueAt(row, 6);
            if (returnDateObj != null && !returnDateObj.toString().isBlank()) {
                JOptionPane.showMessageDialog(this, "This loan is already returned.");
                return;
            }
            int loanId = Integer.parseInt(loanIdObj.toString());
            returnBook(loanId);
        });

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> refreshLoans());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.add(issueBtn);
        actions.add(returnBtn);
        actions.add(refreshBtn);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        top.add(borrowPanel, BorderLayout.WEST);
        top.add(actions, BorderLayout.SOUTH);

        root.add(top, BorderLayout.NORTH);
        root.add(new JScrollPane(loansTable), BorderLayout.CENTER);
        return root;
    }

    private void issueBook(int bookId, int memberId) {
        String checkCopies = "SELECT copies FROM books WHERE id=?";
        String decCopies = "UPDATE books SET copies=copies-1 WHERE id=? AND copies>0";
        String insertLoan = "INSERT INTO loans(book_id, member_id, loan_date, return_date) VALUES (?, ?, ?, NULL)";
        try (Connection con = getCon()) {
            con.setAutoCommit(false);
            try (PreparedStatement chk = con.prepareStatement(checkCopies)) {
                chk.setInt(1, bookId);
                try (ResultSet rs = chk.executeQuery()) {
                    if (!rs.next()) {
                        JOptionPane.showMessageDialog(this, "Book ID not found.");
                        con.rollback(); return;
                    }
                    int copies = rs.getInt(1);
                    if (copies <= 0) {
                        JOptionPane.showMessageDialog(this, "No copies available.");
                        con.rollback(); return;
                    }
                }
            }
            try (PreparedStatement dec = con.prepareStatement(decCopies)) {
                dec.setInt(1, bookId);
                int updated = dec.executeUpdate();
                if (updated == 0) {
                    JOptionPane.showMessageDialog(this, "Failed to decrement copies.");
                    con.rollback(); return;
                }
            }
            try (PreparedStatement ins = con.prepareStatement(insertLoan)) {
                ins.setInt(1, bookId);
                ins.setInt(2, memberId);
                ins.setDate(3, Date.valueOf(LocalDate.now()));
                ins.executeUpdate();
            }
            con.commit();
            JOptionPane.showMessageDialog(this, "Book issued!");
            refreshBooks();
            refreshLoans();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void returnBook(int loanId) {
        String getLoan = "SELECT book_id, return_date FROM loans WHERE id=?";
        String setReturn = "UPDATE loans SET return_date=? WHERE id=? AND return_date IS NULL";
        String incCopies = "UPDATE books SET copies=copies+1 WHERE id=?";
        try (Connection con = getCon()) {
            con.setAutoCommit(false);
            int bookId;
            try (PreparedStatement ps = con.prepareStatement(getLoan)) {
                ps.setInt(1, loanId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { JOptionPane.showMessageDialog(this, "Loan not found."); con.rollback(); return; }
                    if (rs.getDate("return_date") != null) { JOptionPane.showMessageDialog(this, "Already returned."); con.rollback(); return; }
                    bookId = rs.getInt("book_id");
                }
            }
            try (PreparedStatement ps = con.prepareStatement(setReturn)) {
                ps.setDate(1, Date.valueOf(LocalDate.now()));
                ps.setInt(2, loanId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(incCopies)) {
                ps.setInt(1, bookId);
                ps.executeUpdate();
            }
            con.commit();
            JOptionPane.showMessageDialog(this, "Book returned!");
            refreshBooks();
            refreshLoans();
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void refreshLoans() {
        String sql = """
                SELECT l.id, b.id AS book_id, b.title, m.id AS member_id, m.name, l.loan_date, l.return_date
                FROM loans l
                JOIN books b ON l.book_id=b.id
                JOIN members m ON l.member_id=m.id
                ORDER BY l.id
                """;
        loadToTable(sql, loansModel);
    }

    // ====================== UTIL ======================

    private Connection getCon() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    private void loadToTable(String sql, DefaultTableModel model) {
        try (Connection con = getCon();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            // Build rows
            Vector<String> cols = new Vector<>();
            int colCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= colCount; i++) cols.add(rs.getMetaData().getColumnLabel(i));

            // Replace model content safely on EDT
            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= colCount; i++) row.add(rs.getObject(i));
                data.add(row);
            }
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                for (Vector<Object> r : data) model.addRow(r.toArray());
            });
        } catch (SQLException ex) {
            showError(ex);
        }
    }

    private void showError(Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
