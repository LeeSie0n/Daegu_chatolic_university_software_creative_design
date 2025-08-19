package com.example.myapplication
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TutorialAdapter(
    private val context: Context,
    private val pages: List<TutorialPage>,
    private val onStartClicked: () -> Unit
) : RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_tutorial_page, parent, false)
        return TutorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        val page = pages[position]
        holder.image.setImageResource(page.imageResId)
        holder.text.text = page.description
        holder.startButton.visibility = if (position == pages.size - 1) View.VISIBLE else View.GONE

        holder.startButton.setOnClickListener {
            onStartClicked()
        }
    }

    override fun getItemCount(): Int = pages.size

    class TutorialViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.tutorialImage)
        val text: TextView = view.findViewById(R.id.tutorialText)
        val startButton: Button = view.findViewById(R.id.btnStart)
    }
}
