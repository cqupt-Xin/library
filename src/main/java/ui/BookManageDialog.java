package ui;

import model.Book;
import model.ClassInfo;
import service.BookService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class BookManageDialog extends JDialog {

    private final BookService bookService = new BookService();
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> classCombo;

    public BookManageDialog(Frame owner) {
        super(owner, "图书管理", true);
        setSize(1000, 550);
        setLocationRelativeTo(owner);

        // 顶部工具栏
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        searchField = new JTextField(12);
        JButton searchBtn = new JButton("搜索");
        JButton addBtn = new JButton("添加图书");
        JButton editBtn = new JButton("编辑图书");
        JButton deleteBtn = new JButton("删除图书");
        JButton refreshBtn = new JButton("刷新");

        classCombo = new JComboBox<>();
        classCombo.addItem("全部分类");
        for (ClassInfo ci : bookService.getAllClasses()) {
            classCombo.addItem(ci.getClassId() + " " + ci.getClassName());
        }
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
        refreshBtn.addActionListener(e -> loadBooks(bookService.findAll()));

        loadBooks(bookService.findAll());
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
        if (kw.isEmpty()) {
            loadBooks(bookService.findAll());
        } else {
            loadBooks(bookService.search(kw));
        }
    }

    private void doFilter() {
        String selected = (String) classCombo.getSelectedItem();
        if (selected == null || "全部分类".equals(selected)) {
            loadBooks(bookService.findAll());
        } else {
            int classId = Integer.parseInt(selected.split(" ")[0]);
            loadBooks(bookService.findByClassId(classId));
        }
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择一本图书"); return; }
        Long bookId = (Long) tableModel.getValueAt(row, 0);
        Book book = bookService.findById(bookId);
        if (book != null) showEditDialog(book);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请先选择一本图书"); return; }
        Long bookId = (Long) tableModel.getValueAt(row, 0);
        int r = JOptionPane.showConfirmDialog(this, "确认删除该图书?");
        if (r == JOptionPane.YES_OPTION) {
            bookService.deleteBook(bookId);
            loadBooks(bookService.findAll());
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

        JComboBox<String> classCombo = new JComboBox<>();
        for (ClassInfo ci : bookService.getAllClasses()) {
            classCombo.addItem(ci.getClassId() + " " + ci.getClassName());
            if (isEdit && book.getClassId() != null && book.getClassId() == ci.getClassId()) {
                classCombo.setSelectedItem(ci.getClassId() + " " + ci.getClassName());
            }
        }

        String[] labels = {"书名:", "作者:", "出版社:", "ISBN:", "简介:", "语言:", "价格:", "出版日期:", "分类:", "索书号:"};
        java.awt.Component[] fields = {nameField, authorField, pubField, isbnField, introScroll, langField, priceField, dateField, classCombo, pressmarkField};

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
                    JOptionPane.showMessageDialog(dialog, "书名和作者不能为空"); return;
                }
                String selected = (String) classCombo.getSelectedItem();
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

                boolean ok = isEdit ? bookService.updateBook(current) : bookService.addBook(current);
                if (ok) {
                    dialog.dispose();
                    loadBooks(bookService.findAll());
                } else {
                    JOptionPane.showMessageDialog(dialog, "保存失败");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "请检查数字字段格式");
            }
        });

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }
}
