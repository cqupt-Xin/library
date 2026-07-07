package ui;

import model.Borrow;
import model.User;
import service.BorrowService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class BorrowDialog extends JDialog {

    private final BorrowService borrowService = new BorrowService();
    private final User currentUser;
    private JTable table;
    private DefaultTableModel tableModel;

    public BorrowDialog(Frame owner, User user) {
        super(owner, "借阅管理", true);
        this.currentUser = user;
        setSize(950, 500);
        setLocationRelativeTo(owner);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        if (user == null || "admin".equals(user.getRole())) {
            JButton allBtn = new JButton("全部记录");
            JButton activeBtn = new JButton("未归还");
            JButton returnBtn = new JButton("归还");
            topPanel.add(allBtn);
            topPanel.add(activeBtn);
            topPanel.add(returnBtn);

            allBtn.addActionListener(e -> loadBorrows(borrowService.findAll()));
            activeBtn.addActionListener(e -> loadBorrows(borrowService.findActive()));
            returnBtn.addActionListener(e -> doReturn());
        } else {
            JButton borrowBtn = new JButton("借书");
            JButton returnBtn = new JButton("归还");
            JButton myBtn = new JButton("我的借阅");
            topPanel.add(borrowBtn);
            topPanel.add(returnBtn);
            topPanel.add(myBtn);

            borrowBtn.addActionListener(e -> doBorrow());
            returnBtn.addActionListener(e -> doReturn());
            myBtn.addActionListener(e -> loadBorrows(borrowService.findByReaderId(currentUser.getId())));
        }

        tableModel = new DefaultTableModel(
                new String[]{"记录号", "图书ID", "书名", "读者ID", "读者名", "借阅日期", "应归还日期", "状态"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        if (user == null || "admin".equals(user.getRole())) {
            loadBorrows(borrowService.findAll());
        } else {
            loadBorrows(borrowService.findByReaderId(currentUser.getId()));
        }
    }

    private void loadBorrows(List<Borrow> list) {
        tableModel.setRowCount(0);
        for (Borrow b : list) {
            String status = b.getBackDate() == null ? "借出中" : "已归还";
            tableModel.addRow(new Object[]{
                    b.getSernum(), b.getBookId(), b.getBookName(),
                    b.getReaderId(), b.getReaderName(),
                    b.getLendDate(), b.getBackDate(), status
            });
        }
    }

    private void doBorrow() {
        // 管理员代读者借书（需要输入读者ID和图书ID）
        JTextField bookIdField = new JTextField(10);
        JTextField readerIdField = new JTextField(10);

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("图书ID:"));
        panel.add(bookIdField);
        panel.add(new JLabel("读者ID:"));
        panel.add(readerIdField);

        int r = JOptionPane.showConfirmDialog(this, panel, "借书", JOptionPane.OK_CANCEL_OPTION);
        if (r != JOptionPane.OK_OPTION) return;

        try {
            long bookId = Long.parseLong(bookIdField.getText().trim());
            int readerId = Integer.parseInt(readerIdField.getText().trim());

            if (borrowService.borrowBook(bookId, readerId)) {
                JOptionPane.showMessageDialog(this, "借阅成功");
                loadBorrows(borrowService.findAll());
            } else {
                JOptionPane.showMessageDialog(this, "借阅失败");
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "请输入有效的数字");
        }
    }

    private void doReturn() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选择一条借阅记录"); return;
        }
        String status = (String) tableModel.getValueAt(row, 7);
        if (!"借出中".equals(status)) {
            JOptionPane.showMessageDialog(this, "该记录已归还"); return;
        }

        long sernum = (Long) tableModel.getValueAt(row, 0);
        long bookId = (Long) tableModel.getValueAt(row, 1);

        if (borrowService.returnBook(sernum, bookId)) {
            JOptionPane.showMessageDialog(this, "归还成功");
            if (currentUser == null || "admin".equals(currentUser.getRole())) {
                loadBorrows(borrowService.findAll());
            } else {
                loadBorrows(borrowService.findByReaderId(currentUser.getId()));
            }
        } else {
            JOptionPane.showMessageDialog(this, "归还失败");
        }
    }
}
