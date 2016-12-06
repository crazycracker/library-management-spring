package edu.sjsu.cmpe275.lms.dao;

import edu.sjsu.cmpe275.lms.email.SendEmail;
import edu.sjsu.cmpe275.lms.entity.Book;

import javax.persistence.*;

import edu.sjsu.cmpe275.lms.entity.Book;
import edu.sjsu.cmpe275.lms.entity.User;
import edu.sjsu.cmpe275.lms.entity.UserBook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;


import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;


@Transactional
@Repository
public class BookDaoImpl implements BookDao {
    @PersistenceContext(type = PersistenceContextType.EXTENDED)
    private EntityManager entityManager;

    /**
     * Add a book into database
     *
     * @param book
     */


    @Autowired
    private SendEmail eMail;

    @Override
    public boolean addBook(Book book) {
        entityManager.persist(book);
        return true;
    }

    @Override

    public boolean addBook(String isbn, String author, String title, String callnumber, String publisher, String year_of_publication, String location, int num_of_copies, String current_status, String keywords, byte[] image) {
        Book book = new Book(isbn, author, title, callnumber, publisher, year_of_publication, location, num_of_copies, current_status, keywords, image);

        entityManager.persist(book);
        return true;
    }

    /**
     * Return the book by isbn code
     *
     * @param isbn
     * @return book object
     */
    @Override
    public Book getBookByISBN(String isbn) {
        return entityManager.find(Book.class, isbn);
    }


    @Override
    public List<Book> findAll() {
        List<Book> books = (List<Book>) entityManager.createQuery("select b from Book b", Book.class).getResultList();

        return books;

    }

    /* (non-Javadoc)
     * @see edu.sjsu.cmpe275.lms.dao.BookDao#setBookRequest(edu.sjsu.cmpe275.lms.entity.User)
     */
    @Override
    public String setBookRequest(Integer bookId, Integer userId) {
        // TODO Auto-generated method stub
        Book book = entityManager.find(Book.class, bookId);
        User user = entityManager.find(User.class, userId);


        String returnStatus = "";
        if (book.getCurrent_status().equalsIgnoreCase("available")) {

            List<UserBook> currentUsers = book.getCurrentUsers();
            UserBook userBook = new UserBook(book, user, LocalDate.now(), 0);
            currentUsers.add(userBook);
            book.setCurrentUsers(currentUsers);
            entityManager.merge(userBook);
            userBook.UserBookPersist(book, user);
            String due_date = userBook.getDueDate();
            returnStatus = "User request for the book successful \n The Due date is "+due_date;
            eMail.sendMail(user.getUseremail(), returnStatus, returnStatus);

            updateBookStatus(book.getBookId());
            return returnStatus;

        } else {
            List<User> waitlist = book.getWaitlist();
            if (!waitlist.contains(user)) {
                waitlist.add(user);
                book.setWaitlist(waitlist);
                entityManager.merge(book);
                returnStatus = "User is waitlisted! Waitlist number is " + (book.getWaitlist().indexOf(user) + 1);
                eMail.sendMail(user.getUseremail(), returnStatus, returnStatus);

            } else {
                returnStatus = "User has already requested for the book! Waitlist number is " + (book.getWaitlist().indexOf(user) + 1);
            }
            return returnStatus;
        }


    }

    /* (non-Javadoc)
     * @see edu.sjsu.cmpe275.lms.dao.BookDao#getBookbyId(java.lang.Integer)
     */
    @Override
    public Book getBookbyId(Integer bookId) {

        Book book = entityManager.find(Book.class, bookId);
        return book;
    }

    @Override
    public void updateBookStatus(Integer book_Id) {
        String book_query = "select b from Book b where b.bookId = " + book_Id;

        Book book = (Book) entityManager.createQuery(book_query, Book.class).getSingleResult();

        System.out.println("book " + book.getBookId());

        String userbook_query = "select ub from UserBook ub where ub.book.bookId = " + book_Id;


        List<UserBook> userBooks = entityManager.createQuery(userbook_query, UserBook.class).getResultList();

        System.out.println("userbook " + userBooks.size());

        if (book.getNum_of_copies() == userBooks.size()) {
            System.out.println("changing status");
            book.setCurrent_status("Hold");
            entityManager.merge(book);
        }
    }

}
