package com.davek.tictactoe.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.davek.tictactoe.R
import com.davek.tictactoe.databinding.ListItemCellBinding
import com.davek.tictactoe.models.Cell
import com.davek.tictactoe.viewmodels.BoardViewModel
import com.davek.tictactoe.viewmodels.CellState

class CellAdapter(private val viewModel: BoardViewModel) :
    RecyclerView.Adapter<CellAdapter.ViewHolder>() {

    var data = listOf<Cell>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]

        holder.bind(viewModel, item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(
        val binding: ListItemCellBinding,
        context: Context
    ) : RecyclerView.ViewHolder(binding.root) {

        private val mContext = context

        fun bind(viewModel: BoardViewModel, item: Cell) {
            binding.boardVM = viewModel
            binding.cell = item
            binding.viewCell.background = when (item.currentState) {
                CellState.NONE -> null
                CellState.SELECTED_O ->
                    ContextCompat.getDrawable(mContext, R.drawable.ic_outline_o_24)
                CellState.SELECTED_X ->
                    ContextCompat.getDrawable(mContext, R.drawable.ic_outline_x_24)
            }
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemCellBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding, parent.context)
            }
        }
    }
}