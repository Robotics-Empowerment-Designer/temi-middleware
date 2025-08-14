package com.example.mqtt


object KeywordChecker {
    fun containsKeyword(text: String, keywords: List<String>): String? {
        // Überprüfen, ob der Text eines der Schlüsselwörter als vollständiges Wort enthält
        // Ignoriere Groß- und Kleinschreibung
        val regexKeywords = keywords.joinToString("|", "(", ")") { "\\b${Regex.escape(it)}\\b" }
        val regex = Regex(regexKeywords, RegexOption.IGNORE_CASE)
        return regex.find(text)?.value
    }

    @JvmStatic
    fun main(args: Array<String>) {
        val text = "I want to say goodbye"
        val keywords = listOf("go", "say", "hello", "goodbye")
        val keyword = containsKeyword(text, keywords)
        if (keyword != null) {
            println("Der Text enthält das Schlüsselwort: $keyword")
        } else {
            println("Der Text enthält keines der Schlüsselwörter: $keywords")
        }
    }
}






