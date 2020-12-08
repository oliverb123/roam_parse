package eu.obrowne;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

public class Node {
    public List<Node> children = new ArrayList<>();
    public Type type;
    public int startPos;
    public int endPos;

    public static List<Node> extract(List<Atom> atoms, int current, BiPredicate<Integer, Integer> done, int currentDepth) {
        final var nodes = new ArrayList<Node>();
        while(current < atoms.size() && !done.test(current, currentDepth)) {
            /*
             * What do we want to do here:
             * - Check if the next run of atoms is a complete valid node, and if so add that node to the list
             * - If not, check if the next run is a valid node start, and if so call extract() on the nodes following
             *   this node start, and then set the returned nodes as this ones children and close this node
             * - otherwise, or if the above only only finishes because you've reached the end of the atoms, mark this
             *   node as invalid and plod on.
             */

            var node = new Node();

            // Simple full match to a node type
            var full = Type.firstFullMatch(atoms, current);
            if(full.isPresent()) {
                node.type = full.get();
                node.startPos = atoms.get(current).pos;
                current += node.type.fullWidth;
                node.endPos = atoms.get(current - 1).pos;
                nodes.add(node);
                continue;
            }

            // Partial match to a node type
            var start = Type.firstStart(atoms, current);
            if(start.isPresent()) {
                node.type = start.get();
                node.startPos = atoms.get(current).pos;
                var children = extract(
                        atoms,
                        current + node.type.startWidth,
                        (p, d) -> d == currentDepth+1 && node.type.isEnd.test(atoms, p),
                        currentDepth+1);
                var next = current + node.type.startWidth + children.stream().mapToInt(Node::getAtomWidth).sum();
                if(!node.type.isEnd.test(atoms, next)) {
                    node.type = Type.INVALID;
                    nodes.add(node);
                    node.endPos = node.startPos;
                    current += 1;
                } else {
                    current = next;
                    current += node.type.fullWidth - node.type.startWidth;
                    node.endPos = atoms.get(current - 1).pos;
                    node.children = children;
                    nodes.add(node);
                }
                continue;
            }
            node.type = Type.INVALID;
            node.startPos = atoms.get(current).pos;
            node.endPos = node.startPos;
            current += 1;
        }
        return nodes;
    }

    public String toString() {
        return "Node(" + type + ", " + startPos + ", " + endPos + ", " + children + ")";
    }

    private int getAtomWidth() {
        return type.fullWidth + children.stream().mapToInt(Node::getAtomWidth).sum();
    }

    public static enum Type {
        LINK(
                (a, p) -> isPair(a, p, Atom.Type.SQUARE_OPEN) && isPair(a, p+2, Atom.Type.SQUARE_CLOSED),
                (a, p) -> isPair(a, p, Atom.Type.SQUARE_OPEN),
                (a, p) -> isPair(a, p, Atom.Type.SQUARE_CLOSED), 4, 2
        ),
        REF(
                (a, p) -> isPair(a, p, Atom.Type.ROUND_OPEN) && isPair(a, p+2, Atom.Type.ROUND_CLOSED),
                (a, p) -> isPair(a, p, Atom.Type.ROUND_OPEN),
                (a, p) -> isPair(a, p, Atom.Type.ROUND_CLOSED), 4, 2),
        RENDER(
                (a, p) -> isPair(a, p, Atom.Type.CURL_OPEN) && isPair(a, p+2, Atom.Type.CURL_CLOSED),
                (a, p) -> isPair(a, p, Atom.Type.CURL_OPEN),
                (a, p) -> isPair(a, p, Atom.Type.CURL_CLOSED), 4, 2),
        LATEX(
                (a, p) -> isRun(a, p, Atom.Type.DOLLAR, 4),
                (a, p) -> isPair(a, p, Atom.Type.DOLLAR),
                (a, p) -> isPair(a, p, Atom.Type.DOLLAR), 4, 2),
        ALIAS_START(
                (a, p) -> matches(a, p, Atom.Type.SQUARE_OPEN, Atom.Type.SQUARE_CLOSED),
                (a, p) -> a.get(p).type == Atom.Type.SQUARE_OPEN,
                (a, p) -> a.get(p).type == Atom.Type.SQUARE_CLOSED, 2, 1),
        ALIAS_END(
                (a, p) -> matches(a, p, Atom.Type.ROUND_OPEN, Atom.Type.ROUND_CLOSED),
                (a, p) -> a.get(p).type == Atom.Type.ROUND_OPEN,
                (a, p) -> a.get(p).type == Atom.Type.ROUND_CLOSED, 2, 1),
        HIGHLIGHT(
                (a, p) -> isRun(a, p, Atom.Type.CARET, 4),
                (a, p) -> isPair(a, p, Atom.Type.CARET),
                (a, p) -> isPair(a, p, Atom.Type.CARET), 4, 2),
        BOLD(
                (a, p) -> isRun(a, p, Atom.Type.STAR, 4),
                (a, p) -> isPair(a, p, Atom.Type.STAR),
                (a, p) -> isPair(a, p, Atom.Type.STAR), 4, 2),
        ITALIC(
                (a, p) -> isRun(a, p, Atom.Type.UNDER, 4),
                (a, p) -> isPair(a, p, Atom.Type.UNDER),
                (a, p) -> isPair(a, p, Atom.Type.UNDER), 4, 2),
        INVALID(
                (a, p) -> false,
                (a, p) -> false,
                (a, p) -> false, 1, 1),
        ESCAPE(
                (a, p) -> a.get(p).type == Atom.Type.EXCLAIM,
                (a, p) -> a.get(p).type == Atom.Type.EXCLAIM,
                (a, p) -> a.get(p).type == Atom.Type.EXCLAIM,
                1,
                1);

        public final BiPredicate<List<Atom>, Integer> is;
        public final BiPredicate<List<Atom>, Integer> isStart;
        public final BiPredicate<List<Atom>, Integer> isEnd;
        public final int fullWidth;
        public final int startWidth;

        Type(BiPredicate<List<Atom>, Integer> is, BiPredicate<List<Atom>, Integer> isStart, BiPredicate<List<Atom>, Integer> isEnd,  int fullWidth, int startWidth) {
            this.is = is;
            this.isStart = isStart;
            this.isEnd = isEnd;
            this.fullWidth = fullWidth;
            this.startWidth = startWidth;
        }

        public static Optional<Type> firstFullMatch(List<Atom> atoms, int position) {
            for(var t: Type.values()) {
                if(t.is.test(atoms, position)) return Optional.of(t);
            }
            return Optional.empty();
        }

        public static Optional<Type> firstStart(List<Atom> atoms, int position) {
            for(var t: Type.values()) {
                if(t.isStart.test(atoms, position)) return Optional.of(t);
            }
            return Optional.empty();
        }

        private static boolean isPair(List<Atom> a, int p, Atom.Type t) {
            return isRun(a, p, t, 2);
        }

        private static boolean isRun(List<Atom> a, int p, Atom.Type t, int length) {
            if (a.size() < p + length) return false;
            for(int i = p; i < p + length; i++) {
                if(a.get(i).type != t) return false;
            }
            return true;
        }

        private static boolean matches(List<Atom> a, int p, Atom.Type... types){
            if(a.size() < p + types.length) return false;
            for(int i = 0; i < types.length; i++) {
                if(types[i] != a.get(p+i).type) return false;
            }
            return true;
        }
    }

}
