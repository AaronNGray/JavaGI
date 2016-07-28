package javagi.casestudies.publicationbrowser;

public class Book extends Publication {

    String publisher;

    public Book(String s1, String s2, Publication... references) {
        super(s1, references);
        publisher = s2;
    }
}