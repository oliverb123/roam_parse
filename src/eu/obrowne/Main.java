package eu.obrowne;

import java.time.Duration;
import java.time.LocalDateTime;


/**
 * Some notes:
 *
 * - Right now alias starts and ends are two different nodes, which I think is desirable. Adding alias linking would be trivial as a very inexpensive final pass,
 *   since alias start/end pairs necessarily live at the same "level" in the node tree
 * - "return a tree" - the parser returns a list of top level nodes, each of which might have N child nodes within it. Think of it like a tree
 *   with the roots cut
 * - "Update-in-place" left as an exercise to the reader (for now, it's late)
 *    - since re-parsing is basically free speed wise for any reasonable data set and the nodes refer to spans in the parsed text
 *      naive solution would be to simply produce a new string using the span data of the node being updated and the replacement string
 *      content, and then re-parsing that string immediately. Beyond that, a good update-in-place would need to update all nodes after the
 *      update location (so walk the tree "forwards and downwards"), to account for any difference in length. Not too bad really, but I'm feeling lazy
 * - Since String in java is immutable, it is assumed the underlying data the spans in the nodes refer to is not modified. A "deployment-ready"
 *   implementation would wrap the raw Node in a Tree structure or similar, and should probably also store a ref to the raw data. This would also
 *   make implementation of the update in place stuff described above easier.
 * - Custom parsers/extended grammar were vaguely defined, but adding any new Node type is a matter of figuring out how it starts
 *   and ends, and then writing an additional Node.Type to implement that particular Atom sequence. Additional atoms are the easiest way to do this
 *
 * */
public class Main {

    public static void main(String[] args) {
	    var test = "{{[[TODO]]}} The parser is recursive - it can handle [[Nested [[Links]]]] and ^^**bold highlights**^^ and `[html roam]([[Aliases]])`all the ones we haven't done yet as well";
        var test2 = "[![img]([[image-as-alias.com]])](www.roamreasearch.com)";
        var test3 = "[html roam]([[Aliases]])";
        var test4 = "{{{{curly braces}} in{{side}} of {{curly braces}}}}";
        var test5 = "[[[some]([[[thing]])"; // test handling of invalid leading potential node starts ([[ at position 0 and [ before the link in the alias_end)
        for(int i = 0; i < 100; i++) {
            var s = LocalDateTime.now();
            for (int j = 0; j < 1000; j++) {
                Node.extract(Atom.atomize(test), 0, (p, d) -> false, 0);
                Node.extract(Atom.atomize(test2), 0, (p, d) -> false, 0);
                Node.extract(Atom.atomize(test3), 0, (p, d) -> false, 0);
                Node.extract(Atom.atomize(test4), 0, (p, d) -> false, 0);
                Node.extract(Atom.atomize(test5), 0, (p, d) -> false, 0);
            }
            // Averaging around 30-50 ms on my machine, with a max of ~170 ms for first loop.
            // Worth noting these "lines" are terribly short, and I'm running 5000 parses, not 1000
            System.out.println(Duration.between(s, LocalDateTime.now()).toMillis());
        }
        System.out.println(Node.extract(Atom.atomize(test), 0, (p, d) -> false, 0)); // render[link], link[link], highlight[bold], alias_start, alias_end[link]
        System.out.println(Node.extract(Atom.atomize(test2), 0, (p, d) -> false, 0));// alias_start[escape, alias_start, alias_end[link]], alias_end
        System.out.println(Node.extract(Atom.atomize(test3), 0, (p, d) -> false, 0));// alias_start, alias_end[link]
        System.out.println(Node.extract(Atom.atomize(test4), 0, (p, d) -> false, 0));// render[render, render, render]
        System.out.println(Node.extract(Atom.atomize(test5), 0, (p, d) -> false, 0));// invalid, invalid, alias_start, alias_end[invalid, link]

    }
}
