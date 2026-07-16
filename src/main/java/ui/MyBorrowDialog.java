package ui;

import client.ClientNetworkService;
import model.Borrow;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * 我的借阅对话框 — 实验九 C/S 模式
 * 通过 ClientNetworkService 与服务器通信
 */
public class MyBorrowDialog extends JDialog {

    private final User reader;
    private JTable table;
    private DefaultTableModel tableModel;

    public MyBorrowDialog(Frame owner, User reader) {
        super(owner, "我的借阅 (C/S模式)", true);
        this.reader = reader;
        setSize(850, 450);
        setLocationRelativeTo(owner);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton returnBtn = new JButton("归还");
        JButton refreshBtn = new JButton("刷新");
        topPanel.add(returnBtn);
        topPanel.add(refreshBtn);

        tableModel = new DefaultTableModel(
                new String[]{"记录号", "图书ID", "书名", "借阅日期", "应归还日期", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        returnBtn.addActionListener(e -> doReturn());
        refreshBtn.addActionListener(e -> loadData());

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadData();
    }

    private void loadData() {
        tableModel.setRowCount(0);
        try {
            List<Borrow> list = ClientNetworkService.getInstance().findBorrowsByReaderId(reader.getId());
            for (Borrow b : list) {
                String status = b.getBackDate() == null ? "借出中" : "已归还";
                tableModel.addRow(new Object[]{
                        b.getSernum(), b.getBookId(), b.getBookName(),
                        b.getLendDate(), b.getBackDate(), status
                });
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "加载失败: " + e.getMessage());
        }
    }

    private void doReturn() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一条借阅记录");
            return;
        }
        String status = (String) tableModel.getValueAt(row, 5);
        if (!"借出中".equals(status)) {
            JOptionPane.showMessageDialog(this, "该记录已归还");
            return;
        }

        long sernum = (Long) tableModel.getValueAt(row, 0);
        long bookId = (Long) tableModel.getValueAt(row, 1);

        int r = JOptionPane.showConfirmDialog(this, "确认归还《" + tableModel.getValueAt(row, 2) + "》?");
        if (r != JOptionPane.YES_OPTION) return;

        try {
            if (ClientNetworkService.getInstance().returnBook(sernum, bookId)) {
                JOptionPane.showMessageDialog(this, "归还成功!");
                loadData();
            } else {
                JOptionPane.showMessageDialog(this, "归还失败");
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "归还失败: " + e.getMessage());
        }
    }
}
