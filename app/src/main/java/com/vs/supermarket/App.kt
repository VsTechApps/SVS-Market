package com.vs.supermarket

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.vs.supermarket.models.GroceryItem

class App : AppCompatActivity() {

    companion object {
        const val PhoneNumber = "8978998769"

        val auth = Firebase.auth
        val db = FirebaseFirestore.getInstance()

        val foodRef = db.collection("items").document("food").collection("items")
        val groceryRef = db.collection("items").document("grocery").collection("items")
        val beverageRef = db.collection("items").document("beverage").collection("items")
        val detergentsRef = db.collection("items").document("detergents").collection("items")
        val kitchenRef = db.collection("items").document("kitchen").collection("items")
        val otherRef = db.collection("items").document("other").collection("items")

        fun onClickItems(
            item: GroceryItem,
            context: Context,
            activity: Activity,
            category: String
        ) {
            val cartRef =
                db.collection("users").document(auth.currentUser?.uid!!).collection("cart")
            val inflater = activity.layoutInflater
            val dialogView: View = inflater.inflate(R.layout.counter, null)

            val plus = dialogView.findViewById<Button>(R.id.plus)
            val minus = dialogView.findViewById<Button>(R.id.minus)
            val counter = dialogView.findViewById<EditText>(R.id.count)

            cartRef.document(item.id).addSnapshotListener { value, error ->
                if (error == null) {
                    if (value?.get("count") != null) {
                        counter.text =
                            Editable.Factory.getInstance()
                                .newEditable(value.get("count").toString())
                    } else {
                        counter.text = Editable.Factory.getInstance().newEditable("1")
                    }
                } else {
                    counter.text = Editable.Factory.getInstance().newEditable("1")
                }
            }

            plus.setOnClickListener {
                if (counter.text.toString().toInt() < 10) {
                    counter.text = Editable.Factory.getInstance()
                        .newEditable((counter.text.toString().toInt() + 1).toString())
                } else {
                    Toast.makeText(
                        context,
                        "Max Quantity can not be grater than 10",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            minus.setOnClickListener {
                if (counter.text.toString().toInt() > 1) {
                    counter.text = Editable.Factory.getInstance()
                        .newEditable((counter.text.toString().toInt() - 1).toString())
                }
            }

            AlertDialog.Builder(context)
                .setTitle("Quantity")
                .setView(dialogView)
                .setPositiveButton("Add") { dialogInterface: DialogInterface, _: Int ->
                    val data = hashMapOf(
                        "id" to item.id,
                        "count" to counter.text.toString(),
                        "category" to category
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
    }
}