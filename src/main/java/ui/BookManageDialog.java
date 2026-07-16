package ui;

import client.ClientNetworkService;
import model.Book;
import model.ClassInfo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * 图书管理对话框 — 实验九 C/S 模式
 * 通过 ClientNetworkService 与后台服务器通信
 */
public class BookManageDialog extends JDialog {

    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> classCombo;

    public BookManageDialog(Frame owner) {
        super(owner, "图书管理 (C/S模式)", true);
        setSize(1000, 550);
        setLocationRelativeTo(owner);

        // 工具栏
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        searchField = new JTextField(12);
        JButton searchBtn = new JButton("搜索");
        JButton addBtn = new JButton("添加图书");
        JButton editBtn = new JButton("编辑图书");
        JButton deleteBtn = new JButton("删除图书");
        JButton refreshBtn = new JButton("刷新");

        classCombo = new JComboBox<>();
        classCombo.addItem("全部分类");
        try {
            for (ClassInfo ci : ClientNetworkService.getInstance().getAllClasses()) {
                classCombo.addItem(ci.getClassId() + " " + ci.getClassName());
            }
        } catch (IOException ignored) {}
        JButton filterBtn = new JButton("筛选");

        topPanel.add(new JLabel("关键词:"));
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        topPanel.add(new JLabel("分类:"));
        topPanel.add(classCombo);
        topPanel.add(filterBtn);
        topPanel.add(addBtn);
        topPanel.add(editBtn);
        topPanel.add(deleteBtn);
        topPanel.add(refreshBtn);

        // 表格
        tableModel = new DefaultTableModel(
                new String[]{"图书ID", "书名", "作者", "出版社", "ISBN", "分类", "价格", "出版日期", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        searchBtn.addActionListener(e -> doSearch());
        filterBtn.addActionListener(e -> doFilter());
        addBtn.addActionListener(e -> showEditDialog(null));
        editBtn.addActionListener(e -> editSelected());
        deleteBtn.addActionListener(e -> deleteSelected());
        refreshBtn.addActionListener(e -> refreshAll());

        refreshAll();
    }

    private void refreshAll() {
        try {
            loadBooks(ClientNetworkService.getInstance().findAllBooks());
        } catch (IOException e) {
            showError("无法加载图书列表: " + e.getMessage());
        }
    }

    private void loadBooks(List<Book> books) {
        tableModel.setRowCount(0);
        for (Book b : books) {
            tableModel.addRow(new Object[]{
                    b.getBookId(), b.getBookName(), b.getAuthor(), b.getPublish(), b.getIsbn(),
                    b.getClassName(), b.getPrice(),
                    b.getPubdate(), b.getState() == 1 ? "已借出" : "在馆"
            });
        }
    }

    private void doSearch() {
        String kw = searchField.getText().trim();
        try {
            if (kw.isEmpty()) {
                loadBooks(ClientNetworkService.getInstance().findAllBooks());
            } else {
                loadBooks(ClientNetworkService.getInstance().searchBooks(kw));
            }
        } catch (IOException e) {
            showError("搜索失败: " + e.getMessage());
        }
    }

    private void doFilter() {
        String selected = (String) classCombo.getSelectedItem();
        if (selected == null || "全部分类".equals(selected)) {
            refreshAll();
        } else {
            try {
                int classId = Integer.parseInt(selected.split(" ")[0]);
                loadBooks(ClientNetworkService.getInstance().findByClassId(classId));
            } catch (Exception e) {
                showError("筛选失败: " + e.getMessage());
            }
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { showError("请先选择一本图书"); return; }
        Long bookId = (Long) tableModel.getValueAt(row, 0);
        try {
            Book book = ClientNetworkService.getInstance().findBookById(bookId);
            if (book != null) showEditDialog(book);
        } catch (IOException e) {
            showError("查询失败: " + e.getMessage());
        }
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { showError("请先选择一本图书"); return; }
        Long bookId = (Long) tableModel.getValueAt(row, 0);
        int r = JOptionPane.showConfirmDialog(this, "确认删除该图书?");
        if (r == JOptionPane.YES_OPTION) {
            try {
                if (ClientNetworkService.getInstance().deleteBook(bookId)) {
                    refreshAll();
                } else {
                    showError("删除失败");
                }
            } catch (IOException e) {
                showError("删除失败: " + e.getMessage());
            }
        }
    }

    private void showEditDialog(Book book) {
        boolean isEdit = book != null;
        JDialog dialog = new JDialog(this, isEdit ? "编辑图书" : "添加图书", true);
        dialog.setSize(500, 500);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 10, 4, 10);

        JTextField nameField = new JTextField(isEdit ? book.getBookName() : "", 18);
        JTextField authorField = new JTextField(isEdit ? book.getAuthor() : "", 18);
        JTextField pubField = new JTextField(isEdit ? book.getPublish() : "", 18);
        JTextField isbnField = new JTextField(isEdit ? book.getIsbn() : "", 18);
        JTextArea introArea = new JTextArea(isEdit && book.getIntroduction() != null ? book.getIntroduction() : "", 3, 18);
        introArea.setLineWrap(true);
        JScrollPane introScroll = new JScrollPane(introArea);
        JTextField langField = new JTextField(isEdit ? book.getBookLanguage() : "中文", 18);
        JTextField priceField = new JTextField(isEdit && book.getPrice() != null ? book.getPrice().toString() : "0.00", 18);
        JTextField dateField = new JTextField(isEdit && book.getPubdate() != null ? book.getPubdate() : "", 18);
        JTextField pressmarkField = new JTextField(isEdit && book.getPressmark() != null ? String.valueOf(book.getPressmark()) : "", 18);

        JComboBox<String> classComboLocal = new JComboBox<>();
        try {
            for (ClassInfo ci : ClientNetworkService.getInstance().getAllClasses()) {
                classComboLocal.addItem(ci.getClassId() + " " + ci.getClassName());
                if (isEdit && book.getClassId() != null && book.getClassId() == ci.getClassId()) {
                    classComboLocal.setSelectedItem(ci.getClassId() + " " + ci.getClassName());
                }
            }
        } catch (IOException ignored) {}

        String[] labels = {"书名:", "作者:", "出版社:", "ISBN:", "简介:", "语言:", "价格:", "出版日期:", "分类:", "索书号:"};
        java.awt.Component[] fields = {nameField, authorField, pubField, isbnField, introScroll,
                langField, priceField, dateField, classComboLocal, pressmarkField};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i; gbc.anchor = GridBagConstraints.EAST;
            panel.add(new JLabel(labels[i]), gbc);
            gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = i == 4 ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
            panel.add(fields[i], gbc);
        }

        JButton saveBtn = new JButton("保存");
        gbc.gridx = 0; gbc.gridy = labels.length; gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        panel.add(saveBtn, gbc);

        saveBtn.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                String author = authorField.getText().trim();
                if (name.isEmpty() || author.isEmpty()) {
                    showError("书名和作者不能为空"); return;
                }
                String selected = (String) classComboLocal.getSelectedItem();
                Integer classId = null;
                if (selected != null && !selected.isEmpty()) {
                    classId = Integer.parseInt(selected.split(" ")[0]);
                }

                Book current = isEdit ? book : new Book();
                current.setBookName(name);
                current.setAuthor(author);
                current.setPublish(pubField.getText().trim());
                current.setIsbn(isbnField.getText().trim());
                current.setIntroduction(introArea.getText().trim());
                current.setBookLanguage(langField.getText().trim());
                current.setPrice(new BigDecimal(priceField.getText().trim()));
                current.setPubdate(dateField.getText().trim());
                current.setClassId(classId);
                String pm = pressmarkField.getText().trim();
                current.setPressmark(pm.isEmpty() ? null : Integer.parseInt(pm));
                if (!isEdit) current.setState(0);

                boolean ok = isEdit
                        ? ClientNetworkService.getInstance().updateBook(current)
                        : ClientNetworkService.getInstance().addBook(current);
                if (ok) {
                    dialog.dispose();
                    refreshAll();
                } else {
                    showError("保存失败");
                }
            } catch (NumberFormatException ex) {
                showError("请检查数字字段格式");
            } catch (IOException ex) {
                showError("通信失败: " + ex.getMessage());
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg);
    }
}
