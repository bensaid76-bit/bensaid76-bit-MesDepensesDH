package ma.mesdepensesdh

import android.app.*
import android.os.Bundle
import android.content.*
import android.database.sqlite.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.*
import android.widget.*
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

class Store(context: Context) : SQLiteOpenHelper(context, "expenses.db", null, 2) {
    data class Expense(val id: Long, val amount: Double, val category: String, val note: String, val day: String, val time: String)
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY AUTOINCREMENT, amount REAL NOT NULL, category TEXT NOT NULL, note TEXT NOT NULL, day TEXT NOT NULL, time TEXT NOT NULL DEFAULT '00:00')")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) db.execSQL("ALTER TABLE expenses ADD COLUMN time TEXT NOT NULL DEFAULT '00:00'")
    }
    fun save(existing: Expense?, amount: Double, category: String, note: String, day: String, time: String) {
        val values = ContentValues().apply {
            put("amount", amount); put("category", category); put("note", note); put("day", day); put("time", time)
        }
        if (existing == null) writableDatabase.insertOrThrow("expenses", null, values)
        else writableDatabase.update("expenses", values, "id=?", arrayOf(existing.id.toString()))
    }
    fun remove(id: Long) { writableDatabase.delete("expenses", "id=?", arrayOf(id.toString())) }
    fun all(): List<Expense> {
        val result = mutableListOf<Expense>()
        readableDatabase.rawQuery("SELECT id,amount,category,note,day,time FROM expenses ORDER BY day DESC,time DESC,id DESC", null).use { c ->
            while (c.moveToNext()) result.add(Expense(c.getLong(0), c.getDouble(1), c.getString(2), c.getString(3), c.getString(4), c.getString(5)))
        }
        return result
    }
}

class MainActivity : Activity() {
    private val background = Color.rgb(11, 19, 22)
    private val surface = Color.rgb(23, 35, 38)
    private val surfaceAlt = Color.rgb(30, 47, 48)
    private val emerald = Color.rgb(91, 222, 166)
    private val muted = Color.rgb(158, 180, 176)
    private val danger = Color.rgb(255, 145, 133)
    private lateinit var store: Store
    private lateinit var root: LinearLayout
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private val categories = listOf(
        "Café et boissons", "Cigarettes", "Déjeuner", "Dîner", "Courses et marché", "Électricité",
        "Eau", "Téléphone et Internet", "Carburant", "Transport", "Achats en ligne", "Vêtements",
        "Santé et pharmacie", "Maison", "Loisirs", "Soutien familial – Parents",
        "Études à l'étranger – Fille", "Pourboires & Gratifications", "Autres"
    )
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun nowTime() = SimpleDateFormat("HH:mm", Locale.US).format(Date())
    private fun month() = today().take(7)
    private fun money(value: Double) = String.format(Locale.FRANCE, "%,.2f DH", value)
    private fun dp(value: Int) = (resources.displayMetrics.density * value + .5f).toInt()
    private fun shape(color: Int, radius: Int = 20): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun text(value: String, size: Float = 16f, color: Int = Color.WHITE, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(null, Typeface.BOLD)
        setPadding(0, dp(3), 0, dp(3))
    }
    private fun edit(hint: String, value: String = ""): EditText = EditText(this).apply {
        this.hint = hint; setText(value); textSize = 16f
        setTextColor(Color.WHITE); setHintTextColor(muted)
        backgroundTintList = android.content.res.ColorStateList.valueOf(emerald)
        setSingleLine(true)
    }
    private fun button(label: String, action: () -> Unit): Button = Button(this).apply {
        text = label; isAllCaps = false; textSize = 15f
        setTextColor(this@MainActivity.background); background = shape(emerald, 15)
        setOnClickListener { action() }
        val params = LinearLayout.LayoutParams(-1, dp(50))
        params.bottomMargin = dp(10); layoutParams = params
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(19), dp(17), dp(19), dp(17))
        background = shape(surface)
        val params = LinearLayout.LayoutParams(-1, -2)
        params.bottomMargin = dp(15)
        root.addView(this, params)
    }
    private fun page(title: String, subtitle: String = ""): Unit {
        val scroll = ScrollView(this).apply { setBackgroundColor(this@MainActivity.background); isFillViewport = true }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(25), dp(20), dp(35))
        }
        scroll.addView(root); setContentView(scroll)
        root.addView(text("MES DÉPENSES  /  DH", 12f, emerald, true))
        root.addView(text(title, 29f, Color.WHITE, true))
        if (subtitle.isNotBlank()) {
            root.addView(text(subtitle, 14f, muted))
            root.addView(Space(this).apply { minimumHeight = dp(14) })
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = background; window.navigationBarColor = background
        store = Store(this); showHome()
    }
    private fun showHome() {
        val budget = prefs.getFloat("budget", 1998f).toDouble()
        val items = store.all().filter { it.day.startsWith(month()) }
        val spent = items.sumOf { it.amount }
        page("Votre budget", SimpleDateFormat("MMMM yyyy", Locale.FRENCH).format(Date()).replaceFirstChar { it.uppercase() })
        val summary = card().apply { background = shape(surfaceAlt, 24) }
        summary.addView(text("BUDGET MENSUEL", 12f, muted, true))
        summary.addView(text(money(budget), 30f, Color.WHITE, true))
        summary.addView(text("Dépensé  ${money(spent)}", 16f, muted))
        summary.addView(text("Disponible  ${money(budget - spent)}", 20f, if (spent > budget) danger else emerald, true))
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000; this.progress = if (budget > 0) ((spent / budget * 1000).toInt()).coerceIn(0, 1000) else 1000
            progressTintList = android.content.res.ColorStateList.valueOf(if (spent > budget) danger else emerald)
        }
        summary.addView(progress)
        summary.addView(text("${if (budget > 0) (spent / budget * 100).toInt() else 100}% du budget utilisé", 13f, muted))
        root.addView(button("+  Nouvelle dépense") { expenseDialog(null) })
        root.addView(button("Historique des dépenses") { showHistory() })
        root.addView(button("Analyse par catégorie") { showStats() })
        root.addView(button("Exporter en CSV") { exportCsv() })
        root.addView(button("Paramètres") { showSettings() })
        val recent = card()
        recent.addView(text("Dernières opérations", 20f, Color.WHITE, true))
        if (items.isEmpty()) recent.addView(text("Aucune dépense ce mois-ci.", 15f, muted))
        items.take(7).forEach { e ->
            recent.addView(text("${e.category}   •   ${money(e.amount)}", 15f, Color.WHITE, true))
            recent.addView(text("${e.day} à ${e.time}${if (e.note.isNotBlank()) "  ·  ${e.note}" else ""}", 13f, muted))
        }
        if (spent > budget) root.addView(text("Attention : budget mensuel dépassé.", 15f, danger, true))
    }
    private fun expenseDialog(existing: Store.Expense?) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(8)) }
        val amount = edit("Montant en DH", existing?.amount?.toString() ?: "").apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        layout.addView(text("Montant (DH)", 14f, emerald)); layout.addView(amount)
        val choices = (categories + (prefs.getString("custom", "") ?: "").split("|").filter { it.isNotBlank() }).distinct()
        val spinner = Spinner(this)
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, choices)
        existing?.let { val index = choices.indexOf(it.category); if (index >= 0) spinner.setSelection(index) }
        layout.addView(text("Catégorie", 14f, emerald)); layout.addView(spinner)
        val note = edit("Commentaire facultatif", existing?.note ?: "")
        layout.addView(text("Commentaire", 14f, emerald)); layout.addView(note)
        val selected = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try { selected.time = dateFormat.parse(existing?.day ?: today()) ?: Date() } catch (_: Exception) { }
        val originalTime = existing?.time ?: nowTime()
        val timeParts = originalTime.split(":")
        if (timeParts.size == 2) {
            selected.set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 0)
            selected.set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
        }
        val dateButton = Button(this).apply { isAllCaps = false }
        val timeButton = Button(this).apply { isAllCaps = false }
        fun refreshDateTime() {
            dateButton.text = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE).format(selected.time)
            timeButton.text = SimpleDateFormat("HH:mm", Locale.FRANCE).format(selected.time)
        }
        refreshDateTime()
        layout.addView(text("Date de la dépense", 14f, emerald))
        layout.addView(dateButton)
        dateButton.setOnClickListener {
            DatePickerDialog(this, { _, y, m, d ->
                selected.set(y, m, d); refreshDateTime()
            }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show()
        }
        layout.addView(text("Heure de la dépense", 14f, emerald))
        layout.addView(timeButton)
        timeButton.setOnClickListener {
            TimePickerDialog(this, { _, h, m ->
                selected.set(Calendar.HOUR_OF_DAY, h); selected.set(Calendar.MINUTE, m); refreshDateTime()
            }, selected.get(Calendar.HOUR_OF_DAY), selected.get(Calendar.MINUTE), true).show()
        }
        val container = ScrollView(this).apply { addView(layout) }
        AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Nouvelle dépense" else "Modifier la dépense")
            .setView(container)
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Enregistrer") { _, _ ->
                val value = amount.text.toString().replace(',', '.').toDoubleOrNull()
                if (value == null || !value.isFinite() || value <= 0) {
                    Toast.makeText(this, "Montant invalide", Toast.LENGTH_LONG).show()
                    expenseDialog(existing)
                } else {
                    val oldSpent = store.all().filter { it.day.startsWith(month()) }.sumOf { it.amount }
                    store.save(existing, value, spinner.selectedItem.toString(), note.text.toString(),
                        dateFormat.format(selected.time), SimpleDateFormat("HH:mm", Locale.US).format(selected.time))
                    showHome()
                    val budget = prefs.getFloat("budget", 1998f).toDouble()
                    val newSpent = store.all().filter { it.day.startsWith(month()) }.sumOf { it.amount }
                    val oldRatio = if (budget > 0) oldSpent / budget * 100 else 0.0
                    val newRatio = if (budget > 0) newSpent / budget * 100 else 100.0
                    val threshold = listOf(100, 90, 75, 50).firstOrNull { oldRatio < it && newRatio >= it }
                    if (threshold != null) AlertDialog.Builder(this)
                        .setTitle(if (threshold == 100) "Budget atteint" else "Alerte budget")
                        .setMessage("${newRatio.toInt()}% du budget utilisé. Les dépenses restent autorisées.")
                        .setPositiveButton("OK", null).show()
                }
            }.show()
    }
    private fun showHistory() {
        page("Historique", "Toutes vos opérations, avec date et heure")
        root.addView(button("← Tableau de bord") { showHome() })
        val items = store.all()
        if (items.isEmpty()) root.addView(text("Aucune opération enregistrée.", 15f, muted))
        items.forEach { e ->
            val item = card()
            item.addView(text(e.category, 17f, Color.WHITE, true))
            item.addView(text(money(e.amount), 20f, emerald, true))
            item.addView(text("${e.day}  •  ${e.time}", 13f, muted))
            if (e.note.isNotBlank()) item.addView(text(e.note, 14f, muted))
            item.addView(button("Modifier") { expenseDialog(e) })
            item.addView(button("Supprimer") {
                AlertDialog.Builder(this).setMessage("Supprimer cette dépense ?")
                    .setNegativeButton("Annuler", null)
                    .setPositiveButton("Supprimer") { _, _ -> store.remove(e.id); showHistory() }.show()
            })
        }
    }
    private fun showStats() {
        page("Statistiques", "Répartition des dépenses du mois")
        root.addView(button("← Tableau de bord") { showHome() })
        val items = store.all().filter { it.day.startsWith(month()) }
        val total = items.sumOf { it.amount }
        val summary = card(); summary.addView(text("Total du mois", 14f, muted))
        summary.addView(text(money(total), 26f, emerald, true))
        items.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }.forEach { (category, amount) ->
                val item = card()
                val percent = if (total > 0) (100 * amount / total).toInt() else 0
                item.addView(text(category, 16f, Color.WHITE, true))
                item.addView(text("${money(amount)}   •   $percent%", 14f, emerald))
                item.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100; progress = percent
                    progressTintList = android.content.res.ColorStateList.valueOf(emerald)
                })
            }
    }
    private fun showSettings() {
        page("Paramètres", "Personnalisez votre budget et vos catégories")
        root.addView(button("← Tableau de bord") { showHome() })
        val budgetCard = card()
        budgetCard.addView(text("Budget mensuel (DH)", 17f, Color.WHITE, true))
        val amount = edit("Budget", prefs.getFloat("budget", 1998f).toString()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        budgetCard.addView(amount)
        budgetCard.addView(button("Enregistrer le budget") {
            val value = amount.text.toString().replace(',', '.').toFloatOrNull()
            if (value != null && value.isFinite() && value > 0) {
                prefs.edit().putFloat("budget", value).apply(); showHome()
            } else Toast.makeText(this, "Budget invalide", Toast.LENGTH_SHORT).show()
        })
        val categoryCard = card()
        categoryCard.addView(text("Ajouter une catégorie", 17f, Color.WHITE, true))
        val name = edit("Nom de la catégorie")
        categoryCard.addView(name)
        categoryCard.addView(button("Ajouter") {
            val value = name.text.toString().trim()
            if (value.isNotBlank() && !value.contains("|")) {
                val previous = prefs.getString("custom", "") ?: ""
                prefs.edit().putString("custom", if (previous.isEmpty()) value else "$previous|$value").apply()
                name.setText("")
                Toast.makeText(this, "Catégorie ajoutée", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(text("Vos dépenses restent enregistrées localement sur ce téléphone.", 13f, muted))
    }
    private fun exportCsv() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            putExtra(Intent.EXTRA_TITLE, "depenses_${today()}.csv")
        }
        startActivityForResult(intent, 12)
    }
    private fun csvQuote(value: String) = "\"" + value.replace("\"", "\"\"") + "\""
    @Deprecated("Uses legacy activity result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 12 || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            val stream = contentResolver.openOutputStream(uri) ?: throw java.io.IOException("Fichier inaccessible")
            OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                writer.write("\uFEFFDate;Heure;Catégorie;Montant (DH);Commentaire\n")
                store.all().forEach { expense ->
                    writer.write("${csvQuote(expense.day)};${csvQuote(expense.time)};${csvQuote(expense.category)};${expense.amount};${csvQuote(expense.note)}\n")
                }
            }
            Toast.makeText(this, "Export CSV terminé", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur d'export : ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
