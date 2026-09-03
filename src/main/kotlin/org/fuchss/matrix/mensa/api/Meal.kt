package org.fuchss.matrix.mensa.api

import com.vdurmont.emoji.EmojiManager

/**
 * This data class defines all information that should be provided by a meal.
 * @param[name] the name of the meal
 * @param[fish] indicator for fish
 * @param[pork] indicator for pork
 * @param[cow] indicator for cow
 * @param[vegan] indicator for vegan
 * @param[vegetarian] indicator for vegetarian
 * @param[chicken] indicator for chicken
 */
data class Meal(
    val name: String,
    val fish: Boolean,
    val pork: Boolean,
    val cow: Boolean,
    val vegan: Boolean,
    val vegetarian: Boolean,
    val chicken: Boolean
) {
    /**
     * Calculate a nice looking textual entry for a food (including emojis).
     */
    fun entry(): String {
        val emojis = mutableListOf<String>()
        addEmojis(emojis)
        if (emojis.isEmpty()) return name
        return "${emojis.joinToString("/")} $name"
    }

    private fun addEmojis(emojis: MutableList<String>) {
        addFish(emojis)
        addPork(emojis)
        addCow(emojis)
        addVegan(emojis)
        addVegetarian(emojis)
        addChicken(emojis)
    }

    private fun addChicken(emojis: MutableList<String>) {
        if (chicken) emojis.add(":chicken:".toEmoji())
    }

    private fun addVegetarian(emojis: MutableList<String>) {
        if (vegetarian) emojis.add(":carrot:".toEmoji())
    }

    private fun addVegan(emojis: MutableList<String>) {
        if (vegan) emojis.add(":ear_of_rice:".toEmoji())
    }

    private fun addCow(emojis: MutableList<String>) {
        if (cow) emojis.add(":cow:".toEmoji())
    }

    private fun addPork(emojis: MutableList<String>) {
        if (pork) emojis.add(":pig:".toEmoji())
    }

    private fun addFish(emojis: MutableList<String>) {
        if (fish) emojis.add(":fish:".toEmoji())
    }

    private fun String.toEmoji(): String = EmojiManager.getForAlias(this).unicode
}
