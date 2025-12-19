package com.zolitatek.difffrog

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.Alarm
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepositoryManager
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.*
import javax.swing.Timer

class GitDiffToolbarAction : AnAction(), CustomComponentAction {

    private val KEY_TARGET_BRANCH = "com.crossguild.difffrog.targetBranch"
    private val KEY_DELAY_LEVEL = "com.crossguild.difffrog.delayLevel"
    private val KEY_MAX_LINES = "com.crossguild.difffrog.maxLines"

    private var displayedAdded = 0
    private var displayedDeleted = 0
    private var targetAdded = 0
    private var targetDeleted = 0

    private val labelStats = JBLabel("").apply {
        cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        horizontalAlignment = SwingConstants.LEFT
        preferredSize = Dimension(140, 26) // Un poco más ancho para los iconos
    }

    private val loadingIcon = AsyncProcessIcon("GitDiffLoading").apply { isVisible = false }
    private val updateAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, null)

    // Animación de números tipo "contador"
    private val animationTimer = Timer(25) {
        var changed = false

        // Animación líneas añadidas
        if (displayedAdded != targetAdded) {
            val diff = targetAdded - displayedAdded
            val step = (diff / 4).coerceIn(-20, 20).let { if (it == 0) (if (diff > 0) 1 else -1) else it }
            displayedAdded = if (Math.abs(diff) <= Math.abs(step)) targetAdded else displayedAdded + step
            changed = true
        }

        // Animación líneas eliminadas
        if (displayedDeleted != targetDeleted) {
            val diff = targetDeleted - displayedDeleted
            val step = (diff / 4).coerceIn(-20, 20).let { if (it == 0) (if (diff > 0) 1 else -1) else it }
            displayedDeleted = if (Math.abs(diff) <= Math.abs(step)) targetDeleted else displayedDeleted + step
            changed = true
        }

        if (changed) {
            updateLabelText(displayedAdded, displayedDeleted)
        } else {
            (it.source as Timer).stop()
        }
    }

    private var currentProject: Project? = null
    private var isListenerRegistered = false

    // --- Getters / Setters de Configuración ---
    private var targetBranch: String
        get() = PropertiesComponent.getInstance().getValue(KEY_TARGET_BRANCH, "develop")
        set(value) = PropertiesComponent.getInstance().setValue(KEY_TARGET_BRANCH, value)

    private var delayLevel: Int
        get() = PropertiesComponent.getInstance().getInt(KEY_DELAY_LEVEL, 1)
        set(value) = PropertiesComponent.getInstance().setValue(KEY_DELAY_LEVEL, value, 1)

    private var maxLines: Int
        get() = PropertiesComponent.getInstance().getInt(KEY_MAX_LINES, 420)
        set(value) = PropertiesComponent.getInstance().setValue(KEY_MAX_LINES, value, 420)

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        val panel = JPanel(GridBagLayout())
        panel.isOpaque = false
        labelStats.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = showConfigPopup(labelStats)
        })

        val gbc = GridBagConstraints().apply { gridy = 0; insets = JBUI.insets(0, 2) }
        gbc.gridx = 0; panel.add(loadingIcon, gbc)
        gbc.gridx = 1; gbc.weightx = 1.0; panel.add(labelStats, gbc)

        updateLabelText(0, 0)
        return panel
    }

    private fun updateLabelText(added: Int, deleted: Int) {
        val addColor = getInterpolatedGreen(added)
        val delColor = "#C75450" // Rojo Git clásico

        // Lógica de iconos exacta
        val icon = when {
            added == maxLines && deleted == maxLines -> " 🌿" // Solo si es exacto
            added >= maxLines -> " ⚠️" // Alerta si superas o llegas al límite en agregadas
            else -> ""
        }

        labelStats.text = "<html><nobr>" +
                "<font color='$addColor'>+$added</font> " +
                "<font color='$delColor'>-$deleted</font>" +
                " $icon" +
                "</nobr></html>"
    }

    private fun getInterpolatedGreen(added: Int): String {
        // Si es 0 o poco, color blanco. A medida que sube, se vuelve verde.
        val ratio = (added.toFloat() / maxLines.toFloat()).coerceIn(0f, 1f)

        // De Blanco Puro (#FFFFFF) a Verde Intenso (#499C54)
        val r = (255 - (ratio * (255 - 73))).toInt()
        val g = (255 - (ratio * (255 - 156))).toInt()
        val b = (255 - (ratio * (255 - 84))).toInt()

        return String.format("#%02x%02x%02x", r, g, b)
    }

    private fun triggerUpdate() {
        val project = currentProject ?: return
        updateAlarm.cancelAllRequests()

        ApplicationManager.getApplication().invokeLater {
            loadingIcon.isVisible = true
            loadingIcon.resume()
        }

        updateAlarm.addRequest({
            if (project.isDisposed) return@addRequest
            val stats = calculateDiff(project, targetBranch)

            targetAdded = stats.first
            targetDeleted = stats.second

            ApplicationManager.getApplication().invokeLater {
                animationTimer.start()
                loadingIcon.suspend()
                loadingIcon.isVisible = false
            }
        }, mapOf(0 to 5000, 1 to 2000, 2 to 500)[delayLevel] ?: 2000)
    }

    private fun showConfigPopup(anchor: JComponent) {
        val rootPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(15)
        }

        val formPanel = JPanel(GridLayout(2, 2, 8, 8))
        formPanel.add(JBLabel("Comparative branch :"))
        val txtTarget = JBTextField(targetBranch)
        formPanel.add(txtTarget)

        formPanel.add(JBLabel("Límite (Max):"))
        val txtMaxLines = JBTextField(maxLines.toString())
        formPanel.add(txtMaxLines)

        rootPanel.add(formPanel)
        rootPanel.add(Box.createVerticalStrut(15))

        val delaySlider = JSlider(0, 2, delayLevel).apply {
            val labels = Hashtable<Int, JLabel>()
            labels[0] = JLabel("🐢"); labels[1] = JLabel("😐"); labels[2] = JLabel("⚡")
            labelTable = labels; paintLabels = true; snapToTicks = true
        }
        rootPanel.add(JBLabel("Velocidad:").apply { alignmentX = Component.CENTER_ALIGNMENT })
        rootPanel.add(delaySlider)

        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(rootPanel, txtTarget)
            .setTitle("DiffFrog Config")
            .setRequestFocus(true)
            .addListener(object : JBPopupListener {
                override fun onClosed(event: LightweightWindowEvent) {
                    targetBranch = txtTarget.text.trim().ifEmpty { "develop" }
                    maxLines = txtMaxLines.text.toIntOrNull() ?: 420
                    delayLevel = delaySlider.value
                    triggerUpdate()
                }
            })
            .createPopup().showUnderneathOf(anchor)
    }

    private fun calculateDiff(project: Project, bA: String): Pair<Int, Int> {
        val repository = GitRepositoryManager.getInstance(project).repositories.firstOrNull() ?: return Pair(0, 0)
        var added = 0; var deleted = 0
        try {
            val handler = GitLineHandler(project, repository.root, GitCommand.DIFF)
            handler.addParameters(bA, "--numstat")
            val result = Git.getInstance().runCommand(handler)
            result.output.forEach { line ->
                val parts = line.trim().split(Regex("\\s+"))
                if (parts.size >= 2) {
                    added += parts[0].toIntOrNull() ?: 0
                    deleted += parts[1].toIntOrNull() ?: 0
                }
            }
        } catch (e: Exception) { }
        return Pair(added, deleted)
    }

    override fun update(e: AnActionEvent) {
        currentProject = e.project
        if (currentProject != null && !isListenerRegistered) {
            EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) = triggerUpdate()
            }, currentProject!!)
            isListenerRegistered = true
            triggerUpdate()
        }
    }

    override fun actionPerformed(e: AnActionEvent) = showConfigPopup(labelStats)
}