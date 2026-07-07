package model;

public class Borrow {
    private Long sernum;//Serial Number 序号
    private Long bookId;
    private int readerId;
    private String lendDate;
    private String backDate;
    private String bookName;
    private String readerName;

    public Borrow() {}

    public Long getSernum() { return sernum; }
    public void setSernum(Long sernum) { this.sernum = sernum; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public int getReaderId() { return readerId; }
    public void setReaderId(int readerId) { this.readerId = readerId; }

    public String getLendDate() { return lendDate; }
    public void setLendDate(String lendDate) { this.lendDate = lendDate; }

    public String getBackDate() { return backDate; }
    public void setBackDate(String backDate) { this.backDate = backDate; }

    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }

    public String getReaderName() { return readerName; }
    public void setReaderName(String readerName) { this.readerName = readerName; }

    @Override
    public String toString() {
        return "Borrow{sernum=" + sernum + ", bookId=" + bookId + ", readerId=" + readerId + "}";
    }
}