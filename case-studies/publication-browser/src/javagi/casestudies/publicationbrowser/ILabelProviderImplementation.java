package javagi.casestudies.publicationbrowser;

import javax.swing.Icon;

public implementation ILabelProvider[Publication] {
    public String getText() {
        return title;
    }

    public Icon getIcon() {
        return IconCache.get("publication.png");
    }
}

public implementation ILabelProvider[ConferencePublication] {
    public String getText() {
        return title + "@" + conferenceTitle;
    }

    public Icon getIcon() {
        return IconCache.get("conference.png");
    }
}

public implementation ILabelProvider[OOPSLAPublication] {
    public String getText() {
        return title + "@" + conferenceTitle;
    }

    public Icon getIcon() {
        return IconCache.get("oopsla.png");
    }
}

public implementation ILabelProvider[Book] {
    public String getText() {
        return title + " (" + publisher + ")";
    }

    public Icon getIcon() {
        return IconCache.get("book.png");
    }
}

public implementation ILabelProvider[JournalPublication] {
    public String getText() {
        return title + " [" + journalTitle + "]";
    }

    public Icon getIcon() {
        return IconCache.get("journal.png");
    }
}
