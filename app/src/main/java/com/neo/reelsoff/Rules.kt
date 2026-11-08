package com.neo.reelsoff

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Une règle = une fonctionnalité à couper.
 *
 * - detect : vrai si la fonctionnalité est affichée à l'écran.
 * - homeIdFragment / homeLabels : onglet d'accueil à toucher pour quitter la fonctionnalité.
 *   S'il n'y en a pas, on fait "Retour" (utilisé pour les lecteurs plein écran).
 *
 * Idées (pas de code copié) : identifiants de vues d'Instagram (clips_tab / feed_tab) et
 * de YouTube (reel_progress_bar), tirés du projet open source Shorts-Blocker (Apache-2.0).
 */
data class Rule(
    val pkg: String,
    val prefKey: String,
    val label: String,
    val detect: (AccessibilityNodeInfo) -> Boolean,
    val homeIdFragment: String? = null,
    val homeLabels: Set<String> = emptySet()
)

object Rules {

    val all: List<Rule> = listOf(
        // Onglet Reels sélectionné -> on revient à l'onglet Accueil.
        // Les reels reçus en DM ne sont pas visibles ici : l'onglet Reels n'est pas sélectionné.
        Rule(
            pkg = "com.instagram.android",
            prefKey = "insta_reels",
            label = "Reels Instagram",
            detect = { root -> Tree.any(root, 150) { it.idContains("clips_tab") && it.isSelected } },
            homeIdFragment = "feed_tab"
        ),
        // Lecteur Shorts plein écran -> retour.
        Rule(
            pkg = "com.google.android.youtube",
            prefKey = "yt_shorts",
            label = "Shorts YouTube",
            detect = { root -> Tree.any(root, 120) { it.idContains("reel_progress_bar") } }
        ),
        // Onglet Spotlight sélectionné -> on revient sur la Caméra. Non testé.
        Rule(
            pkg = "com.snapchat.android",
            prefKey = "snap_spotlight",
            label = "Spotlight Snapchat",
            detect = { root -> Tree.any(root, 150) { it.isSelected && Tree.label(it) == "spotlight" } },
            homeLabels = setOf("camera", "caméra")
        )
    )

    fun forPackage(pkg: String): List<Rule> = all.filter { it.pkg == pkg }

    /** Tout est coupé par défaut. */
    fun isEnabled(context: Context, rule: Rule): Boolean =
        Prefs.get(context).getBoolean(rule.prefKey, true)
}

object Prefs {
    private const val NAME = "reelsoff"
    fun get(context: Context) = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)
}

fun AccessibilityNodeInfo.idContains(fragment: String): Boolean =
    viewIdResourceName?.contains(fragment, ignoreCase = true) == true

/** Parcours borné de l'arbre d'accessibilité, avec recyclage des nœuds. */
object Tree {
    private const val MAX_DEPTH = 25

    fun label(node: AccessibilityNodeInfo): String? =
        (node.contentDescription ?: node.text)?.toString()?.trim()?.lowercase()

    /**
     * Visite au plus [maxNodes] nœuds. Renvoie true dès que [visit] renvoie true.
     * Ne recycle pas [root] (appartient à l'appelant), recycle tous les enfants.
     */
    fun any(root: AccessibilityNodeInfo, maxNodes: Int, visit: (AccessibilityNodeInfo) -> Boolean): Boolean {
        var budget = maxNodes
        fun go(node: AccessibilityNodeInfo, depth: Int): Boolean {
            if (--budget < 0) return false
            if (visit(node)) return true
            if (depth >= MAX_DEPTH) return false
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    if (go(child, depth + 1)) return true
                } finally {
                    child.recycle()
                }
            }
            return false
        }
        return go(root, 0)
    }
}
