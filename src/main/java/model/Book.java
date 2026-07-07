package model;

import java.math.BigDecimal;

public class Book {
    private Long bookId;
    private String bookName;
    private String author;
    private String publish;
    private String isbn;
    private String introduction;
    private String bookLanguage;
    private BigDecimal price;
    private String pubdate;
    private Integer classId;
    private String className;
    private Integer pressmark;
    private Integer state;

    public Book() {}

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getPublish() { return publish; }
    public void setPublish(String publish) { this.publish = publish; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getIntroduction() { return introduction; }
    public void setIntroduction(String introduction) { this.introduction = introduction; }

    public String getBookLanguage() { return bookLanguage; }
    public void setBookLanguage(String bookLanguage) { this.bookLanguage = bookLanguage; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getPubdate() { return pubdate; }
    public void setPubdate(String pubdate) { this.pubdate = pubdate; }

    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public Integer getPressmark() { return pressmark; }
    public void setPressmark(Integer pressmark) { this.pressmark = pressmark; }

    public Integer getState() { return state; }
    public void setState(Integer state) { this.state = state; }

    @Override
    public String toString() {
        return "Book{bookId=" + bookId + ", bookName='" + bookName + "', author='" + author + "'}";
    }
}
