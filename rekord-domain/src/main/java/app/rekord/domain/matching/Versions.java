package app.rekord.domain.matching;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a title into its core name and its version descriptors.
 *
 * <p>{@code "Substitution (Purple Disco Machine Remix)"} becomes core
 * {@code "Substitution"}, descriptor {@code remix}, remixer
 * {@code "purple disco machine"}. The version is then scored as its own facet,
 * so an original and its remixes compare honestly — the entire
 * pick-the-right-version problem rests on this split.
 */
public final class Versions {

    private static final Pattern BRACKET =
            Pattern.compile("\\(([^()]*)\\)|\\[([^\\[\\]]*)\\]|\\{([^{}]*)\\}");
    private static final Pattern INLINE_FEAT =
            Pattern.compile("\\s+(?:feat|ft|featuring)\\.?\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Set<String> FEAT_LEADERS = Set.of("feat", "ft", "featuring", "with");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s{2,}");
    private static final Pattern DIGITS = Pattern.compile("\\d+");

    /**
     * Ordered, longest first, so "radio edit" canonicalises to {@code radio}
     * rather than {@code radio} plus {@code edit}. A null canonical means the
     * phrase says "no version at all" and is erased — "Original Mix" is not a
     * version, it is the absence of one.
     */
    private static final List<Phrase> PHRASES = List.of(
            new Phrase(List.of("original", "mix"), null),
            new Phrase(List.of("original", "version"), null),
            new Phrase(List.of("original"), null),
            new Phrase(List.of("extended", "mix"), "extended"),
            new Phrase(List.of("extended", "version"), "extended"),
            new Phrase(List.of("extended", "edit"), "extended"),
            new Phrase(List.of("extended"), "extended"),
            new Phrase(List.of("radio", "edit"), "radio"),
            new Phrase(List.of("radio", "mix"), "radio"),
            new Phrase(List.of("radio", "version"), "radio"),
            new Phrase(List.of("radio"), "radio"),
            new Phrase(List.of("club", "mix"), "club"),
            new Phrase(List.of("club", "edit"), "club"),
            new Phrase(List.of("dub", "mix"), "dub"),
            new Phrase(List.of("sped", "up"), "spedup"),
            new Phrase(List.of("slowed", "and", "reverb"), "slowed"),
            new Phrase(List.of("slowed", "reverb"), "slowed"),
            new Phrase(List.of("slowed"), "slowed"),
            new Phrase(List.of("a", "cappella"), "acapella"),
            new Phrase(List.of("acapella"), "acapella"),
            new Phrase(List.of("remastered"), "remaster"),
            new Phrase(List.of("remaster"), "remaster"),
            new Phrase(List.of("remix"), "remix"),
            new Phrase(List.of("rmx"), "remix"),
            new Phrase(List.of("edit"), "edit"),
            new Phrase(List.of("mix"), "mix"),
            new Phrase(List.of("dub"), "dub"),
            new Phrase(List.of("vip"), "vip"),
            new Phrase(List.of("bootleg"), "bootleg"),
            new Phrase(List.of("mashup"), "mashup"),
            new Phrase(List.of("rework"), "rework"),
            new Phrase(List.of("refix"), "refix"),
            new Phrase(List.of("flip"), "flip"),
            new Phrase(List.of("live"), "live"),
            new Phrase(List.of("acoustic"), "acoustic"),
            new Phrase(List.of("instrumental"), "instrumental"),
            new Phrase(List.of("cover"), "cover"),
            new Phrase(List.of("version"), "version"));

    /**
     * A segment carrying a keyword is still not a descriptor if it drags a lot
     * of other text with it — "(I Can't Get No)" has to stay in the title.
     */
    private static final int MAX_EXTRA_TOKENS = 4;

    private record Phrase(List<String> tokens, String canonical) {
    }

    /** Descriptors are sorted and deduplicated; the remixer is null when absent. */
    public record TitleParts(String coreTitle, List<String> descriptors, String remixer,
                             List<String> featured) {
    }

    private Versions() {
    }

    public static TitleParts extract(String title) {
        String source = title == null ? "" : title;
        List<String> descriptors = new ArrayList<>();
        List<String> remixerTokens = new ArrayList<>();
        List<String> featured = new ArrayList<>();

        // Bracketed segments first, replacing only the ones that turn out to be
        // version information and leaving the rest of the title intact.
        StringBuilder core = new StringBuilder();
        Matcher m = BRACKET.matcher(source);
        int last = 0;
        while (m.find()) {
            core.append(source, last, m.start());
            String segment = firstNonNull(m.group(1), m.group(2), m.group(3));
            if (!consume(segment, descriptors, remixerTokens, featured)) {
                core.append(m.group(0));
            }
            last = m.end();
        }
        core.append(source.substring(last));
        String result = core.toString();

        // Then trailing " - Radio Edit" suffixes, repeatedly, because
        // " - Edit - Live" happens.
        boolean changed = true;
        while (changed) {
            changed = false;
            int at = result.lastIndexOf(" - ");
            if (at > 0) {
                String head = result.substring(0, at);
                String tail = result.substring(at + 3);
                if (!head.strip().isEmpty()
                        && consume(tail, descriptors, remixerTokens, featured)) {
                    result = head;
                    changed = true;
                }
            }
        }

        Matcher inline = INLINE_FEAT.matcher(result);
        if (inline.find()) {
            featured.add(String.join(" ", Normalize.tokenize(inline.group(1))));
            result = result.substring(0, inline.start());
        }

        result = MULTI_SPACE.matcher(result).replaceAll(" ");
        result = strip(result, " -–—\t");
        if (result.strip().isEmpty()) {
            // Everything looked like a descriptor, which means it was not one:
            // a title of "Remix" is a song called Remix.
            result = source.strip();
        }

        List<String> unique = new ArrayList<>(new TreeSet<>(descriptors));
        String remixer = remixerTokens.isEmpty() ? null : String.join(" ", remixerTokens);
        List<String> names = new ArrayList<>();
        featured.stream().filter(n -> !n.isEmpty()).forEach(names::add);
        return new TitleParts(result, unique, remixer, names);
    }

    /** True when the segment was version information and has been taken. */
    private static boolean consume(String text, List<String> descriptors,
                                   List<String> remixerTokens, List<String> featured) {
        List<String> tokens = Normalize.tokenize(text);
        if (!tokens.isEmpty() && FEAT_LEADERS.contains(tokens.getFirst())) {
            String rest = String.join(" ", tokens.subList(1, tokens.size()));
            if (!rest.isEmpty()) {
                featured.add(rest);
            }
            return true;
        }
        Classified classified = classify(tokens);
        if (classified == null) {
            return false;
        }
        descriptors.addAll(classified.canonicals());
        remixerTokens.addAll(classified.leftover());
        return true;
    }

    private record Classified(List<String> canonicals, List<String> leftover) {
    }

    private static Classified classify(List<String> tokens) {
        if (tokens.isEmpty()) {
            return null;
        }
        List<String> canonicals = new ArrayList<>();
        List<String> leftover = new ArrayList<>();
        boolean matchedAny = false;

        int i = 0;
        while (i < tokens.size()) {
            Phrase hit = null;
            for (Phrase phrase : PHRASES) {
                int end = i + phrase.tokens().size();
                if (end <= tokens.size() && tokens.subList(i, end).equals(phrase.tokens())) {
                    hit = phrase;
                    break;
                }
            }
            if (hit == null) {
                leftover.add(tokens.get(i));
                i++;
            } else {
                matchedAny = true;
                if (hit.canonical() != null) {
                    canonicals.add(hit.canonical());
                }
                i += hit.tokens().size();
            }
        }

        // Bare numbers are not part of a remixer's name — "(Remix 2)".
        leftover.removeIf(token -> DIGITS.matcher(token).matches());
        if (!matchedAny || leftover.size() > MAX_EXTRA_TOKENS) {
            return null;
        }
        return new Classified(canonicals, leftover);
    }

    private static String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return "";
    }

    /** Python's str.strip(chars): both ends, any of the given characters. */
    private static String strip(String s, String chars) {
        int start = 0;
        int end = s.length();
        while (start < end && chars.indexOf(s.charAt(start)) >= 0) {
            start++;
        }
        while (end > start && chars.indexOf(s.charAt(end - 1)) >= 0) {
            end--;
        }
        return s.substring(start, end);
    }
}
