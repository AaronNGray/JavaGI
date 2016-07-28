package javagi.casestudies.publicationbrowser;

public class ConferencePublication extends Publication {
    String conferenceTitle;

    public ConferencePublication(String s1, String s2, Publication... references) {
        super(s2, references);
        conferenceTitle = s1;
    }
}