package com.davek.tictactoe.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.paging.PagedList
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.davek.tictactoe.R
import com.davek.tictactoe.databinding.FragmentSavedGamesBinding
import com.davek.tictactoe.databinding.ListItemSavedGameBinding
import com.davek.tictactoe.models.SavedGame
import com.davek.tictactoe.models.SavedGameWithId
import com.davek.tictactoe.utils.FireStoreUtils.currentUserDocRef
import com.davek.tictactoe.viewmodels.SavedGamesViewModel
import com.firebase.ui.firestore.paging.FirestorePagingAdapter
import com.firebase.ui.firestore.paging.FirestorePagingOptions
import com.firebase.ui.firestore.paging.LoadingState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.Query

class SavedGamesFragment : Fragment() {

    private val mTag = this::class.java.simpleName

    private lateinit var binding: FragmentSavedGamesBinding
    private lateinit var viewModel: SavedGamesViewModel
    private lateinit var pagingAdapter: FirestorePagingAdapter<SavedGameWithId, SavedGameViewHolder>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_saved_games, container, false
        )
        val vm: SavedGamesViewModel by viewModels()
        viewModel = vm
        observeViewModel()
        setupRecyclerView()
        binding.savedGamesVM = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        return binding.root
    }

    private fun observeViewModel() {
        viewModel.navigateToGameBoard.observe(viewLifecycleOwner, {
            findNavController().navigate(
                MainOnlineFragmentDirections.actionMainOnlineFragmentToBoardFragment(null, it)
            )
        })

        viewModel.showDeleteSavedGameDialog.observe(viewLifecycleOwner, {
            showDeleteSavedGameDialog(it)
        })
    }

    private fun setupRecyclerView() {
        val baseQuery: Query = currentUserDocRef
            .collection("savedGames")
            .orderBy("date", Query.Direction.DESCENDING)
        val config = PagedList.Config.Builder()
            .setEnablePlaceholders(false)
            .setPrefetchDistance(10)
            .setPageSize(20)
            .build()
        val options: FirestorePagingOptions<SavedGameWithId> =
            FirestorePagingOptions.Builder<SavedGameWithId>()
                .setLifecycleOwner(this)
                .setQuery(
                    baseQuery,
                    config
                ) { snapshot ->
                    val savedGame = snapshot.toObject(SavedGame::class.java) ?: SavedGame()
                    SavedGameWithId(snapshot.id, savedGame)
                }
                .build()
        pagingAdapter =
            object : FirestorePagingAdapter<SavedGameWithId, SavedGameViewHolder>(options) {
                override fun onCreateViewHolder(
                    parent: ViewGroup,
                    viewType: Int
                ): SavedGameViewHolder {
                    return SavedGameViewHolder.from(parent)
                }

                override fun onBindViewHolder(
                    holder: SavedGameViewHolder,
                    position: Int,
                    model: SavedGameWithId
                ) {
                    holder.bind(viewModel, model)
                }

                override fun onLoadingStateChanged(state: LoadingState) {
                    when (state) {
                        LoadingState.LOADING_INITIAL, LoadingState.LOADING_MORE -> {
                            binding.swipeRefreshLayout.isRefreshing = true
                        }
                        LoadingState.LOADED -> binding.swipeRefreshLayout.isRefreshing = false
                        LoadingState.FINISHED -> binding.swipeRefreshLayout.isRefreshing = false
                        LoadingState.ERROR -> {
                            Log.e(mTag, "Error onLoadingStateChanged")
                            retry()
                        }
                    }
                }

                override fun onError(e: Exception) {
                    binding.swipeRefreshLayout.isRefreshing = false
                    Log.e(mTag, "Error: FirestorePagingAdapter", e)
                }
            }
        binding.savedGameList.adapter = pagingAdapter
        binding.savedGameList.addItemDecoration(
            DividerItemDecoration(
                context,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.swipeRefreshLayout.setOnRefreshListener { pagingAdapter.refresh() }
    }

    class SavedGameViewHolder private constructor(
        private val binding: ListItemSavedGameBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(viewModel: SavedGamesViewModel, savedGame: SavedGameWithId) {
            binding.savedGameVM = viewModel
            binding.savedGame = savedGame
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): SavedGameViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemSavedGameBinding.inflate(layoutInflater, parent, false)

                return SavedGameViewHolder(binding)
            }
        }
    }

    private fun showDeleteSavedGameDialog(gameDocId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dialog_delete_saved_game_title))
            .setMessage(getString(R.string.dialog_delete_saved_game_body))
            .setPositiveButton(getString(R.string.dialog_delete_saved_game_positive_button)) { dialog, _ ->
                viewModel.deleteSavedGame(gameDocId)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.dialog_delete_saved_game_negative_button)) { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                pagingAdapter.refresh()
            }
            .create()
            .show()
    }
}