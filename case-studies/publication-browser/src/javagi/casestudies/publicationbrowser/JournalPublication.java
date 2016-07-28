package javagi.casestudies.publicationbrowser;

public class JournalPublication extends Publication {
    String journalTitle;

    public JournalPublication(String s1, String s2, Publication... references) {
        super(s2, references);
        journalTitle = s1;
    }
}