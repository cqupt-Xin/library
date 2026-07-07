package service;

import dao.BookDao;
import model.Book;
import model.ClassInfo;

import java.util.List;

public class BookService {

    private final BookDao bookDao = new BookDao();

    public List<Book> findAll() { return bookDao.findAll(); }

    public Book findById(Long id) { return bookDao.findById(id); }

    public List<Book> search(String keyword) { return bookDao.search(keyword); }

    public List<Book> findByClassId(Integer classId) { return bookDao.findByClassId(classId); }

    public boolean addBook(Book book) { return bookDao.addBook(book); }

    public boolean updateBook(Book book) { return bookDao.updateBook(book); }

    public boolean deleteBook(Long id) { return bookDao.deleteBook(id); }

    public List<ClassInfo> getAllClasses() { return bookDao.getAllClasses(); }
}
