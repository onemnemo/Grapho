package com.onemnemo.grapho.ui.theme

import java.io.File

/**
 * A reader for the generated `tokens.css`: just enough to find the declarations of a selector and
 * follow `var(--name)` chains down to a literal. It only understands flat rule blocks, which is all
 * the generator writes.
 */
internal class TokensCss(source: String) {
    private val blocks: List<Pair<List<String>, Map<String, String>>>

    init {
        val clean = source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        blocks = Regex("([^{}]+)\\{([^{}]*)\\}").findAll(clean).map { rule ->
            val selectors = rule.groupValues[1].split(',').map { it.trim() }
            val declarations = Regex("--([\\w-]+):\\s*([^;]+);").findAll(rule.groupValues[2])
                .associate { it.groupValues[1] to it.groupValues[2].trim() }
            selectors to declarations
        }.toList()
    }

    /** The declarations of every rule that lists [selector] as one of its selectors, later rules winning. */
    fun scope(selector: String): Map<String, String> = merged(blocks.filter { selector in it.first })

    /** The primitives and every token no theme or density changes: the rules that select only the root. */
    val root: Map<String, String> = merged(blocks.filter { it.first == listOf(":root") })

    private fun merged(rules: List<Pair<List<String>, Map<String, String>>>): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (rule in rules) out.putAll(rule.second)
        return out
    }

    /** The literal value of [name] in [scope], falling back to the root, with every `var()` followed. */
    fun literal(name: String, scope: Map<String, String>): String {
        var value = scope[name] ?: root[name] ?: error("--$name is not declared")
        while (true) {
            val next = VAR_REFERENCE.matchEntire(value)?.groupValues?.get(1) ?: return value
            value = scope[next] ?: root[next] ?: error("--$next is not declared")
        }
    }

    companion object {
        private val VAR_REFERENCE = Regex("var\\(--([\\w-]+)\\)")

        /** Finds the repository copy by walking up from the test's working directory. */
        fun load(): TokensCss {
            val start = File(System.getProperty("user.dir")).absoluteFile
            var dir: File? = start
            while (dir != null) {
                val file = File(dir, "design/tokens/tokens.css")
                if (file.isFile) return TokensCss(file.readText())
                dir = dir.parentFile
            }
            error("design/tokens/tokens.css not found above $start")
        }
    }
}
