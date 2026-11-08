package com.neo.reelsoff

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Surveille Instagram, YouTube et Snapchat et quitte la fonctionnalité coupée.
 *
 * Règles de prudence (pour ne pas faire bugger les apps) :
 * - on n'agit que si la fenêtre active appartient à l'app surveillée ;
 * - on n'agit que sur les événements de changement d'écran ou de contenu ;
 * - un délai de 1,5 s entre deux actions ;
 * - si l'onglet d'accueil est introuvable, on ne fait rien (pas de "Retour" aveugle).
 */
class BlockerService : AccessibilityService() {

    private var lastActionAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !isRelevant(event)) return
        val pkg = event.packageName?.toString() ?: return

        val rules = Rules.forPackage(pkg).filter { Rules.isEnabled(this, it) }
        if (rules.isEmpty()) return

        val now = SystemClock.uptimeMillis()
        if (now - lastActionAt < COOLDOWN_MS) return

        val root = rootInActiveWindow ?: return
        try {
            if (root.packageName?.toString() != pkg) return
            for (rule in rules) {
                if (rule.detect(root)) {
                    act(rule, root)
                    lastActionAt = now
                    return
                }
            }
        } catch (e: Exception) {
            // Une erreur sur un écran inattendu ne doit jamais planter l'app surveillée.
        } finally {
            root.recycle()
        }
    }

    private fun isRelevant(event: AccessibilityEvent): Boolean =
        event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            (
                event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
                    (event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE) != 0
                )

    private fun act(rule: Rule, root: AccessibilityNodeInfo) {
        if (rule.homeIdFragment == null && rule.homeLabels.isEmpty()) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            return
        }
        Tree.any(root, 150) { node ->
            val isHome = (rule.homeIdFragment != null && node.idContains(rule.homeIdFragment)) ||
                Tree.label(node) in rule.homeLabels
            isHome && clickSelfOrAncestor(node)
        }
    }

    /** Clique sur le nœud ou sur son premier ancêtre cliquable. Recycle ce qu'il crée. */
    private fun clickSelfOrAncestor(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo = node
        var owned = false // false = nœud fourni par Tree (ne pas le recycler ici)
        for (level in 0 until 4) {
            val clicked = current.isClickable && current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            val parent = current.parent
            if (owned) current.recycle()
            if (clicked) {
                parent?.recycle()
                return true
            }
            current = parent ?: return false
            owned = true
        }
        if (owned) current.recycle()
        return false
    }

    override fun onInterrupt() {}

    companion object {
        private const val COOLDOWN_MS = 1500L
    }
}
