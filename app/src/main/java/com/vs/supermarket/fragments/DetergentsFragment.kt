package com.vs.supermarket.fragments

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.vs.supermarket.AdminEditItemsActivity
import com.vs.supermarket.R
import com.vs.supermarket.adapters.GroceryAdapter
import com.vs.supermarket.models.GroceryItem

class DetergentsFragment : Fragment(), GroceryAdapter.OnItemClickListener {

    private val category = "detergents"
    private val db = FirebaseFirestore.getInstance()
    private val detergentRef = db.collection("items").document(category).collection("items")
    private lateinit var adapter: GroceryAdapter
    private val auth = Firebase.auth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_grocery, container, false)
        val query: Query = detergentRef.orderBy("name", Query.Direction.DESCENDING)
        val options = FirestoreRecyclerOptions.Builder<GroceryItem>()
            .setQuery(query, GroceryItem::class.java)
            .build()
        adapter = GroceryAdapter(context!!, this, options)
        val recyclerView: RecyclerView = root.findViewById(R.id.foodView)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter
        return root
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    override fun onClickListener(item: GroceryItem) {
        val cartRef = db.collection("users").document(auth.currentUser?.uid!!).collection("cart")
        val inflater = activity!!.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.counter, null)

        val plus = dialogView.findViewById<Button>(R.id.plus)
        val minus = dialogView.findViewById<Button>(R.id.minus)
        val counter = dialogView.findViewById<EditText>(R.id.count)

        cartRef.document(item.id).addSnapshotListener { value, error ->
            if (error == null) {
                if (value?.get("count") != null) {
                    counter.text =
                        Editable.Factory.getInstance().newEditable(value.get("count").toString())
                } else {
                    counter.text = Editable.Factory.getInstance().newEditable("1")
                }
            } else {
                counter.text = Editable.Factory.getInstance().newEditable("1")
            }
        }

        plus.setOnClickListener {
            counter.text = Editable.Factory.getInstance()
                .newEditable((counter.text.toString().toInt() + 1).toString())
        }

        minus.setOnClickListener {
            counter.text = Editable.Factory.getInstance()
                .newEditable((counter.text.toString().toInt() - 1).toString())
        }

        AlertDialog.Builder(context!!)
            .setView(dialogView)
            .setPositiveButton("Add") { dialogInterface: DialogInterface, _: Int ->
                val data = hashMapOf(
                    "id" to item.id,
                    "count" to counter.text.toString(),
                    "category" to "detergents"
                )
                cartRef.document(item.id).set(data)
                    .addOnSuccessListener {
                        Toast.makeText(
                            context,
                            "Added Successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            context,
                            "Error ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                dialogInterface.dismiss()
            }.setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }.show()
    }

    override fun onLongClickListener(item: GroceryItem) {
        AlertDialog.Builder(context!!)
            .setTitle("Edit")
            .setMessage("Edit/Delete items")
            .setPositiveButton("Edit") { dialogInterface: DialogInterface, _: Int ->
                val intent = Intent(context, AdminEditItemsActivity::class.java)
                intent.putExtra("id", item.id)
                intent.putExtra("name", item.name)
                intent.putExtra("price", item.price)
                intent.putExtra("realPrice", item.realPrice)
                intent.putExtra("image", item.image)
                intent.putExtra("category", category)
                startActivity(intent)
                dialogInterface.dismiss()
            }.setNegativeButton("Delete") { dialogInterface: DialogInterface, _: Int ->
                detergentRef.document(item.id).delete()
                Snackbar.make(requireView(), "Task Deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        detergentRef.document(item.id).set(item)
                    }.show()
                dialogInterface.dismiss()
            }.show()
    }
}