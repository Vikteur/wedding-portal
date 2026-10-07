package app.rekord.domain.matching;

/** How sure the matcher is about a query: the contract's strings are mapped from this elsewhere. */
public enum Bucket {
    AUTO, AMBIGUOUS, UNMATCHED
}
