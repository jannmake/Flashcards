package flashcards

import java.util.*
import java.io.*

var log = mutableListOf<String>()

class Card(var id: Long, var mistakes: Int, var term: String, var definition: String) {
    fun check(answer: String, deck: CardDeck):Boolean {
        if (answer == definition) {
            logPrint("Correct answer.")
            return true
        }
        else {
            mistakes++
            val otherCard = deck.getCardForDefinition(answer)
            if (otherCard != null) {
                logPrint("Wrong answer. The right answer is \"$definition\", " +
                "but your definition is correct for \"${otherCard.term}\".")
            } else {
                logPrint("Wrong answer. The right answer is \"$definition\".")
            }
            return false
        }
    }

    fun queryDefinition(scanner: Scanner, deck: CardDeck): Boolean {
        logPrint("Print the definition of \"$term\":")
        val definition = scanner.nextLine()
        return check(definition, deck)
    }
}

class CardDeck(val cards: List<Card>, val terms: Map<String, Long>,
                val definitions: Map<String, Long>) {
    fun getCardForTerm(term: String): Card? {
        val cardId = terms.getOrDefault(term.toLowerCase(), -1)
        if (cardId > -1) {
            return cards.find { it.id == cardId }
        }
        return null
    }

    fun getCardForDefinition(definition: String): Card? {
        val cardId = definitions.getOrDefault(definition.toLowerCase(), -1)
        if (cardId > -1) {
            return cards.find { it.id == cardId }
        }
        return null
    }

    fun getValidCards(): List<Card> {
        return cards.filter { it.id > -1 }
    }
}

fun logPrint(line: String) {
    log.add(line)
    println(line)
}

fun makeCard(ind: Long, mistakes: Int, terms: MutableMap<String, Long>, definitions: MutableMap<String, Long>): Card? {
    logPrint("The card:")
    while (true) {
        val term = readLine()!!
        if (term in terms) {
            logPrint("The card \"$term\" already exists.")
            return null
        }
        logPrint("The definition of the card:")
        val definition = readLine()!!
        if (definition in definitions) {
            logPrint("The definition \"$definition\" already exists")
            return null
        } else {
            terms[term.toLowerCase()] = ind
            definitions[definition.toLowerCase()] = ind
            logPrint("The pair (\"$term\":\"$definition\") has been added.")
            return Card(ind, mistakes, term, definition)
        }
    }
}

fun removeCard(cards: MutableList<Card>, terms: MutableMap<String, Long>, definitions: MutableMap<String, Long>) {
    logPrint("Which card:")
    val term = readLine()?.toLowerCase()!!
    if (term in terms) {
        val id = terms[term]
        val card = cards.filter { it.id == id } [0]
        val definition = card.definition.toLowerCase()
        card.id = -1
        terms.remove(term)
        definitions.remove(definition)
        logPrint("The card has been removed.")
    } else {
        logPrint("Can't remove \"$term\": there is no such card.")
    }
}

fun getNextId(cards: List<Card>): Long {
    val biggie = cards.maxBy { it.id }
    var nextId = biggie?.id ?: 0
    return ++nextId
}

fun importCards(cards: MutableList<Card>, terms: MutableMap<String, Long>, definitions: MutableMap<String, Long>,
                fileName: String = "") {
    var counter = 0
    var fn = fileName
    if (fn.isEmpty()) {
        logPrint("File name:")
        fn = readLine()!!
    }
    val file = File(fn)
    try {
        file.readLines().forEach() {
            val (id, mistakes, term, definition) = it.split("\t")
            if (term.toLowerCase() in terms) {
                val card = cards.find { c -> c.id == terms[term.toLowerCase()] }
                if (card != null) {
                    definitions.remove(card.definition.toLowerCase())
                    card.definition = definition
                    definitions[definition.toLowerCase()] = card.id
                }
            } else {
                val idLong = id.toLong()
                val card = Card(idLong, mistakes.toInt(), term, definition)
                cards.add(card)
                terms[term.toLowerCase()] = idLong
                definitions[definition.toLowerCase()] = idLong
            }
            counter++
        }
        logPrint("$counter cards have been loaded.")
    } catch (e: FileNotFoundException) {
        logPrint("File not found.")
    }
}

fun exportCards(cards: List<Card>, fileName: String = "") {
    var counter = 0
    var fn = fileName
    if (fn.isEmpty()) {
        logPrint("File name:")
        fn = readLine()!!
    }
    val file = File(fn)
    file.writeText("")
    for (card in cards) {
        if (card.id > 0) {
            file.appendText("${card.id}\t${card.mistakes}\t${card.term}\t${card.definition}\n")
            counter++
        }
    }
    logPrint("$counter cards have been saved.")
}

fun askCards(cards: List<Card>, terms: Map<String, Long>, definitions: Map<String, Long>,
             scanner: Scanner) {
    val cardDeck = CardDeck(cards, terms, definitions)
    logPrint("How many times to ask?")
    var counter = readLine()!!.toInt()
    val validDeck = cardDeck.getValidCards()
    while (counter > 0) {
        validDeck.random().queryDefinition(scanner, cardDeck)
        counter--
    }
}

fun saveLog() {
    logPrint("File name:")
    val fn = readLine()!!
    val file = File(fn)
    file.writeText("")
    for (l in log) {
        file.appendText("$l\n")
    }
    logPrint("The log has been saved.")
}

fun hardestStat(cards: List<Card>) {
    val validCards = cards.filter { it.id > -1 }
    val mistakeNum = validCards.maxBy { it.mistakes }?.mistakes ?: 0
    if (mistakeNum == 0) {
        logPrint("There are no cards with errors.")
        return
    }

    val hards = validCards.filter { it.mistakes == mistakeNum }
    val hardstr = when {
        hards.size == 1 -> hards.joinToString(prefix = "The hardest card is ",
                postfix = ". You have $mistakeNum errors answering it.",
                separator = ", ") { "\"${it.term}\"" }
        else -> hards.joinToString(prefix = "The hardest cards are ",
                postfix = ". You have $mistakeNum errors answering them.",
                separator = ", ") { "\"${it.term}\"" }
    }
    logPrint(hardstr)
}

fun resetStats(cards: List<Card>) {
    cards.forEach() { it.mistakes = 0 }
    logPrint("Card statistics has been reset.")
}

fun main(args: Array<String>) {
    val scanner = Scanner(System.`in`)
    val terms = mutableMapOf<String, Long>()
    val definitions = mutableMapOf<String, Long>()
    val cards = mutableListOf<Card>()
    val impind = args.indexOfFirst { it == "-import" }
    if (impind > -1) {
        val fn = args.getOrElse(impind + 1) { "" }
        importCards(cards, terms, definitions, fn)
    }
    loop@ while (true) {
        logPrint("\nInput the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):")
        val cmd = scanner.nextLine()
        log.add(cmd)
        when (cmd) {
            "add" -> makeCard(getNextId(cards), 0, terms, definitions)?.let { cards.add(it) }
            "remove" -> removeCard(cards, terms, definitions)
            "import" -> importCards(cards, terms, definitions)
            "export" -> exportCards(cards)
            "ask" -> askCards(cards, terms, definitions, scanner)
            "exit" -> break@loop
            "log" -> saveLog()
            "hardest card" -> hardestStat(cards)
            "reset stats" -> resetStats(cards)
        }
    }
    logPrint("Bye bye!")
    val expind = args.indexOfFirst { it == "-export" }
    if (expind > -1) {
        val fn = args.getOrElse(expind + 1) { "" }
        exportCards(cards, fn)
    }
}
