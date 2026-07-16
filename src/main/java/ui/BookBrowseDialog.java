package ui;

import client.ClientNetworkService;
import model.Book;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * 读者借阅浏览对话框 — 实验九 C/S 模式
 * 通过 ClientNetworkService 与服务器通信
 */
public class BookBrowseDialog extends JDialog {

    private final User reader;
    private JTable table;
    private DefaultTableModel tableModel;

    public BookBrowseDialog(Frame owner, User reader) {
        super(owner, "借阅图书 (C/S模式)", true);
        this.reader = reader;
        setSize(1000, 550);
        setLocationRelativeTo(owner);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField searchField = new JTextField(15);
        JButton searchBtn = new JButton("搜索");
        JButton refreshBtn = new JButton("刷新全部");
        JButton borrowBtn = new JButton("借书");
        topPanel.add(new JLabel("搜索:"));
        topPanel.add(searchField);
        topPanel.add(searchBtn);
        topPanel.add(refreshBtn);
        topPanel.add(borrowBtn);

        tableModel = new DefaultTableModel(
                new String[]{"图书ID", "书名", "作者", "出版社", "分类", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(160);
        table.getColumnModel().getColumn(4).setPreferredWidth(100);
        table.getColumnModel().getColumn(5).setPreferredWidth(60);
        JScrollPane scrollPane = new JScrollPane(table);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        searchBtn.addActionListener(e -> {
            String kw = searchField.getText().trim();
            try {
                if (kw.isEmpty()) {
                    loadBooks(ClientNetworkService.getInstance().findAllBooks());
                } else {
                    loadBooks(ClientNetworkService.getInstance().searchBooks(kw));
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "搜索失败: " + ex.getMessage());
            }
        });
        refreshBtn.addActionListener(e -> refreshBooks());
        borrowBtn.addActionListener(e -> doBorrow());

        refreshBooks();
    }

    private void refreshBooks() {
        try {
            loadBooks(ClientNetworkService.getInstance().findAllBooks());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "加载失败: " + e.getMessage());
        }
    }

    private void loadBooks(List<Book> books) {
        tableModel.setRowCount(0);
        for (Book b : books) {
            String stateText;
            if (b.getState() != null && b.getState() == 1) {
                stateText = "已借出";
            } else {
                stateText = "在馆";
            }
            tableModel.addRow(new Object[]{
                    b.getBookId(), b.getBookName(), b.getAuthor(), b.getPublish(),
                    b.getClassName() != null ? b.getClassName() : "", stateText
            });
        }
    }

    private void doBorrow() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一本图书");
            return;
        }
        String state = (String) tableModel.getValueAt(row, 5);
        if ("已借出".equals(state)) {
            JOptionPane.showMessageDialog(this, "该图书已被借出");
            return;
        }

        long bookId = (Long) tableModel.getValueAt(row, 0);

        try {
            if (ClientNetworkService.getInstance().borrowBook(bookId, reader.getId())) {
                JOptionPane.showMessageDialog(this, "借阅成功!");
                refreshBooks();
            } else {
                JOptionPane.showMessageDialog(this, "借阅失败");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "借阅失败: " + e.getMessage());
        }
    }
}
