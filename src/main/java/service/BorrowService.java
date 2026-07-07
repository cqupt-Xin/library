package service;

import dao.BookDao;
import dao.BorrowDao;
import model.Book;
import model.Borrow;

import java.time.LocalDate;
import java.util.List;

public class BorrowService {

    private final BorrowDao borrowDao = new BorrowDao();
    private final BookDao bookDao = new BookDao();

    public boolean borrowBook(long bookId, int readerId) {
        Book book = bookDao.findById(bookId);
        if (book == null || (book.getState() != null && book.getState() == 1)) {
            return false;
        }
        String lendDate = LocalDate.now().toString();
        return borrowDao.borrowBook(bookId, readerId, lendDate);
    }

    public boolean returnBook(long sernum, long bookId) {
        return borrowDao.returnBook(sernum, bookId);
    }

    public List<Borrow> findAll() { return borrowDao.findAll(); }

    public List<Borrow> findByReaderId(int readerId) { return borrowDao.findByReaderId(readerId); }

    public List<Borrow> findActive() { return borrowDao.findActive(); }
}
