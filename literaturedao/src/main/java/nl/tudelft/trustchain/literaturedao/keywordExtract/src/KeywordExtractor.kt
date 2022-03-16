
package KeywordExtractor

import java.io.File
import Java.Snowball.src.Main.main as stem

class KeywordExtractor {
    companion object {
        @JvmStatic fun main(args: Array<String>){

            // Function that loads the acerage stemmed word occurance
            fun instantiateAvgFreqMap(): Map<String, Long>{
                var res = mutableMapOf<String, Long>()
                val path = System.getProperty("user.dir")
                File(path + "\\src\\stemmed_freqs.csv").forEachLine {
                    val key = it.split(",".toRegex())[0]
                    val num = it.split(",".toRegex())[1]
                        .toLong()
                    res[key] = num
                }
                return res
            }

            // Custom data type in order to be able to sort
            class Result constructor(word: String, relativeFreq: Double) {
                val first = word
                val second = relativeFreq
                fun second(): Double {
                    return second
                }
                override fun toString(): String{
                    val builder = StringBuilder()
                    builder.append("(")
                        .append(first)
                        .append(", ")
                        .append(second.toString())
                        .append(")")
                    return  builder.toString()
                }
            }

            // Quik fix alternative if the actual implementation does not work
            fun quikFix(args: Array<String>){

                //List of frequently used words
                val banList = listOf("a", "about", "above", "actually", "after", "again", "against", "all", "almost", "also", "although", "always", "am", "an", "and", "any", "are", "as", "at", "be", "became", "become", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can", "could", "did", "do", "does", "doing", "down", "during", "each", "either", "else", "few", "for", "from", "further", "had", "has", "have", "having", "he", "hence", "her", "here", "hers", "herself", "him", "himself", "his", "how", "I", "if", "in", "into", "is", "it", "its", "itself", "just", "may", "maybe", "me", "might", "mine", "more", "most", "must", "my", "myself", "neither", "nor", "not", "of", "oh", "on", "once", "only", "ok", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "she", "should", "so", "some", "such", "than", "that", "the", "their", "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "we", "were", "what", "when", "whenever", "where", "whereas", "wherever", "whether", "which", "while", "who", "whoever", "whose", "whom", "why", "will", "with", "within", "would", "yes", "yet", "you", "your", "yours", "yourself", "yourselves")


                var input = args[0].split("\\s".toRegex())

                var freqMap = mutableMapOf<String, Int>()
                var total = 0

                // Count words
                for (word in input){
                    if (word in banList) continue
                    total += 1
                    if (word !in freqMap.keys){
                        freqMap[word] = 1
                    } else {
                        freqMap[word] = freqMap.getValue(word) + 1
                    }
                }

                var pressenceList = mutableListOf<Result>()

                //Calculate word pressence
                for ((word, freq) in freqMap.entries){
                    pressenceList.add(Result(word, freq.toDouble() / total.toDouble()))
                }

                pressenceList.sortBy { it.second }
                pressenceList.reverse()
                println(pressenceList)
            }

            fun actualImplementation(args: Array<String>){

                // Establish general averages
                val avgTotal = 588089694315
                val avgFreqMap = instantiateAvgFreqMap()

                // Append a word to the input as the used library forgets about the last word
                args[0] = args[0].plus(" bugFix")

                // Stem the input using the snowball library and split into list of words
                var input = stem(args[0])
                    .split("\\s".toRegex())

                // Instantiate a map for the word counts and a counter for the total words that were recognized
                var freqMap = mutableMapOf<String, Int>()
                var total = 0

                // Count words known in averageFreqMap in input
                for (word in input){
                    val avgFreq = avgFreqMap[word]
                    if (avgFreq == null) continue
                    total += 1
                    if (word !in freqMap.keys){
                        freqMap[word] = 1
                    } else {
                        freqMap[word] = freqMap.getValue(word) + 1
                    }
                }

                // Instantiate a result list
                var relativeFreqList = mutableListOf<Result>()

                //Calculate relative frequencies per word
                for ((word, freq) in freqMap.entries){
                    val avgFreq = avgFreqMap[word]
                    if (avgFreq == null) continue
                    relativeFreqList.add(Result(word, (freq.toDouble() / total.toDouble())  / (avgFreq.toDouble() / avgTotal.toDouble())))
                }

                //sort results (not working yet)
                relativeFreqList.sortBy { it.second }
                relativeFreqList.reverse()
                println(relativeFreqList)
                //return sorted to be implemented in android studio
            }
            //quikFix(args)
            actualImplementation(args)
           // val someMap = instantiateAvgFreqMap()
            //println(someMap["c"])
        }
    }
}
