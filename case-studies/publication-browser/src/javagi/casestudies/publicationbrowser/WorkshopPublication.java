package javagi.casestudies.publicationbrowser;

public class WorkshopPublication extends Publication {
    String workshopTitle;

    public WorkshopPublication(String s1, String s2, Publication... references) {
        super(s2, references);
        workshopTitle = s1;
    }
}