package app.rekord.domain.matching;

/** What was asked for: one song of a playlist. */
public record MatchQuery(int index, String artist, String title, Double durationSec) {
}
