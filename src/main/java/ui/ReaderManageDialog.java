package ui;

import model.User;
import service.UserService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ReaderManageDialog extends JDialog {

    private final UserService userService = new UserService();
    private JTable table;
    private DefaultTableModel tableModel;

    public ReaderManageDialog(Frame owner) {
        super(owner, "读者管理", true);
        setSize(600, 400);
        setLocationRelativeTo(owner);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton disableBtn = new JButton("禁用/启用");
        JButton refreshBtn = new JButton("刷新");
        topPanel.add(disableBtn);
        topPanel.add(refreshBtn);

        tableModel = new DefaultTableModel(new String[]{"读者ID", "姓名", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        disableBtn.addActionListener(e -> toggleStatus());
        refreshBtn.addActionListener(e -> loadReaders());

        loadReaders();
    }

    private void loadReaders() {
        tableModel.setRowCount(0);
        List<User> readers = userService.getAllReaders();
        for (User u : readers) {
            tableModel.addRow(new Object[]{u.getId(), u.getUsername(), u.getStatus()});
        }
    }

    private void toggleStatus() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一位读者"); return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String status = (String) tableModel.getValueAt(row, 2);
        if ("正常".equals(status)) {
            userService.disableReader(id);
        } else {
            userService.enableReader(id);
        }
        loadReaders();
    }
}
