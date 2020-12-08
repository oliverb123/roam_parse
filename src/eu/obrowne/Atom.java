package eu.obrowne;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

public class Atom {
    public int pos;
    public Type type;

    public Atom(int pos, Type t){
        this.pos = pos;
        this.type = t; // type of token
    }

    public static List<Atom> atomize(String data) {
        final var tokens = new ArrayList<Atom>();
        int p = 0;
        while(p < data.length()) {
            var match = Type.firstMatching(data, p);
            if(match.isPresent()) {
                var t = match.get();
                tokens.add(new Atom(p, t));
            }
            p += 1;
        }
        return tokens;
    }

    public String toString() {
        return "Token[" + pos + ", " + type + "]";
    }

    public static enum Type {
        SQUARE_OPEN((s, p) -> s.charAt(p) == '['),
        SQUARE_CLOSED((s, p) -> s.charAt(p) == ']'),
        ROUND_OPEN((s, p) -> s.charAt(p) == '('),
        ROUND_CLOSED((s, p) -> s.charAt(p) == ')'),
        CURL_OPEN((s, p) -> s.charAt(p) == '{'),
        CURL_CLOSED((s, p) -> s.charAt(p) == '}'),
        DOLLAR((s, p) -> s.charAt(p) == '$'),
        CARET((s, p) -> s.charAt(p) == '^'),
        STAR((s, p) -> s.charAt(p) == '*'),
        UNDER((s, p) -> s.charAt(p) == '_'),
        EXCLAIM((s, p) -> s.charAt(p) == '!');

        private BiPredicate<String, Integer> matches;

        Type(BiPredicate<String, Integer> matches){
            this.matches = matches;
        }

        public static Optional<Type> firstMatching(String data, int position) {
            for(var t: Type.values()) {
                if(t.matches.test(data, position)) return Optional.of(t);
            }
            return Optional.empty();
        }
    }
}
