package com.vs.supermarket.fragments

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.paging.PagedList
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.Query
import com.vs.supermarket.AdminEditItemsActivity
import com.vs.supermarket.App
import com.vs.supermarket.App.Companion.beverageRef
import com.vs.supermarket.R
import com.vs.supermarket.adapters.GroceryAdapter
import com.vs.supermarket.models.GroceryItem
import java.util.*

class BeverageFragment : Fragment(), GroceryAdapter.OnItemClickListener {

    private val category = "beverage"
    private lateinit var adapter: GroceryAdapter
    lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_grocery, container, false)

        recyclerView = root.findViewById(R.id.foodView)
        val query: Query = beverageRef.orderBy("name", Query.Direction.ASCENDING)

        val options = FirestoreRecyclerOptions.Builder<GroceryItem>()
            .setQuery(query, GroceryItem::class.java)
            .build()
        adapter = GroceryAdapter(context!!, this, options)
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        val search = root.findViewById<EditText>(R.id.search)
        search.doOnTextChanged { text, _, _, _ ->
            recyclerView(text.toString().trim())
        }

        return root
    }

    private fun recyclerView(text: String) {

        val query: Query = beverageRef.orderBy("name", Query.Direction.ASCENDING)
            .startAt(text.toUpperCase(Locale.ROOT)).endAt("${text.toLowerCase(Locale.ROOT)}\uf8ff")

        val options = FirestoreRecyclerOptions.Builder<GroceryItem>()
            .setQuery(query, GroceryItem::class.java)
            .build()

        adapter.updateOptions(options)
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
        App.onClickItems(item, context!!, activity!!, category)
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
                intent.putExtra("isOutOfStock", item.outOfStock)
                startActivity(intent)
                dialogInterface.dismiss()
            }.setNegativeButton("Delete") { dialogInterface: DialogInterface, _: Int ->
                beverageRef.document(item.id).delete()
                Snackbar.make(requireView(), "Task Deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        beverageRef.document(item.id).set(item)
                    }.show()
                dialogInterface.dismiss()
            }.show()
    }
}