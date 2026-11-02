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
