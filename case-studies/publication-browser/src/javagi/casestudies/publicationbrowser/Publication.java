package javagi.casestudies.publicationbrowser;

public class Publication {
    Publication citedBy;
    String title;
    Publication[] citations;
    public Publication(String s, Publication... references) { 
        title = s;
        this.citations = references;
        for (Publication p : references) {
            p.citedBy = this;
        }
    }
}
