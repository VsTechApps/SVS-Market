package com.vs.supermarket

import android.R.attr.*
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.vs.supermarket.adapters.CartAdapter
import com.vs.supermarket.models.CartItem
import com.vs.supermarket.models.OrderItem
import java.io.File
import java.io.FileWriter
import java.util.*

class CartActivity : AppCompatActivity(), CartAdapter.OnItemClickListener {

    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private var cartRef =
        db.collection("users").document(auth.currentUser?.uid!!).collection("cart")
    private val ordersRef =
        db.collection("orders")
    private lateinit var adapter: CartAdapter
    private lateinit var cost: TextView
    private lateinit var recyclerView: RecyclerView
    private var notAllowOnclick = false

    private val code = 123
    private val tezPackageName = "com.google.android.apps.nbu.paisa.user"

    private lateinit var document: DocumentReference
    private lateinit var orderItem: OrderItem

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        cost = findViewById(R.id.cost)
        val proceed = findViewById<Button>(R.id.proceed)
        val bill = findViewById<Button>(R.id.bill)
        recyclerView = findViewById(R.id.cartView)

        if (intent.getBooleanExtra("fromOrders", false)) {
            cartRef =
                db.collection("users").document(intent.getStringExtra("uid").toString())
                    .collection("cart")
            proceed.visibility = View.GONE
            bill.visibility = View.VISIBLE
        }

        getTotalCost()

        val query: Query = cartRef.orderBy("id", Query.Direction.ASCENDING)
        val options = FirestoreRecyclerOptions.Builder<CartItem>()
            .setQuery(query, CartItem::class.java)
            .build()
        adapter = CartAdapter(this, this, options)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        proceed.setOnClickListener {

            ordersRef.get().addOnSuccessListener { document ->
                document.documents.forEach { order ->
                    if (order.get("userId").toString() == auth.currentUser?.uid) {
                        notAllowOnclick = true
                    }
                }
                if (!notAllowOnclick) {
                    if (cost.text.toString().replace("Total Cost : ", "").toFloat() >= 750f) {
                        order()
                    } else {
                        Toast.makeText(
                            this,
                            "Should order at least 750 Rs for Home Delivery",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (notAllowOnclick) {
                    Toast.makeText(
                        this,
                        "You can't order until your order is delivered",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        var billText = "Name , Qty , Price , Total \n"

        cartRef.get().addOnSuccessListener { snapshot ->
            snapshot?.forEach { model ->
                val itemRef =
                    db.collection("items").document(model.getString("category")!!)
                        .collection("items").document(model.getString("id")!!)

                itemRef.get().addOnSuccessListener {
                    if (it.exists()) {
                        billText += "${it.get("name").toString()} ," +
                                "${model.getString("count").toString()} ," +
                                "${it.getString("price").toString()} ," +
                                "${
                                    model.getString("count").toString()
                                        .toFloat() * it.getString("price").toString().toFloat()
                                }\n"

                    }
                }
            }
        }

        bill.setOnClickListener {
            billText += ",,Total Cost : ,${cost.text.toString().replace("Total Cost : ", "")}"
            writeToFile(billText)
        }
    }

    private fun writeToFile(billText: String) {
        val file = File(
            Environment.getExternalStorageDirectory().absolutePath,
            "SVS Market"
        )
        if (!file.exists()) {
            file.mkdir()
        }
        try {
            val billFile = File(
                file, intent.getStringExtra("name")?.lowercase(Locale.ROOT)
                    ?.trim() + "-" + System.currentTimeMillis().toString() + ".csv"
            )

            val writer = FileWriter(billFile)

            writer.append(billText)
            writer.flush()
            writer.close()

            Toast.makeText(
                this,
                "Saved your text${billFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error!${e.message.toString()}", Toast.LENGTH_LONG).show()
        }
    }

    private fun order() {
        AlertDialog.Builder(this)
            .setTitle("Order")
            .setMessage("Are you sure you want to order")
            .setPositiveButton("Order") { dialogInterface: DialogInterface, _: Int ->

                val inflater = this.layoutInflater
                val dialogView: View = inflater.inflate(R.layout.edit_text, null)
                val name = dialogView.findViewById<EditText>(R.id.name)
                val payNow = dialogView.findViewById<MaterialCheckBox>(R.id.payNow)

                AlertDialog.Builder(this)
                    .setTitle("Your Name")
                    .setView(dialogView)
                    .setPositiveButton("Confirm") { dialog: DialogInterface, _: Int ->
                        if (name.text.isNotEmpty()) {
                            document = ordersRef.document()
                            orderItem = OrderItem(
                                document.id,
                                name.text.toString(),
                                cost.text.toString(),
                                auth.currentUser?.uid!!,
                                auth.currentUser?.phoneNumber!!
                            )

                            if (payNow.isChecked) {
                                val uri: Uri = Uri.Builder()
                                    .scheme("upi")
                                    .authority("pay")
                                    .appendQueryParameter("pa", "nageswararao.vsm@unionbank")
                                    .appendQueryParameter("pn", "SVS Market")
                                    .appendQueryParameter(
                                        "tr",
                                        System.currentTimeMillis().toString()
                                    )
                                    .appendQueryParameter("mc", "BCR2DN6TV6M43AAP")
                                    .appendQueryParameter("tn", "Payment to SVS Market")
                                    .appendQueryParameter(
                                        "url",
                                        "https://srivigneswarasupermarket.business.site"
                                    )
                                    .appendQueryParameter(
                                        "am",
                                        cost.text.toString().replace("Total Cost : ", "")
                                            .split(".")[0]
                                    )
                                    .appendQueryParameter("cu", "INR")
                                    .build()
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.data = uri
                                intent.setPackage(tezPackageName)
                                startActivityForResult(intent, code)
                            } else {
                                placeOrder()
                            }

                        } else {
                            name.error = "Enter Your Name"
                        }
                        dialog.dismiss()
                    }.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
                        dialog.dismiss()
                    }.show()
                dialogInterface.dismiss()
            }.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }.show()
    }

    private fun placeOrder() {
        document.set(orderItem).addOnSuccessListener {

            Toast.makeText(
                this@CartActivity,
                "We have Successfully got you request we will call you to confirm order",
                Toast.LENGTH_LONG
            ).show()


        }.addOnFailureListener {
            Toast.makeText(this@CartActivity, "Error!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == code) {
            if (data?.getStringExtra("Status")!! == "SUCCESS") {
                placeOrder()
            }
            Log.d("result", data.getStringExtra("Status")!!)
        }
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.cart, menu)

        if (intent.getBooleanExtra("fromOrders", false)) {
            menu.findItem(R.id.deleteAll).isVisible = false
        }

        ordersRef.get().addOnSuccessListener { document ->
            document.documents.forEach { order ->
                if (order.get("userId").toString() == auth.currentUser?.uid) {
                    notAllowOnclick = true
                }

                if (notAllowOnclick) {
                    menu.findItem(R.id.deleteAll).isVisible = false
                }
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.deleteAll) {
            AlertDialog.Builder(this)
                .setTitle("Are You Sure?")
                .setMessage("Are You Sure you want to Empty the cart")
                .setPositiveButton("Yes") { dialogInterface: DialogInterface, _: Int ->
                    cartRef.addSnapshotListener { value, error ->
                        if (error == null) {
                            value?.documents?.forEach { documentSnapshot ->
                                documentSnapshot.reference.delete()
                            }
                        }
                    }
                    dialogInterface.dismiss()
                }.setNegativeButton("No") { dialogInterface: DialogInterface, _: Int ->
                    dialogInterface.dismiss()
                }.show()
            return false
        } else {
            return false
        }
    }

    private fun getTotalCost() {
        var totalCost = 0f

        cartRef.get().addOnSuccessListener { query ->
            query?.forEach { model ->
                val itemRef =
                    db.collection("items").document(model.getString("category")!!)
                        .collection("items").document(model.getString("id")!!)

                itemRef.get().addOnSuccessListener {
                    if (it.exists()) {
                        totalCost += model.getString("count").toString()
                            .toFloat() * it.getString("price").toString().toFloat()
                        cost.text = "Total Cost : $totalCost"
                    }
                }
            }
        }
    }

    override fun onClickListener(item: CartItem) {
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.counter, null)

        val plus = dialogView.findViewById<Button>(R.id.plus)
        val minus = dialogView.findViewById<Button>(R.id.minus)
        val counter = dialogView.findViewById<EditText>(R.id.count)

        counter.text = Editable.Factory.getInstance().newEditable(item.count)

        plus.setOnClickListener {
            if (counter.text.toString().toInt() < 10) {
                counter.text = Editable.Factory.getInstance()
                    .newEditable((counter.text.toString().toInt() + 1).toString())
            } else {
                Toast.makeText(this, "Max Quantity can not be grater than 10", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        minus.setOnClickListener {
            if (counter.text.toString().toInt() > 0) {
                counter.text = Editable.Factory.getInstance()
                    .newEditable((counter.text.toString().toInt() - 1).toString())
            }
        }

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Add") { dialogInterface: DialogInterface, _: Int ->
                val data = hashMapOf(
                    "id" to item.id,
                    "count" to counter.text.toString(),
                    "category" to item.category
                )
                if (counter.text.toString().toInt() <= 0) {
                    cartRef.document(item.id).delete()
                    getTotalCost()
                } else {
                    cartRef.document(item.id).set(data)
                        .addOnSuccessListener {
                            Toast.makeText(
                                this,
                                "Updated Successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            getTotalCost()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Error ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                dialogInterface.dismiss()
            }.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }.show()
    }

}