package com.vs.supermarket

import android.R.attr.*
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.vs.supermarket.adapters.CartAdapter
import com.vs.supermarket.models.CartItem
import com.vs.supermarket.models.OrderItem


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        cost = findViewById(R.id.cost)
        val proceed = findViewById<Button>(R.id.proceed)

        if (intent.getBooleanExtra("fromOrders", false)) {
            cartRef =
                db.collection("users").document(intent.getStringExtra("uid").toString())
                    .collection("cart")
            proceed.visibility = View.GONE
        }

        val query: Query = cartRef.orderBy("id", Query.Direction.ASCENDING)
        val options = FirestoreRecyclerOptions.Builder<CartItem>()
            .setQuery(query, CartItem::class.java)
            .build()
        adapter = CartAdapter(this, this, options)
        recyclerView = findViewById(R.id.cartView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        var notAllowOnclick = false

        proceed.setOnClickListener {

            ordersRef.get().addOnSuccessListener { document ->
                document.documents.forEach { order ->
                    if (order.get("userId").toString() == auth.currentUser?.uid) {
                        notAllowOnclick = true
                        Toast.makeText(
                            this,
                            order.get("userId").toString(),
                            Toast.LENGTH_LONG
                        ).show()
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

        getTotalCost()
    }

    private fun order() {
        AlertDialog.Builder(this)
            .setTitle("Order")
            .setMessage("Are you sure you want to order")
            .setPositiveButton("Order") { dialogInterface: DialogInterface, _: Int ->

                val inflater = this.layoutInflater
                val dialogView: View = inflater.inflate(R.layout.edit_text, null)
                val name = dialogView.findViewById<EditText>(R.id.name)

                AlertDialog.Builder(this)
                    .setTitle("Your Name")
                    .setView(dialogView)
                    .setPositiveButton("Confirm") { dialog: DialogInterface, _: Int ->
                        if (name.text.isNotEmpty()) {
                            val document = ordersRef.document()
                            val orderItem = OrderItem(
                                document.id,
                                name.text.toString(),
                                cost.text.toString(),
                                auth.currentUser?.uid!!,
                                auth.currentUser?.phoneNumber!!
                            )

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

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
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
            if(counter.text.toString().toInt() < 10){
                counter.text = Editable.Factory.getInstance()
                    .newEditable((counter.text.toString().toInt() + 1).toString())
            } else {
                Toast.makeText(this, "Max Quantity can not be grater than 10", Toast.LENGTH_SHORT).show()
            }
        }

        minus.setOnClickListener {
            if (counter.text.toString().toInt() > 0){
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