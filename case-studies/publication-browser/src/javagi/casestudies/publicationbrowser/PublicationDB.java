package javagi.casestudies.publicationbrowser;

public class PublicationDB {
    
    private static Publication cham = 
        popl("The reflexive CHAM and the join-calculus");
    private static Publication typesAsModels = 
        popl("Types as models: Model checking message-passing programs");

    public static final Publication root =
        oopsla("Join patterns for visual basic",
               new Book("Concurrent programming in ERLANG", "Prentice Hall"),
               ecoop("Modern Concurrency Abstractions for C#",
                     typesAsModels,
                     cham),
               toplas("Modern Concurrency Abstractions for C#",
                      typesAsModels,
                      cham),
               cham);
    
    private static Publication oopsla(String s, Publication... refs) { 
        return new OOPSLAPublication("OOPSLA", s, refs);
    }

    private static Publication popl(String s, Publication... refs) { 
        return new ConferencePublication("POPL", s, refs);
    }

    private static Publication ecoop(String s, Publication... refs) { 
        return new ConferencePublication("ECOOP", s, refs);
    }

    private static Publication toplas(String s, Publication... refs) {
        return new JournalPublication("TOPLAS", s, refs);
    }
}