package ma.mesdepensesdh

import android.app.*
import android.os.Bundle
import android.content.*
import android.database.sqlite.*
import android.graphics.Color
import android.graphics.Typeface
import android.view.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*
import java.io.OutputStreamWriter

class Store(context: Context): SQLiteOpenHelper(context,"expenses.db",null,1) {
 override fun onCreate(db:SQLiteDatabase) { db.execSQL("CREATE TABLE expenses(id INTEGER PRIMARY KEY AUTOINCREMENT, amount REAL NOT NULL, category TEXT NOT NULL, note TEXT NOT NULL, day TEXT NOT NULL)") }
 override fun onUpgrade(db:SQLiteDatabase,oldVersion:Int,newVersion:Int) {}
 fun add(amount:Double,category:String,note:String,day:String) { writableDatabase.execSQL("INSERT INTO expenses(amount,category,note,day) VALUES(?,?,?,?)",arrayOf(amount,category,note,day)) }
 fun remove(id:Long) { writableDatabase.delete("expenses","id=?",arrayOf(id.toString())) }
 fun update(id:Long,amount:Double,category:String,note:String,day:String) { val v=ContentValues();v.put("amount",amount);v.put("category",category);v.put("note",note);v.put("day",day);writableDatabase.update("expenses",v,"id=?",arrayOf(id.toString())) }
 data class Expense(val id:Long,val amount:Double,val category:String,val note:String,val day:String)
 fun all():List<Expense> { val result=mutableListOf<Expense>(); readableDatabase.rawQuery("SELECT id,amount,category,note,day FROM expenses ORDER BY day DESC,id DESC",null).use { c->while(c.moveToNext()) result.add(Expense(c.getLong(0),c.getDouble(1),c.getString(2),c.getString(3),c.getString(4))) };return result }
}

class MainActivity:Activity() {
 private val bg=Color.rgb(14,33,30);private val panel=Color.rgb(28,62,54);private val mint=Color.rgb(159,231,185)
 private lateinit var store:Store;private lateinit var root:LinearLayout
 private val prefs by lazy { getSharedPreferences("settings",MODE_PRIVATE) }
 private val categories= listOf("Café et boissons","Cigarettes","Déjeuner","Dîner","Courses et marché","Électricité","Eau","Téléphone et Internet","Carburant","Transport","Achats en ligne","Vêtements","Santé et pharmacie","Maison","Loisirs","Autres")
 private fun today()=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date())
 private fun month()=today().take(7)
 private fun money(v:Double)=String.format(Locale.FRANCE,"%,.2f DH",v)
 private fun label(t:String,size:Float=16f,color:Int=Color.WHITE):TextView { val x=TextView(this);x.text=t;x.textSize=size;x.setTextColor(color);x.setPadding(4,8,4,8);return x }
 private fun button(t:String,action:()->Unit):Button { val b=Button(this);b.text=t;b.setTextColor(Color.WHITE);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(42,109,80)));b.setOnClickListener { action() };return b }
 private fun input(hint:String,value:String=""):EditText { val e=EditText(this);e.hint=hint;e.setText(value);e.setTextColor(Color.WHITE);e.setHintTextColor(Color.LTGRAY);e.setSingleLine(true);return e }
 private fun box():LinearLayout { val x=LinearLayout(this);x.orientation=1;x.setPadding(20,16,20,16);x.setBackgroundColor(panel);val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,16);root.addView(x,lp);return x }
 override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState);window.statusBarColor=bg;window.navigationBarColor=bg;store=Store(this);showHome() }
 private fun page(title:String) { val scroll=ScrollView(this);scroll.setBackgroundColor(bg);root=LinearLayout(this);root.orientation=1;root.setPadding(22,24,22,24);scroll.addView(root);setContentView(scroll);root.addView(label(title,26f,mint)) }
 private fun showHome() {
  page("Mes Dépenses DH")
  val budget=prefs.getFloat("budget",1998f).toDouble();val items=store.all().filter { it.day.startsWith(month()) };val spent=items.sumOf { it.amount };val rest=budget-spent
  val summary=box();summary.addView(label("Budget mensuel : ${money(budget)}",20f));summary.addView(label("Dépensé : ${money(spent)}",19f));summary.addView(label("Disponible : ${money(rest)}",19f,if(rest>=0) mint else Color.rgb(255,145,125)))
  val bar=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.max=100;bar.progress=if(budget>0) ((spent/budget*100).toInt()).coerceIn(0,100) else 100;summary.addView(bar);summary.addView(label("${if(budget>0) (spent/budget*100).toInt() else 100}% du budget utilisé",14f))
  root.addView(button("+ Ajouter une dépense") { expenseDialog(null) });root.addView(button("Historique et modification") { showHistory() });root.addView(button("Statistiques par catégorie") { showStats() });root.addView(button("Exporter en CSV") { exportCsv() });root.addView(button("Paramètres") { showSettings() })
  val recent=box();recent.addView(label("Dernières dépenses",20f,mint));items.take(8).forEach { e->recent.addView(label("${e.day}  •  ${e.category}\n${money(e.amount)}${if(e.note.isNotBlank()) " — ${e.note}" else ""}",15f)) }
  if(items.isEmpty())recent.addView(label("Aucune dépense enregistrée ce mois."))
  if(spent>budget) recent.addView(label("Attention : budget mensuel dépassé.",16f,Color.rgb(255,145,125)))
 }
 private fun expenseDialog(existing:Store.Expense?) {
  val layout=LinearLayout(this);layout.orientation=1;layout.setPadding(24,8,24,8)
  val amount=EditText(this);amount.hint="Montant en DH";amount.inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;amount.setText(existing?.amount?.toString()?:"");layout.addView(amount)
  val choices=(categories+prefs.getString("custom","")!!.split("|").filter { it.isNotBlank() }).distinct()
  val spinner=Spinner(this);val adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,choices);spinner.adapter=adapter;layout.addView(spinner);existing?.let { val i=choices.indexOf(it.category);if(i>=0)spinner.setSelection(i) }
  val note=EditText(this);note.hint="Commentaire (facultatif)";note.setText(existing?.note?:"");layout.addView(note)
  val day=EditText(this);day.hint="Date AAAA-MM-JJ";day.setText(existing?.day?:today());layout.addView(day)
  AlertDialog.Builder(this).setTitle(if(existing==null)"Nouvelle dépense" else "Modifier la dépense").setView(layout).setNegativeButton("Annuler",null).setPositiveButton("Enregistrer") { _,_->
   val v=amount.text.toString().replace(',','.').toDoubleOrNull();val d=day.text.toString();val validDate=try { val f=SimpleDateFormat("yyyy-MM-dd",Locale.US);f.isLenient=false;f.parse(d)!=null && d.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) }catch(_:Exception){false}
   if(v==null||v<=0||!validDate){Toast.makeText(this,"Montant ou date invalide",Toast.LENGTH_LONG).show();expenseDialog(existing)} else {
    if(existing==null)store.add(v,spinner.selectedItem.toString(),note.text.toString(),d) else store.update(existing.id,v,spinner.selectedItem.toString(),note.text.toString(),d)
    val budget=prefs.getFloat("budget",1998f).toDouble();val spent=store.all().filter { it.day.startsWith(month()) }.sumOf { it.amount };val percent=if(budget>0)spent/budget*100 else 100.0
    if(percent>=100)AlertDialog.Builder(this).setTitle("Budget dépassé ou atteint").setMessage("${money(spent)} dépensés sur ${money(budget)}. Vous pouvez continuer à enregistrer vos dépenses.").setPositiveButton("OK",null).show()
    else if(listOf(90,75,50).any { percent>=it && (spent-v)/budget*100<it })AlertDialog.Builder(this).setMessage("Vous avez utilisé ${percent.toInt()}% de votre budget mensuel.").setPositiveButton("OK",null).show()
    showHome()
   }
  }.show()
 }
 private fun showHistory() {page("Historique");root.addView(button("Retour") { showHome() });val items=store.all();if(items.isEmpty())root.addView(label("Aucune opération."));items.forEach { e->val b=box();b.addView(label("${e.day} • ${e.category} • ${money(e.amount)}\n${e.note}"));b.addView(button("Modifier") { expenseDialog(e) });b.addView(button("Supprimer") { AlertDialog.Builder(this).setMessage("Supprimer cette dépense ?").setNegativeButton("Annuler",null).setPositiveButton("Supprimer") { _,_->store.remove(e.id);showHistory() }.show() }) } }
 private fun showStats() {page("Statistiques du mois");root.addView(button("Retour") { showHome() });val items=store.all().filter { it.day.startsWith(month()) };val total=items.sumOf { it.amount };root.addView(label("Total : ${money(total)}",20f));items.groupBy { it.category }.mapValues { (_,v)->v.sumOf { it.amount } }.toList().sortedByDescending { it.second }.forEach { (cat,value)->val b=box();b.addView(label("$cat : ${money(value)} (${if(total>0)(100*value/total).toInt() else 0}%)"));val p=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);p.max=100;p.progress=if(total>0)(100*value/total).toInt() else 0;b.addView(p) } }
 private fun showSettings() {page("Paramètres");root.addView(button("Retour") { showHome() });val b=box();b.addView(label("Budget mensuel (DH)"));val amount=input("Budget",prefs.getFloat("budget",1998f).toString());amount.inputType=android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL;b.addView(amount);b.addView(button("Enregistrer le budget") { val v=amount.text.toString().replace(',','.').toFloatOrNull();if(v!=null&&v>0){prefs.edit().putFloat("budget",v).apply();showHome()}else Toast.makeText(this,"Budget invalide",Toast.LENGTH_SHORT).show() });val c=box();c.addView(label("Ajouter une catégorie"));val name=input("Nom de la catégorie");c.addView(name);c.addView(button("Ajouter") { val n=name.text.toString().trim();if(n.isNotBlank()&&!n.contains("|")){val old=prefs.getString("custom","")!!;prefs.edit().putString("custom",if(old.isEmpty())n else "$old|$n").apply();Toast.makeText(this,"Catégorie ajoutée",Toast.LENGTH_SHORT).show();name.setText("")} });root.addView(label("Les données sont conservées sur cet appareil. Aucun compte ni accès Internet requis.",14f)) }
 private fun exportCsv() { val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE);type="text/csv";putExtra(Intent.EXTRA_TITLE,"depenses_${today()}.csv") };startActivityForResult(intent,12) }
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {super.onActivityResult(requestCode,resultCode,data);if(requestCode==12&&resultCode==RESULT_OK){data?.data?.let { uri->try { contentResolver.openOutputStream(uri)?.use { stream->OutputStreamWriter(stream,Charsets.UTF_8).use { w->w.write("\uFEFFDate;Catégorie;Montant (DH);Commentaire\n");store.all().forEach { e->fun q(s:String)="\""+s.replace("\"","\"\"")+"\"";w.write("${q(e.day)};${q(e.category)};${e.amount};${q(e.note)}\n") } } };Toast.makeText(this,"Export CSV terminé",Toast.LENGTH_LONG).show() }catch(e:Exception){Toast.makeText(this,"Erreur d'export : ${e.message}",Toast.LENGTH_LONG).show()} } } }
}
